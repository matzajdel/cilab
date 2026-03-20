package org.example.pipelineservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class LabelEvent {
    private String name;
    private int value = 0;
    private String commitId;
}
