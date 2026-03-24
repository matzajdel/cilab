package org.example.versioncontrolserver.controllers;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.*;
import org.example.versioncontrolserver.services.RepositoryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class RepositoryQueryController {
    private final RepositoryQueryService service;
    private final RepositoryQueryService repositoryQueryService;

    @GetMapping("/repos")
    public List<RepoDTO> getAllRepositories() {
        return service.getAllRepositories();
    }

    @GetMapping("/commits")
    public ResponseEntity<List<CommitSummaryDTO>> getLastCommitsByUser (@RequestParam String authorEmail) {
        return ResponseEntity.ok(service.getLastCommitsByUser(authorEmail));
    }

    @GetMapping("/repos/{repoId}/commits")
    public ResponseEntity<List<CommitSummaryDTO>> getCommitsByRepository(@PathVariable String repoId) {
        return ResponseEntity.ok(service.getCommitsByRepository(repoId));
    }

    @GetMapping("/commits/{commitId}")
    public ResponseEntity<CommitDetailsDTO> getCommitById(@PathVariable String commitId) {
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

    @PostMapping("/labels")
    public ResponseEntity<Void> saveLabel(
            @RequestBody LabelDTO dto,
            @RequestHeader("X-User-Email") String authorEmail
    ) {
        repositoryQueryService.saveLabel(dto, authorEmail);
        return ResponseEntity.ok().build();
    }
}
