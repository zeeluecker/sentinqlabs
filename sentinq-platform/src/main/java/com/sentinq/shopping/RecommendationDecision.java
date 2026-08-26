package com.sentinq.shopping;

import com.sentinq.resolution.CandidateOffer;

public record RecommendationDecision(
        CandidateOffer selectedCandidate,
        String reasoning
) {
}
