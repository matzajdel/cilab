package org.example.pipelineservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PipelineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineServiceApplication.class, args);
    }

//    @Bean
//    public CommandLineRunner cmdLineRunner(PipelineAssignedPublisher publisher) {
//        return args -> {
//            publisher.publish(PipelineAssignedEvent.builder()
//                            .pipelineId(UUID.randomUUID().toString())
//                            .pipelineRunId(UUID.randomUUID().toString())
//                            .stages(null)
//                            .envToSet(Map.of("TEST_RUN", "TRUE"))
//                    .build());
//        };
//    }

}
