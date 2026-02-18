package org.example.pipelineservice.repository;

import org.example.pipelineservice.dto.PipelineRunSummaryDTO;
import org.example.pipelineservice.model.pipeline.Stage;
import org.example.pipelineservice.model.pipelineRun.PipelineRun;
import org.example.pipelineservice.model.pipelineRun.PipelineStatus;
import org.example.pipelineservice.model.pipelineRun.StageRunInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PipelineRunRepository extends MongoRepository<PipelineRun, String> {
    PipelineRun findByStagesInfoStageId(String stageId);

    Optional<List<PipelineRun>> findByPipelineId(String pipelineId);

    Optional<PipelineRun> findByRunId(String runId);
}