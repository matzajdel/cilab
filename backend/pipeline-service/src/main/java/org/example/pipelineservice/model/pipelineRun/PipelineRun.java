package org.example.pipelineservice.model.pipelineRun;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "pipeline_runs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineRun {
    @Id
    private String runId;
    private String pipelineId;
    private Map<String, String> parameters;
    private Map<String, String> envVariables;
    private List<StageRunInfo> stagesInfo;
    private Map<String, Integer> labels;
    private PipelineStatus status;
    @CreatedDate
    private Instant startTime;
    private Instant endTime;
    private String runBy;
}
