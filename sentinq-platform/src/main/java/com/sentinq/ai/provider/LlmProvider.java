package com.sentinq.ai.provider;

import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.evaluation.LlmResult;


public interface LlmProvider {

    String getProviderId();

     LlmResult<InterpretedShoppingGoal> interpretShoppingGoal(
            String rawGoalText
    );

}
