package org.example.commands;

import org.example.VcsFileSystem;

import java.nio.file.Paths;

public class InitCommand implements Command {

    private final VcsFileSystem fs;

    public InitCommand(VcsFileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void execute(String[] args) throws Exception {
        String path = args.length > 1 ? args[1] : ".";
        String defaultRepoName = Paths.get(path).toAbsolutePath().normalize().getFileName().toString();

        String repoName = args.length > 2 ? args[2] : defaultRepoName;
        fs.init(path, repoName);
    }

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public String getDescription() {
        return "Initializes a new repository";
    }
}
