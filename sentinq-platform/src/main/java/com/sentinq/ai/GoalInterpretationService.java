package com.sentinq.ai;

import com.sentinq.ai.provider.LlmProvider;
import com.sentinq.ai.provider.LlmProviderRegistry;
import org.springframework.stereotype.Service;

@Service
public class GoalInterpretationService {

    private final LlmProviderRegistry providerRegistry;

    public GoalInterpretationService(
            LlmProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
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

        return llmprovider.interpretShoppingGoal(
                rawGoalText
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