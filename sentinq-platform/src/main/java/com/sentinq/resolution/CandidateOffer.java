package com.sentinq.resolution;

public class CandidateOffer {

    private String offerId;
    private String merchantId;
    private String merchantName;
    private String productName;
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
// Generate getters and setters
}