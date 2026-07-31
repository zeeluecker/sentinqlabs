package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;

public record ShoppingOrchestrationRequest(
        Goal goal,
        ConsumerPreferences preferences
) {
}