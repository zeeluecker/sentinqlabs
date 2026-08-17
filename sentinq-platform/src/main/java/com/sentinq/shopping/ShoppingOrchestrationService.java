package com.sentinq.shopping;

import com.sentinq.ai.*;
import com.sentinq.goal.*;
import com.sentinq.identity.*;
import com.sentinq.identity.PrincipalService;
import com.sentinq.mandate.MandateBuilder;
import com.sentinq.mandate.MandateEnvelope;
import com.sentinq.preference.*;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.LateBindingResolutionService;
import com.sentinq.resolution.ResolutionResult;
import com.sentinq.trust.*;
import org.springframework.stereotype.Service;
import com.sentinq.audit.AuditEventType;
import com.sentinq.audit.AuditService;
import com.sentinq.audit.ExecutionTrace;

import java.math.BigDecimal;
import java.util.Map;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ShoppingOrchestrationService {

    private final MandateBuilder mandateBuilder;
    private final LateBindingResolutionService resolutionService;
    private final PrincipalService principalService;
    private final AgentIdentityService agentIdentityService;
    private final AgentDelegationService agentDelegationService;
    private final ConsumerPreferencesService consumerPreferencesService;
    private final GoalInterpretationService goalInterpretationService;
    private final GoalFactory goalFactory;
    private final ProductSearchService productSearchService;
    private final CandidateOfferFactory candidateOfferFactory;
    private final AuditService auditService;
    private final TrustMapOrchestrationService trustMapOrchestrationService;

    public ShoppingOrchestrationService(
            MandateBuilder mandateBuilder,
            LateBindingResolutionService resolutionService,
            MockMerchantSearchService merchantSearchService,
            PrincipalService principalService,
            AgentIdentityService agentIdentityService,
            AgentDelegationService agentDelegationService,
            ConsumerPreferencesService consumerPreferencesService,
            GoalInterpretationService goalInterpretationService,
            GoalFactory goalFactory,
            ProductSearchService productSearchService,
            CandidateOfferFactory candidateOfferFactory,
            AuditService auditService,
            TrustMapOrchestrationService trustMapOrchestrationService

    ) {
        this.mandateBuilder = mandateBuilder;
        this.resolutionService = resolutionService;
        this.principalService = principalService;
        this.agentIdentityService = agentIdentityService;
        this.agentDelegationService = agentDelegationService;
        this.consumerPreferencesService = consumerPreferencesService;
        this.goalInterpretationService = goalInterpretationService;
        this.goalFactory = goalFactory;
        this.productSearchService = productSearchService;
        this.candidateOfferFactory = candidateOfferFactory;
        this.auditService = auditService;
        this.trustMapOrchestrationService = trustMapOrchestrationService;
    }

    public ShoppingOrchestrationResult orchestrate(
            ShoppingGoalRequest request
    ) {
        ExecutionTrace trace =
                auditService.startTrace(
                        request.principalId(),
                        request.agentId()
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.REQUEST_RECEIVED,
                "ShoppingOrchestrationService",
                "Shopping orchestration request received.",
                request
        );

        Principal principal =
                principalService.findById(
                        request.principalId()
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.PRINCIPAL_LOADED,
                "PrincipalService",
                "Principal identity loaded.",
                principal
        );

        AgentIdentity agent =
                agentIdentityService.findById(
                        request.agentId()
                );

        auditService.setProviderDetails(
                trace.getTraceId(),
                agent.getProvider(),
                agent.getModel()
        );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.AGENT_SELECTED,
                "AgentIdentityService",
                "Agent and reasoning provider selected.",
                agent
        );


        AgentDelegation delegation =
                agentDelegationService.findActiveDelegation(
                        principal.getPrincipalId(),
                        agent.getAgentId()
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.DELEGATION_VALIDATED,
                "AgentDelegationService",
                "Active delegation validated.",
                delegation
        );


        ConsumerPreferences preferences =
                consumerPreferencesService.findByPrincipalId(
                        principal.getPrincipalId()
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.PREFERENCES_LOADED,
                "ConsumerPreferencesService",
                "Scoped consumer and merchant preferences loaded.",
                preferences
        );

        InterpretedShoppingGoal interpretation =
                goalInterpretationService.interpret(
                        agent.getProvider(),
                        request.goalText()
                );
        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.GOAL_INTERPRETED,
                "GoalInterpretationService",
                "Natural-language goal interpreted.",
                interpretation
        );

        Goal goal =
                goalFactory.create(
                        principal.getPrincipalId(),
                        request.goalText(),
                        interpretation
                );


        MandateEnvelope mandate =
                mandateBuilder.build(
                        goal,
                        preferences
                );

        /*
         * The mandate must carry the governance context
         * under which this task is being executed.
         */
        mandate.setPrincipalId(
                principal.getPrincipalId()
        );

        mandate.setAgentId(
                agent.getAgentId()
        );

        mandate.setDelegationId(
                delegation.getDelegationId()
        );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.MANDATE_CREATED,
                "MandateBuilder",
                "Governed Mandate Envelope created.",
                mandate
        );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.SEARCH_REQUEST_SENT,
                "ProductSearchService",
                "Governed search context sent to the selected provider.",
                Map.of(
                        "provider", agent.getProvider(),
                        "model", agent.getModel(),
                        "goal", goal,
                        "preferences", preferences,
                        "mandate", mandate
                )
        );
        ProductSearchResult searchResult =
                productSearchService.search(
                        agent.getProvider(),
                        goal,
                        preferences
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.CANDIDATES_RECEIVED,
                "ProductSearchService",
                "Merchant candidates returned by provider.",
                searchResult
        );

        List<CandidateOffer> candidates =
                searchResult.offers.stream()
                        .map(candidateOfferFactory::create)
                        .toList();

        /*
         * Resolve first.
         * Cheap transaction checks happen before expensive Trust Maps.
         */
        List<ResolvedCandidate> resolvedCandidates =
                candidates.stream()
                        .map(candidate ->
                                resolveCandidate(
                                        candidate,
                                        mandate
                                )
                        )
                        .toList();

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.CANDIDATES_RESOLVED,
                "LateBindingResolutionService",
                "Candidate offers evaluated against the mandate.",
                resolvedCandidates
        );

        /*
         * Keep only candidates that survived the current
         * resolution / mandate checks.
         */
        List<ResolvedCandidate> viableCandidates =
                resolvedCandidates.stream()
                        .filter(candidate ->
                                isPreliminarilyViable(
                                        candidate,
                                        mandate
                                )
                        )
                        .toList();
        /*
         * MVP reasoning budget.
         *
         * For now we keep at most 3 preliminarily viable contenders.
         * Goal-fit ranking will replace first-3 selection later.
         */
        List<ResolvedCandidate> shortlistedCandidates =
                viableCandidates.stream()
                        .limit(3)
                        .toList();

        /*
         * Expensive Trust Maps runs only on shortlisted contenders.
         *
         * Merchant assessments run concurrently, but each merchant's
         * internal Trust Map now uses the bounded:
         *
         * OBSERVE
         * → SYNTHESIZE
         * → ONE TARGETED RESEARCH ROUND
         * → FINAL SYNTHESIS
         *
         * pipeline.
         */
        List<TrustAssessedCandidate> trustAssessedCandidates;

        if (shortlistedCandidates.isEmpty()) {

            trustAssessedCandidates =
                    List.of();

        } else {

            ExecutorService trustMapExecutor =
                    Executors.newFixedThreadPool(
                            Math.min(
                                    3,
                                    shortlistedCandidates.size()
                            )
                    );

            try {
                List<CompletableFuture<TrustAssessedCandidate>> trustMapFutures =
                        shortlistedCandidates.stream()
                                .map(resolved ->
                                        CompletableFuture.supplyAsync(
                                                () ->
                                                        assessCandidateTrust(
                                                                agent.getProvider(),
                                                                resolved.offer(),
                                                                goal,
                                                                preferences
                                                        ),
                                                trustMapExecutor
                                        )
                                )
                                .toList();

                trustAssessedCandidates =
                        trustMapFutures.stream()
                                .map(CompletableFuture::join)
                                .toList();

            } finally {
                trustMapExecutor.shutdown();
            }
        }

        /*
         * Temporary MVP selection.
         *
         * For now choose the cheapest preliminarily viable
         * shortlisted candidate.
         *
         * This will later become Recommendation Reasoning:
         *
         * goal fit
         * + Trust Map
         * + transaction viability
         * → selected candidate
         */
        Optional<ResolvedCandidate> selectedCandidate =
                shortlistedCandidates.stream()
                        .min(
                                Comparator.comparing(
                                        this::resolvedTotal
                                )
                        );

        ShoppingOrchestrationResult result =
                new ShoppingOrchestrationResult(
                        mandate,
                        trustAssessedCandidates,
                        resolvedCandidates,
                        selectedCandidate.orElse(null)
                );

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.FINAL_DECISION,
                "ShoppingOrchestrationService",
                selectedCandidate.isPresent()
                        ? "Shortlisted candidate selected."
                        : "No preliminarily viable candidate was found.",
                result
        );

        auditService.completeTrace(
                trace.getTraceId()
        );

        return result;
    }

    private boolean isPreliminarilyViable(
            ResolvedCandidate candidate,
            MandateEnvelope mandate
    ) {
        ResolutionResult resolution = candidate.resolution();

        if (!resolution.isInventoryAvailable()) {
            return false;
        }

        if (mandate.getMaximumTotalCents() != null
                && resolution.getResolvedTotalCents() != null
                && resolution.getResolvedTotalCents() > mandate.getMaximumTotalCents()) {
            return false;
        }

        return true;
    }

    private TrustAssessedCandidate assessCandidateTrust(
            String provider,
            CandidateOffer candidate,
            Goal goal,
            ConsumerPreferences preferences
    ) {
        TrustContext trustContext =
                buildTrustContext(
                        goal,
                        candidate,
                        preferences
                );

        MerchantTrustAssessment trustAssessment =
                trustMapOrchestrationService.assessMerchant(
                        provider,
                        candidate,
                        trustContext
                );

        return new TrustAssessedCandidate(
                candidate,
                trustAssessment
        );
    }

    private TrustContext buildTrustContext(
            Goal goal,
            CandidateOffer candidate,
            ConsumerPreferences preferences
    ) {
        return new TrustContext(
                goal.getOriginalRequest(),
                "UNKNOWN",
                goal.getProductName(),
                Map.of(),
                new BigDecimal(
                        candidate.getProductPriceCents()
                ).movePointLeft(2),
                DeliveryUrgency.NORMAL,
                MerchantFamiliarity.UNKNOWN,
                List.of(
                        TrustDimension.PRODUCT_QUALITY,
                        TrustDimension.PRODUCT_DURABILITY,
                        TrustDimension.FULFILLMENT_RELIABILITY,
                        TrustDimension.CUSTOMER_REMEDIATION,
                        TrustDimension.REFUND_RELIABILITY
                )
        );
    }

    private ResolvedCandidate resolveCandidate(
            CandidateOffer candidate,
            MandateEnvelope mandate
    ) {
        ResolutionResult resolution =
                resolutionService.resolve(
                        candidate,
                        mandate
                );

        return new ResolvedCandidate(
                candidate,
                resolution
        );
    }


    private boolean isExecutable(
            ResolvedCandidate candidate
    ) {
        return candidate
                .resolution()
                .isExecutable();
    }


    private Integer resolvedTotal(
            ResolvedCandidate candidate
    ) {
        return candidate
                .resolution()
                .getResolvedTotalCents();
    }
}