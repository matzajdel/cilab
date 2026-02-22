package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.dto.RepoDTO;
import org.example.versioncontrolserver.entities.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoRepository extends JpaRepository<Repo, Long> {
    Optional<Repo> findByName(String repoName);

    List<RepoDTO> findAllBy();
}
