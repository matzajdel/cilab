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

        Stage stage1 = new Stage(UUID.randomUUID(), "echo STAGE 1 && sleep 2 && echo REPO_PATH: ${REPO_PATH} && echo ${STORAGE_PATH} && export ST1_FINAL_MESSAGE=\"Repo dspAlsaLib pulled successfully\" && echo STAGE1 completed && echo __STAGE_RESULTS__ST1_FINAL_MESSAGE=${ST1_FINAL_MESSAGE}");
        Stage stage2 = new Stage(UUID.randomUUID(), "echo STAGE 2 && sleep 2 && echo ${ST1_FINAL_MESSAGE} && echo Stage 2 completed.");
//        Stage stage4 = new Stage(UUID.randomUUID(), "echo from: Stage 4 && sleep 3 && echo Stage 4 completed.");

        PipelineAssignedEvent pipelineEvent = new PipelineAssignedEvent(
                UUID.randomUUID(),
                List.of(List.of(stage1), List.of(stage2)),
                Map.of(
                        "REPO_PATH", "/mnt/storage/AudioManagerPLatform/repo",
                        "STORAGE_PATH", "/mnt/storage"
                )
        );
        parallelPipelineExecutor.submit(pipelineEvent);
    }
}
