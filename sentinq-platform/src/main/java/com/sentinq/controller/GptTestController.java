package com.sentinq.controller;

import com.sentinq.ai.GptTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class GptTestController {

    private final GptTestService gptTestService;

    public GptTestController(
            GptTestService gptTestService
    ) {
        this.gptTestService = gptTestService;
    }

    @GetMapping("/gpt")
    public String testGpt() {
        return gptTestService.test();
    }
}