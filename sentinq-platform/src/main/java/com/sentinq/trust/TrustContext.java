package com.sentinq.trust;
// TrustContext.java

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TrustContext(
        String goal,
        String productCategory,
        String productType,
        Map<String, String> productAttributes,
        BigDecimal transactionValue,
        DeliveryUrgency deliveryUrgency,
        MerchantFamiliarity merchantFamiliarity,
        List<TrustDimension> importantDimensions
) {
}