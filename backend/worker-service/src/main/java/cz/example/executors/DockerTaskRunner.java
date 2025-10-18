package cz.example.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import cz.example.pipeline.StageResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DockerTaskRunner {

    public StageResult runDockerTask(String script, Map<String, String> envToSet) throws Exception {
        String image = "busybox:latest";

        // Configure client (uses environment variables / defaults)
        // Force TCP connection to Docker daemon on localhost:2375
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .build();
        
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        // Prepare Loki client (Loki URL and labels via environment variables)
        String lokiUrl = System.getenv().getOrDefault("LOKI_URL", "http://localhost:3100/loki/api/v1/push");
        String jobLabel = System.getenv().getOrDefault("LOKI_JOB", "worker-service");
        Map<String, String> lokiLabels = new HashMap<>();
        lokiLabels.put("job", jobLabel);

        LokiClient lokiClient = new LokiClient(lokiUrl, lokiLabels);

//        ============= Config finished ===============

        // Creating env list:
        List<String> envList = new ArrayList<>();
        if (envToSet != null) {
            envToSet.forEach((key, value) -> envList.add(key + "=" + value));
        }


        // Generate a unique container name to avoid name collisions when running in parallel
        try {
//            System.out.println("Pulling image: " + image);
            dockerClient.pullImageCmd(image).start().awaitCompletion();


//            System.out.println("Creating container...");
            String containerName = "docker-java-example-" + UUID.randomUUID().toString();
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withCmd("sh", "-c", script)
                .withName(containerName)
                .withEnv(envList)
                .exec();
            String containerId = container.getId();
            System.out.println("Container created: " + containerId);


            System.out.println("Starting container...");
            dockerClient.startContainerCmd(containerId).exec();


//            System.out.println("Attaching to logs...");
            // Collect logs into a buffer so we can parse result lines produced by the process
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
                                // Print to console as before
                                System.out.print(message);
                                // Append to logs buffer for later parsing
                                if (message.contains("__STAGE_RESULTS__")) {
                                    resultEnvs.putAll(parseResultEnv(message));
                                } else {
                                    lokiClient.pushAsync(message);
                                }
                            }
                        }).awaitCompletion();
            } catch (Exception e) {
                System.err.println("Error while streaming logs for container " + containerId + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }


//            System.out.println("Waiting a moment for container to finish...");
            Thread.sleep(1000);
//            System.out.println("Stopping container (if still running)...");
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(1).exec();
            } catch (DockerException e) {
                // ignore if already stopped
            }


            // Inspect container to get exit code
            Long exitCode = null;
            String[] containerEnvs = new String[]{};
            String allLogs = "";
            try {
                InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
                if (inspect != null && inspect.getState() != null) {
                    exitCode = inspect.getState().getExitCodeLong();
                    containerEnvs = inspect.getConfig().getEnv();
                }
            } catch (Exception e) {
                System.err.println("Failed to inspect container " + containerId + ": " + e.getMessage());
            }
            System.out.println("Container exit code: " + String.valueOf(exitCode));
            System.out.println("Container environment variables" + Arrays.toString(containerEnvs));



            System.out.println("Removing container...");
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                System.err.println("Failed to remove container " + containerId + ": " + e.getMessage());
            }



            // If container exited with non-zero code, report and fail the task
            if (exitCode != null && exitCode != 0L) {
                String err = "Container " + containerId + " exited with non-zero exit code: " + exitCode;
                System.err.println(err);
                try { lokiClient.pushAsync(err); } catch (Exception ignore) {}
//                    throw new RuntimeException(err);
            }

            System.out.println("Done.");

            // Use logs we collected earlier

            return buildStageResult(String.valueOf(exitCode), resultEnvs);

        } finally {
            try {
                dockerClient.close();
            } catch (Exception ignored) {}
        }
    }

    private StageResult buildStageResult (String exitCode, Map<String, String> resultEnvs) {

        StageResult result = new StageResult();
        if (Objects.equals(exitCode, "0")) {
            result.setStatus(TaskResultStatus.SUCCESSFULL);
        } else {
            result.setStatus(TaskResultStatus.FAILED);
        }

        result.setResultEnvs(resultEnvs);

        return result;
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
}
