package org.example.pipelineservice.kafka.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageAssignedEvent {
    private String id;
    private String image;
    private String script;
}
