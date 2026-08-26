package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.GoalFitCandidate;

import java.util.List;

public interface GoalFitReasoningProvider {

    String getProviderId();

    GoalFitReasoningDecision rank(
            Goal goal,
            List<CandidateOffer> candidates
    );
}
