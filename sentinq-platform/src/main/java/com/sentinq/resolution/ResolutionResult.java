package com.sentinq.resolution;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ResolutionResult {

    private String offerId;
    private Integer productPriceCents;
    private Integer shippingCents;
    private Integer taxCents;
    private Integer resolvedTotalCents;
    private LocalDate estimatedDeliveryDate;
    private boolean inventoryAvailable;
    private boolean executable;
    private List<String> violations;
    private List<String> warnings;

    public ResolutionResult() {
        this.violations = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public Integer getProductPriceCents() {
        return productPriceCents;
    }

    public void setProductPriceCents(Integer productPriceCents) {
        this.productPriceCents = productPriceCents;
    }

    public Integer getShippingCents() {
        return shippingCents;
    }

    public void setShippingCents(Integer shippingCents) {
        this.shippingCents = shippingCents;
    }

    public Integer getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(Integer taxCents) {
        this.taxCents = taxCents;
    }

    public Integer getResolvedTotalCents() {
        return resolvedTotalCents;
    }

    public void setResolvedTotalCents(Integer resolvedTotalCents) {
        this.resolvedTotalCents = resolvedTotalCents;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public boolean isInventoryAvailable() {
        return inventoryAvailable;
    }

    public void setInventoryAvailable(boolean inventoryAvailable) {
        this.inventoryAvailable = inventoryAvailable;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

// Generate getters and setters
}