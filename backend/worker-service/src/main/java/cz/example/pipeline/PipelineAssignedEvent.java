package cz.example.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PipelineAssignedEvent {
    private String pipelineId;
    private String pipelineRunId;
    private List<List<Stage>> stages;
    private Map<String, String> envToSet;
}
