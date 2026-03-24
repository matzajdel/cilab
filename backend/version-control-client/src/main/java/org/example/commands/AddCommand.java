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

        Map<String, String> index = fs.loadIndex();
        boolean indexChanged = false;

        for (int i=1; i < args.length; i++) {
            String rawInput = args[i].replace("\"", "").replace("'", "").trim();
            Path osPath = Paths.get(rawInput).normalize();
            String vcsPath = osPath.toString().replace("\\", "/");

            if (!Files.exists(osPath)) {
                System.out.println("Error: File not found: " + vcsPath);
                continue;
            }
            if (Files.isDirectory(osPath)) {
                System.out.println("Warning: '" + vcsPath + "' is a directory. Adding whole directories is not supported yet.");
                continue;
            }
            String hash = fs.hashFile(osPath);
            fs.saveBlob(hash, Files.readAllBytes(osPath));

            index.put(vcsPath, hash);
            indexChanged = true;


            System.out.println("Added file: " + vcsPath);
        }

        if (indexChanged) {
            fs.saveIndex(index);
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
