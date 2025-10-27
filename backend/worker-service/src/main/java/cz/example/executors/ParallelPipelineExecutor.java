package cz.example.executors;

import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.PipelineAssignedEvent;

import java.util.concurrent.*;

public class ParallelPipelineExecutor {
    private final ExecutorService pipelineExecutorService;
    private final TaskExecutor taskExecutor;

    public ParallelPipelineExecutor(
            int corePool,
            int maxPool,
            int queueSize
    ) {
        this.pipelineExecutorService = new ThreadPoolExecutor(
            corePool,
            maxPool,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueSize),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.taskExecutor = new TaskExecutor();
    }

    public void submit(PipelineAssignedEvent event) {
        // Run pipeline
        pipelineExecutorService.submit(() -> {
            try {
                PipelineResult result = taskExecutor.execute(event);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
