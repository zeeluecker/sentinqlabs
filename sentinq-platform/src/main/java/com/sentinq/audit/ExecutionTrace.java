package com.sentinq.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExecutionTrace {

    private UUID traceId;
    private UUID principalId;
    private UUID agentId;
    private String provider;
    private String model;
    private Instant startedAt;
    private Instant completedAt;
    private List<AuditEvent> events;

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<AuditEvent> getEvents() {
        return events;
    }

    public void setEvents(List<AuditEvent> events) {
        this.events = events;
    }

    // getters and setters
}
