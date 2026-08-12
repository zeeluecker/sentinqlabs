package com.sentinq.trust.interpretation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the evidence interpretation capability
 * for the requested AI provider.
 */
@Component
public class EvidenceInterpretationProviderRegistry {

    private final Map<String, EvidenceInterpretationProvider>
            providersById;

    public EvidenceInterpretationProviderRegistry(
            List<EvidenceInterpretationProvider> providers
    ) {
        this.providersById =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        EvidenceInterpretationProvider::getProviderId,
                                        Function.identity()
                                )
                        );
    }

    /**
     * Returns the evidence interpretation provider registered
     * for the supplied provider identifier.
     */
    public EvidenceInterpretationProvider getProvider(
            String provider
    ) {
        EvidenceInterpretationProvider interpretationProvider =
                providersById.get(
                        provider
                );

        if (interpretationProvider == null) {
            throw new IllegalArgumentException(
                    "No evidence interpretation provider registered for: "
                            + provider
            );
        }

        return interpretationProvider;
    }
}