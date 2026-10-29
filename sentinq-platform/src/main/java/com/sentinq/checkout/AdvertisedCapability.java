package com.sentinq.checkout;

import com.fasterxml.jackson.databind.JsonNode;

/** One advertised capability version, with its complete protocol declaration. */
public record AdvertisedCapability(String name, String version, JsonNode declaration) {
    public AdvertisedCapability {
        declaration = declaration.deepCopy();
    }

    /** Preserves configuration, extension relationships, and schema references. */
    @Override
    public JsonNode declaration() {
        return declaration.deepCopy();
    }
}
