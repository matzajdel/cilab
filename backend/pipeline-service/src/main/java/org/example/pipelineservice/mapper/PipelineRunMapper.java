package org.example.pipelineservice.mapper;

import org.example.pipelineservice.dto.PipelineRunSummaryDTO;
import org.example.pipelineservice.model.pipelineRun.PipelineRun;
import org.springframework.stereotype.Service;

@Service
public class PipelineRunMapper {
    public PipelineRunSummaryDTO toSummaryDTO(PipelineRun entity) {
        return PipelineRunSummaryDTO.builder()
                .runId(entity.getRunId())
                .pipelineId(entity.getPipelineId())
                .status(entity.getStatus())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .runBy(entity.getRunBy())
                .build();
    }
}
