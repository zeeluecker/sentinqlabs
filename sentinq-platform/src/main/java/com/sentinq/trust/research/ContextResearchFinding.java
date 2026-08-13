package com.sentinq.trust.research;

import com.sentinq.trust.ContextType;
import com.sentinq.trust.EvidenceChannel;
import com.sentinq.trust.EvidenceExpertise;
import com.sentinq.trust.EvidenceHorizon;
import com.sentinq.trust.EvidenceIndependence;
import com.sentinq.trust.EvidenceSourceType;

public record ContextResearchFinding(
        ContextType type,
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