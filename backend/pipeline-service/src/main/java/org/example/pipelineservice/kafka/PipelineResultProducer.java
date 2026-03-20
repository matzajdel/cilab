package org.example.pipelineservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pipelineservice.kafka.events.LabelEvent;
import org.example.pipelineservice.kafka.events.MessageEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineResultProducer {
    private static final String LABELS_TOPIC = "pipeline-labels-events";
    private static final String MESSAGES_TOPIC = "pipeline-messages-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLabelEvent (LabelEvent event) {
        kafkaTemplate.send(LABELS_TOPIC, event.getCommitId(), event);
    }

    public void publishMessageEvent (MessageEvent event) {
        String key = UUID.randomUUID().toString();
        kafkaTemplate.send(MESSAGES_TOPIC, event.getCommitId(), event);
    }
}
