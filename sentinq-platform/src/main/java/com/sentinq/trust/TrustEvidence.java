package com.sentinq.trust;
// TrustEvidence.java

import java.time.Instant;

public record TrustEvidence(
        String evidenceId,
        TrustDimension proposedDimension,
        EvidenceSource source,
        EvidenceChannel channel,
        String rawClaim,
        String sourceExcerpt,
        Instant publishedAt,
        EvidenceHorizon evidenceHorizon,
        String sourceUrl
) {
}