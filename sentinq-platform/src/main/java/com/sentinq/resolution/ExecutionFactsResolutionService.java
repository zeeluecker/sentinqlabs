package com.sentinq.resolution;

import org.springframework.stereotype.Service;

@Service
public class ExecutionFactsResolutionService {

    private final ExecutionFactsProviderRegistry providerRegistry;

    public ExecutionFactsResolutionService(
            ExecutionFactsProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
    }

    public ResolvedExecutionFacts resolve(
            String providerId,
            CandidateOffer offer
    ) {
        ExecutionFactsProvider provider =
                providerRegistry.get(providerId);

        return provider.resolveExecutionFacts(
                offer
        );
    }
}
