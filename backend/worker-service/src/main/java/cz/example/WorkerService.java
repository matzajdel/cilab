package cz.example;

import cz.example.executors.ParallelPipelineExecutor;
import cz.example.executors.TaskAssignedEvent;
import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.Stage;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class WorkerService {

    public static void main(String[] args) throws Exception {
        ParallelPipelineExecutor parallelPipelineExecutor = new ParallelPipelineExecutor(4, 16, 100);
//        parallelPipelineExecutor.submit(new TaskAssignedEvent("asdjkajsdjsldak && echo Hello from Docker nr 1! && sleep 10 && echo Task 1 completed."));
//        parallelTaskExecutor.submit(new TaskAssignedEvent("echo Hello from Docker nr 2! && sleep 1 && echo Task 2 completed."));

        Stage stage1 = new Stage(UUID.randomUUID(),
                "echo STAGE1" +
                        " && " + "sleep 1" +
                        " && " + "echo ${testText}" +
                        " && " + "export readTestValue=\"Barcelona moj klub\"" +
                        " && echo Stage 1 completed.");
//        Stage stage2 = new Stage(UUID.randomUUID(), "echo from: Parallel Stage 2 && sleep 10 && echo Parallel Stage 2 completed.");
//        Stage stage3 = new Stage(UUID.randomUUID(), "echo from: Parallel Stage 3 && sleep 1 && echo Parallel Stage 3 completed.");
//        Stage stage4 = new Stage(UUID.randomUUID(), "echo from: Stage 4 && sleep 3 && echo Stage 4 completed.");

        PipelineAssignedEvent pipelineEvent = new PipelineAssignedEvent(
                UUID.randomUUID(),
                List.of(List.of(stage1)),
                Map.of(
                        "testText", "Jaki dzisiaj piekny dzien"
                )
        );
        parallelPipelineExecutor.submit(pipelineEvent);
    }
}
