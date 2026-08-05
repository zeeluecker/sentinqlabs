package com.sentinq.ai.provider;

import com.sentinq.ai.InterpretedShoppingGoal;

public interface LlmProvider {

    String getProviderId();

    InterpretedShoppingGoal interpretShoppingGoal(
            String rawGoalText
    );
}
