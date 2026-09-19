package com.sentinq.shopping;

import com.sentinq.evaluation.*;
import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.GoalFitCandidate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RecommendationReasoningService {

    private final RecommendationReasoningProviderRegistry
            providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public RecommendationReasoningService(
            RecommendationReasoningProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }

    public RecommendationDecision recommend(
            UUID executionId,
            Goal goal,
            List<TrustAssessedCandidate> candidates,
            ModelAssignment assignment
    ) {
        if (goal == null) {
            throw new IllegalArgumentException(
                    "Goal is required for recommendation reasoning."
            );
        }

        if (candidates == null ||
                candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one trust-assessed candidate is required."
            );
        }

        RecommendationReasoningProvider reasoningProvider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.RECOMMENDATION_REASONING
                );

        RecommendationReasoningDecision decision =
                llmInvocationObserver.execute(
                        capabilityIdentity,
                        () ->
                                reasoningProvider.recommend(
                                        goal,
                                        candidates,
                                        assignment.model()
                                )
                );

        CandidateOffer selectedCandidate =
                candidates.stream()
                        .map(
                                TrustAssessedCandidate::goalFitCandidate
                        )
                        .map(
                                GoalFitCandidate::offer
                        )
                        .filter(candidate ->
                                candidate.getOfferId().equals(
                                        decision.selectedOfferId()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Recommendation provider selected an unknown offer ID: "
                                                + decision.selectedOfferId()
                                )
                        );

        return new RecommendationDecision(
                selectedCandidate,
                decision.reasoning()
        );
    }
}