package org.example.versioncontrolserver.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.versioncontrolserver.dto.CommitRequestDTO;
import org.example.versioncontrolserver.dto.PullManifestDTO;
import org.example.versioncontrolserver.dto.PushRequestDTO;
import org.example.versioncontrolserver.entities.Branch;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.CommitStatus;
import org.example.versioncontrolserver.entities.Repo;
import org.example.versioncontrolserver.exception.PushRejectedException;
import org.example.versioncontrolserver.mapper.CommitMapper;
import org.example.versioncontrolserver.repositories.BranchRepository;
import org.example.versioncontrolserver.repositories.CommitRepository;
import org.example.versioncontrolserver.repositories.RepoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VcsSyncService {

    private final CommitMapper commitMapper;

    private final RepoRepository repoRepository;
    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;

    private final ReviewService reviewService;

    private static final Path GLOBAL_OBJECTS_DIR = Paths.get("server-storage/global-objects");

    @Transactional
    public List<String> handlePushInit(String repoName, PushRequestDTO request) {
        Repo repo = repoRepository.findByName(repoName)
                .orElseGet(() -> repoRepository.save(new Repo(repoName)));

        CommitRequestDTO incomingCommit = request.commitData();
        String parentId = incomingCommit.parentId();

        if (parentId != null && !commitRepository.existsById(parentId)) {
            throw new PushRejectedException(
                    "Push rejected: Parent commit " + parentId.substring(0, 8) +
                    " is missing on the server. Please push commits one by one."
            );
        }

        String targetRef = request.branchName();
        boolean isReview = targetRef.startsWith("refs/for/");

        if (isReview) {
            saveCommitEntity(repo, incomingCommit, CommitStatus.IN_REVIEW);
            String realTargetBranch = targetRef.substring("refs/for/".length());
            reviewService.createReviewRequest(request.commitId(), realTargetBranch, incomingCommit.message());

            System.out.println("Push (Review): Created request for branch " + realTargetBranch);
        } else {
            String branchName = targetRef.replace("refs/heads/", "");

            validateParentNotInReview(parentId);
            validateFastForward(repo, branchName, parentId);

            saveCommitEntity(repo, incomingCommit, CommitStatus.MERGED);
            updateBranchHead(repo, branchName, request.commitId());

            System.out.println("Push (Direct): Updated branch " + branchName);
        }

        List<String> missing = new ArrayList<>();
        if (!request.objects().isEmpty()) {
            for (String hash : request.objects()) {
                if (!Files.exists(GLOBAL_OBJECTS_DIR.resolve(hash))) {
                    missing.add(hash);
                }
            }
        }

        return missing;
    }

    private void validateFastForward(Repo repo, String branchName, String incomingParentId) {
        branchRepository.findByRepoAndName(repo, branchName)
                .map(Branch::getHeadCommitId)
                .ifPresent(currentServerHead -> {
                    if (!currentServerHead.equals(incomingParentId)) {
                        throw new PushRejectedException(
                                "Push rejected: Non-fast-forward. The remote branch '" + branchName +
                                "' contains work that you do not have locally (Server is at " + currentServerHead.substring(0, 8) +
                                "). Please run 'myvcs pull' to merge/rebase changes before pushing again."
                        );
                    }
                });
    }

    private void validateParentNotInReview(String parentId) {
        if (parentId == null) {
            return;
        }

        if (commitRepository.countInReviewAncestors(parentId) > 0) {
            throw new PushRejectedException("Push rejected: Found IN_REVIEW ancestor in history.");
        }
    }

    private void updateBranchHead(Repo repo, String branchName, String commitId) {
        Branch branch = branchRepository.findByRepoAndName(repo, branchName)
                .orElseGet(() -> Branch.builder()
                        .name(branchName)
                        .repo(repo)
                        .build()
                );

        branch.setHeadCommitId(commitId);
        branchRepository.save(branch);
    }

    private boolean saveCommitEntity(Repo repo, CommitRequestDTO commitRequestDTO, CommitStatus status) {
        if (commitRepository.existsById(commitRequestDTO.commitId())) {
            return false;
        }

        Commit commit = commitMapper.toEntity(commitRequestDTO);
        commit.setRepo(repo);
        commit.setStatus(status);

        if (!commitRequestDTO.files().isEmpty()) {
            for (Map.Entry<String, String> fileEntry : commitRequestDTO.files().entrySet()) {
                commit.addFile(fileEntry.getKey(), fileEntry.getValue());
            }
        }

        commitRepository.save(commit);
        return true;
    }

    @Transactional(readOnly = true)
    public PullManifestDTO preparePullManifest(String repoName, String target) throws IOException {
        Repo repo = repoRepository.findByName(repoName)
                .orElseThrow(() -> new IOException("Repo: " + repoName + "does not exist on server"));


        var branchOpt = branchRepository.findByRepoAndName(repo, target);
        String startCommitId = branchOpt.map(Branch::getHeadCommitId).orElse(target);

        List<CommitRequestDTO> history = new ArrayList<>();
        Set<String> allBlobHashes = new HashSet<>();

        String currentId = startCommitId;
        while (currentId != null) {
            var commitOpt = commitRepository.findById(currentId);
            if (commitOpt.isEmpty()) break;

            Commit commit = commitOpt.get();
            history.add(commitMapper.toDTO(commit));

            commit.getFiles().forEach(f -> allBlobHashes.add(f.getBlobHash()));

            currentId = commit.getParentId();
        }

        return new PullManifestDTO(history, allBlobHashes);

    }

//    public void handlePushObjectStream(InputStream zipStream) throws IOException {
//        if (!Files.exists(GLOBAL_OBJECTS_DIR)) {
//            Files.createDirectories(GLOBAL_OBJECTS_DIR);
//        }
//
//        try (
//                ZipInputStream zis = new ZipInputStream(zipStream);
//        ) {
//            ZipEntry entry;
//            while ((entry = zis.getNextEntry()) != null) {
//                String hash = entry.getName();
//                Path targetPath = GLOBAL_OBJECTS_DIR.resolve(hash);
//
//                if (!Files.exists(targetPath)) {
//                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
//                }
//                zis.closeEntry();
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    @Transactional(readOnly = true)
//    public void handlePullStream(String repoName, String target, OutputStream outputStream) throws IOException {
//        Repo repo = repoRepository.findByName(repoName)
//                .orElseThrow(() -> new IOException("Repo: " + repoName + "does not exist on server"));
//
//
//        var branchOpt = branchRepository.findByRepoAndName(repo, target);
//        String startCommitId = branchOpt.map(Branch::getHeadCommitId).orElse(target);
//
//        List<Commit> history = new ArrayList<>();
//        Set<String> allBlobHashes = new HashSet<>();
//
//        String currentId = startCommitId;
//        while (currentId != null) {
//            var commitOpt = commitRepository.findById(currentId);
//            if (commitOpt.isEmpty()) break;
//
//            Commit commit = commitOpt.get();
//            history.add(commit);
//
//            commit.getFiles().forEach(f -> allBlobHashes.add(f.getBlobHash()));
//
//            currentId = commit.getParentId();
//        }
//
//        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
//            ZipEntry metaDataEntry = new ZipEntry("commit-metadata.json");
//            zos.putNextEntry(metaDataEntry);
//
//            Commit headCommit = history.get(0);
//            CommitRequestDTO headDto = commitMapper.toDTO(headCommit);
//            byte[] jsonBytes = objectMapper.writeValueAsBytes(headDto);
//            zos.write(jsonBytes);
//
//            zos.closeEntry();
//
//            for (Commit c : history) {
//                String entryName = "commits/" + c.getId() + ".json";
//                zos.putNextEntry(new ZipEntry(entryName));
//
//                CommitRequestDTO dto = commitMapper.toDTO(c);
//                zos.write(objectMapper.writeValueAsBytes(dto));
//
//                zos.closeEntry();
//            }
//
//            for (String hash : allBlobHashes) {
//                Path blobPath = GLOBAL_OBJECTS_DIR.resolve(hash);
//
//                zos.putNextEntry(new ZipEntry("objects/" + hash));
//                Files.copy(blobPath, zos);
//                zos.closeEntry();
//            }
//        }
//    }
}
