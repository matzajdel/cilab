package org.example.pipelineservice.model.pipelineRun;

import lombok.*;
import org.example.pipelineservice.model.pipeline.StageEnvironment;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StageRunInfo {
    private String stageId;
    private String name;
    private String image;
    private Map<String, String> stageEnvVariables;
    private Map<String, String> resultEnvs;
    private Instant startTime;
    private Instant endTime;
    private StageStatus status;
    private String message;
}
