package com.sentinq.ai;

import com.sentinq.ai.provider.LlmProvider;
import com.sentinq.ai.provider.LlmProviderRegistry;
import com.sentinq.evaluation.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GoalInterpretationService {

    private final LlmProviderRegistry providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public GoalInterpretationService(
            LlmProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }


    public InterpretedShoppingGoal interpret(
            UUID executionId,
            String rawGoalText,
            ModelAssignment assignment
    ) {
        validateGoalText(rawGoalText);

        LlmProvider llmprovider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.GOAL_INTERPRETATION
                );

        return llmInvocationObserver.execute(
                capabilityIdentity,
                () -> llmprovider.interpretShoppingGoal(
                        rawGoalText,
                        assignment.model()
                )
        );
    }


    private void validateGoalText(
            String rawGoalText
    ) {
        if (rawGoalText == null ||
                rawGoalText.isBlank()) {
            throw new IllegalArgumentException(
                    "Goal text is required."
            );
        }
    }
}