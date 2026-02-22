package org.example;

import org.example.dto.VCSContract;
import org.example.dto.VCSContract.CommitDTO;
import org.example.dto.VCSContract.FetchResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class VcsEngine {

    private final VcsFileSystem fs;
    private final VcsNetworkClient network;

    public VcsEngine(VcsFileSystem fs, VcsNetworkClient network) {
        this.fs = fs;
        this.network = network;
    }

    public void commit(String message) throws IOException {
        Map<String, String> index = fs.loadIndex();
        if (index.isEmpty()) {
            System.out.println("No changes to commit.");
            return;
        }

        String parentId = fs.resolveHead();
        String commitId = UUID.randomUUID().toString();

        String branchName = fs.getCurrentBranchName();

        VCSContract.CommitDTO commit = new VCSContract.CommitDTO(
                commitId,
                parentId,
                null,
                message,
                "sid_cilab@cilab.com",
                branchName,
                System.currentTimeMillis(),
                index
        );

        fs.saveLocalCommit(commit);
        fs.updateCurrentRef(commitId);

        System.out.println("Committed changes with id: " + commitId);
    }

    public void checkout(String targetName) throws Exception {
        String commitId;
        boolean isBranch = false;

        if (fs.branchExists(targetName)) {
            isBranch = true;
            commitId = fs.resolveRef(targetName);
        } else {
            commitId = targetName;
        }

        VCSContract.CommitDTO targetCommit = fs.getCommit(commitId);
        if (targetCommit == null) {
            throw new IllegalArgumentException("Commit not found: " + commitId);
        }

        List<String> conflicts = checkUncommittedChanges(targetCommit.files());

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Checkout aborted due to uncommitted changes/untracked files:\n  - " +
                            String.join("\n  - ", conflicts) +
                            "\nPlease commit or delete your changes before checking out."
            );
        }

        checkoutFilesOnly(commitId);

        if (isBranch) {
            fs.setHeadToBranch(targetName);
            System.out.println("Checked out to branch: " + targetName);
        } else {
            fs.setDetachedHead(commitId);
            System.out.println("Checked out to detached commit: " + commitId);
        }
    }

    public String createBranch(String branchName) throws IOException {
        String currentCommitId = fs.resolveHead();

        if (currentCommitId == null) {
            throw new IllegalStateException("Cannot create branch in an empty repo. Make a commit first.");
        }

        if (fs.branchExists(branchName)) {
            throw new IllegalArgumentException("Branch '" + branchName + "' already exists.");
        }

        fs.createBranchRef(branchName, currentCommitId);

        return currentCommitId;
    }

    public VCSContract.FetchResult fetch(String targetRef) throws Exception {
        String repoName = fs.getRepoName();

        if (targetRef == null) {
            targetRef = fs.getCurrentBranchName();
            if (targetRef == null) {
                throw new IllegalStateException("Current branch unknown. Please specify a target branch.");
            }
        }

        try (InputStream zipStream = network.fetchRepositoryData(repoName, targetRef)) {

            FetchResult result = fs.unpackFetchStream(zipStream);

            if (result.fetchedCommitId() != null) {
                fs.saveFetchHead(result.fetchedCommitId());
            }

            return result;
        }
    }

    public void pull(String targetRef, boolean rebaseMode) throws Exception {
        // 1. FETCH
        FetchResult fetchResult = fetch(targetRef);
        String fetchedId = fetchResult.fetchedCommitId();

        if (fetchedId == null) {
            throw new IllegalStateException("Fetch completed, but no commits were received.");
        }

        // 2. Integration
        if (rebaseMode) {
            System.out.println("Rebasing current HEAD onto " + fetchedId.substring(0, 8) + "...");

            String newHead = rebase(fetchedId);

            if (newHead == null) {
                System.out.println("Already up to date.");
            }
        } else {
            System.out.println("Merging fetched commit " + fetchedId.substring(0, 8) + "...");

            merge(fetchedId);
        }
    }

    public void checkoutReview(String commitId) throws Exception {
        // 1. KROK FETCH: Pobieramy ten konkretny commit z serwera
        FetchResult result = fetch(commitId);
        String fetchedId = result.fetchedCommitId();

        if (fetchedId == null) {
            throw new IllegalStateException("Failed to fetch review commit: " + commitId);
        }

        // 2. KROK SETUP: Skracamy hash do 8 znaków i budujemy nazwę brancha
        String shortId = fetchedId.length() > 8 ? fetchedId.substring(0, 8) : fetchedId;
        String branchName = "review/" + shortId;

        System.out.println("Setting up review branch: " + branchName);

        // 3. Zapisujemy wskaźnik brancha na dysku (używamy metody VcsFileSystem)
        // Nadpisujemy, jeśli taki branch już lokalnie istniał (bo np. sprawdzamy nową wersję tego samego review)
        fs.createBranchRef(branchName, fetchedId);

        // 4. KROK CHECKOUT: Przełączamy się na nowo utworzony branch
        // Nasza istniejąca metoda checkout() zrobi tu bezpieczną podmianę plików i zaktualizuje HEAD
        checkout(branchName);
    }

    public String rebase(String upstreamRef) throws Exception {
        String currentHeadHash = fs.resolveHead();

        if (currentHeadHash == null) {
            throw new IllegalStateException("Cannot rebase: You are in an empty state.");
        }

        String upstreamHash = fs.resolveRef(upstreamRef);
        if (upstreamHash == null) {
            throw new IllegalArgumentException("Cannot rebase: Upstream branch '" + upstreamRef + "' not found.");
        }

        if (currentHeadHash.equals(upstreamHash)) {
            return null;
        }

        CommitDTO myCommit = fs.getCommit(currentHeadHash);
        CommitDTO upstreamCommit = fs.getCommit(upstreamHash);

        // Base - parent of "myCommit"
        String baseHash = myCommit.parentId();
        CommitDTO baseCommit = (baseHash != null) ? fs.getCommit(baseHash) : null;

        Map<String, String> myFiles = myCommit.files();
        Map<String, String> upstreamFiles = upstreamCommit.files();
        Map<String, String> baseFiles = (baseCommit != null) ? baseCommit.files() : new HashMap<>();

        Map<String, String> newIndex = new HashMap<>();
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(myFiles.keySet());
        allPaths.addAll(upstreamFiles.keySet());
        allPaths.addAll(baseFiles.keySet());

        List<String> conflicts = new ArrayList<>();

        // Logic 3-Way Merge
        for (String path : allPaths) {
            String hashBase = baseFiles.get(path);
            String hashMine = myFiles.get(path);
            String hashUpstream = upstreamFiles.get(path);

            boolean changedByMe = !Objects.equals(hashMine, hashBase);
            boolean changedByUpstream = !Objects.equals(hashUpstream, hashBase);

            if (changedByMe && changedByUpstream) {
                if (!Objects.equals(hashMine, hashUpstream)) {
                    conflicts.add(path); // CONFLICT
                    fs.restoreFile(path, hashMine); // Keeping version to manual correct
                } else {
                    newIndex.put(path, hashMine);
                    fs.restoreFile(path, hashMine);
                }
            } else if (changedByMe) {
                if (hashMine != null) {
                    newIndex.put(path, hashMine);
                    fs.restoreFile(path, hashMine);
                } else {
                    fs.deleteFile(path); // Delegacja do systemu plików
                }
            } else if (changedByUpstream) {
                if (hashUpstream != null) {
                    newIndex.put(path, hashUpstream);
                    fs.restoreFile(path, hashUpstream);
                } else {
                    fs.deleteFile(path);
                }
            } else {
                if (hashUpstream != null) {
                    newIndex.put(path, hashUpstream);
                    fs.restoreFile(path, hashUpstream);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Rebase failed due to conflicts in:\n  - " + String.join("\n  - ", conflicts) +
                            "\nPlease resolve conflicts manually in the files, then run: myvcs add <file> -> myvcs commit"
            );
        }

        // SUCCESS, creating new commit
        String newCommitId = UUID.randomUUID().toString();

        VCSContract.CommitDTO newRebasedCommit = new CommitDTO(
                newCommitId,
                upstreamHash, // <--
                null,
                myCommit.message(),
                myCommit.authorEmail(),
                myCommit.branchName(),
                System.currentTimeMillis(),
                newIndex
        );

        fs.saveLocalCommit(newRebasedCommit);
        fs.saveIndex(newIndex);
        fs.updateCurrentRef(newCommitId);

        return newCommitId;
    }

    public void merge(String targetRefOrHash) throws Exception {
        String currentHeadHash = fs.resolveHead();
        String targetHash = fs.resolveRef(targetRefOrHash);
//        if (targetHash == null) targetHash = targetRefOrHash;

        if (targetHash == null || targetHash.isEmpty()) {
            throw new IllegalArgumentException("Invalid merge target. Cannot resolve '" + targetRefOrHash + "'.");
        }

        // SCENARIO 1: First Pull (Empty Repo)
        if (currentHeadHash == null) {
            System.out.println("Initial merge (setup). Updating workspace...");
            checkoutFilesOnly(targetHash);
            fs.updateCurrentRef(targetHash);
            return;
        }

        if (currentHeadHash.equals(targetHash)) {
            System.out.println("Already up to date.");
            return;
        }


        // SCENARIO 2: Fast-Forward
        String baseHash = findMergeBase(currentHeadHash, targetHash);
        if (baseHash != null && baseHash.equals(currentHeadHash)) {
            System.out.println("Fast-forward merge detected.");
            checkoutFilesOnly(targetHash);
            fs.updateCurrentRef(targetHash);
            System.out.println("Fast-forward complete.");
            return;
        }

        // Scenario 3: True Merge (3-Way Merge)
        System.out.println("Performing 3-Way Merge...");
        perform3WayMerge(currentHeadHash, targetHash, baseHash, targetRefOrHash);
    }

    private void perform3WayMerge(String currentHeadHash, String targetHash, String baseHash, String sourceName) throws Exception {
        CommitDTO baseCommit = (baseHash != null) ? fs.getCommit(baseHash) : null;
        CommitDTO currentCommit = fs.getCommit(currentHeadHash);
        CommitDTO targetCommit = fs.getCommit(targetHash);

        Map<String, String> baseFiles = (baseCommit != null) ? baseCommit.files() : new HashMap<>();
        Map<String, String> currentFiles = currentCommit.files();
        Map<String, String> targetFiles = targetCommit.files();

        Map<String, String> newIndex = new HashMap<>(currentFiles);
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(baseFiles.keySet());
        allFiles.addAll(currentFiles.keySet());
        allFiles.addAll(targetFiles.keySet());

        List<String> conflicts = new ArrayList<>();

        for (String file : allFiles) {
            String hBase = baseFiles.get(file);
            String hCurrent = currentFiles.get(file);
            String hTarget = targetFiles.get(file);

            boolean changedInCurrent = !Objects.equals(hBase, hCurrent);
            boolean changedInTarget = !Objects.equals(hBase, hTarget);

            if (changedInCurrent && changedInTarget) {
                if (!Objects.equals(hCurrent, hTarget)) {
                    conflicts.add(file);
                }
            } else if (changedInTarget) {
                if (hTarget == null) {
                    newIndex.remove(file);
                } else {
                    newIndex.put(file, hTarget);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new Exception("Merge aborted due to conflicts in files: " + String.join(", ", conflicts));
        }

        // Finalization of Merge
        String mergeCommitId = UUID.randomUUID().toString();
        String mergeMsg = "Merge '" + sourceName + "' into " + (fs.getCurrentBranchName() != null ? fs.getCurrentBranchName() : "HEAD");

        CommitDTO mergeCommit = new CommitDTO(
                mergeCommitId, currentHeadHash, targetHash, mergeMsg,
                "author@vcs.com", fs.getCurrentBranchName(), System.currentTimeMillis(), newIndex
        );

        fs.saveLocalCommit(mergeCommit);
        checkoutFilesOnly(mergeCommitId);
        fs.updateCurrentRef(mergeCommitId);
        System.out.println("Merge successful! New commit: " + mergeCommitId.substring(0, 8));
    }

    private Set<String> getAncestors(String commitId) throws IOException {
        Set<String> ancestors = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId);

        while (!queue.isEmpty()) {
            String currentCommitId = queue.poll();

            if (ancestors.contains(currentCommitId)) continue;
            ancestors.add(currentCommitId);

            VCSContract.CommitDTO currentCommit = fs.getCommit(currentCommitId);
            if (currentCommit != null) {
                if (currentCommit.parentId() != null) queue.add(currentCommit.parentId());
                if (currentCommit.secondParentId() != null) queue.add(currentCommit.secondParentId());
            }
        }

        return ancestors;
    }

    private String findMergeBase(String commitA, String commitB) throws IOException {
        Set<String> ancestorsA = getAncestors(commitA);

        Queue<String> visited = new LinkedList<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(commitB);

        while (!queue.isEmpty()) {
            String currentCommitBId = queue.poll();

            if (visited.contains(currentCommitBId)) continue;
            visited.add(currentCommitBId);

            if (ancestorsA.contains(currentCommitBId)) {
                return currentCommitBId;
            }

            VCSContract.CommitDTO currentCommitB = fs.getCommit(currentCommitBId);
            if (currentCommitB != null) {
                if (currentCommitB.parentId() != null) queue.add(currentCommitB.parentId());
                if (currentCommitB.secondParentId() != null) queue.add(currentCommitB.secondParentId());
            }
        }

        return null;
    }

    private void checkoutFilesOnly(String commitId) throws IOException, NoSuchAlgorithmException {
        System.out.println("Updating working directory files to state: " + commitId.substring(0, 8) + "...");

        VCSContract.CommitDTO targetCommit = fs.getCommit(commitId);
        if (targetCommit == null) {
            throw new IOException("Target commit object not found: " + commitId);
        }

        Map<String, String> targetFiles = targetCommit.files();
        Map<String, String> currentIndex = fs.loadIndex();

        // Deleting files
        for (String path : currentIndex.keySet()) {
            if (!targetFiles.containsKey(path)) {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("Deleted: " + path);
                    fs.deleteEmptyParentDirs(filePath.getParent());
                }
            }
        }

        // Writing files
        for (Map.Entry<String, String> entry : targetFiles.entrySet()) {
            String filePathStr = entry.getKey();
            String blobHash = entry.getValue();

            fs.restoreFile(filePathStr, blobHash);
        }

        fs.saveIndex(targetFiles);

        System.out.println("Working directory updated.");
    }

    private List<String> checkUncommittedChanges(Map<String, String> targetFiles) throws Exception {
        List<String> conflicts = new ArrayList<>();
        Map<String, String> currentIndex = fs.loadIndex();

        for (Map.Entry<String, String> entry : currentIndex.entrySet()) {
            String path = entry.getKey();
            String currentHashInIndex = entry.getValue();
            String targetHash = targetFiles.get(path);

            if (!Objects.equals(currentHashInIndex, targetHash)) {
                if (fs.fileExists(path)) {
                    String realDiskHash = fs.hashFile(fs.getPath(path));
                    if (!realDiskHash.equals(currentHashInIndex)) {
                        conflicts.add(path + " (uncommitted changes)");
                    }
                }
            }
        }

        for (String targetPath : targetFiles.keySet()) {
            if (!currentIndex.containsKey(targetPath)) {
                if (fs.fileExists(targetPath)) {
                    conflicts.add(targetPath + " (untracked file exists, checkout wants to overwrite)");
                }
            }
        }

        return conflicts;
    }
}
