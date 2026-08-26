package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationReasoningService {

    public RecommendationDecision recommend(
            String provider,
            Goal goal,
            List<TrustAssessedCandidate> candidates
    ) {

        /*
         * Recommendation reasoning combines:
         *
         * - the consumer's original goal
         * - lightweight goal-fit reasoning
         * - contextual Trust Map assessments
         *
         * It selects the candidate Sentinq intends
         * to proceed with.
         *
         * It does NOT:
         * - resolve current execution facts
         * - validate late-binding constraints
         * - authorize execution
         */

        throw new UnsupportedOperationException(
                "Recommendation reasoning not implemented yet."
        );
    }
}
