package org.example.versioncontrolserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class RunPipelineDTO {
    private String pipelineId;
    private String runByEmail;
    private Map<String, String> parameters;
}

