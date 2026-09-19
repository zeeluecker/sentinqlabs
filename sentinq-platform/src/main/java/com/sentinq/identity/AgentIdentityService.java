package com.sentinq.identity;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Service
public class AgentIdentityService {

    private final Map<UUID, AgentIdentity> agents =
            new HashMap<>();

    @PostConstruct
    public void initialize() {

        AgentIdentity shoppingAgent =
                new AgentIdentity();

        shoppingAgent.setAgentId(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222222"
                )
        );

        shoppingAgent.setAgentName(
                "Sentinq Shopping Agent"
        );

        shoppingAgent.setActive(
                true
        );

        agents.put(
                shoppingAgent.getAgentId(),
                shoppingAgent
        );
    }

    public AgentIdentity findById(
            UUID agentId
    ) {

        AgentIdentity agent =
                agents.get(agentId);

        if (agent == null) {
            throw new IllegalArgumentException(
                    "Agent not found: " + agentId
            );
        }

        return agent;
    }

    public List<AgentIdentity> findAllActive() {
        return agents.values()
                .stream()
                .filter(AgentIdentity::isActive)
                .toList();
    }
}