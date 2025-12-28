package cz.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dockerjava.api.DockerClient;
import cz.example.executors.DockerTaskRunner;
import cz.example.executors.ParallelPipelineExecutor;
import cz.example.executors.TaskExecutor;
import cz.example.executors.docker.DockerClientFactory;
import cz.example.kafka.PipelineAssignedConsumer;
import cz.example.kafka.PipelineResultProducer;
import cz.example.loki.LokiService;
import cz.example.pipeline.PipelineAssignedEvent;
import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.Stage;
import cz.example.pipeline.StageResultStatus;
import cz.example.redis.RedisLogPublisher;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


public class WorkerService {

    public static void main(String[] args) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(setKafkaProducerProperties());
        PipelineResultProducer resultProducer = new PipelineResultProducer(objectMapper, kafkaProducer);

        RedisLogPublisher redisLogPublisher = new RedisLogPublisher(
                objectMapper,
                System.getenv().getOrDefault("REDIS_HOST", "localhost"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))
        );
        LokiService.init("http://localhost:3100", objectMapper);

        DockerClient dockerClient = DockerClientFactory.createInstance();
        DockerTaskRunner dockerRunner = new DockerTaskRunner(dockerClient, objectMapper, redisLogPublisher);

        TaskExecutor taskExecutor = new TaskExecutor(objectMapper, resultProducer, dockerRunner);

        ParallelPipelineExecutor parallelPipelineExecutor = new ParallelPipelineExecutor(
                objectMapper,
                taskExecutor,
                resultProducer,
                4, 16, 100
        );

        PipelineAssignedConsumer pipelineAssignedConsumer = new PipelineAssignedConsumer(
                "worker-service-group",
                "pipeline-assigned-events",
                objectMapper,
                parallelPipelineExecutor
        );

        Thread kafkaConsumerThread = new Thread(pipelineAssignedConsumer);
        kafkaConsumerThread.start();

        // Shutdown hook:
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            pipelineAssignedConsumer.shutdown();
            try {
                kafkaConsumerThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                parallelPipelineExecutor.shutdown();
                resultProducer.close();

                try {
                    dockerClient.close();
                } catch (IOException e) {
                    System.err.println("Error closing Docker client: " + e.getMessage());
                }
            }
        }));
    }

    private static Properties setKafkaProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);    //32 KB batch size

        return props;
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
