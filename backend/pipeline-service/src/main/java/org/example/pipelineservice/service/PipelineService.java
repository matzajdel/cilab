package org.example.pipelineservice.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.example.pipelineservice.dto.PipelineRequestDTO;
import org.example.pipelineservice.dto.PipelineResponseDTO;
import org.example.pipelineservice.exception.PipelineNotFoundException;
import org.example.pipelineservice.mapper.PipelineMapper;
import org.example.pipelineservice.model.pipeline.Pipeline;
import org.example.pipelineservice.repository.PipelineRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PipelineService {
    private final PipelineRepository pipelineRepository;
    private final PipelineMapper pipelineMapper;

    public Pipeline getPipelineById(String id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new PipelineNotFoundException("Pipeline not found with id: " + id));
    }

    public PipelineResponseDTO createPipeline(PipelineRequestDTO request) {
        Pipeline newPipeline = pipelineRepository.save(pipelineMapper.toModel(request));

        return pipelineMapper.toDTO(newPipeline);
    }

    public PipelineResponseDTO updatePipeline(PipelineRequestDTO request, String id) {
        Pipeline existingPipeline = pipelineRepository.findById(id)
                .orElseThrow(() -> new PipelineNotFoundException("Pipeline not found with id: " + id));

        Pipeline updatedPipeline = pipelineRepository.save(mergePipeline(existingPipeline, request));
        return pipelineMapper.toDTO(updatedPipeline);
    }

    private Pipeline mergePipeline(Pipeline existingPipeline, PipelineRequestDTO request) {
        if (StringUtils.isNotBlank(request.getName()))
            existingPipeline.setName(request.getName());
        if (StringUtils.isNotBlank(request.getAuthorEmail()))
            existingPipeline.setAuthorEmail(request.getAuthorEmail());
        if (request.getParameters() != null)
            existingPipeline.setParameters(request.getParameters());
        if (request.getEnvVariables() != null)
            existingPipeline.setEnvVariables(request.getEnvVariables());
        if (request.getStages() != null)
            existingPipeline.setStages(request.getStages());

        return existingPipeline;
    }

    public void deletePipeline(String id) {
        Pipeline pipelineToDelete = pipelineRepository.findById(id)
                .orElseThrow(() -> new PipelineNotFoundException("Pipeline not found with id: " + id));

        pipelineRepository.delete(pipelineToDelete);
    }
}
