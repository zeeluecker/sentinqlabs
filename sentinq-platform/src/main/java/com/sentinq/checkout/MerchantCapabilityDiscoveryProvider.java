package com.sentinq.checkout;

import com.sentinq.resolution.CandidateOffer;

import java.util.Optional;

/**
 * Discovers merchant commerce capabilities and
 * normalizes them into Sentinq terminology.
 *
 * Protocol-specific discovery details must not
 * escape this boundary.
 */
public interface MerchantCapabilityDiscoveryProvider {

    CommerceProtocolType getProtocolType();

    Optional<MerchantCapabilities> discover(
            CandidateOffer offer
    );
}