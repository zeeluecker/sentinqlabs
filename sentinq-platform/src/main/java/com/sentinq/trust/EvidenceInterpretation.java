package com.sentinq.trust;

import java.util.List;

public record EvidenceInterpretation(
        String interpretationId,
        String evidenceId,
        InterpretationStatus status,
        String apparentMeaning,
        List<ContextRequirement> contextRequirements,
        List<ContextFinding> contextFindings,
        String contextualMeaning,
        TrustSignal signal,
        double confidence,
        List<String> supportingEvidenceIds,
        List<String> contradictingEvidenceIds
) {
}
