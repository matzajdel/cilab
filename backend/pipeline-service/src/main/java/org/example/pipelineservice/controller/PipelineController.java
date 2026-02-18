package org.example.pipelineservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.pipelineservice.dto.PipelineRequestDTO;
import org.example.pipelineservice.dto.PipelineResponseDTO;
import org.example.pipelineservice.dto.PipelineSummaryDTO;
import org.example.pipelineservice.service.PipelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
public class PipelineController {
    private final PipelineService pipelineService;

    @GetMapping ("/{id}")
    public ResponseEntity<PipelineResponseDTO> getPipelineInfo(@PathVariable String id) {
        return ResponseEntity.ok(pipelineService.getPipelineInfo(id));
    }

    @GetMapping
    public ResponseEntity<List<PipelineSummaryDTO>> getPipelineSummaries() {
        return ResponseEntity.ok(pipelineService.getPipelineSummaries());
    }

    // createPipeline
    @PostMapping
    public ResponseEntity<PipelineResponseDTO> createPipeline(
            @RequestBody PipelineRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pipelineService.createPipeline(request));
    }

    // updatePipeline
    @PutMapping("/{id}")
    public ResponseEntity<PipelineResponseDTO> updatePipeline(
            @RequestBody PipelineRequestDTO request,
            @PathVariable String id
    ) {
        return ResponseEntity
                .ok()
                .body(pipelineService.updatePipeline(request, id));
    }

    // deletePipeline
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePipeline(
            @PathVariable String id
    ) {
        pipelineService.deletePipeline(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}
