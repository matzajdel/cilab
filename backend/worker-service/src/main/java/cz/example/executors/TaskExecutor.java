package cz.example.executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.kafka.PipelineResultProducer;
import cz.example.pipeline.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
    private final ObjectMapper objectMapper;
    private final PipelineResultProducer pipelineResultProducer;
    private final DockerTaskRunner runner;
    private final ExecutorService dockerExecutor;

    public TaskExecutor(ObjectMapper objectMapper, PipelineResultProducer pipelineResultProducer, DockerTaskRunner runner) {
        this.objectMapper = objectMapper;
        this.pipelineResultProducer = pipelineResultProducer;
        this.runner = runner;
        this.dockerExecutor = Executors.newFixedThreadPool(50);
    }

    public PipelineResult execute(PipelineAssignedEvent event) throws Exception {
        List<List<Stage>> stages = event.getStages();
        List<StageResult> allResults = new ArrayList<>();

        Map<String, String> accumulatedEnv = new HashMap<>();
        if (event.getEnvToSet() != null) {
            accumulatedEnv.putAll(event.getEnvToSet());
        }

        for (List<Stage> stageGroup : stages) {
            Map<String, String> envSnapshot = new HashMap<>(accumulatedEnv);

            List<CompletableFuture<StageResult>> futures = stageGroup.stream()
                    .map(stage -> CompletableFuture.supplyAsync(
                            () -> processStage(stage, envSnapshot),
                            dockerExecutor
                    ))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<StageResult> groupResults = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            allResults.addAll(groupResults);

            boolean groupFailed = false;
            for (StageResult res : groupResults) {
                if (res.getStatus() == StageResultStatus.FAILED) {
                    groupFailed = true;
                } else if (res.getResultEnvs() != null) {
                    accumulatedEnv.putAll(res.getResultEnvs());
                }
            }

            if (groupFailed) {
                handleSkippedStages(stages, allResults);
                return new PipelineResult(event.getPipelineRunId(), PipelineResultStatus.FAILED);
            }
        }

        return new PipelineResult(event.getPipelineRunId(), PipelineResultStatus.SUCCESSFUL);
    }

    private StageResult processStage(Stage stage, Map<String, String> envSnapshot) {
        try {
            sendNotification("Starting stage", createStageResult(stage.getId(), StageResultStatus.IN_PROGRESS, null));

            StageResult result = runner.runDockerTask(stage.getId(), stage.getScript(), envSnapshot, stage.getImage());
            result.setStageId(stage.getId());

            sendNotification("Finished stage", result);
            return result;
        } catch (Exception e) {
            StageResult failed = createStageResult(stage.getId(), StageResultStatus.FAILED, "Exception: " + e.getMessage());
            sendNotification("Stage failed with exception", failed);
            return failed;
        }
    }

    private void handleSkippedStages(List<List<Stage>> allStages, List<StageResult> results) {
        Set<String> finishedStageIds = results.stream()
                .map(StageResult::getStageId)
                .collect(Collectors.toSet());

        allStages.stream()
                .flatMap(List::stream)
                .filter(stage -> !finishedStageIds.contains(stage.getId()))
                .forEach(skippedStage -> {
                    StageResult result = StageResult.builder()
                            .stageId(skippedStage.getId())
                            .status(StageResultStatus.SKIPPED)
                            .endTime(Instant.now())
                            .build();

                    sendNotification("Skipping stage", result);
                });
    }

    private void sendNotification(String logMsg, StageResult result) {
        logger.info("{}: {}", logMsg, result.getStageId());

        pipelineResultProducer.sendStageResult(result);

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Payload: {}", objectMapper.writeValueAsString(result));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize result for Kafka", e);
        }
    }

    private StageResult createStageResult(String id, StageResultStatus status, String message) {
        StageResult result = new StageResult(id, status);
        if (status == StageResultStatus.IN_PROGRESS) {
            result.setStartTime(new Date().toInstant());
        } else if (status == StageResultStatus.SUCCESSFUL || status == StageResultStatus.FAILED) {
            result.setEndTime(new Date().toInstant());
        }

        if (message != null) result.setMessage(message);

        return result;
    }
}
