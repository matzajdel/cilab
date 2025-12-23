package org.example.pipelineservice.mapper;

import org.example.pipelineservice.dto.PipelineRequestDTO;
import org.example.pipelineservice.dto.PipelineResponseDTO;
import org.example.pipelineservice.model.pipeline.Pipeline;
import org.springframework.stereotype.Service;

@Service
public class PipelineMapper {
    public Pipeline toModel (PipelineRequestDTO request) {
        return Pipeline.builder()
                .name(request.getName())
                .authorEmail(request.getAuthorEmail())
                .parameters(request.getParameters())
                .envVariables(request.getEnvVariables())
                .stages(request.getStages())
                .labels(request.getLabels())
                .build();
    }

    public PipelineResponseDTO toDTO (Pipeline pipeline) {
        return PipelineResponseDTO.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .authorEmail(pipeline.getAuthorEmail())
                .parameters(pipeline.getParameters())
                .envVariables(pipeline.getEnvVariables())
                .stages(pipeline.getStages())
                .build();
    }
}
