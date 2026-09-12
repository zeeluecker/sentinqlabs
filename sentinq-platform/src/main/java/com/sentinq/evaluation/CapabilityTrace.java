package com.sentinq.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CapabilityTrace {

    private UUID capabilityTraceId;
    private UUID executionTraceId;
    private String capability;

    private Instant startedAt;
    private Instant completedAt;
    private Long latencyMs;

    private String status;

    private List<LlmCapabilityTrace> llmTraces;

    public UUID getCapabilityTraceId() {
        return capabilityTraceId;
    }

    public void setCapabilityTraceId(UUID capabilityTraceId) {
        this.capabilityTraceId = capabilityTraceId;
    }

    public UUID getExecutionTraceId() {
        return executionTraceId;
    }

    public void setExecutionTraceId(UUID executionTraceId) {
        this.executionTraceId = executionTraceId;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
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

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LlmCapabilityTrace> getLlmTraces() {
        return llmTraces;
    }

    public void setLlmTraces(List<LlmCapabilityTrace> llmTraces) {
        this.llmTraces = llmTraces;
    }
}