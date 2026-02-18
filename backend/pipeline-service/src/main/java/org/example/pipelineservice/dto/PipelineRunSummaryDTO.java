package org.example.pipelineservice.dto;

import lombok.Builder;
import org.example.pipelineservice.model.pipelineRun.PipelineStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Builder
public record PipelineRunSummaryDTO(
        String runId,
        String pipelineId,
        PipelineStatus status,
        @Field("startTime")
        Instant startTime,
        @Field("endTime")
        Instant endTime,
        String runBy
) {
}
