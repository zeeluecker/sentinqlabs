package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.GoalFitCandidate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalFitReasoningService {

    private final GoalFitReasoningProviderRegistry
            providerRegistry;

    public GoalFitReasoningService(
            GoalFitReasoningProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
    }

    public List<GoalFitCandidate> rank(
            String provider,
            Goal goal,
            List<CandidateOffer> candidates
    ) {
        if (candidates == null ||
                candidates.isEmpty()) {
            return List.of();
        }

        GoalFitReasoningProvider reasoningProvider =
                providerRegistry.getProvider(provider);

        GoalFitReasoningDecision decision =
                reasoningProvider.rank(
                        goal,
                        candidates
                );

        return decision.candidates()
                .stream()
                .map(rankedCandidate -> {

                    CandidateOffer originalOffer =
                            candidates.stream()
                                    .filter(candidate ->
                                            candidate.getOfferId()
                                                    .equals(
                                                            rankedCandidate.offer()
                                                                    .getOfferId()
                                                    )
                                    )
                                    .findFirst()
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Goal-fit reasoning returned unknown offer ID: "
                                                            + rankedCandidate.offer()
                                                            .getOfferId()
                                            )
                                    );

                    return new GoalFitCandidate(
                            originalOffer,
                            rankedCandidate.rank(),
                            rankedCandidate.reasoning()
                    );
                })
                .sorted(
                        java.util.Comparator.comparingInt(
                                GoalFitCandidate::rank
                        )
                )
                .toList();
    }
}
