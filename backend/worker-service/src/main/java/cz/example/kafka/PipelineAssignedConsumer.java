package cz.example.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.executors.ParallelPipelineExecutor;
import cz.example.pipeline.PipelineAssignedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class PipelineAssignedConsumer implements Runnable {

    private final String groupId;
    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ObjectMapper objectMapper;
    private final ParallelPipelineExecutor pipelineExecutor;
    private volatile boolean running = true;

    public PipelineAssignedConsumer(String groupId, String topic) {
        this.groupId = groupId;

        this.kafkaConsumer = new KafkaConsumer<>(setKafkaConsumerProperties());
        this.kafkaConsumer.subscribe(List.of(topic));

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.pipelineExecutor = new ParallelPipelineExecutor(4, 16, 100);
    }

    @Override
    public void run() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        PipelineAssignedEvent event = objectMapper.readValue(record.value(), PipelineAssignedEvent.class);
                        log.info("Received object: {}", objectMapper.writeValueAsString(event));
                        pipelineExecutor.submit(event);
                        commitOffset(record);
                    } catch (Exception e) {
                        log.error("Error processing message from topic {}: {}", record.topic(), e.getMessage(), e);
                        // TODO: DLT (Dead Letter Topic)
                    }
                }
            }
        } catch (WakeupException e) {
            if (running) throw e; // Wakeup używany do przerwania poll() przy zamykaniu
        } catch (Exception e) {
            log.error("Unexpected error in consumer loop: {}", e.getMessage(), e);
        } finally {
            closeConsumer();
        }
    }

    private void commitOffset(ConsumerRecord<String, String> record) {
        try {
            TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
            OffsetAndMetadata offset = new OffsetAndMetadata(record.offset() + 1);
            kafkaConsumer.commitSync(Map.of(topicPartition, offset));
        } catch (Exception e) {
            log.warn("Failed to commit offset for topic {} partition {}: {}", record.topic(), record.partition(), e.getMessage());
        }
    }

    private void closeConsumer () {
        try {
            kafkaConsumer.close();
        } catch (Exception e) {
            log.warn("Error while closing consumer: {}", e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        kafkaConsumer.wakeup();
    }

    private Properties setKafkaConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50"); // kontrola batcha
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"); // 5 minut na przetwarzanie

        return props;
    }
}
