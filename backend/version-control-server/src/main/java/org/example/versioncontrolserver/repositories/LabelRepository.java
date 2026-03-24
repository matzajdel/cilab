package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.entities.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByCommitIdAndName(String commitId, String name);
}
