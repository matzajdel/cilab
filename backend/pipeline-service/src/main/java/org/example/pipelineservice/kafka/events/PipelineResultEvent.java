package org.example.pipelineservice.kafka.events;

import lombok.Getter;
import lombok.Setter;
import org.example.pipelineservice.model.pipelineRun.PipelineStatus;

@Getter
@Setter
public class PipelineResultEvent {
    private String pipelineRunId;
    private PipelineStatus status;
}
