package com.sentinq.trust.synthesis;

import com.sentinq.trust.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class MerchantTargetedResearchEvidenceFactory {

    public TrustEvidence create(
            MerchantTargetedResearchFinding finding
    ) {
        return new TrustEvidence(
                UUID.randomUUID().toString(),
                finding.dimension(),
                new EvidenceSource(
                        finding.sourceType(),
                        finding.sourceName(),
                        finding.sourceIndependence(),
                        finding.sourceExpertise()
                ),
                finding.channel(),
                finding.finding(),
                finding.sourceExcerpt(),
                Instant.now(),
                finding.evidenceHorizon(),
                finding.sourceUrl()
        );
    }
}