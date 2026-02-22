package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;

public class RebaseCommand implements Command {

    private final VcsEngine engine;

    public RebaseCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: myvcs rebase <upstream_branch>");
            return;
        }

        String upstreamRef = args[1];
        System.out.println("Rebasing current HEAD onto " + upstreamRef + "...");

        try {
            String newCommitId = engine.rebase(upstreamRef);

            if (newCommitId != null) {
                System.out.println("Successfully rebased onto " + upstreamRef + " (New HEAD: " + newCommitId.substring(0, 8) + ")");
            } else {
                System.out.println("Already up to date with " + upstreamRef + ".");
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "rebase";
    }

    @Override
    public String getDescription() {
        return "Moves given commit to the top of repo history";
    }
}
