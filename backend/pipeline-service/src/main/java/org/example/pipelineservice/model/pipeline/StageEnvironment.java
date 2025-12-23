package org.example.pipelineservice.model.pipeline;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StageEnvironment {
    Map<String, String> stageEnvVariables;
    List<String> results;
}
