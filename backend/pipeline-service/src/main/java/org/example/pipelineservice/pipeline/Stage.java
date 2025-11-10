package org.example.pipelineservice.pipeline;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage {
    private UUID stageId;
    private String script;
}
