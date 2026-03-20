package org.example.pipelineservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.pipelineservice.dto.RunPipelineDTO;
import org.example.pipelineservice.kafka.events.PipelineResultEvent;
import org.example.pipelineservice.kafka.events.StageResultEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PipelineResultEvent> pipelineResultContainerFactory() {
        return createContainerFactory(PipelineResultEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StageResultEvent> stageResultContainerFactory() {
        return createContainerFactory(StageResultEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RunPipelineDTO> webhookContainerFactory() {
        return createContainerFactory(RunPipelineDTO.class);
    }

    private <T> ConsumerFactory<String, T> createConsumerFactory(Class<T> clazz) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        //Json Deserializer settings
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, clazz.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "cz.example.*");

        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createContainerFactory(Class<T> clazz) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(clazz));
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }
}
