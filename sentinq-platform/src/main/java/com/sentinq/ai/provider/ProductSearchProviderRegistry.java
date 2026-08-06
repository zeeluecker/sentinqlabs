package com.sentinq.ai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductSearchProviderRegistry {

    private final Map<String, ProductSearchProvider> providers;

    public ProductSearchProviderRegistry(
            List<ProductSearchProvider> providers
    ) {
        this.providers = providers.stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                ProductSearchProvider::getProviderId,
                                Function.identity()
                        )
                );
    }

    public ProductSearchProvider getProvider(
            String providerId
    ) {
        ProductSearchProvider provider =
                providers.get(
                        normalizeProviderId(providerId)
                );

        if (provider == null) {
            throw new IllegalArgumentException(
                    "No product-search capability is registered "
                            + "for provider: "
                            + providerId
            );
        }

        return provider;
    }

    private String normalizeProviderId(
            String providerId
    ) {
        if (providerId == null ||
                providerId.isBlank()) {
            throw new IllegalArgumentException(
                    "providerId is required."
            );
        }

        return providerId
                .trim()
                .toLowerCase();
    }
}