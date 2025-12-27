package cz.example.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.example.pipeline.PipelineResult;
import cz.example.pipeline.StageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class PipelineResultProducer implements Closeable {

    private final KafkaProducer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;
    private final static String PIPELINE_RESULT_TOPIC = "pipeline-result-events";
    private final static String STAGE_RESULT_TOPIC = "stage-result-events";

    public PipelineResultProducer(ObjectMapper objectMapper, KafkaProducer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    public void sendPipelineResult(PipelineResult event) {
        try {
            String key = event.getPipelineRunId();
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord record = new ProducerRecord<>(PIPELINE_RESULT_TOPIC, key, payload);
            kafkaProducer.send(record, new LoggingCallback("PipelineResult", key));
        } catch (Exception e)  { //TODO
            log.error("Error while sending pipeline result event to Kafka {}", e);
        }
    }

    public void sendStageResult(StageResult event) {
        try {
            String key = event.getStageId();
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord record = new ProducerRecord<>(STAGE_RESULT_TOPIC, key, payload);
            kafkaProducer.send(record, new LoggingCallback("StageResult", key));
        } catch (Exception e) {
            log.error("Error while sending stage result event to Kafka {}", e);
        }
    }

    private record LoggingCallback(String eventType, String key) implements Callback {

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e == null) {
                log.debug("Sent event: {} [key: {}] to topic: {} partition: {} @ offset {}",
                        eventType, key, recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
            } else {
                log.error("Failed to send {} [key: {}] to Kafka: {}", eventType, key, e.getMessage());
            }
        }
    }

    private Properties setKafkaProducerProperties() {
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

    @Override
    public void close() {
        try {
            kafkaProducer.flush();
            kafkaProducer.close();
        } catch (Exception e) {
            log.error("Error while closing Kafka producer {}", e);
        }
    }
}
