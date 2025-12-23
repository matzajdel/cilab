package cz.example.executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.example.kafka.PipelineResultProducer;
import cz.example.pipeline.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TaskExecutor {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final PipelineResultProducer pipelineResultProducer = new PipelineResultProducer();

    public PipelineResult execute(PipelineAssignedEvent event) throws Exception {
        DockerTaskRunner runner = new DockerTaskRunner();
        List<List<Stage>> stages = event.getStages();
        List<StageResult> stageResults = new ArrayList<>();

        Map<String, String> accumulatedEnv = new HashMap<>();
        if (event.getEnvToSet() != null) {
            accumulatedEnv.putAll(event.getEnvToSet());
        }

        for (List<Stage> stageGroup : stages) {
            List<CompletableFuture<StageResult>> futures = stageGroup.stream()
                    .map(stage -> CompletableFuture.supplyAsync(() -> {
                        try {
                            //Send kafka notification about stage start
                            System.out.println("FAKEKAFKA: Starting stage: " + objectMapper.writeValueAsString(createStageResult(stage.getId(), StageResultStatus.IN_PROGRESS, null)));
                            pipelineResultProducer.sendStageResult(createStageResult(stage.getId(), StageResultStatus.IN_PROGRESS, null));

                            Map<String, String> envSnapshot = new HashMap<>(accumulatedEnv);
                            StageResult result = runner.runDockerTask(stage.getId(), stage.getScript(), envSnapshot, stage.getImage());
                            result.setStageId(stage.getId());

                            System.out.println("FAKEKAFKA: Finished stage: " + objectMapper.writeValueAsString(result));
                            pipelineResultProducer.sendStageResult(result);
                            return result;
                        } catch (Exception e) {
                            return createStageResult(stage.getId(), StageResultStatus.FAILED, "Exception: " + e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());

            List<StageResult> groupResults = futures.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception e) {
                            return createStageResult(null, StageResultStatus.FAILED, "Exception: " + e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());

            stageResults.addAll(groupResults);

            boolean groupFailed = false;
            for (StageResult res : groupResults) {
                if (res.getStatus() == StageResultStatus.FAILED) {
                    groupFailed = true;
                } else if (res.getResultEnvs() != null) {
                    accumulatedEnv.putAll(res.getResultEnvs());
                }
            }

            if (groupFailed) {
                Set<String> finishedStageIds = stageResults.stream()
                        .map(StageResult::getStageId)
                        .collect(Collectors.toSet());

                List<Stage> skippedStages = stages.stream()
                        .flatMap(List::stream)
                        .filter(stage -> !finishedStageIds.contains(stage.getId()))
                        .toList();

                skippedStages.forEach(failedStage -> {
                    try {
                        System.out.println("FAKEKAFKA: Stage failed: " + objectMapper.writeValueAsString(new StageResult(failedStage.getId(), StageResultStatus.SKIPPED)));
                        pipelineResultProducer.sendStageResult(new StageResult(failedStage.getId(), StageResultStatus.SKIPPED));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

                return new PipelineResult(event.getPipelineRunId(), PipelineResultStatus.FAILED);
            }
        }

        // Persist accumulated envs back to the event so callers can see final envs
//        event.setEnvToSet(accumulatedEnv);

        return new PipelineResult(event.getPipelineRunId(), PipelineResultStatus.SUCCESSFUL);
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

//public class TaskExecutor {
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final DockerTaskRunner runner = new DockerTaskRunner();
//
//    public PipelineResult execute(PipelineAssignedEvent event) throws Exception {
//        List<List<Stage>> stages = event.getStages();
//        List<String> finshedStageIds = new ArrayList<>();
//
//        // ?
//        AtomicBoolean anyFailed = new AtomicBoolean(false);
//
//        Map<String, String> accumulatedEnv = new HashMap<>();
//        if (event.getEnvToSet() != null) {
//            accumulatedEnv.putAll(event.getEnvToSet());
//        }
//
//        for (List<Stage> stageGroup : stages) {
//            List<CompletableFuture<StageResult>> futures = stageGroup.stream()
//                    .map(stage -> CompletableFuture.supplyAsync(() -> {
//                        try {
//                            //Send kafka notification about stage start
//
//
//                            Map<String, String> envSnapshot = new HashMap<>(accumulatedEnv);
//                            StageResult result = runner.runDockerTask(stage.getScript(), envSnapshot, stage.getImage());
//                            result.setStageId(stage.getStageId());
//                            return result;
//                        } catch (Exception e) {
//                            anyFailed.set(true);
//                            throw new RuntimeException(e);
//                        }
//                    }))
//                    .collect(Collectors.toList());
//
//            try {
//                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//            } catch (Exception ignored) {
//            }
//
//            for (CompletableFuture<StageResult> f : futures) {
//                try {
//                    StageResult res = f.join();
//                    finshedStageIds.add(res.getStageId());
//                    if (res != null && res.getResultEnvs() != null) {
//                        // Send kafka notofication about stage completion
//                        System.out.println(objectMapper.writeValueAsString(res));
//
//                        accumulatedEnv.putAll(res.getResultEnvs());
//                    }
//                    // If any stage reported failure, mark overall as failed
//                    if (res != null && res.getStatus() != null && res.getStatus() != StageResultStatus.SUCCESSFUL) {
//                        anyFailed.set(true);
//                    }
//                } catch (Exception ignored) {
//                    // already marked as failed in the supplier
//                }
//            }
//
//            if (anyFailed.get()) {
//                // Send kafka notification about other stages
//                List<Stage> failedStages = stages.stream()
//                        .flatMap(List::stream)
//                        .filter(stage -> !finshedStageIds.contains(stage.getStageId()))
//                        .collect(Collectors.toList());
//
//                return new PipelineResult(PipelineResultStatus.FAILED);
//            }
//        }
//
//        // Persist accumulated envs back to the event so callers can see final envs
//        event.setEnvToSet(accumulatedEnv);
//
//        return new PipelineResult(PipelineResultStatus.SUCCESSFUL);
//    }
//}
