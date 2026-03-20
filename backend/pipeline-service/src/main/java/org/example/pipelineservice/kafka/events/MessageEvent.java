package org.example.pipelineservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MessageEvent {
    private String text;
    private String authorEmail;
    private String commitId;
}
