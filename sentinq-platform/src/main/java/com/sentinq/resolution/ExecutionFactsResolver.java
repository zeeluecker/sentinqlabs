package com.sentinq.resolution;

import org.springframework.stereotype.Service;

/**
 * Resolves current execution facts for a candidate offer.
 *
 * For the MVP these values are mocked.
 * Eventually they may be resolved from merchant APIs,
 * checkout sessions, shipping services, inventory systems,
 * tax services, or commerce protocols.
 */
@Service
public class ExecutionFactsResolver {

    public ResolvedExecutionFacts resolve(
            CandidateOffer offer
    ) {

        ResolvedExecutionFacts facts =
                new ResolvedExecutionFacts();

        /*
         * Mock late-binding execution facts for the MVP.
         *
         * These will eventually be replaced by real
         * resolution against merchant and commerce systems.
         */
        facts.setShippingCents(1800);
        facts.setTaxCents(600);
        facts.setInventoryAvailable(true);

        return facts;
    }
}
