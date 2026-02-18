package org.example.pipelineservice.dto;

import java.time.Instant;

public record PipelineSummaryDTO(
        String id,
        String name,
        String authorEmail,
        Instant lastUpdated
) { }
