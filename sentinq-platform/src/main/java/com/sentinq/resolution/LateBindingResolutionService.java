package com.sentinq.resolution;

import com.sentinq.mandate.MandateEnvelope;
import org.springframework.stereotype.Service;
//Does this specific offer satisfy that definition of success?
@Service
public class LateBindingResolutionService {

    /**
     * Resolves a candidate offer against the active Mandate Envelope.
     *
     * The resolution layer combines current execution facts with
     * consumer-defined constraints and preferences to determine
     * whether the candidate can proceed.
     */
    public ResolutionResult resolve(
            CandidateOffer offer,
            MandateEnvelope mandate,
            ResolvedExecutionFacts facts
    ) {
        ResolutionResult result =
                new ResolutionResult();

        result.setOfferId(
                offer.getOfferId()
        );

        result.setProductPriceCents(
                offer.getProductPriceCents()
        );

        Integer shippingCents =
                facts.getShippingCents();

        Integer taxCents =
                facts.getTaxCents();

        int resolvedTotalCents =
                offer.getProductPriceCents()
                        + (shippingCents != null
                        ? shippingCents
                        : 0)
                        + (taxCents != null
                        ? taxCents
                        : 0);

        result.setShippingCents(
                shippingCents
        );

        result.setTaxCents(
                taxCents
        );

        result.setResolvedTotalCents(
                resolvedTotalCents
        );

        result.setInventoryAvailable(
                Boolean.TRUE.equals(
                        facts.getInventoryAvailable()
                )
        );

        result.setEstimatedDeliveryDate(
                facts.getEstimatedDeliveryDate()
        );

        /*
         * Enforce hard merchant restrictions carried
         * by the consumer's governed mandate.
         */
        if (mandate.getProhibitedMerchants() != null &&
                mandate.getProhibitedMerchants()
                        .contains(
                                offer.getMerchantId()
                        )) {

            result.getViolations().add(
                    "Merchant is prohibited"
            );
        }

        /*
         * Enforce the consumer's maximum total budget
         * when a budget constraint exists.
         */
        Integer maximumTotalCents =
                mandate.getMaximumTotalCents();

        if (maximumTotalCents != null &&
                resolvedTotalCents >
                        maximumTotalCents) {

            result.getViolations().add(
                    "Resolved total exceeds budget"
            );
        }

        /*
         * Enforce the required delivery deadline when
         * the consumer has specified one.
         */
        if (mandate.getDeliveryDeadline() != null &&
                result.getEstimatedDeliveryDate() != null &&
                result.getEstimatedDeliveryDate()
                        .isAfter(
                                mandate.getDeliveryDeadline()
                        )) {

            result.getViolations().add(
                    "Delivery misses deadline"
            );
        }

        /*
         * Inventory availability is a hard execution
         * constraint and therefore blocks the candidate.
         */
        if (!result.isInventoryAvailable()) {
            result.getViolations().add(
                    "Product is unavailable"
            );
        }

        /*
         * Evaluate fulfillment quality only when the
         * consumer has configured a preferred minimum.
         *
         * This is currently treated as a warning rather
         * than a hard execution violation.
         */
        Integer preferredMinimumFulfillmentScore =
                mandate.getPreferredMinimumFulfillmentScore();

        if (preferredMinimumFulfillmentScore != null &&
                offer.getFulfillmentScore() != null &&
                offer.getFulfillmentScore() <
                        preferredMinimumFulfillmentScore) {

            result.getWarnings().add(
                    "Merchant is below preferred fulfillment score"
            );
        }

        /*
         * Evaluate merchant review quality only when the
         * consumer has configured a preferred minimum.
         *
         * Like fulfillment score, this is currently a
         * preference warning rather than a hard constraint.
         */
        Integer preferredMinimumReviewScore =
                mandate.getPreferredMinimumReviewScore();

        if (preferredMinimumReviewScore != null &&
                offer.getReviewScore() != null &&
                offer.getReviewScore() <
                        preferredMinimumReviewScore) {

            result.getWarnings().add(
                    "Merchant is below preferred review score"
            );
        }

        /*
         * A candidate is executable only when no hard
         * mandate violations remain after resolution.
         */
        result.setExecutable(
                result.getViolations().isEmpty()
        );

        return result;
    }
}