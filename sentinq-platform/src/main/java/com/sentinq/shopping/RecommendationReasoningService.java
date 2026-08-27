package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.GoalFitCandidate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationReasoningService {

    private final RecommendationReasoningProviderRegistry
            providerRegistry;

    public RecommendationReasoningService(
            RecommendationReasoningProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
    }

    public RecommendationDecision recommend(
            String provider,
            Goal goal,
            List<TrustAssessedCandidate> candidates
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
                providerRegistry.getProvider(provider);

        RecommendationReasoningDecision decision =
                reasoningProvider.recommend(
                        goal,
                        candidates
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
