package com.sentinq.ai;

import com.sentinq.ai.provider.ProductSearchProvider;
import com.sentinq.ai.provider.ProductSearchProviderRegistry;
import com.sentinq.evaluation.LlmInvocationObserver;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.stereotype.Service;

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
            String providerId,
            Goal goal,
            ConsumerPreferences preferences
    ) {
        ProductSearchProvider provider =
                providerRegistry.getProvider(
                        providerId
                );

        return llmInvocationObserver.execute(
                () ->
                        provider.searchProducts(
                                goal,
                                preferences
                        )
        );
    }
}