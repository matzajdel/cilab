package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class MyVCSServerDelta {
    private static final String REPO_ROOT = "server_repos";
    private static final int PORT = 9000;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(REPO_ROOT));
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server listening on " + PORT);
            while (true) {
                Socket s = server.accept();
                new Thread(() -> handleClient(s)).start();
            }
        }
    }

    private static void handleClient(Socket socket) {
        System.out.println("Client connected: " + socket.getRemoteSocketAddress());
        // Per-connection memory: client-known hashes
        Set<String> clientHashes = new HashSet<>();

        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
             InputStream in = socket.getInputStream()) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                System.out.println("REQ: " + line);
                String[] parts = line.split(" ");
                String cmd = parts[0];

                switch (cmd) {
                    case "HASHES" -> {
                        // read client hashes until "END"
                        clientHashes.clear();
                        String h;
                        while (!(h = reader.readLine()).equals("END")) {
                            clientHashes.add(h.trim());
                        }
                        writer.writeBytes("OK\n");
                    }

                    case "SEND" -> {
                        // SEND <repo>
                        if (parts.length < 2) { writer.writeBytes("ERROR Missing repo\n"); break; }
                        String repo = parts[1];
                        Path objDir = Paths.get(REPO_ROOT, repo, ".myvcs", "objects");
                        Files.createDirectories(objDir);

                        // next line is count
                        String cntLine = reader.readLine();
                        int count = Integer.parseInt(cntLine.trim());
                        for (int i = 0; i < count; i++) {
                            String hash = reader.readLine().trim();
                            long size = Long.parseLong(reader.readLine().trim());
                            Path out = objDir.resolve(hash);
                            try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                                long remaining = size;
                                byte[] buf = new byte[8192];
                                while (remaining > 0) {
                                    int r = in.read(buf, 0, (int)Math.min(buf.length, remaining));
                                    if (r == -1) throw new EOFException("Unexpected EOF while receiving blob");
                                    fos.write(buf, 0, r);
                                    remaining -= r;
                                }
                            }
                            System.out.println("Stored blob " + hash + " for repo " + repo);
                        }
                        writer.writeBytes("OK\n");
                    }

                    case "PUSH" -> {
                        // PUSH <repo> <commitId>
                        if (parts.length < 3) { writer.writeBytes("ERROR Bad PUSH\n"); break; }
                        String repo = parts[1], commitId = parts[2];
                        // Expect commit JSON lines until END
                        Path pending = Paths.get(REPO_ROOT, repo, ".myvcs", "commits", "pending");
                        Files.createDirectories(pending);
                        Path commitFile = pending.resolve(commitId + ".json");

                        StringBuilder sb = new StringBuilder();
                        String cl;
                        while (!(cl = reader.readLine()).equals("END")) sb.append(cl).append("\n");
                        Files.writeString(commitFile, sb.toString());
                        writer.writeBytes("OK Pushed commit " + commitId + " (pending)\n");
                    }

                    case "MISSING_REQ" -> {
                        if (parts.length < 2) { writer.writeBytes("MISSING 0\nEND\n"); break; }
                        String repo = parts[1];
                        Path repoPath = Paths.get(REPO_ROOT, repo, ".myvcs");
                        Path objDir = repoPath.resolve("objects");
                        Files.createDirectories(objDir);

                        // zbierz hashe istniejące na serwerze
                        Set<String> serverHashes = new HashSet<>();
                        try (DirectoryStream<Path> ds = Files.newDirectoryStream(objDir)) {
                            for (Path p : ds) {
                                if (Files.isRegularFile(p)) serverHashes.add(p.getFileName().toString());
                            }
                        }

                        // klientowe hashe (poprzednio wczytane przy komendzie HASHES)
                        // clientHashes zawiera listę hashy, które klient posiada
                        List<String> missing = new ArrayList<>();
                        for (String ch : clientHashes) {
                            if (!serverHashes.contains(ch)) missing.add(ch);
                        }

                        writer.writeBytes("MISSING " + missing.size() + "\n");
                        for (String h : missing) writer.writeBytes(h + "\n");
                        writer.writeBytes("END\n");
                    }

                    case "MERGE" -> {
                        if (parts.length < 3) {
                            writer.writeBytes("ERROR Bad MERGE\n");
                            break;
                        }
                        String repo = parts[1], commitId = parts[2];

                        Path repoPath = Paths.get(REPO_ROOT, repo, ".myvcs");
                        Path pending = repoPath.resolve("commits/pending/" + commitId + ".json");
                        Path mainDir = repoPath.resolve("commits/main");
                        Files.createDirectories(mainDir);

                        if (!Files.exists(pending)) {
                            writer.writeBytes("ERROR Commit not found\n");
                            break;
                        }

                        // przenieś commit z pending → main
                        Path merged = mainDir.resolve(pending.getFileName());
                        Files.move(pending, merged, StandardCopyOption.REPLACE_EXISTING);

                        // pokaż commit w logach serwera (dla diagnostyki)
                        String commitJson = Files.readString(merged);
                        System.out.println("Merged commit content:\n" + commitJson);

                        // ------------- NOWE: ODTWORZENIE PLIKÓW W KATALOGU REPO -----------------
                        Map<String, String> files = parseFilesFromJson(commitJson);
                        Path repoDir = Paths.get(REPO_ROOT, repo); // katalog repo (bez .myvcs)
                        if (files.isEmpty()) {
                            System.out.println("No files found in commit " + commitId);
                        }

                        for (Map.Entry<String, String> e : files.entrySet()) {
                            String rawKey = e.getKey();
                            String hash = e.getValue();

                            // normalizacja ścieżki (usuń .\, \, C:\ itp.)
                            String cleanKey = rawKey.replace('\\', '/');
                            while (cleanKey.startsWith("./")) cleanKey = cleanKey.substring(2);
                            while (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
                            if (cleanKey.matches("^[A-Za-z]:/.*")) {
                                cleanKey = cleanKey.replaceFirst("^[A-Za-z]:", "");
                                while (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
                            }
                            if (cleanKey.isBlank()) {
                                System.out.println("Skipped empty key for hash " + hash);
                                continue;
                            }

                            Path blob = repoPath.resolve("objects").resolve(hash);
                            Path out = repoDir.resolve(cleanKey).normalize();

                            try {
                                Path parent = out.getParent();
                                if (parent != null) Files.createDirectories(parent);
                                if (Files.exists(blob)) {
                                    Files.copy(blob, out, StandardCopyOption.REPLACE_EXISTING);
                                    System.out.println("Restored file: " + out.toString());
                                } else {
                                    System.out.println("Blob not found (cannot restore): " + hash + " for file key=" + rawKey);
                                }
                            } catch (Exception ex) {
                                System.err.println("Failed to restore file " + cleanKey + " : " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                        // ----------------------------------------------------------------------

                        // zapisz HEAD
                        Files.createDirectories(repoPath.resolve("refs"));
                        Files.writeString(repoPath.resolve("refs/HEAD"), merged.getFileName().toString());

                        writer.writeBytes("OK Merged " + commitId + "\n");
                    }


                    case "PULL" -> {
                        // PULL <repo> [commitId]
                        if (parts.length < 2) {
                            writer.writeBytes("ERROR Bad PULL\n");
                            writer.flush();
                            break;
                        }
                        String repo = parts[1];
                        String requestedCommit = parts.length >= 3 ? parts[2] : null;

                        Path repoPath = Paths.get(REPO_ROOT, repo, ".myvcs");
                        Path objectsDir = repoPath.resolve("objects");
                        Files.createDirectories(objectsDir);

                        List<CommitInfo> mainCommits = readCommits(repoPath.resolve("commits/main"), "main");
                        List<CommitInfo> pendingCommits = readCommits(repoPath.resolve("commits/pending"), "pending");
                        List<CommitInfo> responseCommits = new ArrayList<>();

                        if (requestedCommit == null || requestedCommit.isBlank()) {
                            responseCommits.addAll(mainCommits);
                        } else {
                            Optional<CommitInfo> targetMain = mainCommits.stream()
                                    .filter(ci -> ci.id().equals(requestedCommit))
                                    .findFirst();
                            if (targetMain.isPresent()) {
                                for (CommitInfo ci : mainCommits) {
                                    responseCommits.add(ci);
                                    if (ci.id().equals(requestedCommit)) break;
                                }
                            } else {
                                Optional<CommitInfo> targetPending = pendingCommits.stream()
                                        .filter(ci -> ci.id().equals(requestedCommit))
                                        .findFirst();
                                if (targetPending.isPresent()) {
                                    responseCommits.addAll(mainCommits);
                                    responseCommits.add(targetPending.get());
                                } else {
                                    writer.writeBytes("ERROR Commit not found\n");
                                    writer.flush();
                                    break;
                                }
                            }
                        }

                        writer.writeBytes("COMMITS " + responseCommits.size() + "\n");
                        for (CommitInfo ci : responseCommits) {
                            writer.writeBytes("COMMIT " + ci.id() + " " + ci.state() + "\n");
                            writer.writeBytes(ci.json());
                            if (!ci.json().endsWith("\n")) writer.writeBytes("\n");
                            writer.writeBytes("END\n");
                        }
                        writer.flush();

                        // aggregate required blobs across all returned commits
                        Set<String> required = new LinkedHashSet<>();
                        for (CommitInfo ci : responseCommits) {
                            required.addAll(ci.files().values());
                        }

                        List<String> needed = new ArrayList<>();
                        for (String hash : required) {
                            if (!clientHashes.contains(hash)) needed.add(hash);
                        }

                        writer.writeBytes("MISSING " + needed.size() + "\n");
                        for (String h : needed) writer.writeBytes(h + "\n");
                        writer.writeBytes("END\n");
                        writer.flush();

                        for (String h : needed) {
                            Path blob = objectsDir.resolve(h);
                            System.out.println("Sending blob " + h + " from " + blob.toAbsolutePath());
                            if (!Files.exists(blob)) continue;
                            long size = Files.size(blob);
                            System.out.println("Sending blob " + h + " (" + size + " bytes)");
                            writer.writeBytes(h + "\n");
                            writer.writeBytes(String.valueOf(size) + "\n");
                            writer.flush();

                            try (InputStream fis = Files.newInputStream(blob)) {
                                byte[] buf = new byte[8192];
                                long rem = size;
                                while (rem > 0) {
                                    int r = fis.read(buf, 0, (int) Math.min(buf.length, rem));
                                    if (r == -1) break;
                                    writer.write(buf, 0, r);
                                    rem -= r;
                                }
                            }
                            writer.flush();
                        }

                        writer.writeBytes("END\n");
                        writer.flush();
                        System.out.println("Finished PULL for repo " + repo + (requestedCommit == null ? "" : ", request=" + requestedCommit));
                    }

                    case "LATEST_COMMIT" -> {
                        if (parts.length < 2) {
                            writer.writeBytes("ERROR Missing repo name\n");
                            break;
                        }
                        String repo = parts[1];
                        Path commitsDir = Paths.get(REPO_ROOT, repo, ".myvcs/commits/main");
                        if (!Files.exists(commitsDir)) {
                            writer.writeBytes("ERROR No commits\n");
                            break;
                        }

                        List<Path> commits = new ArrayList<>();
                        try (DirectoryStream<Path> ds = Files.newDirectoryStream(commitsDir, "*.json")) {
                            for (Path p : ds) commits.add(p);
                        }
                        if (commits.isEmpty()) {
                            writer.writeBytes("ERROR No commits\n");
                            break;
                        }

                        // znajdź commit z największym timestamp
                        Path latest = null;
                        long maxTs = Long.MIN_VALUE;
                        for (Path p : commits) {
                            String json = Files.readString(p);
                            long ts = extractJsonTimestamp(json);
                            if (ts > maxTs) {
                                maxTs = ts;
                                latest = p;
                            }
                        }

                        if (latest != null) {
                            writer.writeBytes(latest.getFileName().toString().replace(".json","") + "\n");
                        } else {
                            writer.writeBytes("ERROR No commits\n");
                        }
                    }

                    case "LOG" -> {
                        // LOG <repo>
                        if (parts.length < 2) { writer.writeBytes("ERROR Bad LOG\n"); break; }
                        String repo = parts[1];
                        Path commitsRoot = Paths.get(REPO_ROOT, repo, ".myvcs", "commits");
                        writer.writeBytes("MAIN:\n");
                        Path main = commitsRoot.resolve("main");
                        if (Files.exists(main)) {
                            try (DirectoryStream<Path> ds = Files.newDirectoryStream(main, "*.json")) {
                                for (Path p : ds) writer.writeBytes(" - " + p.getFileName().toString().replace(".json","") + "\n");
                            }
                        }
                        writer.writeBytes("PENDING:\n");
                        Path pending = commitsRoot.resolve("pending");
                        if (Files.exists(pending)) {
                            try (DirectoryStream<Path> ds = Files.newDirectoryStream(pending, "*.json")) {
                                for (Path p : ds) writer.writeBytes(" - " + p.getFileName().toString().replace(".json","") + "\n");
                            }
                        }
                        writer.writeBytes("END\n");
                    }

                    default -> writer.writeBytes("ERROR Unknown command\n");
                }
            }

        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<CommitInfo> readCommits(Path dir, String state) throws IOException {
        List<CommitInfo> commits = new ArrayList<>();
        if (!Files.exists(dir)) return commits;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : ds) {
                String json = Files.readString(p);
                String id = p.getFileName().toString().replace(".json", "");
                long ts = extractJsonTimestamp(json);
                Map<String, String> files = new LinkedHashMap<>(parseFilesFromJson(json));
                commits.add(new CommitInfo(id, state, json, ts, files));
            }
        }

        commits.sort(Comparator.comparingLong(CommitInfo::timestamp));
        return commits;
    }

    // crude JSON files parser: extract "files" mapping => filename -> hash
    private static Map<String, String> parseFilesFromJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        // regex: "some/path.txt"  :  "abcdef..."
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([0-9a-fA-F]{6,64})\"");
        java.util.regex.Matcher m = p.matcher(json);
        boolean inFiles = json.contains("\"files\"");
        while (m.find()) {
            // make sure match is inside the "files" object area (we're simple: assume 'files' occurs before these matches)
            // To be safer, ensure the match occurs after "files"
            int filesIdx = json.indexOf("\"files\"");
            if (filesIdx >= 0 && m.start() > filesIdx) {
                String key = m.group(1);
                String val = m.group(2);
                map.put(key, val);
            }
        }
        return map;
    }

    private static long extractJsonTimestamp(String json) {
        String pattern = "\"timestamp\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private record CommitInfo(String id, String state, String json, long timestamp, Map<String, String> files) {}

}
