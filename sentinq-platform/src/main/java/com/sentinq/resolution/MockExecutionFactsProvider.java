package com.sentinq.resolution;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MockExecutionFactsProvider
        implements ExecutionFactsProvider {

    @Override
    public String getProviderId() {
        return "mock";
    }

    @Override
    public ResolvedExecutionFacts resolveExecutionFacts(
            CandidateOffer offer
    ) {
        ResolvedExecutionFacts facts =
                new ResolvedExecutionFacts();

        facts.setShippingCents(1800);
        facts.setTaxCents(600);
        facts.setInventoryAvailable(true);

        // Temporary deterministic MVP value.
        facts.setEstimatedDeliveryDate(
                LocalDate.now().plusDays(5)
        );

        return facts;
    }
}
