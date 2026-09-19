package com.sentinq.evaluation;

import java.util.UUID;

public class CapabilityIdentity {

    private UUID executionId;
    private UUID capabilityInvocationId;
    private Capability capability;

    public CapabilityIdentity() {
    }

    public CapabilityIdentity(
            UUID executionId,
            UUID capabilityInvocationId,
            Capability capability
    ) {
        this.executionId =
                executionId;

        this.capabilityInvocationId =
                capabilityInvocationId;

        this.capability =
                capability;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(
            UUID executionId
    ) {
        this.executionId =
                executionId;
    }

    public UUID getCapabilityInvocationId() {
        return capabilityInvocationId;
    }

    public void setCapabilityInvocationId(
            UUID capabilityInvocationId
    ) {
        this.capabilityInvocationId =
                capabilityInvocationId;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(
            Capability capability
    ) {
        this.capability =
                capability;
    }
}