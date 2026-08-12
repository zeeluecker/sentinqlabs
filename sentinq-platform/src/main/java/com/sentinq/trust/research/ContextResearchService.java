package com.sentinq.trust.research;

import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextResearchService {

    private final ContextResearchProviderRegistry providerRegistry;

    public ContextResearchService(
            ContextResearchProviderRegistry providerRegistry
    ) {
        this.providerRegistry =
                providerRegistry;
    }

    public ContextResearchDecision research(
            String provider,
            String merchantId,
            String merchantName,
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    ) {
        validateInputs(
                provider,
                originalEvidence,
                context,
                requirements
        );

        ContextResearchProvider researchProvider =
                providerRegistry.getProvider(
                        provider
                );

        return researchProvider.researchContext(
                merchantId,
                merchantName,
                originalEvidence,
                context,
                requirements
        );
    }

    private void validateInputs(
            String provider,
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    ) {
        if (provider == null ||
                provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider is required."
            );
        }

        if (originalEvidence == null) {
            throw new IllegalArgumentException(
                    "Original trust evidence is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }

        if (requirements == null ||
                requirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one context requirement is required."
            );
        }
    }
}
