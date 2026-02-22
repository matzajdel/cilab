package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;

public class PullCommand implements Command {

    private final VcsEngine engine;

    public PullCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        boolean rebaseMode = false;
        String target = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--rebase")) {
                rebaseMode = true;
            } else {
                target = args[i];
            }
        }

        System.out.println("Pulling latest changes...");

        try {
            engine.pull(target, rebaseMode);

        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Pull failed: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "pull";
    }

    @Override
    public String getDescription() {
        return "pull current version of remote repository";
    }
}
