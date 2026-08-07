
package com.sentinq.audit;

import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.mandate.MandateEnvelope;
import com.sentinq.preference.ConsumerPreferences;

import java.time.Instant;
import java.util.UUID;

public class AuditRecord {

    private UUID auditId;
    private Instant timestamp;
    private UUID principalId;
    private UUID agentId;
    private String provider;
    private String model;
    private String rawGoal;
    private InterpretedShoppingGoal interpretedGoal;
    private ConsumerPreferences consumerPreferences;
    private MandateEnvelope mandate;
    private String providerSearchRequest;
    private ProductSearchResult searchResult;
    private Object resolutionOutcome;

    public Object getResolutionOutcome() {
        return resolutionOutcome;
    }

    public void setResolutionOutcome(Object resolutionOutcome) {
        this.resolutionOutcome = resolutionOutcome;
    }

    public ProductSearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(ProductSearchResult searchResult) {
        this.searchResult = searchResult;
    }

    public String getProviderSearchRequest() {
        return providerSearchRequest;
    }

    public void setProviderSearchRequest(String providerSearchRequest) {
        this.providerSearchRequest = providerSearchRequest;
    }

    public MandateEnvelope getMandate() {
        return mandate;
    }

    public void setMandate(MandateEnvelope mandate) {
        this.mandate = mandate;
    }

    public ConsumerPreferences getConsumerPreferences() {
        return consumerPreferences;
    }

    public void setConsumerPreferences(ConsumerPreferences consumerPreferences) {
        this.consumerPreferences = consumerPreferences;
    }

    public InterpretedShoppingGoal getInterpretedGoal() {
        return interpretedGoal;
    }

    public void setInterpretedGoal(InterpretedShoppingGoal interpretedGoal) {
        this.interpretedGoal = interpretedGoal;
    }

    public String getRawGoal() {
        return rawGoal;
    }

    public void setRawGoal(String rawGoal) {
        this.rawGoal = rawGoal;
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

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getAuditId() {
        return auditId;
    }

    public void setAuditId(UUID auditId) {
        this.auditId = auditId;
    }

    // getters and setters
}
