package org.example.versioncontrolserver.kafka;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.RunPipelineDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookTrigger {
    private static final String WEBHOOK_TOPIC = "webhook-events";

    private final KafkaTemplate<String, RunPipelineDTO> webhooksKafkaTemplate;

    public void triggerPipeline(RunPipelineDTO event) {
        webhooksKafkaTemplate.send(WEBHOOK_TOPIC, event.getPipelineId(), event);
    }
}