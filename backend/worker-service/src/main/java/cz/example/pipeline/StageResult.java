package cz.example.pipeline;

import java.util.Map;
import java.util.UUID;

public class StageResult {
    private UUID stageId;
    private PipelineResultStatus status;
    private Map<String, String> resultEnvs;
    private String message;

    public StageResult() {
    }

    public StageResult(PipelineResultStatus status, Map<String, String> resultEnvs, String message) {
        this.status = status;
        this.resultEnvs = resultEnvs;
        this.message = message;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

    public PipelineResultStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineResultStatus status) {
        this.status = status;
    }

    public Map<String, String> getResultEnvs() {
        return resultEnvs;
    }

    public void setResultEnvs(Map<String, String> resultEnvs) {
        this.resultEnvs = resultEnvs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
