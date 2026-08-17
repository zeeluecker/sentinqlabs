package com.sentinq.trust.synthesis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MerchantTargetedResearchProviderRegistry {

    private final Map<String, MerchantTargetedResearchProvider> providers;

    public MerchantTargetedResearchProviderRegistry(
            List<MerchantTargetedResearchProvider> providers
    ) {
        this.providers =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        MerchantTargetedResearchProvider::providerId,
                                        Function.identity()
                                )
                        );
    }

    public MerchantTargetedResearchProvider getProvider(
            String providerId
    ) {
        MerchantTargetedResearchProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported merchant targeted research provider: "
                            + providerId
            );
        }

        return provider;
    }
}
