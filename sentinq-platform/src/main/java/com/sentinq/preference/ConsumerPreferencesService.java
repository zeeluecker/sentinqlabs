package com.sentinq.preference;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConsumerPreferencesService {

    private final ConsumerPreferencesRepository repository;

    public ConsumerPreferencesService(
            ConsumerPreferencesRepository repository
    ) {
        this.repository = repository;
    }

    public ConsumerPreferences findByPrincipalId(
            UUID principalId
    ) {
        validatePrincipalId(principalId);

        return repository
                .findByPrincipalId(principalId)
                .orElseGet(() ->
                        createAndSaveDefaultPreferences(
                                principalId
                        )
                );
    }

    public ConsumerPreferences save(
            UUID principalId,
            ConsumerPreferences preferences
    ) {
        validatePrincipalId(principalId);

        if (preferences == null) {
            throw new IllegalArgumentException(
                    "Consumer preferences are required."
            );
        }

        preferences.setPrincipalId(principalId);

        normalizeCollections(preferences);
        validateScores(preferences);

        return repository.save(preferences);
    }

    public void deleteByPrincipalId(
            UUID principalId
    ) {
        validatePrincipalId(principalId);

        repository.deleteByPrincipalId(
                principalId
        );
    }

    private ConsumerPreferences
    createAndSaveDefaultPreferences(
            UUID principalId
    ) {
        ConsumerPreferences preferences =
                new ConsumerPreferences();

        preferences.setPrincipalId(
                principalId
        );

        preferences.setPreferredMerchants(
                new ArrayList<>(
                        List.of(
                                "merchant-specialist-nursery"
                        )
                )
        );

        preferences.setAvoidedMerchants(
                new ArrayList<>(
                        List.of(
                                "merchant-x"
                        )
                )
        );

        preferences.setPreferredMerchantTypes(
                new ArrayList<>(
                        List.of(
                                "SPECIALIST_NURSERY"
                        )
                )
        );

        preferences.setPreferredMinimumFulfillmentScore(
                90
        );

        preferences.setPreferredMinimumReviewScore(
                90
        );

        preferences.setAskBeforeUsingNewMerchant(
                true
        );

        return repository.save(preferences);
    }

    private void normalizeCollections(
            ConsumerPreferences preferences
    ) {
        if (preferences.getPreferredMerchants() == null) {
            preferences.setPreferredMerchants(
                    new ArrayList<>()
            );
        }

        if (preferences.getAvoidedMerchants() == null) {
            preferences.setAvoidedMerchants(
                    new ArrayList<>()
            );
        }

        if (preferences.getPreferredMerchantTypes() == null) {
            preferences.setPreferredMerchantTypes(
                    new ArrayList<>()
            );
        }
    }

    private void validateScores(
            ConsumerPreferences preferences
    ) {
        validateScore(
                "preferredMinimumFulfillmentScore",
                preferences
                        .getPreferredMinimumFulfillmentScore()
        );

        validateScore(
                "preferredMinimumReviewScore",
                preferences
                        .getPreferredMinimumReviewScore()
        );
    }

    private void validateScore(
            String fieldName,
            Integer score
    ) {
        if (score == null) {
            return;
        }

        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between 0 and 100."
            );
        }
    }

    private void validatePrincipalId(
            UUID principalId
    ) {
        if (principalId == null) {
            throw new IllegalArgumentException(
                    "principalId is required."
            );
        }
    }
}