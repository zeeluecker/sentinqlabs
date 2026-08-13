package com.sentinq.trust;

import java.util.List;

public record MerchantTrustAssessment(
        String merchantId,
        String merchantName,
        List<TrustEvidenceAssessment> evidenceAssessments
) {
}