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

        AgentIdentity openAiAgent =
                new AgentIdentity();

        openAiAgent.setAgentId(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222222"
                )
        );

        openAiAgent.setAgentName(
                "Sentinq Shopping Agent (GPT)"
        );

        openAiAgent.setProvider(
                "openai"
        );

        openAiAgent.setModel(
                "gpt-5.2"
        );

        openAiAgent.setActive(true);

        agents.put(
                openAiAgent.getAgentId(),
                openAiAgent
        );

        AgentIdentity claudeAgent =
                new AgentIdentity();

        claudeAgent.setAgentId(
                UUID.fromString(
                        "33333333-3333-3333-3333-333333333333"
                )
        );

        claudeAgent.setAgentName(
                "Sentinq Shopping Agent (Claude)"
        );

        claudeAgent.setProvider(
                "claude"
        );

        claudeAgent.setModel(
                "claude-sonnet-5"
        );

        claudeAgent.setActive(true);

        agents.put(
                claudeAgent.getAgentId(),
                claudeAgent
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