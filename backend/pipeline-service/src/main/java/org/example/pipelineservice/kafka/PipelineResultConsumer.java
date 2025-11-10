package org.example.pipelineservice.kafka;

import org.example.pipelineservice.kafka.events.PipelineResultEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class PipelineResultConsumer {

    @KafkaListener(
            topics = "pipeline-result-events",
            groupId = "pipeline-service-group",
            containerFactory = "kafkaListenerContaienerFactory"
    )
    public void consumePipelineResult(PipelineResultEvent event, Acknowledgment ack) {
        // consume
        ack.acknowledge();
    }
}
