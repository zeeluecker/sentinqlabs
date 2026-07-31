package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping")
public class ShoppingController {

    private final ShoppingOrchestrationService orchestrationService;

    public ShoppingController(
            ShoppingOrchestrationService orchestrationService
    ) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/orchestrate")
    public ShoppingOrchestrationResult orchestrate(
            @RequestBody ShoppingOrchestrationRequest request
    ) {
        return orchestrationService.orchestrate(
                request.goal(),
                request.preferences()
        );
    }
}