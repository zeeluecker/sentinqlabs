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
            CapabilityIdentity capabilityIdentity,
            Supplier<LlmResult<T>> invocation
    ) {
        long startedAtNanos =
                System.nanoTime();

        try {
            LlmResult<T> result =
                    invocation.get();

            long durationMs =
                    (System.nanoTime() - startedAtNanos)
                            / 1_000_000;

            LlmCapabilityTrace trace =
                    result.getTrace();

            trace.setExecutionId(
                    capabilityIdentity.getExecutionId()
            );

            trace.setCapabilityInvocationId(
                    capabilityIdentity.getCapabilityInvocationId()
            );

            trace.setCapability(
                    capabilityIdentity.getCapability()
            );

            trace.setDurationMs(
                    durationMs
            );

            llmTraceRecorder.record(
                    trace
            );

            return result.getResult();

        } catch (LlmInvocationException e) {

            long durationMs =
                    (System.nanoTime() - startedAtNanos)
                            / 1_000_000;

            LlmCapabilityTrace trace =
                    e.getTrace();

            trace.setExecutionId(
                    capabilityIdentity.getExecutionId()
            );

            trace.setCapabilityInvocationId(
                    capabilityIdentity.getCapabilityInvocationId()
            );

            trace.setCapability(
                    capabilityIdentity.getCapability()
            );

            trace.setDurationMs(
                    durationMs
            );

            llmTraceRecorder.record(
                    trace
            );

            throw e;
        }
    }
}