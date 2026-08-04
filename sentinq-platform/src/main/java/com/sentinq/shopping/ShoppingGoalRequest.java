package com.sentinq.shopping;

import java.util.UUID;

public record ShoppingGoalRequest(
        UUID principalId,
        UUID agentId,
        String goalText
) {
}