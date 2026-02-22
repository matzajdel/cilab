package org.example.commands;

import org.example.VcsFileSystem;
import org.example.dto.VCSContract;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogCommand implements Command {

    private final VcsFileSystem fs;

    public LogCommand(VcsFileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void execute(String[] args) throws Exception {
        String currentCommitId = fs.resolveHead();

        Map<String, List<String>> refsMap = fs.loadRefs();

        String headTarget = fs.getHeadContent();
        String currentBranchName = headTarget.startsWith("ref: refs/heads/")
                ? headTarget.substring(16)
                : null;

        System.out.println("Changes history:");

        while (currentCommitId != null) {
            VCSContract.CommitDTO commit = fs.getCommit(currentCommitId);
            if (commit == null) {
                System.out.println("\n(End of local history. Missing commit object: " + currentCommitId + ")");
                break;
            }

            String decoration = buildDecoration(currentCommitId, currentBranchName, headTarget, refsMap);

            String date = Instant.ofEpochMilli(commit.timestamp())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            System.out.println("* \033[33m" + commit.commitId() + "\033[0m" + decoration + " | " + date);
            System.out.println("  " + commit.message());

            currentCommitId = commit.parentId();
            if (currentCommitId != null )   System.out.println("|");
        }
    }

    private String buildDecoration(String currentCommitId, String currentBranchName, String headTarget, Map<String, List<String>> refsMap) {
        StringBuilder decoration = new StringBuilder();
        List<String> branchesHere = refsMap.getOrDefault(currentCommitId, new ArrayList<>());

        boolean isDetachedHeadHere = currentBranchName == null && headTarget.equals(currentCommitId);
        if (isDetachedHeadHere || !branchesHere.isEmpty()) {
            decoration.append(" \033[36m(");

            boolean first = true;
            if (isDetachedHeadHere) {
                decoration.append("HEAD");
                first = false;
            }

            for (String branch : branchesHere) {
                if (!first) decoration.append(", ");

                if (branch.equals(currentBranchName)) {
                    decoration.append("HEAD -> \033[32m").append(branch).append("\033[36m");
                } else {
                    decoration.append("\033[32m").append(branch).append("\033[36m");
                }
                first = false;
            }
            decoration.append(")\033[0m");
        }

        return decoration.toString();
    }

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public String getDescription() {
        return "Log info about the current state of repository";
    }
}
