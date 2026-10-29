package com.sentinq.checkout.ucp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/** Wire profile retaining UCP declarations without discarding protocol fields. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UcpDiscoveryProfile {
    private JsonNode ucp;
    public JsonNode getUcp() { return ucp; }
    public void setUcp(JsonNode ucp) { this.ucp = ucp; }
}
