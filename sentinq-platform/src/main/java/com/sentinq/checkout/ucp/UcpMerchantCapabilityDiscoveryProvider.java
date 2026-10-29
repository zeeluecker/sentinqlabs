package com.sentinq.checkout.ucp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sentinq.checkout.*;
import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads UCP advertisements; does not invoke capabilities or gather screening facts. */
@Component
public class UcpMerchantCapabilityDiscoveryProvider implements MerchantCapabilityDiscoveryProvider {
    private final RestClient restClient;

    public UcpMerchantCapabilityDiscoveryProvider() { this(RestClient.create()); }

    /** Allows a configured client to be supplied for controlled discovery tests. */
    public UcpMerchantCapabilityDiscoveryProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CommerceProtocolType getProtocolType() { return CommerceProtocolType.UCP; }

    /**
     * Missing URLs and HTTP 404 mean no profile was discovered. Other request
     * errors and malformed profiles are failures, not evidence of absent support.
     */
    @Override
    public Optional<MerchantCapabilities> discover(CandidateOffer offer) {
        if (offer.getMerchantUrl() == null || offer.getMerchantUrl().isBlank()) {
            return Optional.empty();
        }
        try {
            String discoveryUrl = buildDiscoveryUrl(offer.getMerchantUrl());
            String response = restClient.get().uri(discoveryUrl)
                    .retrieve().body(String.class);
            UcpDiscoveryProfile profile = response == null ? null
                    : new ObjectMapper().readValue(response, UcpDiscoveryProfile.class);
            if (profile == null || profile.getUcp() == null || !profile.getUcp().isObject()) {
                throw new IllegalArgumentException("Missing UCP metadata object");
            }
            JsonNode ucp = profile.getUcp();
            String version = requiredText(ucp, "version");
            if (!ucp.path("services").isObject() || !ucp.path("payment_handlers").isObject()) {
                throw new IllegalArgumentException("Missing UCP services or payment_handlers registry");
            }

            List<AdvertisedCapability> capabilities = new ArrayList<>();
            JsonNode registry = ucp.get("capabilities");
            if (registry != null) {
                requireRegistry(registry);
                registry.fields().forEachRemaining(entry -> {
                    requireDeclarations(entry.getValue());
                    for (JsonNode declaration : entry.getValue()) {
                        capabilities.add(new AdvertisedCapability(entry.getKey(),
                                requiredText(declaration, "version"), declaration));
                    }
                });
            }

            List<MerchantServiceEndpoint> endpoints = new ArrayList<>();
            ucp.get("services").fields().forEachRemaining(entry -> {
                requireDeclarations(entry.getValue());
                for (JsonNode declaration : entry.getValue()) {
                    endpoints.add(new MerchantServiceEndpoint(entry.getKey(),
                            requiredText(declaration, "version"),
                            requiredText(declaration, "transport"),
                            optionalText(declaration, "endpoint"), declaration));
                }
            });

            MerchantCapabilities result = new MerchantCapabilities();
            result.setMerchantId(offer.getMerchantId());
            result.setProtocol(CommerceProtocolType.UCP);
            result.setProtocolVersion(version);
            result.setDiscoveryUrl(discoveryUrl);
            result.setAdvertisedCapabilities(capabilities);
            result.setEndpoints(endpoints);
            return Optional.of(result);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) { return Optional.empty(); }
            throw failure(offer, ex);
        } catch (JsonProcessingException ex) {
            throw new MerchantCapabilityDiscoveryException(
                    "Unreadable UCP profile for merchant: " + offer.getMerchantId(), ex);
        } catch (RuntimeException ex) {
            throw failure(offer, ex);
        }
    }

    /** Resolves discovery at the origin, removing product paths, queries, and fragments. */
    private String buildDiscoveryUrl(String merchantUrl) {
        URI uri = URI.create(merchantUrl);
        if (uri.getHost() == null || uri.getUserInfo() != null ||
                !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Merchant URL must be an absolute HTTP(S) URL without credentials");
        }
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    "/.well-known/ucp", null, null).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid merchant origin", ex);
        }
    }

    private static void requireRegistry(JsonNode registry) {
        if (!registry.isObject()) { throw new IllegalArgumentException("Invalid capability registry"); }
    }

    private static void requireDeclarations(JsonNode declarations) {
        if (!declarations.isArray()) { throw new IllegalArgumentException("Registry entries must be arrays"); }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing declaration field: " + field);
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) { return null; }
        if (!value.isTextual()) { throw new IllegalArgumentException("Invalid declaration field: " + field); }
        return value.textValue();
    }

    private static MerchantCapabilityDiscoveryException failure(CandidateOffer offer, RuntimeException cause) {
        return new MerchantCapabilityDiscoveryException(
                "UCP discovery failed for merchant: " + offer.getMerchantId(), cause);
    }
}
