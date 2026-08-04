package com.sentinq.mandate;

import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.stereotype.Service;
//What does success mean for this user and task?
@Service
public class MandateBuilder {

    public MandateEnvelope build(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        MandateEnvelope mandate = new MandateEnvelope();

        mandate.setGoalId(goal.getGoalId());
        mandate.setPrincipalId(goal.getPrincipalId());
        mandate.setObjective(goal.getProductName());

        mandate.setMaximumTotalCents(goal.getMaximumTotalCents());
        mandate.setDeliveryDeadline(goal.getDeliveryDeadline());
        mandate.setSubstitutionsAllowed(goal.isSubstitutionsAllowed());

        mandate.getProhibitedMerchants()
                .addAll(preferences.getAvoidedMerchants());

        mandate.getPreferredMerchants()
                .addAll(preferences.getPreferredMerchants());

        mandate.getPreferredMerchantTypes()
                .addAll(preferences.getPreferredMerchantTypes());

        mandate.setPreferredMinimumFulfillmentScore(
                preferences.getPreferredMinimumFulfillmentScore()
        );

        mandate.setPreferredMinimumReviewScore(
                preferences.getPreferredMinimumReviewScore()
        );

        mandate.setAskBeforeUsingNewMerchant(
                preferences.isAskBeforeUsingNewMerchant()
        );

        return mandate;
    }
}