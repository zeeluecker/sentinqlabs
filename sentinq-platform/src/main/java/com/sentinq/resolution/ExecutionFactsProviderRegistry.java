package com.sentinq.resolution;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExecutionFactsProviderRegistry {

    private final Map<String, ExecutionFactsProvider> providers;

    public ExecutionFactsProviderRegistry(
            List<ExecutionFactsProvider> providers
    ) {
        this.providers =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        ExecutionFactsProvider::getProviderId,
                                        Function.identity()
                                )
                        );
    }

    public ExecutionFactsProvider get(
            String providerId
    ) {
        ExecutionFactsProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unknown execution facts provider: "
                            + providerId
            );
        }

        return provider;
    }
}
