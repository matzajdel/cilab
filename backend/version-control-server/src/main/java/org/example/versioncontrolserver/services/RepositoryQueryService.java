package org.example.versioncontrolserver.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.*;
import org.example.versioncontrolserver.entities.Branch;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.CommitStatus;
import org.example.versioncontrolserver.entities.Repo;
import org.example.versioncontrolserver.mapper.CommitFileMapper;
import org.example.versioncontrolserver.mapper.CommitMapper;
import org.example.versioncontrolserver.repositories.BranchRepository;
import org.example.versioncontrolserver.repositories.CommitRepository;
import org.example.versioncontrolserver.repositories.RepoRepository;
import org.example.versioncontrolserver.repositories.ReviewRequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RepositoryQueryService {

    private final CommitMapper commitMapper;
    private final CommitFileMapper commitFileMapper;

    private final RepoRepository repoRepository;
    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;

    public List<RepoDTO> getAllRepositories() {
        return repoRepository.findAllBy();
    }

    public CommitSummaryDTO getCommitById(String commitId) {
        return commitRepository.findProjectedById(commitId)
                .orElseThrow(() -> new EntityNotFoundException("Not found commit with id: " + commitId));
    }

    public List<CommitSummaryDTO> getCommitsByRepository(String repoId) {
        return commitRepository.findAllByRepo_IdOrderByTimestampDesc(Long.parseLong(repoId));
    }

    public List<CommitSummaryDTO> getLastCommitsByUser(String authorEmail) {
        return commitRepository.findFirst6AllByAuthorEmailOrderByTimestampDesc(authorEmail);
    }

    public List<BranchDTO> getBranchesByRepository(String repoId) {
        return branchRepository.findAllByRepo_Id(Long.parseLong(repoId));
    }

    @Transactional(readOnly = true)
    public List<CommitFileDTO> getDiffFilesByCommit(String commitId) {
        Commit currentCommit = commitRepository.findById(commitId)
                .orElseThrow(() -> new EntityNotFoundException("Commit with id: " + commitId + " not found"));

        String parentCommitId = currentCommit.getParentId();
        if (parentCommitId == null) {
            return currentCommit.getFiles().stream()
                    .map(commitFileMapper::toDTO)
                    .toList();
        }

        Commit parentCommit = commitRepository.findById(parentCommitId)
                .orElseThrow(() -> new EntityNotFoundException("Unable to find parent commit"));

        Map<String, String> parentCommitFiles = parentCommit.getFileMap();

        return currentCommit.getFiles().stream()
                .filter(ccf -> {
                    String parentHash = parentCommitFiles.get(ccf.getPath());

                    return !Objects.equals(ccf.getBlobHash(), parentHash);
                })
                .map(commitFileMapper::toDTO)
                .toList();
    }
}
