package cz.example.exception;

public enum DockerTaskExceptionType {
    PULL_IMAGE_ERROR("Error while pulling docker image"),
    CREATE_CONTAINER_ERROR("Error while creating docker container"),
    START_CONTAINER_ERROR("Error while starting docker container"),
    HANDLE_LOGS_ERROR("Error while handling docker container logs"),
    GET_EXIT_CODE_ERROR("Error while getting docker container exit code"),
    REMOVE_CONTAINER_ERROR("Error while removing docker container");

    private final String description;

    DockerTaskExceptionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
