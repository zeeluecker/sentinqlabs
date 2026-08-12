package com.sentinq.trust.interpretation;

import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.InterpretationStatus;
import com.sentinq.trust.TrustSignal;

import java.util.List;

/**
 * Structured reasoning output returned by an AI provider
 * when interpreting raw trust evidence.
 *
 * Sentinq later enriches this provider decision with
 * interpretation identity, evidence references, and context findings.
 */
public record EvidenceInterpretationDecision(
        InterpretationStatus status,
        String apparentMeaning,
        List<ContextRequirement> contextRequirements,
        String contextualMeaning,
        TrustSignal signal,
        double confidence,
        List<String> supportingEvidenceIds,
        List<String> contradictingEvidenceIds
) {
}