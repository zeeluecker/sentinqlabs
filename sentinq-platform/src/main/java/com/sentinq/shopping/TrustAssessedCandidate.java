package com.sentinq.shopping;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.MerchantTrustAssessment;

public record TrustAssessedCandidate(
        CandidateOffer candidate,
        MerchantTrustAssessment trustAssessment
) {
}