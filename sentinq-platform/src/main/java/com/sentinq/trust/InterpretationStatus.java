package com.sentinq.trust;

// InterpretationStatus.java

public enum InterpretationStatus {
    DIRECT_SIGNAL,
    CONTEXT_REQUIRED,
    CONTEXT_RESOLVED,
    AMBIGUOUS,
    MISLEADING_WITHOUT_CONTEXT,
    INSUFFICIENT_EVIDENCE,
    NOT_RELEVANT
}
