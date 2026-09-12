package com.sentinq.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class CapabilityTraceWriter {

    private static final Path TRACE_FILE =
            Path.of("traces", "sentinq-orchestration.jsonl");

    private final ObjectMapper objectMapper;

    public CapabilityTraceWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(CapabilityTrace trace) {
        try {
            Files.createDirectories(
                    TRACE_FILE.getParent()
            );

            String json =
                    objectMapper.writeValueAsString(trace);

            Files.writeString(
                    TRACE_FILE,
                    json + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize capability trace.",
                    e
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to write capability trace.",
                    e
            );
        }
    }
}