package com.sentinq.trust.observations;

import com.sentinq.trust.EvidenceSource;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class MerchantEvidenceFactory {

    public TrustEvidence create(
            MerchantEvidenceObservation observation
    ) {
        return new TrustEvidence(
                UUID.randomUUID().toString(),
                observation.proposedDimension(),
                new EvidenceSource(
                        observation.sourceType(),
                        observation.sourceName(),
                        observation.sourceIndependence(),
                        observation.sourceExpertise()
                ),
                observation.channel(),
                observation.claim(),
                observation.rawContent(),
                Instant.now(),
                observation.evidenceHorizon(),
                observation.sourceUrl()
        );
    }
}