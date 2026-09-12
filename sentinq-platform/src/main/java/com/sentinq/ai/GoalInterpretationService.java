package com.sentinq.ai;

import com.sentinq.ai.provider.LlmProvider;
import com.sentinq.ai.provider.LlmProviderRegistry;
import com.sentinq.evaluation.LlmInvocationObserver;
import com.sentinq.evaluation.LlmResult;
import org.springframework.stereotype.Service;

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
            String provider,
            String rawGoalText
    ) {
        validateGoalText(rawGoalText);

        LlmProvider llmprovider =
                providerRegistry.getProvider(
                        provider
                );

        return llmInvocationObserver.execute(
                () ->
                        llmprovider.interpretShoppingGoal(
                                rawGoalText
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