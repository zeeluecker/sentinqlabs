package com.sentinq.trust.observations;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MerchantEvidenceCollectionProviderRegistry {

    private final List<MerchantEvidenceCollectionProvider> providers;

    public MerchantEvidenceCollectionProviderRegistry(
            List<MerchantEvidenceCollectionProvider> providers
    ) {
        this.providers = providers;
    }

    public MerchantEvidenceCollectionProvider getProvider(
            String providerId
    ) {
        return providers.stream()
                .filter(provider ->
                        provider.getProviderId()
                                .equalsIgnoreCase(providerId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No merchant evidence collection provider registered for: "
                                        + providerId
                        )
                );
    }
}