package cz.example.executors;

import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.PipelineResultStatus;
import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.Stage;
import cz.example.pipeline.StageResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TaskExecutor {

    public PipelineResult execute(PipelineAssignedEvent event) throws Exception {
        List<List<Stage>> steps = event.getStages();

        // ?
        AtomicBoolean anyFailed = new AtomicBoolean(false);

        Map<String, String> accumulatedEnv = new HashMap<>();
        if (event.getEnvToSet() != null) {
            accumulatedEnv.putAll(event.getEnvToSet());
        }

        for (List<Stage> stepGroup : steps) {
            List<CompletableFuture<StageResult>> futures = stepGroup.stream()
                    .map(stage -> CompletableFuture.supplyAsync(() -> {
                        try {
                            DockerTaskRunner runner = new DockerTaskRunner();
                            Map<String, String> envSnapshot = new HashMap<>(accumulatedEnv);
                            StageResult result = runner.runDockerTask(stage.getScript(), envSnapshot);
                            result.setStageId(stage.getStageId());
                            return result;
                        } catch (Exception e) {
                            anyFailed.set(true);
                            throw new RuntimeException(e);
                        }
                    }))
                    .collect(Collectors.toList());

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception ignored) {
            }

            for (CompletableFuture<StageResult> f : futures) {
                try {
                    StageResult res = f.join();
                    if (res != null && res.getResultEnvs() != null) {
                        accumulatedEnv.putAll(res.getResultEnvs());
                    }
                    // If any stage reported failure, mark overall as failed
                    if (res != null && res.getStatus() != null && res.getStatus() != PipelineResultStatus.SUCCESSFUL) {
                        anyFailed.set(true);
                    }
                } catch (Exception ignored) {
                    // already marked as failed in the supplier
                }
            }

            if (anyFailed.get()) {
                // Send kafka notification about other stages
                return new PipelineResult(PipelineResultStatus.FAILED);
            }
        }

        // Persist accumulated envs back to the event so callers can see final envs
        event.setEnvToSet(accumulatedEnv);

        return new PipelineResult(PipelineResultStatus.SUCCESSFUL);
    }
}
