package org.example.pipelineservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.pipelineservice.model.pipeline.PipelineParameter;
import org.example.pipelineservice.model.pipeline.Stage;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PipelineRequestDTO {
    @NotBlank(message = "Pipeline name is required")
    private String name;

    @Email(message = "Email should have a valid format")
    @NotBlank(message = "Author email is required")
    private String authorEmail;

    @Valid
    private List<PipelineParameter> parameters;
    private Map<String, String> envVariables;

    @Valid
    @NotNull(message = "Stages cannot be null")
    private List<List<Stage>> stages;
    private List<String> labels;
}
