package com.sentinq.trust;
// ContextFinding.java

public record ContextFinding(
        ContextType type,
        String finding,
        String sourceEvidenceId,
        double confidence
) {
}
