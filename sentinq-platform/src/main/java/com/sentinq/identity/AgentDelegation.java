package com.sentinq.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentDelegation {

    private UUID delegationId;
    private UUID principalId;
    private UUID agentId;

    private List<String> permittedActions;
    private List<String> prohibitedActions;

    private boolean approvalRequiredForExecution;
    private boolean active;

    private Instant createdAt;
    private Instant expiresAt;

    public AgentDelegation() {
        this.delegationId = UUID.randomUUID();
        this.permittedActions = new ArrayList<>();
        this.prohibitedActions = new ArrayList<>();
        this.approvalRequiredForExecution = true;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getDelegationId() {
        return delegationId;
    }

    public void setDelegationId(UUID delegationId) {
        this.delegationId = delegationId;
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

    public List<String> getPermittedActions() {
        return permittedActions;
    }

    public void setPermittedActions(List<String> permittedActions) {
        this.permittedActions = permittedActions;
    }

    public List<String> getProhibitedActions() {
        return prohibitedActions;
    }

    public void setProhibitedActions(List<String> prohibitedActions) {
        this.prohibitedActions = prohibitedActions;
    }

    public boolean isApprovalRequiredForExecution() {
        return approvalRequiredForExecution;
    }

    public void setApprovalRequiredForExecution(boolean approvalRequiredForExecution) {
        this.approvalRequiredForExecution = approvalRequiredForExecution;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
// Generate getters and setters
}