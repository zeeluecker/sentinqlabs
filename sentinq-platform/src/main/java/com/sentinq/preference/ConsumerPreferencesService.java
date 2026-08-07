package com.sentinq.preference;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and retrieves consumer preferences used to govern
 * Sentinq orchestration decisions.
 *
 * Preferences are persisted in memory for the current MVP.
 * The storage implementation can later be replaced by a
 * database-backed repository without changing orchestration.
 */
@Service
public class ConsumerPreferencesService {

    private final Map<UUID, ConsumerPreferences> preferencesByPrincipal =
            new ConcurrentHashMap<>();

    /**
     * Returns the saved preferences for the specified principal.
     *
     * If no preferences have been saved yet, a default preference
     * profile is created and stored for that principal.
     */
    public ConsumerPreferences findByPrincipalId(
            UUID principalId
    ) {
        return preferencesByPrincipal.computeIfAbsent(
                principalId,
                this::createDefaultPreferences
        );
    }

    /**
     * Saves or replaces the current preference profile for a
     * principal so future orchestrations use the updated values.
     */
    public ConsumerPreferences save(
            UUID principalId,
            ConsumerPreferences preferences
    ) {
        preferencesByPrincipal.put(
                principalId,
                preferences
        );

        return preferences;
    }

    /**
     * Creates the initial preference profile used when a principal
     * has not configured merchant preferences yet.
     */
    private ConsumerPreferences createDefaultPreferences(
            UUID principalId
    ) {
        ConsumerPreferences preferences =
                new ConsumerPreferences();

        preferences.setPrincipalId(
                principalId
        );

        return preferences;
    }

    /**
     * Deletes the saved preference profile for the specified principal.
     *
     * After deletion, the next preference lookup will recreate
     * the principal's default in-memory preference profile.
     */
    public void deleteByPrincipalId(
            UUID principalId
    ) {
        preferencesByPrincipal.remove(
                principalId
        );
    }
}