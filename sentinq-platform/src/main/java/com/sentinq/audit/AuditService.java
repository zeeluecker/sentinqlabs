package com.sentinq.audit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Comparator;

@Service
public class AuditService {

    private final Map<UUID, ExecutionTrace> traces =
            new ConcurrentHashMap<>();

    public ExecutionTrace startTrace(
            UUID principalId,
            UUID agentId
    ) {
        ExecutionTrace trace =
                new ExecutionTrace();

        trace.setTraceId(
                UUID.randomUUID()
        );

        trace.setPrincipalId(
                principalId
        );

        trace.setAgentId(
                agentId
        );

        trace.setStartedAt(
                Instant.now()
        );

        trace.setEvents(
                new CopyOnWriteArrayList<>()
        );

        traces.put(
                trace.getTraceId(),
                trace
        );

        return trace;
    }

    public void setProviderDetails(
            UUID traceId,
            String provider,
            String model
    ) {
        ExecutionTrace trace =
                getRequiredTrace(traceId);

        trace.setProvider(provider);
        trace.setModel(model);
    }

    public void recordEvent(
            UUID traceId,
            AuditEventType eventType,
            String component,
            String summary,
            Object details
    ) {
        ExecutionTrace trace =
                getRequiredTrace(traceId);

        AuditEvent event =
                new AuditEvent();

        event.setEventId(
                UUID.randomUUID()
        );

        event.setTraceId(
                traceId
        );

        event.setTimestamp(
                Instant.now()
        );

        event.setEventType(
                eventType
        );

        event.setComponent(
                component
        );

        event.setSummary(
                summary
        );

        event.setDetails(
                details
        );

        trace.getEvents()
                .add(event);
    }

    public void completeTrace(
            UUID traceId
    ) {
        getRequiredTrace(traceId)
                .setCompletedAt(
                        Instant.now()
                );
    }

    public List<ExecutionTrace> findAll() {
        return traces.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                ExecutionTrace::getStartedAt
                        ).reversed()
                )
                .toList();
    }

    private ExecutionTrace getRequiredTrace(
            UUID traceId
    ) {
        ExecutionTrace trace =
                traces.get(traceId);

        if (trace == null) {
            throw new IllegalArgumentException(
                    "Execution trace not found: "
                            + traceId
            );
        }

        return trace;
    }

    public ExecutionTrace findById(
            UUID traceId
    ) {
        return getRequiredTrace(traceId);
    }
}
