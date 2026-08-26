package com.sentinq.resolution;

import java.time.LocalDate;

/**
 * Represents execution facts that become known
 * as an agent moves closer to transaction execution.
 *
 * These facts may eventually come from merchant APIs,
 * checkout sessions, inventory systems, shipping services,
 * tax calculations, or commerce protocols.
 * What we learned when we got closer to actually buying it.
 *
 * LBRL does not determine these facts.
 * It evaluates what they mean against the active mandate.
 */
public class ResolvedExecutionFacts {

    private Integer shippingCents;

    private Integer taxCents;

    private Boolean inventoryAvailable;

    private LocalDate estimatedDeliveryDate;


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


    public Boolean getInventoryAvailable() {
        return inventoryAvailable;
    }

    public void setInventoryAvailable(Boolean inventoryAvailable) {
        this.inventoryAvailable = inventoryAvailable;
    }


    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(
            LocalDate estimatedDeliveryDate
    ) {
        this.estimatedDeliveryDate =
                estimatedDeliveryDate;
    }
}
