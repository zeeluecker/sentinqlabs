package com.sentinq.trust.interpretation;

import com.sentinq.trust.ContextFinding;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;

import java.util.List;

/**
 * Defines the provider-independent contract for reasoning providers
 * that can interpret Trust Map evidence.
 */
public interface EvidenceInterpretationProvider {

    String getProviderId();

    /**
     * Performs the initial interpretation of raw evidence.
     */
    EvidenceInterpretationDecision interpretEvidence(
            TrustEvidence evidence,
            TrustContext context
    );

    /**
     * Reinterprets the original evidence after Sentinq has researched
     * the missing context.
     *
     * The original evidence remains unchanged.
     */
    EvidenceInterpretationDecision reinterpretEvidence(
            TrustEvidence evidence,
            TrustContext context,
            List<TrustEvidence> researchedEvidence,
            List<ContextFinding> contextFindings
    );
}

