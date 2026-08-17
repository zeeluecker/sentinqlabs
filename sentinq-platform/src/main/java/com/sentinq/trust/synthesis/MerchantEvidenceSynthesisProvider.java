package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.synthesis.MerchantEvidenceSynthesis;

import java.util.List;

public interface MerchantEvidenceSynthesisProvider {

    String providerId();

    MerchantEvidenceSynthesis synthesizeEvidence(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    );
}