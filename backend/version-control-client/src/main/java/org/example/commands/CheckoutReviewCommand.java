package org.example.commands;

import org.example.VcsEngine;

public class CheckoutReviewCommand implements Command {

    private final VcsEngine engine;

    public CheckoutReviewCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: myvcs checkout-review <commit_hash>");
            return;
        }

        String commitId = args[1];
        System.out.println("Setting up review workspace for commit: \033[33m" + commitId + "\033[0m...");

        try {
            engine.checkoutReview(commitId);

            System.out.println("Ready for review.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Network or FileSystem error: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "checkout-review";
    }

    @Override
    public String getDescription() {
        return "Fetch and checkout to given commit Id which is in review state";
    }
}
