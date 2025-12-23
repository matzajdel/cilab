package org.example.pipelineservice.repository;

import org.example.pipelineservice.model.pipeline.Pipeline;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PipelineRepository extends MongoRepository<Pipeline, String> {
}
