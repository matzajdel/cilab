package cz.example.pipeline;

public class PipelineResult {
    private PipelineResultStatus status;

    public PipelineResult(PipelineResultStatus status) {
        this.status = status;
    }

    public PipelineResultStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineResultStatus status) {
        this.status = status;
    }
}
