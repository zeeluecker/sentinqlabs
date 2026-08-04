package com.sentinq.preference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsumerPreferences {

    private UUID principalId;
    private List<String> preferredMerchants;
    private List<String> avoidedMerchants;
    private List<String> preferredMerchantTypes;
    private Integer preferredMinimumFulfillmentScore;
    private Integer preferredMinimumReviewScore;
    private boolean askBeforeUsingNewMerchant;

    public ConsumerPreferences() {
        this.preferredMerchants = new ArrayList<>();
        this.avoidedMerchants = new ArrayList<>();
        this.preferredMerchantTypes = new ArrayList<>();
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public List<String> getPreferredMerchants() {
        return preferredMerchants;
    }

    public void setPreferredMerchants(List<String> preferredMerchants) {
        this.preferredMerchants = preferredMerchants;
    }

    public List<String> getAvoidedMerchants() {
        return avoidedMerchants;
    }

    public void setAvoidedMerchants(List<String> avoidedMerchants) {
        this.avoidedMerchants = avoidedMerchants;
    }

    public List<String> getPreferredMerchantTypes() {
        return preferredMerchantTypes;
    }

    public void setPreferredMerchantTypes(List<String> preferredMerchantTypes) {
        this.preferredMerchantTypes = preferredMerchantTypes;
    }

    public Integer getPreferredMinimumFulfillmentScore() {
        return preferredMinimumFulfillmentScore;
    }

    public void setPreferredMinimumFulfillmentScore(Integer preferredMinimumFulfillmentScore) {
        this.preferredMinimumFulfillmentScore = preferredMinimumFulfillmentScore;
    }

    public Integer getPreferredMinimumReviewScore() {
        return preferredMinimumReviewScore;
    }

    public void setPreferredMinimumReviewScore(Integer preferredMinimumReviewScore) {
        this.preferredMinimumReviewScore = preferredMinimumReviewScore;
    }

    public boolean isAskBeforeUsingNewMerchant() {
        return askBeforeUsingNewMerchant;
    }

    public void setAskBeforeUsingNewMerchant(boolean askBeforeUsingNewMerchant) {
        this.askBeforeUsingNewMerchant = askBeforeUsingNewMerchant;
    }
// Generate getters and setters
}
