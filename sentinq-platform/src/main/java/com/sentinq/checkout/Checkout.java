package com.sentinq.checkout;

import com.sentinq.resolution.ResolvedExecutionFacts;

public class Checkout {

    private String checkoutId;

    private String merchantId;

    private String protocol;

    private String status;

    private ResolvedExecutionFacts resolvedExecutionFacts;


    public String getCheckoutId() {
        return checkoutId;
    }

    public void setCheckoutId(String checkoutId) {
        this.checkoutId = checkoutId;
    }


    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public ResolvedExecutionFacts getResolvedExecutionFacts() {
        return resolvedExecutionFacts;
    }

    public void setResolvedExecutionFacts(
            ResolvedExecutionFacts resolvedExecutionFacts
    ) {
        this.resolvedExecutionFacts =
                resolvedExecutionFacts;
    }
}
