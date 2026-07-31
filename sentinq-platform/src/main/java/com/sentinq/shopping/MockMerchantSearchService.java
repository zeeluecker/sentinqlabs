package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockMerchantSearchService {

    public List<CandidateOffer> search(Goal goal) {
        CandidateOffer offerOne = new CandidateOffer();
        offerOne.setOfferId("offer-001");
        offerOne.setMerchantId("merchant-david-austin");
        offerOne.setMerchantName("David Austin Roses");
        offerOne.setProductName(goal.getProductName());
        offerOne.setProductPriceCents(3200);
        offerOne.setFulfillmentScore(95);
        offerOne.setReviewScore(96);

        CandidateOffer offerTwo = new CandidateOffer();
        offerTwo.setOfferId("offer-002");
        offerTwo.setMerchantId("merchant-specialist-nursery");
        offerTwo.setMerchantName("Specialist Dahlia Nursery");
        offerTwo.setProductName(goal.getProductName());
        offerTwo.setProductPriceCents(2800);
        offerTwo.setFulfillmentScore(88);
        offerTwo.setReviewScore(94);

        return List.of(offerOne, offerTwo);
    }
}