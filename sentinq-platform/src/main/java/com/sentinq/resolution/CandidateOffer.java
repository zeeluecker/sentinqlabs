package com.sentinq.resolution;

public class CandidateOffer {

    private String offerId;
    private String merchantId;
    private String merchantName;
    private String productName;

    /*
     * Product information established during discovery
     * and preserved for downstream goal-fit reasoning.
     */
    private String productDescription;

    /*
     * Discovery-stage explanation of why this offer
     * appeared relevant to the consumer's goal.
     *
     * This is a discovery signal, not a final
     * recommendation.
     */
    private String discoveryMatchReason;

    private Integer productPriceCents;
    private Integer fulfillmentScore;
    private Integer reviewScore;

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getProductPriceCents() {
        return productPriceCents;
    }

    public void setProductPriceCents(Integer productPriceCents) {
        this.productPriceCents = productPriceCents;
    }

    public Integer getFulfillmentScore() {
        return fulfillmentScore;
    }

    public void setFulfillmentScore(Integer fulfillmentScore) {
        this.fulfillmentScore = fulfillmentScore;
    }

    public Integer getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(Integer reviewScore) {
        this.reviewScore = reviewScore;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getDiscoveryMatchReason() {
        return discoveryMatchReason;
    }

    public void setDiscoveryMatchReason(String discoveryMatchReason) {
        this.discoveryMatchReason = discoveryMatchReason;
    }
}