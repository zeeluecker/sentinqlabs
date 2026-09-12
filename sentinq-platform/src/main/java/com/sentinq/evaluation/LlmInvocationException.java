package com.sentinq.evaluation;

public class LlmInvocationException
        extends RuntimeException {

    private final LlmCapabilityTrace trace;

    public LlmInvocationException(
            String message,
            Throwable cause,
            LlmCapabilityTrace trace
    ) {
        super(
                message,
                cause
        );

        this.trace = trace;
    }

    public LlmCapabilityTrace getTrace() {
        return trace;
    }
}
