package com.sentinq.ai.provider;

import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;

public interface ProductSearchProvider {

    String getProviderId();

    ProductSearchResult searchProducts(
            Goal goal,
            ConsumerPreferences preferences
    );
}
