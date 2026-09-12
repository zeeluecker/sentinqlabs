package com.sentinq.shopping;

import com.sentinq.evaluation.LlmResult;
import com.sentinq.goal.Goal;

import java.util.List;

public interface RecommendationReasoningProvider {

    String getProviderId();

    LlmResult<RecommendationReasoningDecision> recommend(
            Goal goal,
            List<TrustAssessedCandidate> candidates
    );
}
