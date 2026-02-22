package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;

public class MergeCommand implements Command {

    private final VcsEngine engine;

    public MergeCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: myvcs merge <branch>");
            return;
        }

        String targetRef = args[1];

        try {
            engine.merge(targetRef);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "merge";
    }

    @Override
    public String getDescription() {
        return "Merges given branch into an current branch";
    }
}
