package cz.example.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.pipeline.PipelineResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class PipelineResultProducer implements Closeable {

    private final KafkaProducer<String, String> kafkaProducer;
    private final static ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static String TOPIC = "pipeline-result-events";

    public PipelineResultProducer() {
        this.kafkaProducer = new KafkaProducer<>(setKafkaProducerProperties());
    }

    public void send(PipelineResult event) {
        try {
            String eventAsString = objectMapper.writeValueAsString(event);
            String key = UUID.randomUUID().toString();
            kafkaProducer.send(new ProducerRecord<>(TOPIC, key, eventAsString));
        } catch (Exception e)  {
            log.error("Error while sending pipeline result event to Kafka {}", e);
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
