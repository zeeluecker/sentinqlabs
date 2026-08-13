package com.sentinq.trust;

import java.util.List;

public record TrustEvidenceAssessment(
        TrustEvidence originalEvidence,
        EvidenceInterpretation interpretation,
        List<TrustEvidence> researchedEvidence,
        List<ContextFinding> contextFindings,
        int researchRounds
) {
}
