package org.example.pipelineservice.controller;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.example.pipelineservice.model.pipelineRun.StageRunInfo;
import org.example.pipelineservice.repository.PipelineRunRepository;
import org.example.pipelineservice.service.LokiLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/pipelines/logs")
@RequiredArgsConstructor
public class LogController {
    private final LokiLogService lokiLogService;
    private final PipelineRunRepository pipelineRunRepository;

    @GetMapping(value = "/{stageId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getStageLogs(@PathVariable String stageId) {
        return lokiLogService.streamLogs(stageId);
    }

}
