package com.sentinq.evaluation;

public class LlmResult<T> {

    private T result;
    private LlmCapabilityTrace trace;

    public LlmResult(T result, LlmCapabilityTrace trace) {
        this.result = result;
        this.trace = trace;
    }

    public T getResult() {
        return result;
    }

    public LlmCapabilityTrace getTrace() {
        return trace;
    }
}
