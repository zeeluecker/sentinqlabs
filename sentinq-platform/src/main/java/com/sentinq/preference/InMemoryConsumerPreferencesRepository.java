package com.sentinq.preference;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryConsumerPreferencesRepository
        implements ConsumerPreferencesRepository {

    private final Map<UUID, ConsumerPreferences>
            preferencesByPrincipalId =
            new ConcurrentHashMap<>();

    @Override
    public Optional<ConsumerPreferences> findByPrincipalId(
            UUID principalId
    ) {
        return Optional.ofNullable(
                preferencesByPrincipalId.get(principalId)
        );
    }

    @Override
    public ConsumerPreferences save(
            ConsumerPreferences preferences
    ) {
        UUID principalId =
                preferences.getPrincipalId();

        if (principalId == null) {
            throw new IllegalArgumentException(
                    "Cannot save preferences without a principalId."
            );
        }

        preferencesByPrincipalId.put(
                principalId,
                preferences
        );

        return preferences;
    }

    @Override
    public void deleteByPrincipalId(
            UUID principalId
    ) {
        preferencesByPrincipalId.remove(
                principalId
        );
    }
}