package com.sentinq.evaluation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryTraceCollector
         {

    private final List<LlmCapabilityTrace> traces =
            new CopyOnWriteArrayList<>();



    public List<LlmCapabilityTrace> getTraces() {
        return List.copyOf(
                traces
        );
    }
}