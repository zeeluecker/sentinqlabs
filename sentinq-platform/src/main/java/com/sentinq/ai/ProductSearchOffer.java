package com.sentinq.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ProductSearchOffer {

    @JsonPropertyDescription(
            "Merchant name shown on the product page."
    )
    public String merchantName;

    @JsonPropertyDescription(
            "Product name shown on the product page."
    )
    public String productName;

    @JsonPropertyDescription(
            "Product page URL."
    )
    public String productUrl;

    @JsonPropertyDescription(
            "Listed product price in cents, excluding unresolved shipping and tax."
    )
    public int productPriceCents;

    @JsonPropertyDescription(
            "Whether the page indicates that the item is currently available."
    )
    public boolean inventoryAvailable;

    @JsonPropertyDescription(
            "Short explanation of why the product matches the goal."
    )
    public String matchReason;

    @JsonPropertyDescription(
            "Concise factual description of product attributes relevant to the shopping goal, based only on information found during discovery."
    )
    public String productDescription;
}