package org.example.versioncontrolserver.controllers;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.BranchDTO;
import org.example.versioncontrolserver.dto.CommitFileDTO;
import org.example.versioncontrolserver.dto.CommitSummaryDTO;
import org.example.versioncontrolserver.dto.RepoDTO;
import org.example.versioncontrolserver.services.RepositoryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class RepositoryQueryController {
    private final RepositoryQueryService service;

    @GetMapping("/repos")
    public List<RepoDTO> getAllRepositories() {
        return service.getAllRepositories();
    }

    @GetMapping("/repos/{repoId}/commits")
    public ResponseEntity<List<CommitSummaryDTO>> getCommitsByRepository(@PathVariable String repoId) {
        return ResponseEntity.ok(service.getCommitsByRepository(repoId));
    }

    @GetMapping("/commits/{commitId}")
    public ResponseEntity<CommitSummaryDTO> getCommitById(@PathVariable String commitId) {
        return ResponseEntity.ok(service.getCommitById(commitId));
    }

    @GetMapping("/repos/{repoId}/branches")
    public ResponseEntity<List<BranchDTO>> getBranchesByRepo(@PathVariable String repoId) {
        return ResponseEntity.ok(service.getBranchesByRepository(repoId));
    }

//    @GetMapping("/repos/{repoId}/branches/{branchId}/files")
//    public List<CommitFileDTO> getBranchFiles(@PathVariable String repoId, @PathVariable String branchId) {
//
//    }

    @GetMapping("/commits/{commitId}/diff")
    public ResponseEntity<List<CommitFileDTO>> getDiffFilesByCommit(@PathVariable String commitId) {
        return ResponseEntity.ok(service.getDiffFilesByCommit(commitId));
    }
}
