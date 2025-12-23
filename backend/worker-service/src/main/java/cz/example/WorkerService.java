package cz.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.executors.ParallelPipelineExecutor;
import cz.example.kafka.PipelineAssignedConsumer;
import cz.example.kafka.PipelineResultProducer;
import cz.example.loki.LokiService;
import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.Stage;
import cz.example.pipeline.StageResultStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class WorkerService {

    public WorkerService() {
//        this.producer = new PipelineResultProducer();
//
//        // Rejestrujemy shutdown hook, żeby zamknąć producenta przy wyjściu
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("Shutting down worker service...");
//            try {
//                producer.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }));
    }

    public static void main(String[] args) throws Exception {

        Thread kafkaConsumerThread = new Thread(new PipelineAssignedConsumer(
                "worker-service-group",
                "pipeline-assigned-events"
        ));
        kafkaConsumerThread.setDaemon(false);
        kafkaConsumerThread.start();

        ObjectMapper objectMapper = new ObjectMapper();
        LokiService.init("http://localhost:3100", objectMapper);
    }
}





//        PipelineResultProducer producer = new PipelineResultProducer();
//        producer.send(new PipelineResult(StageResultStatus.SUCCESSFUL));
//


//        ParallelPipelineExecutor parallelPipelineExecutor = new ParallelPipelineExecutor(4, 16, 100);
////        parallelPipelineExecutor.submit(new TaskAssignedEvent("asdjkajsdjsldak && echo Hello from Docker nr 1! && sleep 10 && echo Task 1 completed."));
////        parallelTaskExecutor.submit(new TaskAssignedEvent("echo Hello from Docker nr 2! && sleep 1 && echo Task 2 completed."));
//
//        Stage stage1 = new Stage(UUID.randomUUID().toString(), "busybox:latest", "echo P1S1: STAGE 1 started && sleep 10 && echo P1S1: REPO_PATH: ${REPO_PATH} && echo P1S1: ${STORAGE_PATH} && export ST1_FINAL_MESSAGE=\"Repo dspAlsaLib pulled successfully\" && echo P1S1: STAGE1 completed && echo __STAGE_RESULTS__ST1_FINAL_MESSAGE=${ST1_FINAL_MESSAGE}");
//        Stage stage2 = new Stage(UUID.randomUUID().toString(), "busybox:latest", "echo P1S2: STAGE 2 started && sadasfasdafsleep 10 && echo P1S2: ${ST1_FINAL_MESSAGE} && echo P1S2: Stage 2 completed.");
//        Stage stage3 = new Stage(UUID.randomUUID().toString(), "busybox:latest", "echo P1S3: STAGE 3 started && sleep 4 && echo P1S3: ${ST1_FINAL_MESSAGE} && echo P1S3: Stage 3 completed.");
//        Stage stage4 = new Stage(UUID.randomUUID().toString(), "busybox:latest", "echo P1S4: Pipeline 1 successfully finished");
//
////        Stage p2stage1 = new Stage(UUID.randomUUID(), "echo P2S1: STAGE 1 started && sleep 2 && echo P1S1: REPO_PATH: ${REPO_PATH} && echo P2S1: ${STORAGE_PATH} && export ST1_FINAL_MESSAGE=\"Repo dspAlsaLib pulled successfully\" && echo P2S1: STAGE1 completed && echo __STAGE_RESULTS__ST1_FINAL_MESSAGE=${ST1_FINAL_MESSAGE}");
////        Stage p2stage2 = new Stage(UUID.randomUUID(), "echo P2S2: STAGE 2 started && sleep 7 && echo P2S2: ${ST1_FINAL_MESSAGE} && echo P2S2: Stage 2 completed.");
////        Stage p2stage3 = new Stage(UUID.randomUUID(), "echo P2S3: STAGE 3 started && sleep 10 && echo P2S3: ${ST1_FINAL_MESSAGE} && echo P2S3: Stage 3 completed.");
////        Stage p2stage4 = new Stage(UUID.randomUUID(), "echo P2S4: Pipeline 2 successfully finished");
//
//        PipelineAssignedEvent pipelineEvent = new PipelineAssignedEvent(
//                UUID.randomUUID().toString(),
//                UUID.randomUUID().toString(),
//                List.of(List.of(stage1), List.of(stage2, stage3), List.of(stage4)),
//                Map.of(
//                        "REPO_PATH", "/mnt/storage/AudioManagerPLatform/repo",
//                        "STORAGE_PATH", "/mnt/storage"
//                )
//        );
//        parallelPipelineExecutor.submit(pipelineEvent);






//        ParallelPipelineExecutor parallelPipelineExecutor = new ParallelPipelineExecutor(4, 16, 100);
////        parallelPipelineExecutor.submit(new TaskAssignedEvent("asdjkajsdjsldak && echo Hello from Docker nr 1! && sleep 10 && echo Task 1 completed."));
////        parallelTaskExecutor.submit(new TaskAssignedEvent("echo Hello from Docker nr 2! && sleep 1 && echo Task 2 completed."));
//
//        Stage stage1 = new Stage(UUID.randomUUID(), "echo P1S1: STAGE 1 started && sleep 20 && echo P1S1: REPO_PATH: ${REPO_PATH} && echo P1S1: ${STORAGE_PATH} && export ST1_FINAL_MESSAGE=\"Repo dspAlsaLib pulled successfully\" && echo P1S1: STAGE1 completed && echo __STAGE_RESULTS__ST1_FINAL_MESSAGE=${ST1_FINAL_MESSAGE}");
//        Stage stage2 = new Stage(UUID.randomUUID(), "echo P1S2: STAGE 2 started && sleep 10 && echo P1S2: ${ST1_FINAL_MESSAGE} && echo P1S2: Stage 2 completed.");
//        Stage stage3 = new Stage(UUID.randomUUID(), "echo P1S3: STAGE 3 started && sleep 4 && echo P1S3: ${ST1_FINAL_MESSAGE} && echo P1S3: Stage 3 completed.");
//        Stage stage4 = new Stage(UUID.randomUUID(), "echo P1S4: Pipeline 1 successfully finished");
//
//        Stage p2stage1 = new Stage(UUID.randomUUID(), "echo P2S1: STAGE 1 started && sleep 2 && echo P1S1: REPO_PATH: ${REPO_PATH} && echo P2S1: ${STORAGE_PATH} && export ST1_FINAL_MESSAGE=\"Repo dspAlsaLib pulled successfully\" && echo P2S1: STAGE1 completed && echo __STAGE_RESULTS__ST1_FINAL_MESSAGE=${ST1_FINAL_MESSAGE}");
//        Stage p2stage2 = new Stage(UUID.randomUUID(), "echo P2S2: STAGE 2 started && sleep 7 && echo P2S2: ${ST1_FINAL_MESSAGE} && echo P2S2: Stage 2 completed.");
//        Stage p2stage3 = new Stage(UUID.randomUUID(), "echo P2S3: STAGE 3 started && sleep 10 && echo P2S3: ${ST1_FINAL_MESSAGE} && echo P2S3: Stage 3 completed.");
//        Stage p2stage4 = new Stage(UUID.randomUUID(), "echo P2S4: Pipeline 2 successfully finished");
//
//        PipelineAssignedEvent pipelineEvent = new PipelineAssignedEvent(
//                UUID.randomUUID(),
//                List.of(List.of(stage1), List.of(stage2, stage3), List.of(stage4)),
//                Map.of(
//                        "REPO_PATH", "/mnt/storage/AudioManagerPLatform/repo",
//                        "STORAGE_PATH", "/mnt/storage"
//                )
//        );
//        parallelPipelineExecutor.submit(pipelineEvent);
//
//        PipelineAssignedEvent pipelineEvent2 = new PipelineAssignedEvent(
//                UUID.randomUUID(),
//                List.of(List.of(p2stage1), List.of(p2stage2, p2stage3), List.of(p2stage4)),
//                Map.of(
//                        "REPO_PATH", "/mnt/storage/AudioManagerPLatform/repo",
//                        "STORAGE_PATH", "/mnt/storage"
//                )
//        );
//        parallelPipelineExecutor.submit(pipelineEvent2);
