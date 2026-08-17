package com.sentinq.trust.synthesis;

import com.sentinq.trust.*;

public record MerchantTargetedResearchFinding(
        TrustDimension dimension,
        ContextType contextType,
        String finding,
        EvidenceSourceType sourceType,
        String sourceName,
        EvidenceIndependence sourceIndependence,
        EvidenceExpertise sourceExpertise,
        EvidenceChannel channel,
        EvidenceHorizon evidenceHorizon,
        String sourceUrl,
        String sourceExcerpt,
        double confidence
) {
}