package com.sentinq.shopping;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GoalFitReasoningProviderRegistry {

    private final Map<String, GoalFitReasoningProvider> providers;

    public GoalFitReasoningProviderRegistry(
            List<GoalFitReasoningProvider> providers
    ) {
        this.providers =
                providers.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        GoalFitReasoningProvider::getProviderId,
                                        Function.identity()
                                )
                        );
    }

    public GoalFitReasoningProvider getProvider(
            String providerId
    ) {
        GoalFitReasoningProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported goal-fit reasoning provider: "
                            + providerId
            );
        }

        return provider;
    }
}
