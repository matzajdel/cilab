package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;

public class CheckoutCommand implements Command {

    private final VcsEngine engine;

    public CheckoutCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: myvcs checkout <branch_name_or_commit_hash>");
            return;
        }

        String target = args[1];
        System.out.println("Preparing to checkout '" + target + "'...");

        try {
            engine.checkout(target);
            System.out.println("Checkout successful.");
        } catch (IllegalStateException e) {
            System.out.println("Error while checkout: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "checkout";
    }

    @Override
    public String getDescription() {
        return "Move to the given branch or commit ID";
    }
}
