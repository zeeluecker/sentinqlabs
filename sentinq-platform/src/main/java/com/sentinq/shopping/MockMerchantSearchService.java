package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.resolution.CandidateOffer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockMerchantSearchService {
//This will have a list of all merchants that offer the services for the goal
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

        CandidateOffer offerThree = new CandidateOffer();
        offerThree.setOfferId("offer-003");
        offerThree.setMerchantId("fast-growing-trees");
        offerThree.setMerchantName("Fast Growing Trees");
        offerThree.setProductName(goal.getProductName());
        offerThree.setProductPriceCents(5700);
        offerThree.setFulfillmentScore(98);
        offerThree.setReviewScore(98);

        return List.of(offerOne, offerTwo, offerThree);
    }
}