package cz.example.pipeline;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PipelineAssignedEvent {
    private UUID pipelineId;
    private List<List<Stage>> stages;
    private Map<String, String> envToSet;


    public PipelineAssignedEvent(UUID pipelineId, List<List<Stage>> stages) {
        this.pipelineId = pipelineId;
        this.stages = stages;
    }

    public PipelineAssignedEvent(UUID pipelineId, List<List<Stage>> stages, Map<String, String> envToSet) {
        this.pipelineId = pipelineId;
        this.stages = stages;
        this.envToSet = envToSet;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public List<List<Stage>> getStages() {
        return stages;
    }

    public void setStages(List<List<Stage>> stages) {
        this.stages = stages;
    }

    public Map<String, String> getEnvToSet() {
        return envToSet;
    }

    public void setEnvToSet(Map<String, String> envToSet) {
        this.envToSet = envToSet;
    }
}
