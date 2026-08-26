package com.sentinq.shopping;

import com.sentinq.ai.*;
import com.sentinq.goal.*;
import com.sentinq.identity.*;
import com.sentinq.identity.PrincipalService;
import com.sentinq.mandate.MandateBuilder;
import com.sentinq.mandate.MandateEnvelope;
import com.sentinq.preference.*;
import com.sentinq.resolution.*;
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
    private final GoalFitReasoningService goalFitReasoningService;
    private final RecommendationReasoningService
            recommendationReasoningService;
    private ExecutionFactsResolver executionFactsResolver;

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
            TrustMapOrchestrationService trustMapOrchestrationService,
            GoalFitReasoningService goalFitReasoningService,
            RecommendationReasoningService
                    recommendationReasoningService,
            ExecutionFactsResolver executionFactsResolver

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
        this.goalFitReasoningService = goalFitReasoningService;
        this.recommendationReasoningService = recommendationReasoningService;
        this.executionFactsResolver = executionFactsResolver;
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

        candidates.forEach(candidate -> {
            System.out.println();
            System.out.println("FACTORY OUTPUT");
            System.out.println(
                    "Offer ID: [" + candidate.getOfferId() + "]"
            );
            System.out.println(
                    "Merchant ID: [" + candidate.getMerchantId() + "]"
            );
            System.out.println(
                    "Merchant: [" + candidate.getMerchantName() + "]"
            );
            System.out.println(
                    "Product: [" + candidate.getProductName() + "]"
            );
        });

        /*
         * Hard screening.
         *
         * Cheap, deterministic exclusions happen before
         * expensive goal-fit reasoning and Trust Maps.
         *
         * Surviving this stage does NOT mean that a candidate
         * satisfies the mandate. It only means that we cannot
         * cheaply prove that the candidate should be excluded.
         *
         * Late-binding execution facts such as shipping, tax,
         * final delivery commitment, and current inventory
         * are resolved later for the selected candidate.
         */
        List<CandidateOffer> screenedCandidates =
                candidates.stream()
                        .filter(candidate ->
                                !shouldExcludeDuringHardScreening(
                                        candidate,
                                        mandate
                                )
                        )
                        .toList();

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.CANDIDATES_SCREENED,
                "ShoppingOrchestrationService",
                "Cheap deterministic exclusions applied to discovered candidates.",
                screenedCandidates
        );
        /*
         * MVP reasoning budget.
         *
         * For now we keep at most 3 preliminarily viable contenders.
         * Goal-fit ranking will replace first-3 selection later.
         */

        List<GoalFitCandidate> goalFitCandidates;
        goalFitCandidates = goalFitReasoningService.rank(
                agent.getProvider(),
                goal,
                screenedCandidates
        );

        List<GoalFitCandidate> shortlistedCandidates =
                goalFitCandidates.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        GoalFitCandidate::rank
                                )
                        )
                        .limit(3)
                        .toList();

        shortlistedCandidates.forEach(
                goalFitCandidate -> {

                    CandidateOffer offer =
                            goalFitCandidate.offer();

                    System.out.println();
                    System.out.println("TRUST MAP INPUT");
                    System.out.println(
                            "Rank: "
                                    + goalFitCandidate.rank()
                    );
                    System.out.println(
                            "Offer ID: "
                                    + offer.getOfferId()
                    );
                    System.out.println(
                            "Merchant ID: "
                                    + offer.getMerchantId()
                    );
                    System.out.println(
                            "Merchant: "
                                    + offer.getMerchantName()
                    );
                    System.out.println(
                            "Product: "
                                    + offer.getProductName()
                    );
                }
        );
        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.GOAL_FIT_EVALUATED,
                "GoalFitReasoningService",
                "Screened candidates ranked against the consumer's goal.",
                goalFitCandidates
        );
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
                                .map(goalFitCandidate ->
                                        CompletableFuture.supplyAsync(
                                                () ->
                                                        assessCandidateTrust(
                                                                agent.getProvider(),
                                                                goalFitCandidate,
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
         * Recommendation reasoning happens only after
         * Trust Maps have completed for all shortlisted
         * contenders.
         */
        RecommendationDecision recommendation = null;

        if (!trustAssessedCandidates.isEmpty()) {
            recommendation =
                    recommendationReasoningService.recommend(
                            agent.getProvider(),
                            goal,
                            trustAssessedCandidates
                    );
        }

        if (recommendation != null) {
            auditService.recordEvent(
                    trace.getTraceId(),
                    AuditEventType.RECOMMENDATION_CREATED,
                    "RecommendationReasoningService",
                    "Recommendation selected from goal-fit and trust-assessed contenders.",
                    recommendation
            );
        }

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
                                .map(goalFitCandidate ->
                                        CompletableFuture.supplyAsync(
                                                () ->
                                                        assessCandidateTrust(
                                                                agent.getProvider(),
                                                                goalFitCandidate,
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

        ResolvedCandidate resolvedCandidate = null;

        if (recommendation != null &&
                recommendation.selectedCandidate() != null) {

            CandidateOffer selectedOffer =
                    recommendation.selectedCandidate();

            ResolvedExecutionFacts executionFacts =
                    executionFactsResolver.resolve(
                            selectedOffer
                    );

            ResolutionResult resolution =
                    resolutionService.resolve(
                            selectedOffer,
                            mandate,
                            executionFacts
                    );

            resolvedCandidate =
                    new ResolvedCandidate(
                            selectedOffer,
                            resolution
                    );
        }
        ShoppingOrchestrationResult result =
                new ShoppingOrchestrationResult(
                        mandate,
                        trustAssessedCandidates,
                        recommendation,
                        resolvedCandidate
                );
        String finalDecisionMessage;

        if (resolvedCandidate == null) {

            finalDecisionMessage =
                    "No candidate reached late-binding resolution.";

        } else if (resolvedCandidate.resolution().isExecutable()) {

            finalDecisionMessage =
                    "Recommended candidate passed late-binding resolution.";

        } else {

            finalDecisionMessage =
                    "Recommended candidate failed late-binding resolution.";
        }

        auditService.recordEvent(
                trace.getTraceId(),
                AuditEventType.FINAL_DECISION,
                "ShoppingOrchestrationService",
                finalDecisionMessage,
                result
        );

        auditService.completeTrace(
                trace.getTraceId()
        );

        return result;
    }

    private boolean shouldExcludeDuringHardScreening(
            CandidateOffer candidate,
            MandateEnvelope mandate
    ) {

        /*
         * Hard screening is intentionally asymmetric.
         *
         * It can prove that a candidate should be excluded,
         * but it does not prove that a surviving candidate
         * is executable.
         */

        if (mandate.getProhibitedMerchants() != null &&
                mandate.getProhibitedMerchants()
                        .contains(candidate.getMerchantId())) {
            return true;
        }

        Integer maximumTotalCents =
                mandate.getMaximumTotalCents();

        /*
         * If product price alone already exceeds the
         * consumer's maximum total budget, later resolution
         * of shipping and tax cannot make it compliant.
         */
        if (maximumTotalCents != null &&
                candidate.getProductPriceCents() >
                        maximumTotalCents) {
            return true;
        }

        return false;
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
            GoalFitCandidate goalFitCandidate,
            Goal goal,
            ConsumerPreferences preferences
    ) {

        CandidateOffer candidate =
                goalFitCandidate.offer();

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
                goalFitCandidate,
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
            MandateEnvelope mandate,
            ResolvedExecutionFacts executionFacts

    ) {
        ResolutionResult resolution =
                resolutionService.resolve(
                        candidate,
                        mandate,
                        executionFacts
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