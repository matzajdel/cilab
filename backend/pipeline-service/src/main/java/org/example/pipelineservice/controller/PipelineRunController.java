package org.example.pipelineservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.pipelineservice.dto.PipelineRunSummaryDTO;
import org.example.pipelineservice.model.pipelineRun.PipelineRun;
import org.example.pipelineservice.service.PipelineRunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/runs")
@RequiredArgsConstructor
public class PipelineRunController {
    private final PipelineRunService service;

    @PostMapping("/pipelines/{pipelineId}")
    public ResponseEntity<String> runPipeline(
            @PathVariable String pipelineId,
            @RequestParam(defaultValue = "mateusz.zajdel@cilab.com") String runnedByEmail,
            @RequestBody Map<String, String> parameters
    ) {
        service.runPipeline(pipelineId, runnedByEmail, parameters);
        return ResponseEntity
                .ok(String.format("Run for pipeline %s initiated successfully.", pipelineId));
    }

    @GetMapping("/pipelines/{pipelineId}")
    public ResponseEntity<List<PipelineRunSummaryDTO>> getRunsByPipelineId(
            @PathVariable String pipelineId
    ) {
        return ResponseEntity
                .ok(service.getRunsByPipelineId(pipelineId));
    }

    @GetMapping()
    public ResponseEntity<List<PipelineRunSummaryDTO>> getRunsByUser(
            @RequestParam String authorEmail
    ) {
        return ResponseEntity.ok(service.getRunsByUser(authorEmail));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<PipelineRun> getPipelineRunById(
            @PathVariable String runId
    ) {
        return ResponseEntity
                .ok(service.getPipelineRunById(runId));
    }
}
