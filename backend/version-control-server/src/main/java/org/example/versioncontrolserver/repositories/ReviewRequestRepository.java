package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.entities.ReviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {
    Optional<ReviewRequest> findByCommitId(String commitId);
}
