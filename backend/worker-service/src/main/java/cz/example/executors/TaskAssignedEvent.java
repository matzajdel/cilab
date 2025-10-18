package cz.example.executors;

public class TaskAssignedEvent {
    private String script;

    public TaskAssignedEvent(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
