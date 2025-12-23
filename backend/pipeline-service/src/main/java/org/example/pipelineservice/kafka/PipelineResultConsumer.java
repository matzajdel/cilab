package org.example.pipelineservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pipelineservice.kafka.events.PipelineResultEvent;
import org.example.pipelineservice.kafka.events.StageResultEvent;
import org.example.pipelineservice.service.PipelineRunService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineResultConsumer {
    private final ObjectMapper objectMapper;
    private final PipelineRunService pipelineRunService;

    @KafkaListener(
            topics = "pipeline-result-events",
            groupId = "pipeline-service-group",
            containerFactory = "pipelineResultContainerFactory"
    )
    public void consumePipelineResult(PipelineResultEvent event, Acknowledgment ack) throws JsonProcessingException {
        log.info("Received pipeline result: {}", objectMapper.writeValueAsString(event));
        pipelineRunService.updatePipelineRunInfo(event);
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "stage-result-events",
            groupId = "pipeline-service-group",
            containerFactory = "stageResultContainerFactory"
    )
    public void consumeStageResult(StageResultEvent event, Acknowledgment ack) throws JsonProcessingException {
        System.out.println("Received stage result: " + objectMapper.writeValueAsString(event));
        pipelineRunService.updateStageRunInfo(event);
        ack.acknowledge();
    }
}