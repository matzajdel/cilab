package org.example.commands;

import org.example.MyVCSClient;
import org.example.VcsFileSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AddCommand implements Command {
    private final VcsFileSystem fs;

    public AddCommand(VcsFileSystem vcsFileSystem) {
        this.fs = vcsFileSystem;
    }

    @Override
    public void execute(String[] args) throws Exception {

        for (int i=1; i < args.length; i++) {
            String file = args[i];
            Path p = Paths.get(file);
            String hash = fs.hashFile(p);
            fs.saveBlob(hash, Files.readAllBytes(p));

            Map<String, String> index = fs.loadIndex();
            index.put(file, hash);
            fs.saveIndex(index);

            System.out.println("Added file: " + file);
        }
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getDescription() {
        return "Add file to the staging area";
    }
}
