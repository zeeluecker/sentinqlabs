package com.sentinq.ai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderRegistry {

    private final Map<String, LlmProvider> providers;

    public LlmProviderRegistry(
            List<LlmProvider> providers
    ) {
        this.providers = providers.stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                LlmProvider::getProviderId,
                                Function.identity()
                        )
                );
        System.out.println(
                "Registered LLM providers: "
                        + this.providers.keySet()
        );
    }

    public LlmProvider getProvider(
            String providerId
    ) {
        LlmProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported LLM provider: "
                            + providerId
            );
        }

        return provider;
    }

    public List<String> getAvailableProviderIds() {
        return providers.keySet()
                .stream()
                .sorted()
                .toList();
    }
}