package com.sentinq.checkout;

/** Discovery failed; this must not be interpreted as no protocol support. */
public class MerchantCapabilityDiscoveryException extends RuntimeException {
    public MerchantCapabilityDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
