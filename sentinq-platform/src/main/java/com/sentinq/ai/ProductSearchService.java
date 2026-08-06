package com.sentinq.ai;

import com.sentinq.ai.provider.ProductSearchProvider;
import com.sentinq.ai.provider.ProductSearchProviderRegistry;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {

    private final ProductSearchProviderRegistry providerRegistry;

    public ProductSearchService(
            ProductSearchProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
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

        return provider.searchProducts(
                goal,
                preferences
        );
    }
}