package com.sentinq.identity;

import java.time.Instant;
import java.util.UUID;

public class Principal {

    private UUID principalId;
    private PrincipalType principalType;

    private String displayName;
    private String email;

    private boolean active;
    private Instant createdAt;

    public Principal() {
        this.principalId = UUID.randomUUID();
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(PrincipalType principalType) {
        this.principalType = principalType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}