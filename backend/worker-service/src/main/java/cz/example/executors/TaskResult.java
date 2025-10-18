package cz.example.executors;

public class TaskResult {
    private TaskResultStatus status;

    public TaskResult(TaskResultStatus status) {
        this.status = status;
    }

    public TaskResultStatus getStatus() {
        return status;
    }

    public void setStatus(TaskResultStatus status) {
        this.status = status;
    }
}
