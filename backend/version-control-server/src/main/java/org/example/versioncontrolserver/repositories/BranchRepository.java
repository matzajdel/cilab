package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.dto.BranchDTO;
import org.example.versioncontrolserver.entities.Branch;
import org.example.versioncontrolserver.entities.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByRepoAndName(Repo repo, String s);

    Optional<Branch> findByName(String branchName);

    List<BranchDTO> findAllByRepo_Id(long l);
}
