package com.sentinq.shopping;

import com.sentinq.evaluation.LlmResult;
import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;

import java.util.List;

public interface GoalFitReasoningProvider {

    String getProviderId();

    LlmResult<GoalFitReasoningDecision> rank(
            Goal goal,
            List<CandidateOffer> candidates
    );
}