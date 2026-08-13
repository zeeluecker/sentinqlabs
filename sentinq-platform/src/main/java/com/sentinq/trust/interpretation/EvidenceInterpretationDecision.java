package com.sentinq.trust.interpretation;

import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.InterpretationStatus;
import com.sentinq.trust.TrustSignal;

import java.util.List;

/**
 * Structured reasoning output returned by an AI provider
 * after interpreting a piece of raw trust evidence.
 *
 * This is an intermediate provider decision and is later
 * converted into Sentinq's EvidenceInterpretation domain object.
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