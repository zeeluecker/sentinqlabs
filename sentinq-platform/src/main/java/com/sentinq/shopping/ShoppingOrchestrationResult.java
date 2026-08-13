package com.sentinq.shopping;

import com.sentinq.mandate.MandateEnvelope;

import java.util.List;

public record ShoppingOrchestrationResult(
        MandateEnvelope mandate,
        List<TrustAssessedCandidate> trustAssessedCandidates,
        List<ResolvedCandidate> candidates,
        ResolvedCandidate selectedCandidate
) {
}