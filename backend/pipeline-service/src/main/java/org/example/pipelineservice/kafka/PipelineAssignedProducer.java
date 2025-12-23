package org.example.pipelineservice.kafka;

import lombok.RequiredArgsConstructor;
import org.example.pipelineservice.kafka.events.PipelineAssignedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PipelineAssignedProducer {
    private static final String TOPIC = "pipeline-assigned-events";
    private final KafkaTemplate<String, PipelineAssignedEvent> kafkaTemplate;

    public void publish (PipelineAssignedEvent event) {
        String key = event.getPipelineId().toString();
        kafkaTemplate.send(TOPIC, key, event);
    }
}
