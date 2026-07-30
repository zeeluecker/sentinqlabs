package com.sentinq.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class SystemController {

    @GetMapping("/api/system/health")
    public HealthResponse health() {

        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setService("Sentinq Platform");
        response.setVersion("0.0.1");
        response.setTimestamp(Instant.now());
        return response;
    }
}
