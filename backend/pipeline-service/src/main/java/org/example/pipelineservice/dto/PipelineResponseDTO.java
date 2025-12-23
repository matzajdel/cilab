package org.example.pipelineservice.dto;

import lombok.*;
import org.example.pipelineservice.model.pipeline.PipelineParameter;
import org.example.pipelineservice.model.pipeline.Stage;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineResponseDTO {
    private String id;
    private String name;
    private String authorEmail;
    private List<PipelineParameter> parameters;
    private Map<String, String> envVariables;
    private List<List<Stage>> stages;
}
