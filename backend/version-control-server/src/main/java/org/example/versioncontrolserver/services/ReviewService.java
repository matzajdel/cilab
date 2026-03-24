package org.example.versioncontrolserver.services;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.MessageDTO;
import org.example.versioncontrolserver.entities.*;
import org.example.versioncontrolserver.exception.SubmitRejectedException;
import org.example.versioncontrolserver.mapper.CommitFileMapper;
import org.example.versioncontrolserver.mapper.CommitMapper;
import org.example.versioncontrolserver.repositories.BranchRepository;
import org.example.versioncontrolserver.repositories.CommitRepository;
import org.example.versioncontrolserver.repositories.RepoRepository;
import org.example.versioncontrolserver.repositories.ReviewRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final RepoRepository repoRepository;
    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;
    private final ReviewRequestRepository reviewRequestRepository;

    private final RepositoryQueryService repositoryQueryService;

    public void createReviewRequest(String commitId, String realTargetBranch, String message) {
        if (reviewRequestRepository.findByCommitId(commitId).isEmpty()) {
            reviewRequestRepository.save(
                    ReviewRequest.builder()
                            .commitId(commitId)
                            .targetBranch(realTargetBranch)
                            .message(message)
                            .build()
            );
        }
    }

    @Transactional
    public void submitReview(String commitId, String authorEmail) throws SubmitRejectedException {
        Commit commit = commitRepository.findById(commitId)
                .orElseThrow(() -> new IllegalArgumentException("Commit not found for commit: " + commitId));
        
        checkLabels(commit.getLabels());
        
        Repo repo = commit.getRepo();

        ReviewRequest request = reviewRequestRepository.findByCommitId(commitId)
                .orElseThrow(() -> new IllegalArgumentException("Review request not found for commit: " + commitId));


        commit.setStatus(CommitStatus.MERGED);
        commitRepository.save(commit);

        String targetBranchName = request.getTargetBranch();;

        Branch branch = branchRepository.findByRepoAndName(repo, targetBranchName)
                .orElseGet(() -> Branch.builder()
                        .name(targetBranchName)
                        .repo(repo)
                        .build());

        String currentHeadId = branch.getHeadCommitId();

        if (currentHeadId != null && !currentHeadId.equals(commitId)) {
            boolean isSafe = isFastForward(commitId, currentHeadId);

            if (!isSafe) {
                throw new IllegalStateException(
                        "Submit failed: NOT_FAST_FORWARD. " +
                                "Target branch '" + targetBranchName + "' has moved forward since you started. " +
                                "Please pull latest changes, rebase your work, and push again."
                );
            }
        }

        branch.setHeadCommitId(commitId);
        branchRepository.save(branch);

        reviewRequestRepository.delete(request);

        repositoryQueryService.saveMessage(MessageDTO.builder()
                .text("User " + authorEmail + " rebased change")
                .authorEmail(authorEmail)
                .commitId(commitId)
                .build());

        System.out.println("Submitted review " + commitId + " to branch " + targetBranchName);
    }

    private void checkLabels(List<Label> labels) throws SubmitRejectedException {
        for (Label label : labels) {
            if (label.getValue() < 1) {
                throw new SubmitRejectedException("Check labels aren't set");
            }
        }
    }

    private boolean isFastForward(String descendantId, String ancestorId) {
        if (ancestorId == null) return true;
        if (descendantId.equals(ancestorId)) return true;

        return commitRepository.isAncestor(descendantId, ancestorId);
    }

    @Transactional
    public String rebase(String rebasedCommitId, String targetBranchName, String authorEmail) {
        Commit rebasedCommit = commitRepository.findById(rebasedCommitId)
                .orElseThrow(() -> new IllegalArgumentException("Commit object not found: " + rebasedCommitId));

        Repo repo = rebasedCommit.getRepo();

        Branch branch = branchRepository.findByName(targetBranchName)
                .orElseThrow(() -> new IllegalArgumentException("Target branch '" + targetBranchName + "' does not exist"));

        String upstreamHeadId = branch.getHeadCommitId();

        if (upstreamHeadId.equals(rebasedCommit.getParentId())) {
            System.out.println("Commit is already based on " + targetBranchName);
            return rebasedCommitId;
        }

        Commit upstreamCommit = commitRepository.findById(upstreamHeadId)
                .orElseThrow(() -> new IllegalStateException("Upstream commit data missing"));

        String baseCommitId = rebasedCommit.getParentId();
        Commit baseCommit = baseCommitId != null
                ? commitRepository.findById(baseCommitId).orElse(null)
                : null;

        Map<String, String> rebasedFiles = rebasedCommit.getFileMap();
        Map<String, String> upstreamFiles = upstreamCommit.getFileMap();
        Map<String, String> baseFiles = (baseCommit != null)
                ? baseCommit.getFileMap()
                : new HashMap<>();

        Map<String, String> newIndex = new HashMap<>();
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(rebasedFiles.keySet());
        allPaths.addAll(upstreamFiles.keySet());
        allPaths.addAll(baseFiles.keySet());

        for (String path : allPaths) {
            String rebasedHash = rebasedFiles.get(path);
            String upstreamHash = upstreamFiles.get(path);
            String baseHash = baseFiles.get(path);

            boolean changedByRebased = !Objects.equals(rebasedHash, baseHash);
            boolean changedByUpstream = !Objects.equals(upstreamHash, baseHash);

            if (changedByRebased && changedByUpstream) {
                if (!Objects.equals(rebasedHash, upstreamHash)) {
                    // CONFLICT
                    throw new IllegalStateException("Conflict detected in file: " + path);
                }
                newIndex.put(path, rebasedHash); // Same change
            } else if (changedByRebased) {
                if (rebasedHash != null) newIndex.put(path, rebasedHash);
            } else {
                if (upstreamHash != null) newIndex.put(path, upstreamHash);
            }
        }

        String newCommitId = UUID.randomUUID().toString();
        Commit newCommit = Commit.builder()
                .id(newCommitId)
                .repo(repo)
                .timestamp(System.currentTimeMillis())
                .message(rebasedCommit.getMessage())
                .parentId(upstreamHeadId)
                .authorEmail(rebasedCommit.getAuthorEmail())
                .branch(rebasedCommit.getBranch())
                .status(rebasedCommit.getStatus())
                .build();
        // TODO: adding author

        List<CommitFile> commitFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : newIndex.entrySet()) {
            commitFiles.add(new CommitFile(entry.getKey(), entry.getValue(), newCommit));
        }

        newCommit.setFiles(commitFiles);
        commitRepository.save(newCommit);

        ReviewRequest request = reviewRequestRepository.findByCommitId(rebasedCommitId)
                .orElseThrow(() -> new IllegalArgumentException("Review request not found for original commit"));

        request.setCommitId(newCommitId);
        request.setTargetBranch(targetBranchName);
        reviewRequestRepository.save(request);

        repositoryQueryService.saveMessage(MessageDTO.builder()
                .text("User " + authorEmail + " rebased change")
                .authorEmail(authorEmail)
                .commitId(newCommitId)
                .build());

        System.out.println("Rebased " + rebasedCommitId + " -> " + newCommitId + " onto " + targetBranchName);
        return newCommitId;
    }
}
