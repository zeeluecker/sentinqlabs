package com.sentinq.ai;

import com.sentinq.ai.provider.LlmProvider;
import org.springframework.stereotype.Service;

@Service
public class GoalInterpretationService {

    private final LlmProvider llmProvider;

    public GoalInterpretationService(
            LlmProvider llmProvider
    ) {
        this.llmProvider = llmProvider;
    }

    public InterpretedShoppingGoal interpret(
            String rawGoalText
    ) {
        validateGoalText(rawGoalText);

        return llmProvider.interpretShoppingGoal(
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