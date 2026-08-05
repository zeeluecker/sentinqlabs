package com.sentinq.shopping;

import com.sentinq.ai.ProductSearchOffer;
import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Component;


import java.util.UUID;

@Component
public class CandidateOfferFactory {

    public CandidateOffer create(
            ProductSearchOffer searchOffer
    ) {
        CandidateOffer candidate =
                new CandidateOffer();

        candidate.setOfferId(
                UUID.randomUUID().toString()
        );

        candidate.setMerchantId(
                normalizeMerchantId(
                        searchOffer.merchantName
                )
        );

        candidate.setMerchantName(
                searchOffer.merchantName
        );

        candidate.setProductName(
                searchOffer.productName
        );

        candidate.setProductPriceCents(
                searchOffer.productPriceCents
        );

        /*
         * These values are not yet backed by a real reputation service.
         * Keep them neutral rather than pretending search supplied them.
         */
        candidate.setFulfillmentScore(0);
        candidate.setReviewScore(0);

        return candidate;
    }

    private String normalizeMerchantId(
            String merchantName
    ) {
        return merchantName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}