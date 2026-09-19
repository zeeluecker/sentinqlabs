package com.sentinq.ai;

import com.sentinq.ai.provider.ProductSearchProvider;
import com.sentinq.ai.provider.ProductSearchProviderRegistry;
import com.sentinq.evaluation.*;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductSearchService {

    private final ProductSearchProviderRegistry providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public ProductSearchService(
            ProductSearchProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }

    public ProductSearchResult search(
            UUID executionId,
            Goal goal,
            ConsumerPreferences preferences,
            ModelAssignment assignment
    ) {
        ProductSearchProvider provider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.PRODUCT_DISCOVERY
                );

        return llmInvocationObserver.execute(
                capabilityIdentity,
                () ->
                        provider.searchProducts(
                                goal,
                                preferences,
                                assignment.model()
                        )
        );
    }
}