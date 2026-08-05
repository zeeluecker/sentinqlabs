package com.sentinq.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class ProductSearchResult {

    @JsonPropertyDescription(
            "Real product offers found through web search."
    )
    public List<ProductSearchOffer> offers;
}