package com.sentinq.goal;

import java.time.LocalDate;
import java.util.UUID;

public class Goal {

    private UUID goalId;
    private UUID principalId;
    private String originalRequest;
    private String productName;
    private Integer maximumTotalCents;
    private LocalDate deliveryDeadline;
    private boolean substitutionsAllowed;

    public Goal() {
        this.goalId = UUID.randomUUID();
    }

    public UUID getGoalId() {
        return goalId;
    }

    public void setGoalId(UUID goalId) {
        this.goalId = goalId;
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public String getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(String originalRequest) {
        this.originalRequest = originalRequest;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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
    // Generate getters and setters
}
