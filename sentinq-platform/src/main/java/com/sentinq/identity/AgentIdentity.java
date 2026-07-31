package com.sentinq.identity;

import java.time.Instant;
import java.util.UUID;

public class AgentIdentity {

    private UUID agentId;
    private String agentName;
    private String provider;
    private String model;
    private boolean active;
    private Instant createdAt;

 /*
 agentName = Shopping Agent
 provider = OpenAI
 model = GPT
  */
    public AgentIdentity() {
        this.agentId = UUID.randomUUID();
        this.active = true;
        this.createdAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }
// Generate getters and setters
}