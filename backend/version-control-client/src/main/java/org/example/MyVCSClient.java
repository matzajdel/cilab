package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commands.*;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MyVCSClient {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        Path currentWorkingDir = Paths.get("").toAbsolutePath();
        VcsFileSystem vcsFileSystem = new VcsFileSystem(currentWorkingDir, objectMapper);

        HttpClient httpClient = HttpClient.newHttpClient();
        String serverUrl = "http://localhost:8080/api/v1/vcs";
        VcsNetworkClient vcsNetworkClient = new VcsNetworkClient(serverUrl, httpClient, objectMapper);

        VcsEngine vcsEngine = new VcsEngine(vcsFileSystem, vcsNetworkClient);

        Map<String, Command> commands = new HashMap<>();
        commands.put("init", new InitCommand(vcsFileSystem));
        commands.put("clone", new CloneCommand(vcsFileSystem, vcsEngine));
        commands.put("add", new AddCommand(vcsFileSystem));
        commands.put("commit", new CommitCommand(vcsEngine));
        commands.put("log", new LogCommand(vcsFileSystem));
        commands.put("checkout", new CheckoutCommand(vcsEngine));
        commands.put("branch", new BranchCommand(vcsEngine));
        commands.put("merge", new MergeCommand(vcsEngine));
        commands.put("push", new PushCommand(vcsFileSystem, vcsNetworkClient));
        commands.put("pull", new PullCommand(vcsEngine));
        commands.put("fetch", new FetchCommand(vcsEngine));
        commands.put("rebase", new RebaseCommand(vcsEngine));
        commands.put("checkout-review", new CheckoutReviewCommand(vcsEngine));

        if (args.length == 0) {
            printHelp(commands);
            return;
        }

        String commandName = args[0];
        Command command = commands.get(commandName);

        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            printHelp(commands);
            return;
        }

        try {
            command.execute(args);
        } catch (Exception e) {
            System.err.println("Fatal error occurred during '" + commandName + "': " + e.getMessage());
        }
    }

    private static void printHelp(Map<String, Command> commands) {
        commands.values().forEach(command -> {
            System.out.printf("  %-15s %s%n", command.getName(), command.getDescription());
        });
    }
}