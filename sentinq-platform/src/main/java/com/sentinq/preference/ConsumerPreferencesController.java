package com.sentinq.preference;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/preferences")
public class ConsumerPreferencesController {

    private final ConsumerPreferencesService
            consumerPreferencesService;

    public ConsumerPreferencesController(
            ConsumerPreferencesService
                    consumerPreferencesService
    ) {
        this.consumerPreferencesService =
                consumerPreferencesService;
    }

    @GetMapping("/{principalId}")
    public ConsumerPreferences getPreferences(
            @PathVariable UUID principalId
    ) {
        return consumerPreferencesService
                .findByPrincipalId(
                        principalId
                );
    }

    /**
     * Persists the latest Consumer Preferences entered
     * through the Command Center.
     */
    @PutMapping("/{principalId}")
    public ConsumerPreferences updatePreferences(
            @PathVariable UUID principalId,
            @RequestBody
            ConsumerPreferences preferences
    ) {
        return consumerPreferencesService.save(
                principalId,
                preferences
        );
    }

    /**
     * Deletes the saved Consumer Preferences for the requested principal.
     */
    @DeleteMapping("/{principalId}")
    public void deletePreferences(
            @PathVariable UUID principalId
    ) {
        consumerPreferencesService
                .deleteByPrincipalId(
                        principalId
                );
    }
}