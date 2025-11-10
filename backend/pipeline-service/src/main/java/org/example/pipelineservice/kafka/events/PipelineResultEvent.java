package org.example.pipelineservice.kafka.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PipelineResultEvent {
    private PipelineResultStatus status;
}
