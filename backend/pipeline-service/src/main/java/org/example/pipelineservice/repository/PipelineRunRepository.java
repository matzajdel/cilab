package org.example.pipelineservice.repository;

import org.example.pipelineservice.dto.PipelineRunSummaryDTO;
import org.example.pipelineservice.model.pipelineRun.PipelineRun;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PipelineRunRepository extends MongoRepository<PipelineRun, String> {
    PipelineRun findByStagesInfoStageId(String stageId);

    Optional<List<PipelineRun>> findByPipelineId(String pipelineId);

    Optional<PipelineRun> findByRunId(String runId);

    List<PipelineRunSummaryDTO> findTop6AllByRunByOrderByStartTimeDesc(String authorEmail);

    @Query(value = "{ '_id' : ?0 }", fields = "{ 'parameters.COMMIT_ID' : 1 }")
    Optional<PipelineRun> findProjectedById(String runId);

    default Optional<String> findOnlyCommitIdById(String runId) {
        return findProjectedById(runId)
                .map(PipelineRun::getParameters)
                .map(params -> params.get("COMMIT_ID"));
    }
}