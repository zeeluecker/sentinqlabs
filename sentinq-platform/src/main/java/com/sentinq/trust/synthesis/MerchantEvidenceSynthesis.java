package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustDimension;

import java.util.List;

public record MerchantEvidenceSynthesis(
        String merchantId,
        String merchantName,
        List<TrustEvidenceTheme> themes,
        List<MaterialTrustQuestion> materialQuestions,
        List<String> supportingEvidenceIds,
        double confidence
) {
}
