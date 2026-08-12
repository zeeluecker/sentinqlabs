package com.sentinq.trust.interpretation;

import com.sentinq.trust.ContextFinding;
import com.sentinq.trust.EvidenceInterpretation;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates contextual interpretation of raw trust evidence.
 *
 * The service delegates reasoning to the selected provider,
 * then converts the provider's structured decision into
 * Sentinq's immutable EvidenceInterpretation domain object.
 */
@Service
public class EvidenceInterpretationService {

    private final EvidenceInterpretationProviderRegistry
            providerRegistry;

    public EvidenceInterpretationService(
            EvidenceInterpretationProviderRegistry providerRegistry
    ) {
        this.providerRegistry =
                providerRegistry;
    }

    /**
     * Interprets raw trust evidence using the reasoning provider
     * selected for the current Sentinq execution.
     *
     * Raw TrustEvidence remains unchanged. The provider returns
     * a structured interpretation decision which Sentinq records
     * separately as an EvidenceInterpretation.
     */
    public EvidenceInterpretation interpret(
            String provider,
            TrustEvidence evidence,
            TrustContext context
    ) {
        validateInputs(
                provider,
                evidence,
                context
        );

        EvidenceInterpretationProvider interpretationProvider =
                providerRegistry.getProvider(
                        provider
                );

        EvidenceInterpretationDecision decision =
                interpretationProvider.interpretEvidence(
                        evidence,
                        context
                );

        return createInterpretation(
                evidence,
                decision
        );
    }

    /**
     * Converts the provider's structured reasoning decision into
     * Sentinq's immutable EvidenceInterpretation record.
     *
     * Context findings are empty at this stage because contextual
     * enrichment has not yet been performed.
     */
    private EvidenceInterpretation createInterpretation(
            TrustEvidence evidence,
            EvidenceInterpretationDecision decision
    ) {
        return new EvidenceInterpretation(
                UUID.randomUUID().toString(),
                evidence.evidenceId(),
                decision.status(),
                decision.apparentMeaning(),
                decision.contextRequirements(),
                List.of(),
                decision.contextualMeaning(),
                decision.signal(),
                decision.confidence(),
                decision.supportingEvidenceIds(),
                decision.contradictingEvidenceIds()
        );
    }

    /**
     * Validates the minimum inputs required before sending
     * trust evidence to a reasoning provider.
     */
    private void validateInputs(
            String provider,
            TrustEvidence evidence,
            TrustContext context
    ) {
        if (provider == null ||
                provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider is required."
            );
        }

        if (evidence == null) {
            throw new IllegalArgumentException(
                    "Trust evidence is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}