package org.example.pipelineservice.kafka.events;

import lombok.*;
import org.example.pipelineservice.pipeline.Stage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineAssignedEvent {
    private UUID pipelineId;
    private List<List<Stage>> stages;
    private Map<String, String> envToSet;
}
