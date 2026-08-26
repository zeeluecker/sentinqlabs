package com.sentinq.shopping;

import com.sentinq.resolution.GoalFitCandidate;
import com.sentinq.trust.MerchantTrustAssessment;

public record TrustAssessedCandidate(
        GoalFitCandidate goalFitCandidate,
        MerchantTrustAssessment trustAssessment
) {
}