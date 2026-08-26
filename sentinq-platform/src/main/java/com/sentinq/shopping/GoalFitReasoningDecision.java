package com.sentinq.shopping;

import com.sentinq.resolution.GoalFitCandidate;

import java.util.List;

public record GoalFitReasoningDecision(
        List<GoalFitCandidate> candidates
) {
}
