package cz.example.executors.exception;

public class DockerTaskException extends RuntimeException {
    private final DockerTaskExceptionType errorType;

    public DockerTaskException(DockerTaskExceptionType errorType) {
        super(errorType.getDescription());
        this.errorType = errorType;
    }

    public DockerTaskException(DockerTaskExceptionType errorType, Throwable cause) {
        super(errorType.getDescription(), cause);
        this.errorType = errorType;
    }

    public DockerTaskExceptionType getErrorType() {
        return errorType;
    }
}
