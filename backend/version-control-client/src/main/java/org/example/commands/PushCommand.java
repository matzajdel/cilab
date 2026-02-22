package org.example.commands;

import org.example.VcsFileSystem;
import org.example.VcsNetworkClient;
import org.example.dto.VCSContract.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PushCommand implements Command {

    private final VcsFileSystem fs;
    private final VcsNetworkClient network;

    public PushCommand(VcsFileSystem fs, VcsNetworkClient network) {
        this.fs = fs;
        this.network = network;
    }

    @Override
    public void execute(String[] args) throws Exception {
        boolean isReview = false;
        String targetBranch = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--review")) {
                isReview = true;
            } else {
                targetBranch = args[i];
            }
        }

        if (targetBranch == null) targetBranch = fs.getCurrentBranchName();
        if (targetBranch == null) {
            System.out.println("Error: Detached HEAD. Usage: myvcs push <branch_name> [--review]");
            return;
        }

        String repoName = fs.getRepoName();
        String currentCommitId = fs.resolveHead();

        if (currentCommitId == null) {
            System.out.println("Nothing to push (empty repo).");
            return;
        }

        String effectiveTargetRef = isReview ? "refs/for/" + targetBranch : "refs/heads/" + targetBranch;
        System.out.println("Preparing push for commit: \033[33m" + currentCommitId.substring(0, 8) + "\033[0m");

        CommitDTO currentCommit = fs.getCommit(currentCommitId);
        List<String> allBlobHashes = new ArrayList<>(currentCommit.files().values());

        PushRequest initRequest = new PushRequest(effectiveTargetRef, currentCommitId, currentCommit, allBlobHashes);

        // 1: Init
        List<String> missingHashes = network.initiatePush(repoName, initRequest);

        if (missingHashes.isEmpty()) {
            System.out.println("Server has all objects. Push complete!");
            return;
        }

        System.out.println("Uploading " + missingHashes.size() + " missing objects...");

        Path tempZip = Files.createTempFile("vcs-push", ".zip");
        try {
            // 2: Packing ZIP (FileSystem)
            fs.packObjectsToZip(missingHashes, tempZip);

            // 3: Send objects (Network)
            network.uploadObjects(repoName, tempZip);
            System.out.println("Push successful!");
        } catch (IllegalStateException e) {

            System.out.println("Error: \033[31m" + e.getMessage() + "\033[0m");

            if (e.getMessage().contains("Parent commit") && e.getMessage().contains("is missing")) {
                System.out.println("\n Hint: To push multiple commits one by one:");
                System.out.println("  1. myvcs log (Find the missing parent hash)");
                System.out.println("  2. myvcs checkout <parent_hash>");
                System.out.println("  3. myvcs push");
                System.out.println("  4. myvcs checkout " + targetBranch);
                System.out.println("  5. myvcs push");
            }
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    @Override
    public String getName() {
        return "push";
    }

    @Override
    public String getDescription() {
        return "Push local changes to remote repository";
    }
}
