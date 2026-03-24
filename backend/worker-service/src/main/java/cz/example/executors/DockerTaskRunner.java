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
import cz.example.loki.LokiService;
import cz.example.loki.model.LogLineBody;
import cz.example.pipeline.StageResultStatus;
import cz.example.exception.DockerTaskException;
import cz.example.pipeline.StageResult;
import cz.example.redis.RedisLogPublisher;
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
    private static final long LOG_WAIT_TIMEOUT_MIN = 60;
    private final DockerClient dockerClient;
    private final ObjectMapper objectMapper;
    private final RedisLogPublisher redisLogPublisher;

    public DockerTaskRunner(DockerClient dockerClient, ObjectMapper objectMapper, RedisLogPublisher redisLogPublisher) {
        this.dockerClient = dockerClient;
        this.objectMapper = objectMapper;
        this.redisLogPublisher = redisLogPublisher;
    }

    public StageResult runDockerTask(String stageId, String script, Map<String, String> envToSet, String image) {

        String containerId = null;

        try {
            pullImage(image);
            containerId = createContainer(image, script, envToSet);
            startContainer(containerId);
            Map<String, String> resultEnvs = handleContainerLogs(containerId, stageId);
            long exitCode = getContainerExitCode(containerId);


            return StageResult.builder()
                    .status(exitCode == 0 ? StageResultStatus.SUCCESSFUL : StageResultStatus.FAILED)
                    .resultEnvs(resultEnvs)
                    .message(exitCode == 0 ? "Stage executed successfully" : "Stage execution failed with exit code " + exitCode)
                    .endTime(new Date().toInstant())
                    .build();

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
            dockerClient.inspectImageCmd(image).exec();

            log.info("Image '{}' found locally. Skipping pull.", image);
        } catch (NotFoundException e) {
            log.info("Image '{}' not found locally. Pulling from registry...", image);
            try {
                dockerClient.pullImageCmd(image)
                        .start()
                        .awaitCompletion(60, TimeUnit.MINUTES);
                log.info("Successfully pulled image: {}", image);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Image pull was interrupted", ie);
                throw new RuntimeException("Interrupted while pulling image", ie);
            }
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
                                long tsNs = System.currentTimeMillis() * 1_000_000L;
                                redisLogPublisher.publish(tsNs, message, stageId);
                                queueLogToLoki(tsNs, message, stageId);
                            }
                        }
                    })
                    .awaitCompletion(LOG_WAIT_TIMEOUT_MIN, TimeUnit.MINUTES);

            return resultEnvs;
        } catch (Exception e) {
            throw new DockerTaskException(HANDLE_LOGS_ERROR, e);
        }
    }

    private void queueLogToLoki(Long tsNs, String message, String stageId) {
        LogLineBody logBody = new LogLineBody(stageId, message);

        try {
            String jsonBody = objectMapper.writeValueAsString(logBody);

            LokiService.get().enqueueLog(jsonBody, tsNs);
        } catch (JsonProcessingException e) {
            System.err.println("Serialization error: " + e.getMessage());
        }
    }

    private Map<String, String> parseResultEnv(String message) {;
        if (message == null || !message.contains("__STAGE_RESULTS__")) return Collections.emptyMap();

        Map<String, String> map = new HashMap<>();

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
}
