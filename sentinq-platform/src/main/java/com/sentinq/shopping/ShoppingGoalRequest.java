package com.sentinq.shopping;



import com.sentinq.evaluation.Capability;
import com.sentinq.evaluation.ModelAssignment;

import java.util.Map;
import java.util.UUID;

public record ShoppingGoalRequest(
        UUID principalId,
        UUID agentId,
        String goalText,
        Map<Capability, ModelAssignment> modelAssignments
) {
}