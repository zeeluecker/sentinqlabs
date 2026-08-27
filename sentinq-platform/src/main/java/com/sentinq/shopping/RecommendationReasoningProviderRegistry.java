package com.sentinq.shopping;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RecommendationReasoningProviderRegistry {

    private final Map<String, RecommendationReasoningProvider> providers;

    public RecommendationReasoningProviderRegistry(
            List<RecommendationReasoningProvider> providers
    ) {
        this.providers = providers.stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                RecommendationReasoningProvider::getProviderId,
                                Function.identity()
                        )
                );
    }

    public RecommendationReasoningProvider getProvider(
            String providerId
    ) {
        RecommendationReasoningProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported recommendation reasoning provider: "
                            + providerId
            );
        }

        return provider;
    }
}
