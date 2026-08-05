package com.sentinq.preference;

import java.util.Optional;
import java.util.UUID;

public interface ConsumerPreferencesRepository {

    Optional<ConsumerPreferences> findByPrincipalId(
            UUID principalId
    );

    ConsumerPreferences save(
            ConsumerPreferences preferences
    );

    void deleteByPrincipalId(
            UUID principalId
    );
}