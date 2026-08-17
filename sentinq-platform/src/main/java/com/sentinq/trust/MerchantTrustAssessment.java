package com.sentinq.trust;

import com.sentinq.trust.synthesis.MerchantEvidenceSynthesis;

import java.util.List;

public record MerchantTrustAssessment(
        String merchantId,
        String merchantName,
        List<TrustEvidence> observedEvidence,
        List<TrustEvidence> researchedEvidence,
        MerchantEvidenceSynthesis synthesis
) {
}