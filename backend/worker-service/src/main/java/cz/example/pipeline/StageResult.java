package cz.example.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StageResult {
    private String stageId;
    private StageResultStatus status;
    private Map<String, String> resultEnvs;
    private String message;
    private Instant startTime;
    private Instant endTime;

    public StageResult(String stageId, StageResultStatus status) {
        this.stageId = stageId;
        this.status = status;
    }

    public StageResult(StageResultStatus status, Map<String, String> resultEnvs, String message) {
        this.status = status;
        this.resultEnvs = resultEnvs;
        this.message = message;
    }
}
