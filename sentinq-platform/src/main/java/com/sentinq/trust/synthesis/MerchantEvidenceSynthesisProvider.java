package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;

import java.util.List;

public interface MerchantEvidenceSynthesisProvider {

    String providerId();

    MerchantEvidenceSynthesis synthesizeEvidence(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    );

    MerchantEvidenceSynthesis refineSynthesis(
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context
    );
}