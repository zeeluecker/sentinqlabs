package com.sentinq.preference;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/principals/{principalId}/preferences"
)
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

    @GetMapping
    public ConsumerPreferences getPreferences(
            @PathVariable UUID principalId
    ) {
        return consumerPreferencesService
                .findByPrincipalId(
                        principalId
                );
    }

    @PutMapping
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

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreferences(
            @PathVariable UUID principalId
    ) {
        consumerPreferencesService
                .deleteByPrincipalId(
                        principalId
                );
    }
}