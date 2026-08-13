package com.sentinq.trust.observations;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.TrustContext;

public interface MerchantEvidenceCollectionProvider {

    String getProviderId();

    MerchantEvidenceCollectionDecision collectMerchantEvidence(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    );
}