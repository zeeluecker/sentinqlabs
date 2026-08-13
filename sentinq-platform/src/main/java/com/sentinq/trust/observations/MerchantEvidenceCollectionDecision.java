package com.sentinq.trust.observations;

import java.util.List;

public record MerchantEvidenceCollectionDecision(
        List<MerchantEvidenceObservation> observations
) {
}