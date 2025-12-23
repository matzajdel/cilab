package org.example.pipelineservice.model.pipeline;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Repo {
    @NotBlank(message = "Repository name is required")
    private String name;
    private String commitId;
}
