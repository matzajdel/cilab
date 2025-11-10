package cz.example.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import cz.example.pipeline.PipelineResultStatus;
import cz.example.executors.exception.DockerTaskException;
import cz.example.pipeline.StageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.dockerjava.api.command.WaitContainerResultCallback;

import java.util.*;

import static cz.example.executors.exception.DockerTaskExceptionType.*;

//TODO: excepltion handling in lokiClient
public class DockerTaskRunner {
    private static final Logger log = LoggerFactory.getLogger(DockerTaskRunner.class);
    private final DockerClient dockerClient;
    private final LokiClient lokiClient;

    public DockerTaskRunner() {
        // pattern: constructor chaining
        this(createDefaultDockerClient(), createDefaultLokiClient());
    }

    public DockerTaskRunner(DockerClient dockerClient, LokiClient lokiClient) {
        this.dockerClient = dockerClient;
        this.lokiClient = lokiClient;
    }

    public StageResult runDockerTask(String script, Map<String, String> envToSet) {
        String image = "busybox:latest";

        String containerId = null;
        try {
            pullImage(image);
            containerId = createContainer(image, script, envToSet);
            startContainer(containerId);
            Map<String, String> resultEnvs = handleContainerLogs(containerId);
            long exitCode = getContainerExitCode(containerId);


            return new StageResult(
                    exitCode == 0 ? PipelineResultStatus.SUCCESSFUL : PipelineResultStatus.FAILED,
                    resultEnvs,
                    "Stage completed successfully"
            );

        } catch (DockerTaskException e) {
            log.error("Docker Task Error: {}", e.getErrorType().getDescription());
            return new StageResult(
                    PipelineResultStatus.FAILED,
                    Collections.emptyMap(),
                    e.getErrorType().getDescription()
            );
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return new StageResult(
                    PipelineResultStatus.FAILED,
                    Collections.emptyMap(),
                    "Unexpected error"
            );
        }
        finally {
            if (containerId != null) {
                removeContainer(containerId);
            }
        }
    }

    private void pullImage(String image) {
        try {
            log.info("Pulling docker image: {}", image);
            dockerClient.pullImageCmd(image).start().awaitCompletion();
        } catch (Exception e) {
            throw new DockerTaskException(PULL_IMAGE_ERROR, e);
        }
    }

    private String createContainer(String image, String script, Map<String, String> envMap) {
        String containerName = "worker-service-container-" + UUID.randomUUID().toString();

        try {
            List<String> envList = new ArrayList<>();
            if (envMap != null) {
                envMap.forEach((key, value) -> envList.add(key + "=" + value));
            }

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withCmd("sh", "-c", script)
                    .withName(containerName)
                    .withEnv(envList)
                    .exec();

            log.info("Creating docker container: with id: {}", container.getId());
            return container.getId();
        } catch (Exception e) {
            throw new DockerTaskException(CREATE_CONTAINER_ERROR, e);
        }
    }

    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (Exception e) {
            throw new DockerTaskException(START_CONTAINER_ERROR, e);
        }
    }

    public Map<String, String> handleContainerLogs(String containerId) {
        Map<String, String> resultEnvs = new HashMap<>();

        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame item) {
                            String message = new String(item.getPayload());
                            System.out.println(message);

                            if (message.contains("__STAGE_RESULTS__")) {
                                resultEnvs.putAll(parseResultEnv(message));
                            } else {
                                // TODO: exception handling
                                lokiClient.pushAsync(message);
                            }
                        }
                    })
                    .awaitCompletion();

            return resultEnvs;
        } catch (Exception e) {
            throw new DockerTaskException(HANDLE_LOGS_ERROR, e);
        }
    }

    private Map<String, String> parseResultEnv(String message) {
        Map<String, String> map = new HashMap<>();
        if (message == null || !message.contains("__STAGE_RESULTS__")) return map;

        int idx = message.indexOf("__STAGE_RESULTS__");
        String payload = message.substring(idx);

        String[] parts = payload.split(",");
        for (String part : parts) {
            part = part.trim();
            if (!part.startsWith("__STAGE_RESULTS__")) continue;
            // Remove prefix
            String rest = part.substring("__STAGE_RESULTS__".length());
            int eq = rest.indexOf('=');
            if (eq <= 0) continue;
            String key = rest.substring(0, eq).trim();
            String value = rest.substring(eq + 1).trim();
            // strip trailing commas/newlines/carriage returns
            value = value.replaceAll("[\r\n]+$", "");
            if (value.endsWith(",")) value = value.substring(0, value.length() - 1).trim();
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }

        return map;
    }

    private long getContainerExitCode(String containerId) {
        try {
            log.info("Waiting for container {} to finish...", containerId);

            WaitContainerResultCallback callback = new WaitContainerResultCallback();

            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(callback)
                    .awaitStatusCode();

            log.info("Container {} exited with code {}", containerId, exitCode);

            return exitCode != null ? exitCode : 1L; // null-safe fallback

        } catch (Exception e) {
            throw new DockerTaskException(GET_EXIT_CODE_ERROR, e);
        }
    }

    private void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("Container with id: {} successfully removed", containerId);
        } catch (NotFoundException e) {
            log.warn("Failed to remove conteiner {}, {}", containerId, e.getMessage());
        }
    }

    private static DockerClient createDefaultDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .build();

        return DockerClientBuilder.getInstance(config).build();
    }

    private static LokiClient createDefaultLokiClient() {
        String lokiUrl = System.getenv().getOrDefault("LOKI_URL", "http://localhost:3100/loki/api/v1/push");
        String jobLabel = System.getenv().getOrDefault("LOKI_JOB", "worker-service");

        Map<String, String> lokiLabels = new HashMap<>();
        lokiLabels.put("job", jobLabel);

        return new LokiClient(lokiUrl, lokiLabels);
    }
}
