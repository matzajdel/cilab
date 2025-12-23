package org.example.pipelineservice.model.pipeline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "pipeline_definitions")
@EnableMongoAuditing
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {
    @Id
    private String id;
    private String name;
    private String authorEmail;
    private List<PipelineParameter> parameters;
    private Map<String, String> envVariables;
    private List<List<Stage>> stages; //List<List<>> for parallel stages
    private List<String> labels;
    @LastModifiedDate
    private Instant lastUpdated;
}
