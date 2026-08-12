package com.sentinq.trust;
// MerchantTrustMap.java

import java.time.Instant;
import java.util.List;

public record MerchantTrustMap(
        String merchantId,
        String merchantName,
        TrustContext context,
        List<TrustEvidence> evidence,
        List<EvidenceInterpretation> interpretations,
        List<TrustDimensionAssessment> dimensions,
        ContextualTrustAssessment assessment,
        Instant generatedAt
) {
}