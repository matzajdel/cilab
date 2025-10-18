package cz.example.executors;

import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TaskExecutor {

    public TaskResult execute(PipelineAssignedEvent event) throws Exception {
        List<List<Stage>> steps = event.getStages();
        AtomicBoolean anyFailed = new AtomicBoolean(false);

        for (List<Stage> stepGroup : steps) {
            List<CompletableFuture<Void>> futures = stepGroup.stream()
                    .map(stage -> CompletableFuture.runAsync(() -> {
                        try {
                            // Run the stage (blocking call to Docker runner)
                            DockerTaskRunner runner = new DockerTaskRunner();
                            runner.runDockerTask(stage.getScript(), event.getEnvToSet());

                        } catch (Exception e) {
                            // mark failure and rethrow to complete the future exceptionally
                            anyFailed.set(true);
                            throw new RuntimeException(e);
                        }
                    }))
                    .collect(Collectors.toList());

            // Wait for all stages in this group to finish
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception ignored) {
                // exceptions are recorded in anyFailed; join() wraps exceptions in CompletionException
            }

            // If any stage failed in this group, fail-fast and stop executing further groups
            if (anyFailed.get()) {
                return new TaskResult(TaskResultStatus.FAILED);
            }
        }

        // Use SUCCESSFULL (requested) while keeping SUCCESSFUL present for backward compatibility
        return new TaskResult(TaskResultStatus.SUCCESSFULL);
    }
}
