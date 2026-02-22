package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsEngine;
import org.example.dto.VCSContract;

public class FetchCommand implements Command {

    private final VcsEngine engine;

    public FetchCommand(VcsEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(String[] args) throws Exception {
        String target = (args.length > 1) ? args[1] : null;

        System.out.println("Fetching " + (target != null ? target : "current branch") + " from server...");

        try {
            VCSContract.FetchResult result = engine.fetch(target);

            if (result.fetchedCommitId() != null) {
                System.out.println(" Fetched " + result.commitsCount() + " commits and " + result.objectsCount() + " objects.");
                System.out.println("FETCH_HEAD -> \033[33m" + result.fetchedCommitId().substring(0, 8) + "\033[0m");
            } else {
                System.out.println("Fetch completed, but no commit metadata found (repository might be empty).");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Network or FileSystem error: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "fetch";
    }

    @Override
    public String getDescription() {
        return "Fetch given commit Id or branch from remote repository";
    }
}
