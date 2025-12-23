package cz.example.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PipelineResult {
    private String pipelineRunId;
    private PipelineResultStatus status;
}
