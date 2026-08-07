package com.sentinq.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit/traces")
public class AuditController {

    private final AuditService auditService;

    public AuditController(
            AuditService auditService
    ) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<ExecutionTrace> getAllTraces() {
        return auditService.findAll();
    }

    @GetMapping("/{traceId}")
    public ExecutionTrace getTraceById(
            @PathVariable UUID traceId
    ) {
        return auditService.findById(
                traceId
        );
    }
}
