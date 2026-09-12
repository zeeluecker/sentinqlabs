package com.sentinq.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class JsonlLlmTraceRecorder
        implements LlmTraceRecorder {

    private static final Path TRACE_FILE =
            Path.of(
                    "traces",
                    "llm-invocations.jsonl"
            );

    private final ObjectMapper objectMapper;

    public JsonlLlmTraceRecorder(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                objectMapper;
    }

    @Override
    public synchronized void record(
            LlmCapabilityTrace trace
    ) {
        try {
            Files.createDirectories(
                    TRACE_FILE.getParent()
            );

            String json =
                    objectMapper.writeValueAsString(
                            trace
                    );

            Files.writeString(
                    TRACE_FILE,
                    json + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (JsonProcessingException e) {

            System.err.println(
                    "Failed to serialize LLM invocation trace: "
                            + e.getMessage()
            );

        } catch (IOException e) {

            System.err.println(
                    "Failed to write LLM invocation trace: "
                            + e.getMessage()
            );
        }
    }
}
