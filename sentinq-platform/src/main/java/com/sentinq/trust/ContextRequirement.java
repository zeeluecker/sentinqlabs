package com.sentinq.trust;

// ContextRequirement.java

public record ContextRequirement(
        ContextType type,
        String question,
        boolean required
) {
}
