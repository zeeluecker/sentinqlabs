package com.sentinq.checkout;

import java.util.List;

/**
 * Discovery information for one merchant through one commerce protocol.
 * Capability versions and service bindings retain their advertised details.
 * This identifies possible information sources, not transaction executability.
 */
public class MerchantCapabilities {
    private String merchantId;
    private CommerceProtocolType protocol;
    private String protocolVersion;
    private String discoveryUrl;
    private List<AdvertisedCapability> advertisedCapabilities = List.of();
    private List<MerchantServiceEndpoint> endpoints = List.of();

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public CommerceProtocolType getProtocol() { return protocol; }
    public void setProtocol(CommerceProtocolType protocol) { this.protocol = protocol; }
    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String discoveryUrl) { this.discoveryUrl = discoveryUrl; }

    /** All advertised capability declarations, including multiple versions. */
    public List<AdvertisedCapability> getAdvertisedCapabilities() { return advertisedCapabilities; }
    public void setAdvertisedCapabilities(List<AdvertisedCapability> capabilities) {
        this.advertisedCapabilities = List.copyOf(capabilities);
    }

    /** Service bindings retain their service name, transport, and endpoint together. */
    public List<MerchantServiceEndpoint> getEndpoints() { return endpoints; }
    public void setEndpoints(List<MerchantServiceEndpoint> endpoints) {
        this.endpoints = List.copyOf(endpoints);
    }
}
