package com.sentinq.checkout;

import com.fasterxml.jackson.databind.JsonNode;

/** A service binding; endpoint may be absent in an advertised declaration. */
public record MerchantServiceEndpoint(
        String service, String version, String transport,
        String endpoint, JsonNode declaration
) {
    public MerchantServiceEndpoint {
        declaration = declaration.deepCopy();
    }

    /** Returns the complete binding, including schema and configuration. */
    @Override
    public JsonNode declaration() {
        return declaration.deepCopy();
    }
}
