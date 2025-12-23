package org.example.pipelineservice.kafka.events;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineAssignedEvent {
    private String pipelineId;
    private String pipelineRunId;
    private List<List<StageAssignedEvent>> stages;
    private Map<String, String> envToSet;
}
