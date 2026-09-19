package com.sentinq.evaluation;

import java.util.EnumMap;
import java.util.Map;

public class ModelCapabilityAssignments {

    private final Map<Capability, ModelAssignment> assignments;

    public ModelCapabilityAssignments(
            Map<Capability, ModelAssignment> assignments
    ) {
        this.assignments = new EnumMap<>(assignments);
    }

    public ModelAssignment assignmentFor(
            Capability capability
    ) {
        ModelAssignment assignment =
                assignments.get(capability);

        if (assignment == null) {
            throw new IllegalStateException(
                    "No model assignment for capability: "
                            + capability
            );
        }

        return assignment;
    }
}