package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;

import java.util.List;

public interface MerchantTargetedResearchProvider {

    String providerId();

    MerchantTargetedResearchDecision research(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    );
}
