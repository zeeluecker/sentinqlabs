package com.sentinq.mandate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MandateEnvelope {

    private UUID principalId;
    private UUID agentId;
    private UUID delegationId;
    private UUID mandateId;
    private UUID goalId;
    private String consumerId;
    private String objective;

    private Integer maximumTotalCents;
    private LocalDate deliveryDeadline;
    private boolean substitutionsAllowed;

    private List<String> prohibitedMerchants;
    private List<String> preferredMerchants;
    private List<String> preferredMerchantTypes;

    private Integer preferredMinimumFulfillmentScore;
    private Integer preferredMinimumReviewScore;
    private boolean askBeforeUsingNewMerchant;

    public MandateEnvelope() {
        this.mandateId = UUID.randomUUID();
        this.prohibitedMerchants = new ArrayList<>();
        this.preferredMerchants = new ArrayList<>();
        this.preferredMerchantTypes = new ArrayList<>();
    }

    public UUID getMandateId() {
        return mandateId;
    }

    public void setMandateId(UUID mandateId) {
        this.mandateId = mandateId;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public void setGoalId(UUID goalId) {
        this.goalId = goalId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public Integer getMaximumTotalCents() {
        return maximumTotalCents;
    }

    public void setMaximumTotalCents(Integer maximumTotalCents) {
        this.maximumTotalCents = maximumTotalCents;
    }

    public LocalDate getDeliveryDeadline() {
        return deliveryDeadline;
    }

    public void setDeliveryDeadline(LocalDate deliveryDeadline) {
        this.deliveryDeadline = deliveryDeadline;
    }

    public boolean isSubstitutionsAllowed() {
        return substitutionsAllowed;
    }

    public void setSubstitutionsAllowed(boolean substitutionsAllowed) {
        this.substitutionsAllowed = substitutionsAllowed;
    }

    public List<String> getProhibitedMerchants() {
        return prohibitedMerchants;
    }

    public void setProhibitedMerchants(List<String> prohibitedMerchants) {
        this.prohibitedMerchants = prohibitedMerchants;
    }

    public List<String> getPreferredMerchants() {
        return preferredMerchants;
    }

    public void setPreferredMerchants(List<String> preferredMerchants) {
        this.preferredMerchants = preferredMerchants;
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

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getDelegationId() {
        return delegationId;
    }

    public void setDelegationId(UUID delegationId) {
        this.delegationId = delegationId;
    }
    // Generate getters and setters
}