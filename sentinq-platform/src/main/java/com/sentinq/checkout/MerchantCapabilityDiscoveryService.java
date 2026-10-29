package com.sentinq.checkout;


import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Coordinates discovery of a merchant's advertised commerce capabilities
 * through the registered protocol-specific providers, such as UCP or ACP.
 *
 * Discovery identifies a possible route for obtaining screening information;
 * it does not gather that information or assess compliance with a mandate.
 * If no provider discovers capabilities, the caller can use the normal
 * information-gathering process instead.
 */
@Service
public class MerchantCapabilityDiscoveryService {

    private final List<MerchantCapabilityDiscoveryProvider>
            discoveryProviders;

    /**
     * Receives the discovery providers registered with Spring.
     * Providers are consulted in the order supplied in this list.
     */
    public MerchantCapabilityDiscoveryService(
            List<MerchantCapabilityDiscoveryProvider>
                    discoveryProviders
    ) {
        this.discoveryProviders =
                discoveryProviders;
    }

    /**
     * Asks each provider to discover capabilities for the offer's merchant
     * and collects every available result, preserving each protocol's
     * endpoint and advertised capabilities together.
     *
     * An empty result means no registered provider discovered capabilities;
     * it does not prove that the merchant supports no commerce protocol.
     * Provider failures currently propagate to the caller rather than being
     * treated as an absence of capabilities.
     *
     * @param offer the candidate offer identifying the merchant to inspect
     * @return all discovered protocol results, or an empty list so the caller can
     *         choose its normal information-gathering process
     */
    public List<MerchantCapabilities> discover(
            CandidateOffer offer
    ) {

        List<MerchantCapabilities> discovered = new ArrayList<>();

        for (MerchantCapabilityDiscoveryProvider provider
                : discoveryProviders) {

            Optional<MerchantCapabilities> capabilities =
                    provider.discover(
                            offer
                    );

            capabilities.ifPresent(discovered::add);
        }

        return List.copyOf(discovered);
    }
}
