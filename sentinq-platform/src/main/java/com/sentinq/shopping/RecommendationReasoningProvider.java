package com.sentinq.shopping;

import com.sentinq.goal.Goal;

import java.util.List;

public interface RecommendationReasoningProvider {

    String getProviderId();

    RecommendationReasoningDecision recommend(
            Goal goal,
            List<TrustAssessedCandidate> candidates
    );
}
