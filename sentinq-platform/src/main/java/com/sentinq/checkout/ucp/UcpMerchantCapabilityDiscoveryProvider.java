package com.sentinq.checkout.ucp;

import com.sentinq.checkout.CommerceProtocolType;
import com.sentinq.checkout.MerchantCapabilities;
import com.sentinq.checkout.MerchantCapabilityDiscoveryProvider;
import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class UcpMerchantCapabilityDiscoveryProvider
        implements MerchantCapabilityDiscoveryProvider {

    private final RestClient restClient;

    public UcpMerchantCapabilityDiscoveryProvider() {
        this.restClient =
                RestClient.create();
    }

    @Override
    public CommerceProtocolType getProtocolType() {
        return CommerceProtocolType.UCP;
    }

    @Override
    public Optional<MerchantCapabilities> discover(
            CandidateOffer offer
    ) {

        if (offer.getMerchantUrl() == null ||
                offer.getMerchantUrl().isBlank()) {
            return Optional.empty();
        }

        String discoveryUrl =
                buildDiscoveryUrl(
                        offer.getMerchantUrl()
                );

        UcpDiscoveryProfile profile =
                restClient.get()
                        .uri(discoveryUrl)
                        .retrieve()
                        .body(UcpDiscoveryProfile.class);

        if (profile == null ||
                profile.getUcp() == null) {
            return Optional.empty();
        }

        UcpDiscoveryProfile.Ucp ucp =
                profile.getUcp();

        if (ucp.getCapabilities() == null ||
                !ucp.getCapabilities().containsKey(
                        "dev.ucp.shopping.checkout"
                )) {
            return Optional.empty();
        }

        if (ucp.getServices() == null) {
            return Optional.empty();
        }

        var shoppingServices =
                ucp.getServices().get(
                        "dev.ucp.shopping"
                );

        if (shoppingServices == null) {
            return Optional.empty();
        }

        Optional<UcpDiscoveryProfile.Service> restService =
                shoppingServices.stream()
                        .filter(service ->
                                "rest".equalsIgnoreCase(
                                        service.getTransport()
                                )
                        )
                        .filter(service ->
                                service.getEndpoint() != null &&
                                        !service.getEndpoint().isBlank()
                        )
                        .findFirst();

        if (restService.isEmpty()) {
            return Optional.empty();
        }

        MerchantCapabilities capabilities =
                new MerchantCapabilities();

        capabilities.setMerchantId(
                offer.getMerchantId()
        );

        capabilities.setProtocol(
                CommerceProtocolType.UCP
        );

        capabilities.setEndpoint(
                restService.get().getEndpoint()
        );

        capabilities.setCheckoutInitiationSupported(
                true
        );

        capabilities.setCheckoutExecutionSupported(
                true
        );

        return Optional.of(
                capabilities
        );
    }

    private String buildDiscoveryUrl(
            String merchantUrl
    ) {

        String normalizedMerchantUrl =
                merchantUrl.endsWith("/")
                        ? merchantUrl.substring(
                        0,
                        merchantUrl.length() - 1
                )
                        : merchantUrl;

        return normalizedMerchantUrl
                + "/.well-known/ucp";
    }
}