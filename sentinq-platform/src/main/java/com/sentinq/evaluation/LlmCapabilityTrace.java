package com.sentinq.evaluation;

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
}
