package com.sentinq.checkout;


public class MerchantCapabilities {

    private String merchantId;

    private CommerceProtocolType protocol;

    private String endpoint;

    private boolean checkoutInitiationSupported;

    private boolean checkoutExecutionSupported;


    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }


    public CommerceProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(CommerceProtocolType protocol) {
        this.protocol = protocol;
    }


    public boolean isCheckoutInitiationSupported() {
        return checkoutInitiationSupported;
    }

    public void setCheckoutInitiationSupported(
            boolean checkoutInitiationSupported
    ) {
        this.checkoutInitiationSupported =
                checkoutInitiationSupported;
    }


    public boolean isCheckoutExecutionSupported() {
        return checkoutExecutionSupported;
    }

    public void setCheckoutExecutionSupported(
            boolean checkoutExecutionSupported
    ) {
        this.checkoutExecutionSupported =
                checkoutExecutionSupported;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
