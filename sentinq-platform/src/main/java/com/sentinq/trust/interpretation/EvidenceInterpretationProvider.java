package com.sentinq.trust.interpretation;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;

/**
 * Defines the provider-independent contract for reasoning providers
 * that can interpret Trust Map evidence.
 */
public interface EvidenceInterpretationProvider {

    /**
     * Returns the provider identifier used by Sentinq
     * to resolve the correct implementation at runtime.
     */
    String getProviderId();

    /**
     * Interprets raw trust evidence within a specific context
     * and returns a structured interpretation decision.
     *
     * Raw evidence must not be modified by this operation.
     */
    EvidenceInterpretationDecision interpretEvidence(
            TrustEvidence evidence,
            TrustContext context
    );
}