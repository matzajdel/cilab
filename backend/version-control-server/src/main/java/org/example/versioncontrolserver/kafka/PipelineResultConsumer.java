package org.example.versioncontrolserver.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.versioncontrolserver.dto.LabelDTO;
import org.example.versioncontrolserver.dto.MessageDTO;
import org.example.versioncontrolserver.services.RepositoryQueryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineResultConsumer {
    private final ObjectMapper objectMapper;
    private final RepositoryQueryService service;

    @KafkaListener(
            topics = "pipeline-labels-events",
            groupId = "vcs-label-result-group-v2",
            containerFactory = "labelContainerFactory"
    )
    public void consumeLabelEvent(LabelDTO event, Acknowledgment ack) throws JsonProcessingException {
        log.info("Received pipeline result: {}", objectMapper.writeValueAsString(event));
        service.saveLabel(event, "sid-pipeline-cilab@mail.com");
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "pipeline-messages-events",
            groupId = "vcs-message-result-group-v2",
            containerFactory = "messageContainerFactory"
    )
    public void consumeMessageEvent(MessageDTO event, Acknowledgment ack) throws JsonProcessingException {
        System.out.println("Received stage result: " + objectMapper.writeValueAsString(event));
        service.saveMessage(event);
        ack.acknowledge();
    }
}
