package org.example.commands;

import org.example.VcsEngine;

public class CommitCommand implements Command {

    private final VcsEngine engine;

    public CommitCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: myvcs commit \"message\"");
        }

        engine.commit(args[1]);
    }

    @Override
    public String getName() {
        return "commit";
    }

    @Override
    public String getDescription() {
        return "Makes commit";
    }
}
