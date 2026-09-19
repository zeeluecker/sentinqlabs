package com.sentinq.shopping;

import com.sentinq.evaluation.*;
import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.GoalFitCandidate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GoalFitReasoningService {

    private final GoalFitReasoningProviderRegistry
            providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public GoalFitReasoningService(
            GoalFitReasoningProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }

    public List<GoalFitCandidate> rank(
            UUID executionId,
            Goal goal,
            List<CandidateOffer> candidates,
            ModelAssignment assignment
    ) {
        if (candidates == null ||
                candidates.isEmpty()) {
            return List.of();
        }

        GoalFitReasoningProvider reasoningProvider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.GOAL_FIT_REASONING
                );

        GoalFitReasoningDecision decision =
                llmInvocationObserver.execute(
                        capabilityIdentity,
                        () ->
                                reasoningProvider.rank(
                                        goal,
                                        candidates,
                                        assignment.model()
                                )
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