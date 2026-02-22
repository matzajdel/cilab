package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;
import org.example.VcsFileSystem;

public class CloneCommand implements Command {

    private final VcsFileSystem fs;
    private final VcsEngine engine;

    public CloneCommand(VcsFileSystem fs, VcsEngine engine) {
        this.fs = fs;
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: myvcs clone <repository_name>");
            return;
        }

        String repoName = args[1];
        System.out.println("Cloning repository '" + repoName + "'...");

        if (fs.isInitialized()) {
            System.out.println("Error: Current directory already contains a .myvcs repository.");
            return;
        }

        try {
            fs.init(".", repoName);

            System.out.println("Pulling project data from server...");
            engine.pull("master", false);

            System.out.println("Clone complete! Repository is ready.");
        } catch (Exception e) {
            System.out.println("Clone failed: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "Clones remote repository";
    }

    @Override
    public String getDescription() {
        return "clone";
    }
}
