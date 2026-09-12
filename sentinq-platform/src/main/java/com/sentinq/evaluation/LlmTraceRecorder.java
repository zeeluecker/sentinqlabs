package com.sentinq.evaluation;
import com.sentinq.evaluation.LlmCapabilityTrace;

public interface LlmTraceRecorder {

    void record(
            LlmCapabilityTrace trace
    );
}
