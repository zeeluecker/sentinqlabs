package com.sentinq.evaluation;

import java.util.UUID;

public class LlmCapabilityTrace
{
    private String responseId;
    private String model;
    private String status;

    private Long inputTokens;
    private Long outputTokens;
    private Long reasoningTokens;
    private Long totalTokens;
    private LlmInvocationOutcome outcome;
    private Long durationMs;
    private UUID executionId;
    private UUID capabilityInvocationId;
    private Capability capability;

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Long getReasoningTokens() {
        return reasoningTokens;
    }

    public void setReasoningTokens(Long reasoningTokens) {
        this.reasoningTokens = reasoningTokens;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public LlmInvocationOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(
            LlmInvocationOutcome outcome
    ) {
        this.outcome = outcome;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(
            Long durationMs
    ) {
        this.durationMs =
                durationMs;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public UUID getCapabilityInvocationId() {
        return capabilityInvocationId;
    }

    public void setCapabilityInvocationId(UUID capabilityInvocationId) {
        this.capabilityInvocationId = capabilityInvocationId;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(Capability capability) {
        this.capability = capability;
    }
}
