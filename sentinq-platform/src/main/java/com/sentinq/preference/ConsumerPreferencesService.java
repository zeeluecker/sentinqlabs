package com.sentinq.preference;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConsumerPreferencesService {

    public ConsumerPreferences findByPrincipalId(
            UUID principalId
    ) {
        ConsumerPreferences preferences =
                new ConsumerPreferences();

        preferences.setPrincipalId(principalId);

        preferences.setPreferredMerchants(List.of(
                "merchant-specialist-nursery"
        ));

        preferences.setAvoidedMerchants(List.of(
                "merchant-x"
        ));

        preferences.setPreferredMerchantTypes(List.of(
                "SPECIALIST_NURSERY"
        ));

        preferences.setPreferredMinimumFulfillmentScore(90);
        preferences.setPreferredMinimumReviewScore(90);
        preferences.setAskBeforeUsingNewMerchant(true);

        return preferences;
    }
}