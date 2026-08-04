package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.identity.*;
import com.sentinq.identity.PrincipalService;
import com.sentinq.mandate.MandateBuilder;
import com.sentinq.mandate.MandateEnvelope;
import com.sentinq.preference.*;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.LateBindingResolutionService;
import com.sentinq.resolution.ResolutionResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ShoppingOrchestrationService {

    private final MandateBuilder mandateBuilder;
    private final LateBindingResolutionService resolutionService;
    private final MockMerchantSearchService merchantSearchService;
    private final PrincipalService principalService;
    private final AgentIdentityService agentIdentityService;
    private final AgentDelegationService agentDelegationService;
    private final ConsumerPreferencesService consumerPreferencesService;

    public ShoppingOrchestrationService(
            MandateBuilder mandateBuilder,
            LateBindingResolutionService resolutionService,
            MockMerchantSearchService merchantSearchService,
            PrincipalService principalService,
            AgentIdentityService agentIdentityService,
            AgentDelegationService agentDelegationService,
            ConsumerPreferencesService consumerPreferencesService
    ) {
        this.mandateBuilder = mandateBuilder;
        this.resolutionService = resolutionService;
        this.merchantSearchService = merchantSearchService;
        this.principalService = principalService;
        this.agentIdentityService = agentIdentityService;
        this.agentDelegationService = agentDelegationService;
        this.consumerPreferencesService = consumerPreferencesService;
    }

    public ShoppingOrchestrationResult orchestrate(
            ShoppingGoalRequest request
    ) {
        Principal principal =
                principalService.findById(
                        request.principalId()
                );

        AgentIdentity agent =
                agentIdentityService.findById(
                        request.agentId()
                );

        AgentDelegation delegation =
                agentDelegationService.findActiveDelegation(
                        principal.getPrincipalId(),
                        agent.getAgentId()
                );

        ConsumerPreferences preferences =
                consumerPreferencesService.findByPrincipalId(
                        principal.getPrincipalId()
                );

        Goal goal = new Goal();

        goal.setPrincipalId(
                principal.getPrincipalId()
        );

        goal.setOriginalRequest(
                request.goalText()
        );

        // Temporary hardcoded interpretation.
        // GPT will replace this block next.
        goal.setProductName(
                "Container-suitable dahlia"
        );

        goal.setMaximumTotalCents(
                5000
        );

        goal.setDeliveryDeadline(
                LocalDate.of(2026, 8, 31)
        );

        goal.setSubstitutionsAllowed(
                true
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

        List<CandidateOffer> candidates =
                merchantSearchService.search(
                        goal
                );

        List<ResolvedCandidate> resolvedCandidates =
                candidates.stream()
                        .map(candidate ->
                                resolveCandidate(
                                        candidate,
                                        mandate
                                )
                        )
                        .toList();

        Optional<ResolvedCandidate> selectedCandidate =
                resolvedCandidates.stream()
                        .filter(this::isExecutable)
                        .min(
                                Comparator.comparing(
                                        this::resolvedTotal
                                )
                        );

        return new ShoppingOrchestrationResult(
                mandate,
                resolvedCandidates,
                selectedCandidate.orElse(null)
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