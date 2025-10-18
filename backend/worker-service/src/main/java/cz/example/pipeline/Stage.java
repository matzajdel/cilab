package cz.example.pipeline;

import java.util.UUID;

public class Stage {
    private UUID stageId;
    private String script;

    public Stage(UUID stageId, String script) {
        this.stageId = stageId;
        this.script = script;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
