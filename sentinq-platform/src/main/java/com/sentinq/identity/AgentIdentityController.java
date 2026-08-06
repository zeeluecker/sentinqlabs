package com.sentinq.identity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentIdentityController {

    private final AgentIdentityService agentIdentityService;

    public AgentIdentityController(
            AgentIdentityService agentIdentityService
    ) {
        this.agentIdentityService =
                agentIdentityService;
    }

    @GetMapping
    public List<AgentIdentity> getActiveAgents() {
        return agentIdentityService.findAllActive();
    }
}
