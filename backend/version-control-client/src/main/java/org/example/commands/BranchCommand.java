package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;

public class BranchCommand implements Command {

    private final VcsEngine engine;

    public BranchCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: myvcs branch <branch_name>");
            return;
        }

        String branchName = args[1];

        try {
            String commitId = engine.createBranch(branchName);

            System.out.println("Created new branch: \033[32m" + branchName + "\033[0m using commit: " + commitId.substring(0, 8));
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "branch";
    }

    @Override
    public String getDescription() {
        return "Creates a new branch";
    }
}
