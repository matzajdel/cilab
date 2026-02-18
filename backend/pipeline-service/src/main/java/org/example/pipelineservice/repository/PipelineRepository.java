package org.example.pipelineservice.repository;

import org.example.pipelineservice.dto.PipelineSummaryDTO;
import org.example.pipelineservice.model.pipeline.Pipeline;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PipelineRepository extends MongoRepository<Pipeline, String> {
    @Query(value = "{}", fields = "{ 'name': 1, 'authorEmail': 1, 'lastUpdated': 1 }")
    List<PipelineSummaryDTO> findAllPipelineSummaries();
}
