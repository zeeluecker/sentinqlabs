package com.sentinq.trust.research;

import com.sentinq.trust.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ContextResearchEvidenceFactory {

    public TrustEvidence createTrustEvidence(
            TrustEvidence originalEvidence,
            ContextResearchFinding finding
    ) {
        String evidenceId =
                UUID.randomUUID().toString();

        return new TrustEvidence(
                evidenceId,
                originalEvidence.proposedDimension(),
                new EvidenceSource(
                        finding.sourceType(),
                        finding.sourceName(),
                        finding.sourceIndependence(),
                        finding.sourceExpertise()
                ),
                finding.channel(),
                finding.finding(),
                finding.sourceExcerpt(),
                null,
                finding.evidenceHorizon(),
                finding.sourceUrl()
        );
    }

    public ContextFinding createContextFinding(
            ContextResearchFinding finding,
            TrustEvidence evidence
    ) {
        return new ContextFinding(
                finding.type(),
                finding.finding(),
                evidence.evidenceId(),
                finding.confidence()
        );
    }
}