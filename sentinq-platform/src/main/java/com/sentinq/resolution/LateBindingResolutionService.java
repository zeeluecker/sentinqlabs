package com.sentinq.resolution;

import com.sentinq.mandate.MandateEnvelope;
import org.springframework.stereotype.Service;
//Does this specific offer satisfy that definition of success?
@Service
public class LateBindingResolutionService {

    public ResolutionResult resolve(
            CandidateOffer offer,
            MandateEnvelope mandate
    ) {
        ResolutionResult result = new ResolutionResult();

        result.setOfferId(offer.getOfferId());
        result.setProductPriceCents(offer.getProductPriceCents());

        // Mock values for the first MVP.
        int shippingCents = 1800;
        int taxCents = 600;
        int resolvedTotalCents =
                offer.getProductPriceCents() + shippingCents + taxCents;

        result.setShippingCents(shippingCents);
        result.setTaxCents(taxCents);
        result.setResolvedTotalCents(resolvedTotalCents);
        result.setInventoryAvailable(true);
        result.setEstimatedDeliveryDate(
                mandate.getDeliveryDeadline().plusDays(2)
        );

        if (mandate.getProhibitedMerchants()
                .contains(offer.getMerchantId())) {
            result.getViolations().add("Merchant is prohibited");
        }

        if (resolvedTotalCents > mandate.getMaximumTotalCents()) {
            result.getViolations().add("Resolved total exceeds budget");
        }

        if (result.getEstimatedDeliveryDate()
                .isAfter(mandate.getDeliveryDeadline())) {
            result.getViolations().add("Delivery misses deadline");
        }

        if (!result.isInventoryAvailable()) {
            result.getViolations().add("Product is unavailable");
        }

        if (offer.getFulfillmentScore()
                < mandate.getPreferredMinimumFulfillmentScore()) {
            result.getWarnings().add(
                    "Merchant is below preferred fulfillment score"
            );
        }

        result.setExecutable(result.getViolations().isEmpty());

        return result;
    }
}