package com.sentinq.checkout;


import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MerchantCapabilityDiscoveryService {

    private final List<MerchantCapabilityDiscoveryProvider>
            discoveryProviders;

    public MerchantCapabilityDiscoveryService(
            List<MerchantCapabilityDiscoveryProvider>
                    discoveryProviders
    ) {
        this.discoveryProviders =
                discoveryProviders;
    }

    public MerchantCapabilities discover(
            CandidateOffer offer
    ) {

        for (MerchantCapabilityDiscoveryProvider provider
                : discoveryProviders) {

            Optional<MerchantCapabilities> capabilities =
                    provider.discover(
                            offer
                    );

            if (capabilities.isPresent()) {
                return capabilities.get();
            }
        }

        throw new IllegalStateException(
                "No supported commerce protocol discovered for merchant: "
                        + offer.getMerchantId()
        );
    }
}
