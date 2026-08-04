package com.sentinq.identity;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentIdentityService {

    private static final UUID SHOPPING_AGENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    public AgentIdentity findById(UUID agentId) {
        if (!SHOPPING_AGENT_ID.equals(agentId)) {
            throw new IllegalArgumentException(
                    "Agent not found: " + agentId
            );
        }

        AgentIdentity agent = new AgentIdentity();
        agent.setAgentId(SHOPPING_AGENT_ID);
        agent.setAgentName("Sentinq Shopping Agent");
        agent.setProvider("OpenAI");
        agent.setModel("GPT");
        agent.setActive(true);

        return agent;
    }
}