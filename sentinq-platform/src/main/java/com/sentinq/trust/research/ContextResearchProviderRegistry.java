package com.sentinq.trust.research;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ContextResearchProviderRegistry {

    private final Map<String, ContextResearchProvider> providers;

    public ContextResearchProviderRegistry(
            List<ContextResearchProvider> providers
    ) {
        this.providers =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        ContextResearchProvider::getProviderId,
                                        Function.identity()
                                )
                        );
    }

    public ContextResearchProvider getProvider(
            String providerId
    ) {
        ContextResearchProvider provider =
                providers.get(
                        providerId
                );

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unknown context research provider: "
                            + providerId
            );
        }

        return provider;
    }
}
