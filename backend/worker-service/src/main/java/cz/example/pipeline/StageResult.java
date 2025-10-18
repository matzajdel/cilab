package cz.example.pipeline;

import cz.example.executors.TaskResultStatus;

import java.util.Map;
import java.util.UUID;

public class StageResult {
    private UUID stageId;
    private TaskResultStatus status;
    private Map<String, String> resultEnvs;

    public StageResult() {
    }

    public StageResult(UUID stageId, TaskResultStatus status, Map<String, String> resultEnvs) {
        this.stageId = stageId;
        this.status = status;
        this.resultEnvs = resultEnvs;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

    public TaskResultStatus getStatus() {
        return status;
    }

    public void setStatus(TaskResultStatus status) {
        this.status = status;
    }

    public Map<String, String> getResultEnvs() {
        return resultEnvs;
    }

    public void setResultEnvs(Map<String, String> resultEnvs) {
        this.resultEnvs = resultEnvs;
    }
}
