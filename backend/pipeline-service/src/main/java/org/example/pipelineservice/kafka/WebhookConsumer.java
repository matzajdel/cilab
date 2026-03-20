package org.example.pipelineservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pipelineservice.dto.RunPipelineDTO;
import org.example.pipelineservice.kafka.events.PipelineResultEvent;
import org.example.pipelineservice.service.PipelineRunService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookConsumer {
    private final ObjectMapper objectMapper;
    private final PipelineRunService pipelineRunService;

    @KafkaListener(
            topics = "webhook-events",
            groupId = "webhook-group",
            containerFactory = "webhookContainerFactory"
    )
    public void consumeWebhookEvent(RunPipelineDTO event, Acknowledgment ack) throws JsonProcessingException {
        log.info("Received webhook event: {}", objectMapper.writeValueAsString(event));
        pipelineRunService.runPipeline(event.getPipelineId(), event.getRunByEmail(), event.getParameters());
        ack.acknowledge();
    }
}