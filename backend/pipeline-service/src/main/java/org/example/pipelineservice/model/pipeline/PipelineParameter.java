package org.example.pipelineservice.model.pipeline;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineParameter {
    @NotBlank(message = "Pipeline parameter name is required")
    private String name;
    private String defaultValue;
    @NotBlank(message = "Pipeline parameter description is required")
    private String description;
}
