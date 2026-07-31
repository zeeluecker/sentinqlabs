package com.sentinq.shopping;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.ResolutionResult;

public record ResolvedCandidate(
        CandidateOffer offer,
        ResolutionResult resolution
) {
}