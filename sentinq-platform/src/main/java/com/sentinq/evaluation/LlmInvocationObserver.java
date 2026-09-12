package com.sentinq.evaluation;

import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class LlmInvocationObserver {

    private final LlmTraceRecorder llmTraceRecorder;

    public LlmInvocationObserver(
            LlmTraceRecorder llmTraceRecorder
    ) {
        this.llmTraceRecorder =
                llmTraceRecorder;
    }

    public <T> T execute(
            Supplier<LlmResult<T>> invocation
    ) {
        try {
            LlmResult<T> result =
                    invocation.get();

            llmTraceRecorder.record(
                    result.getTrace()
            );

            return result.getResult();

        } catch (LlmInvocationException e) {

            llmTraceRecorder.record(
                    e.getTrace()
            );

            throw e;
        }
    }
}
