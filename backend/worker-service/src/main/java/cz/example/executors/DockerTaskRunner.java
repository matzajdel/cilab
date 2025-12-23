package cz.example.executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import cz.example.loki.LokiClient;
import cz.example.loki.LokiService;
import cz.example.loki.model.LogLineBody;
import cz.example.pipeline.StageResultStatus;
import cz.example.exception.DockerTaskException;
import cz.example.pipeline.StageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.dockerjava.api.command.WaitContainerResultCallback;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cz.example.exception.DockerTaskExceptionType.*;

//TODO: excepltion handling in lokiClient
public class DockerTaskRunner {
    private static final Logger log = LoggerFactory.getLogger(DockerTaskRunner.class);
    private final DockerClient dockerClient;
//    private final LokiClient lokiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long LOG_WAIT_TIMEOUT_MIN = 60;

    public DockerTaskRunner() {
        // pattern: constructor chaining
        this(createDefaultDockerClient());
    }

    public DockerTaskRunner(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public StageResult runDockerTask(String stageId, String script, Map<String, String> envToSet, String image) {
//        String image = "busybox:latest";

        String containerId = null;
        try {
            pullImage(image);
            containerId = createContainer(image, script, envToSet);
            startContainer(containerId);
            Map<String, String> resultEnvs = handleContainerLogs(containerId, stageId);
            long exitCode = getContainerExitCode(containerId);


            StageResult result = new StageResult(
                    exitCode == 0 ? StageResultStatus.SUCCESSFUL : StageResultStatus.FAILED,
                    resultEnvs,
                    exitCode == 0 ? "Stage executed successfully" : "Stage execution failed with exit code " + exitCode
            );
            result.setEndTime(new Date().toInstant());

            return result;

        } catch (DockerTaskException e) {
            log.error("Docker Task Error: {}", e.getErrorType().getDescription());
            return new StageResult(
                    StageResultStatus.FAILED,
                    Collections.emptyMap(),
                    e.getErrorType().getDescription()
            );
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return new StageResult(
                    StageResultStatus.FAILED,
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
            dockerClient.pullImageCmd(image)
                    .start()
                    .awaitCompletion(60, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new DockerTaskException(PULL_IMAGE_ERROR, e);
        }
    }

    private String createContainer(String image, String script, Map<String, String> envMap) {
        String containerName = "worker-" + UUID.randomUUID().toString();

        List<String> envList = new ArrayList<>();
        if (envMap != null) {
            envMap.forEach((key, value) -> envList.add(key + "=" + value));
        }

        try {
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

    public Map<String, String> handleContainerLogs(String containerId, String stageId) {
        Map<String, String> resultEnvs = new HashMap<>();

        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame item) {
                            String message = new String(item.getPayload(), StandardCharsets.UTF_8);
                            System.out.println(message);

                            if (message.contains("__STAGE_RESULTS__")) {
                                resultEnvs.putAll(parseResultEnv(message));
                            } else {
                                // TODO: exception handling
                                queueLogToLoki(message, stageId);
                            }
                        }
                    })
                    .awaitCompletion(LOG_WAIT_TIMEOUT_MIN, TimeUnit.MINUTES);

            return resultEnvs;
        } catch (Exception e) {
            throw new DockerTaskException(HANDLE_LOGS_ERROR, e);
        }
    }

    private void queueLogToLoki(String message, String stageId) {
        LogLineBody logBody = new LogLineBody(stageId, message);

        try {
            String jsonBody = objectMapper.writeValueAsString(logBody);
            LokiService.get().enqueueLog(jsonBody);
        } catch (JsonProcessingException e) {
            System.err.println("Serialization error: " + e.getMessage());
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
            log.warn("Failed to remove container {}, {}", containerId, e.getMessage());
        }
    }

    private static DockerClient createDefaultDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .build();

        return DockerClientBuilder.getInstance(config).build();
    }

//    private static LokiClient createDefaultLokiClient() {
//        String lokiUrl = System.getenv().getOrDefault("LOKI_URL", "http://localhost:3100/loki/api/v1/push");
//        String jobLabel = System.getenv().getOrDefault("LOKI_JOB", "worker-service");
//
//        Map<String, String> lokiLabels = new HashMap<>();
//        lokiLabels.put("job", jobLabel);
//
//        return new LokiClient(lokiUrl, lokiLabels);
//    }
}
