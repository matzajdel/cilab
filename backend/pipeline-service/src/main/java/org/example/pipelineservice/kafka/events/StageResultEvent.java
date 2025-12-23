package org.example.pipelineservice.kafka.events;

import lombok.*;
import org.example.pipelineservice.model.pipelineRun.StageStatus;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StageResultEvent {
    private String stageId;
    private StageStatus status;
    private Map<String, String> resultEnvs;
    private String message;
    private Instant startTime;
    private Instant endTime;
}
