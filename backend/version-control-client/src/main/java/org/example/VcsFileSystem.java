package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.VCSContract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.example.dto.VCSContract.*;

public class VcsFileSystem {

    private final ObjectMapper objectMapper;

    private final Path workingDir;
    private final Path vcsDir;
    private final Path commitsDir;
    private final Path objectsDir;
    private final Path indexFile;
    private final Path headFile;
    private final Path remoteFile;
    private final Path headsDir;

    public VcsFileSystem(Path currentWorkingDir, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.workingDir = currentWorkingDir;
        this.vcsDir = workingDir.resolve(".myvcs");
        this.commitsDir = vcsDir.resolve("commits");
        this.objectsDir = vcsDir.resolve("objects");
        this.indexFile = vcsDir.resolve("index");
        this.headFile = vcsDir.resolve("HEAD");
        this.remoteFile = vcsDir.resolve("remote");
        this.headsDir = vcsDir.resolve("refs").resolve("heads");
    }

    public void init(String path, String repoName) throws IOException {
        System.out.println("Initializing repository...");

        Files.createDirectories(commitsDir);
        Files.createDirectories(objectsDir);
        Files.createDirectories(headsDir);
        if (!Files.exists(indexFile)) Files.writeString(indexFile, "{}");

        Files.writeString(headFile, "ref: refs/heads/master");
        Files.writeString(remoteFile, repoName);

        System.out.println("Repository initialized at " + path + "/" + vcsDir.toString());
    }

    public void createBranchRef(String branchName, String commitId) throws IOException {
        Path branchRef = headsDir.resolve(branchName);

        if (branchRef.getParent() != null) {
            Files.createDirectories(branchRef.getParent());
        }

        Files.writeString(branchRef, commitId);
    }

    public Map<String, String> loadIndex() throws IOException {
        if (!Files.exists(indexFile)) return new HashMap<>();

        return objectMapper.readValue(indexFile.toFile(), new TypeReference<>(){});
    }

    public void saveIndex(Map<String, String> index) throws IOException {
        objectMapper.writeValue(indexFile.toFile(), index);
    }

    public String hashFile(Path p) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = md.digest(Files.readAllBytes(p));

        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));

        return sb.toString();
    }

    public void saveBlob(String hash, byte[] data) throws IOException {
        Path objectsPath = objectsDir.resolve(hash);

        // if (!Files.exists(objectsPath)) Files.write(objectsPath, data);
        // Using GZIP compression instead of standard file writing
        if (!Files.exists(objectsPath)) {
            try (OutputStream fos = Files.newOutputStream(objectsPath);
                GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                gzos.write(data);
            }
        }
    }

    public VCSContract.CommitDTO getCommit(String commitId) throws IOException {
        if (commitId == null) return null;

        Path p = commitsDir.resolve(commitId + ".json");
        if (!Files.exists(p)) return null;

        return objectMapper.readValue(p.toFile(), VCSContract.CommitDTO.class);
    }

    public void saveLocalCommit(VCSContract.CommitDTO commit) throws IOException {
        Path commitPath = commitsDir.resolve(commit.commitId() + ".json");

        if (!Files.exists(commitPath.getParent())) {
            Files.createDirectories(commitPath.getParent());
        }

        objectMapper.writeValue(commitPath.toFile(), commit);
    }

    public String getRepoName() throws IOException {
        if (!Files.exists(remoteFile)) {
            throw new IllegalStateException("Remote repository name not configured. Are you in a valid .myvcs directory?");
        }

        return Files.readString(remoteFile).trim();
    }

    public void restoreFile(String filename, String hashTarget) throws IOException, NoSuchAlgorithmException {
        Path targetPath = Paths.get(filename);
        Path blobPath = objectsDir.resolve(hashTarget);

        if (Files.exists(targetPath)) {
            String currentHashDisk = hashFile(targetPath);

            if (hashTarget.equals(currentHashDisk)) return;
        }

        Files.createDirectories(targetPath.getParent());
        try (
                InputStream fis = Files.newInputStream(blobPath);
                GZIPInputStream gzis = new GZIPInputStream(fis);
        ) {
            Files.copy(gzis, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Map<String, List<String>> loadRefs() throws IOException {
        Map<String, List<String>> refs = new HashMap<>();

        if (!Files.exists(headsDir)) return refs;

        try (Stream<Path> stream = Files.walk(headsDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String commitId = Files.readString(path).trim();

                            String branchName = headsDir.relativize(path).toString();

                            branchName = branchName.replace("\\", "/");

                            refs.computeIfAbsent(commitId, k -> new ArrayList<>()).add(branchName);
                        } catch (IOException e) {
                            System.err.println("Warning: Could not read ref " + path);
                        }
                    });
        }
        return refs;
    }

    public String resolveHead() throws IOException {
        if (!Files.exists(headFile)) return null;

        String headContent = Files.readString(headFile).trim();

        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5);
            Path refFile = vcsDir.resolve(refPath);

            if (Files.exists(refFile)) {
                return Files.readString(refFile).trim();
            } else {
                return null;
            }
        }

        return headContent;
    }

    public String getHeadContent() throws IOException {
        if (Files.exists(headFile)) {
            return Files.readString(headFile).trim();
        }

        return null;
    }

    public String getCurrentBranchName() throws IOException {
        if (!Files.exists(headFile)) return null;

        String headContent = Files.readString(headFile).trim();
        if (headContent.startsWith("ref: refs/heads")) {
            return headContent.substring(16);
        }

        return null; // Detached state
    }

    public void updateCurrentRef(String newCommitId) throws IOException {
        String headContent = "";
        if (Files.exists(headFile)) {
            headContent = Files.readString(headFile).trim();
        }

        if (headContent.startsWith("ref: ")) {
            String branchRefPath = headContent.substring(5); // wycinamy "ref: "
            Path refPath = vcsDir.resolve(branchRefPath);

            if (refPath.getParent() != null) {
                Files.createDirectories(refPath.getParent());
            }

            Files.writeString(refPath, newCommitId);
            System.out.println("Branch ref updated: " + branchRefPath + " -> " + newCommitId);

        } else {
            Files.writeString(headFile, newCommitId);
            System.out.println("HEAD updated (detached): " + newCommitId);
        }
    }

    public void deleteEmptyParentDirs(Path directory) throws IOException {
        while (directory != null && !directory.toString().equals(".")) {
            if (!Files.exists(directory) || Files.isDirectory(directory)) break;

            try (
                    var files = Files.list(directory);
            ) {
                if (files.findAny().isPresent()) {
                    break;
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Files.delete(directory);
            directory = directory.getParent();
        }
    }

    public String resolveRef(String ref) throws IOException {
        if (ref == null || ref.isBlank()) {
            return null;
        }

        Path branchPath = headsDir.resolve(ref);
        if (Files.exists(branchPath)) {
            return Files.readString(branchPath).trim();
        }

        if (ref.equals("HEAD")) {
            return resolveHead();
        }

        Path commitJsonPath = commitsDir.resolve(ref + ".json");
        if (Files.exists(commitJsonPath)) {
            return ref;
        }

        Path fetchHeadPath = vcsDir.resolve("FETCH_HEAD");
        if (ref.equals("FETCH_HEAD") && Files.exists(fetchHeadPath)) {
            return Files.readString(fetchHeadPath).trim();
        }

        return null;
    }

    public FetchResult unpackFetchStream(InputStream zipStream) throws IOException {
        String fetchedCommitId = null;
        int objectsCount = 0;
        int commitsCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.equals("commit-metadata.json")) {
                    byte[] jsonBytes = zis.readAllBytes();
                    CommitDTO commitDto = objectMapper.readValue(jsonBytes, CommitDTO.class);
                    saveLocalCommit(commitDto);
                    fetchedCommitId = commitDto.commitId();
                }
                else if (name.startsWith("commits/") && name.endsWith(".json")) {
                    byte[] jsonBytes = zis.readAllBytes();
                    CommitDTO commitDto = objectMapper.readValue(jsonBytes, CommitDTO.class);
                    saveLocalCommit(commitDto);
                    commitsCount++;
                }
                else if (name.startsWith("objects/")) {
                    String hash = name.substring(8);
                    Path targetPath = objectsDir.resolve(hash);

                    if (!Files.exists(targetPath)) {
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        objectsCount++;
                    }
                }
                zis.closeEntry();
            }
        }

        return new FetchResult(fetchedCommitId, commitsCount, objectsCount);
    }

    public boolean branchExists(String branchName) {
        return Files.exists(headsDir.resolve(branchName));
    }

    public boolean fileExists(String filePath) {
        return Files.exists(workingDir.resolve(filePath));
    }

    public Path getPath(String filePath) {
        return workingDir.resolve(filePath);
    }

    public void setHeadToBranch(String branchName) throws IOException {
        Files.writeString(headFile, "ref: refs/heads/" + branchName);
    }

    public void setDetachedHead(String commitId) throws IOException {
        Files.writeString(headFile, commitId);
    }

    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(workingDir.resolve(filePath));
    }

    public void saveFetchHead(String commitId) throws IOException {
        Files.writeString(vcsDir.resolve("FETCH_HEAD"), commitId);
    }

    public boolean isInitialized() {
        if (Files.exists(vcsDir)) {
            System.out.println("Error: Current directory already contains a .myvcs repository.");
            return true;
        }

        return false;
    }

    public void packObjectsToZip(List<String> hashesToPack, Path targetZipPath) throws IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(targetZipPath))) {
            for (String hash : hashesToPack) {
                Path blobPath = objectsDir.resolve(hash);
                if (Files.exists(blobPath)) {
                    zos.putNextEntry(new java.util.zip.ZipEntry(hash));
                    Files.copy(blobPath, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
