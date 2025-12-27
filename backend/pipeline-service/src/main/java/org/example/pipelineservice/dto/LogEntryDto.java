package org.example.pipelineservice.dto;

public record LogEntryDto(
        long timestampNs,
        String message
) {
}
