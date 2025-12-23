package org.example.pipelineservice.model.pipeline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stage {
    @NotBlank(message = "Stage name is required")
    private String name;
    @NotBlank(message = "Stage image is required")
    private String image;

    @Valid
    private Repo repo;

    @Valid
    private StageEnvironment stageEnvironment;

    @NotBlank(message = "Stage script is required")
    private String script;
}
