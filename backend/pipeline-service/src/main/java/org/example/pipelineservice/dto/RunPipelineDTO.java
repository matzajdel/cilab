package org.example.pipelineservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RunPipelineDTO {
    private String pipelineId;
    private String runByEmail;
    private Map<String, String> parameters;
}
