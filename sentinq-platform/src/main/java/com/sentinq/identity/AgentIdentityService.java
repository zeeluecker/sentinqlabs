package com.sentinq.identity;

import com.sentinq.ai.provider.ClaudeProvider;
import com.sentinq.ai.provider.GeminiProvider;
import com.sentinq.ai.provider.OpenAiProvider;
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
                OpenAiProvider.MODEL
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
                ClaudeProvider.MODEL
        );

        claudeAgent.setActive(true);


        agents.put(
                claudeAgent.getAgentId(),
                claudeAgent
        );

        AgentIdentity geminiAgent =
                new AgentIdentity();

        geminiAgent.setAgentId(
                UUID.fromString(
                        "44444444-4444-4444-4444-444444444444"
                )
        );

        geminiAgent.setAgentName(
                "Sentinq Shopping Agent (Gemini)"
        );

        geminiAgent.setProvider(
                "gemini"
        );

        geminiAgent.setModel(
                GeminiProvider.MODEL
        );

        geminiAgent.setActive(
                true
        );

        agents.put(
                geminiAgent.getAgentId(),
                geminiAgent
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