package com.sentinq.trust.observations;

import com.sentinq.trust.*;

public record MerchantEvidenceObservation(
        TrustDimension proposedDimension,
        EvidenceSourceType sourceType,
        String sourceName,
        EvidenceIndependence sourceIndependence,
        EvidenceExpertise sourceExpertise,
        EvidenceChannel channel,
        String claim,
        String rawContent,
        EvidenceHorizon evidenceHorizon,
        String sourceUrl
) {
}