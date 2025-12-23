package cz.example.executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.kafka.PipelineResultProducer;
import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.PipelineResultStatus;

import java.util.concurrent.*;

public class ParallelPipelineExecutor {
    private final ExecutorService pipelineExecutorService;
    private final TaskExecutor taskExecutor = new TaskExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PipelineResultProducer pipelineResultProducer = new PipelineResultProducer();

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
    }

    public void submit(PipelineAssignedEvent event) {
        // Run pipeline
        pipelineExecutorService.submit(() -> {
            try {
                // Send kafka notification about pipeline start
                System.out.println("FAKEKAFKA: Pipeline started: " + objectMapper.writeValueAsString(event));
                pipelineResultProducer.sendPipelineResult(new PipelineResult(event.getPipelineRunId(), PipelineResultStatus.IN_PROGRESS));

                PipelineResult result = taskExecutor.execute(event);

                // Send kafka notification about pipeline end with result
                System.out.println("FAKEKAFKA: Pipeline finished: " + objectMapper.writeValueAsString(result));
                pipelineResultProducer.sendPipelineResult(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
