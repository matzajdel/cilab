package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MyVCSClientDelta {

    private static final String HOST = "localhost";
    private static final int PORT = 9000;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("Usage: <cmd> ..."); return; }
        String cmd = args[0];

        switch (cmd) {
            case "init" -> initLocal(args[1]);
            case "add" -> addLocal(args[1]);
            case "commit" -> commitLocal(args[1], args[2]);
            case "push" -> deltaPush(args[1], args[2]);               // repo, commitId
            case "pull" -> {
                String commitId = args.length > 2 ? args[2] : null;
                deltaPull(args[1], commitId);
            }// repo, commitId
            case "merge" -> remoteMerge(args[1], args[2]);            // repo, commitId
            case "log" -> showClientLog();
            case "log-remote" -> remoteLog(args[1]);
            default -> System.out.println("Unknown cmd");
        }
    }

    // ---------- local helpers ----------
    private static void initLocal(String repoDir) throws IOException {
        Path dir = Paths.get(repoDir, ".myvcs");
        Files.createDirectories(dir.resolve("objects"));
        Files.createDirectories(dir.resolve("commits"));
        // initialize empty index
        Files.writeString(dir.resolve("index"), "");
        System.out.println("Initialized local repo at " + dir.toAbsolutePath());
    }

    private static void addLocal(String file) throws Exception {
        Path p = Paths.get(file);
        byte[] data = Files.readAllBytes(p);
        String hash = sha1(data);
        Path obj = Paths.get(".myvcs/objects", hash);
        Files.createDirectories(obj.getParent());
        Files.write(obj, data);

        // Zapis mappingu nazwaPliku -> hash do index
        Path indexFile = Paths.get(".myvcs/index");
        List<String> index = new ArrayList<>();
        if (Files.exists(indexFile)) index = Files.readAllLines(indexFile);
        // usuń stare wpisy dla tego pliku
        index.removeIf(line -> line.startsWith(file + ":"));
        index.add(file + ":" + hash);
        Files.write(indexFile, index);

        System.out.println("Added blob " + hash + " for file " + file);
    }

//    private static void commitLocal(String repoDir, String message) throws Exception {
//        Path indexFile = Paths.get(".myvcs/index");
//        if (!Files.exists(indexFile)) {
//            System.out.println("Nothing to commit");
//            return;
//        }
//        List<String> index = Files.readAllLines(indexFile);
//        Map<String,String> files = new LinkedHashMap<>();
//        for (String line : index) {
//            String[] sp = line.split(":", 2);
//            if (sp.length == 2) files.put(sp[0], sp[1]);
//        }
//
//        String id = UUID.randomUUID().toString().substring(0,8);
//        StringBuilder json = new StringBuilder();
//        json.append("{\n  \"id\":\"").append(id).append("\",\n");
//        json.append("  \"message\":\"").append(message).append("\",\n");
//        json.append("  \"files\":{\n");
//        int i=0;
//        for (var e: files.entrySet()) {
//            json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
//            if (++i < files.size()) json.append(",");
//            json.append("\n");
//        }
//        json.append("  }\n}");
//        Files.createDirectories(Paths.get(".myvcs/commits"));
//        Files.writeString(Paths.get(".myvcs/commits", id + ".json"), json.toString());
//
//        // po commicie można wyczyścić index
//        Files.writeString(indexFile, "");
//
//        System.out.println("Committed local id=" + id);
//    }

    private static void commitLocal(String repoDir, String message) throws Exception {
        Path indexFile = Paths.get(".myvcs/index");
        if (!Files.exists(indexFile)) {
            System.out.println("Nothing to commit");
            return;
        }
        List<String> index = Files.readAllLines(indexFile);
        Map<String,String> files = new LinkedHashMap<>();
        for (String line : index) {
            String[] sp = line.split(":", 2);
            if (sp.length == 2) files.put(sp[0], sp[1]);
        }

        String id = UUID.randomUUID().toString().substring(0,8);
        long timestamp = System.currentTimeMillis(); // <-- dodajemy timestamp

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"id\":\"").append(id).append("\",\n");
        json.append("  \"message\":\"").append(message).append("\",\n");
        json.append("  \"timestamp\":").append(timestamp).append(",\n"); // <-- tutaj
        json.append("  \"files\":{\n");
        int i=0;
        for (var e: files.entrySet()) {
            json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
            if (++i < files.size()) json.append(",");
            json.append("\n");
        }
        json.append("  }\n}");
        Files.createDirectories(Paths.get(".myvcs/commits"));
        Files.writeString(Paths.get(".myvcs/commits", id + ".json"), json.toString());

        // po commicie można wyczyścić index
        Files.writeString(indexFile, "");

        System.out.println("Committed local id=" + id + ", timestamp=" + timestamp);
    }

    // ---------- network helpers ----------
    private static void deltaPush(String repo, String commitId) throws Exception {
        // gather local blobs
        Map<String, Path> local = getLocalBlobs(Paths.get("."));
        try (Socket s = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             DataOutputStream writer = new DataOutputStream(s.getOutputStream());
             InputStream in = s.getInputStream()) {

            // 1) send HASHES
            writer.writeBytes("HASHES\n");
            for (String h : local.keySet()) writer.writeBytes(h + "\n");
            writer.writeBytes("END\n");
            String ok = reader.readLine(); // OK
            System.out.println("Server: " + ok);

            // 2) ask server which blobs it is missing
            writer.writeBytes("MISSING_REQ " + repo + "\n");
            List<String> missing = new ArrayList<>();
            String line;
            while (!(line = reader.readLine()).equals("END")) {
                if (line.startsWith("MISSING")) continue;
                missing.add(line.trim());
            }
            System.out.println("Server missing " + missing.size() + " objects");

            // 3) send only missing blobs
            if (!missing.isEmpty()) {
                writer.writeBytes("SEND " + repo + "\n");
                writer.writeBytes(missing.size() + "\n");
                for (String h : missing) {
                    Path p = local.get(h);
                    long size = Files.size(p);
                    writer.writeBytes(h + "\n");
                    writer.writeBytes(String.valueOf(size) + "\n");
                    try (InputStream fis = Files.newInputStream(p)) {
                        byte[] buf = new byte[8192];
                        long rem = size;
                        while (rem > 0) {
                            int r = fis.read(buf, 0, (int)Math.min(buf.length, rem));
                            writer.write(buf, 0, r);
                            rem -= r;
                        }
                    }
                }
                String resp = reader.readLine();
                System.out.println("SEND response: " + resp);
            }

            // 4) send commit
            Path commitFile = Paths.get(".myvcs/commits", commitId + ".json");
            String json = Files.readString(commitFile);
            writer.writeBytes("PUSH " + repo + " " + commitId + "\n");
            writer.writeBytes(json + "\n");
            writer.writeBytes("END\n");
            System.out.println("Push response: " + reader.readLine());
        }
    }

    private static void deltaPull(String repo, String commitId) throws Exception {
        Path repoRoot = Paths.get(".");
        Map<String, Path> local = getLocalBlobs(repoRoot);

        try (Socket s = new Socket(HOST, PORT);
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            // 1) Wyślij lokalne HASHES
            out.writeBytes("HASHES\n");
            for (String h : local.keySet()) out.writeBytes(h + "\n");
            out.writeBytes("END\n");

            String serverOk = readLine(in);
            System.out.println("Sent HASHES to server; server reply: " + serverOk);

            // 2) Żądanie PULL
            if (commitId == null || commitId.isBlank()) {
                out.writeBytes("PULL " + repo + "\n");
                System.out.println("Pulling full merged history for repo " + repo);
            } else {
                out.writeBytes("PULL " + repo + " " + commitId + "\n");
                System.out.println("Pulling history up to commit " + commitId + " for repo " + repo);
            }

            // 3) Odbierz listę commitów
            String line = readLine(in);
            if (line == null) {
                System.out.println("Server closed connection unexpectedly.");
                return;
            }
            if (line.startsWith("ERROR")) {
                System.out.println("Server error: " + line);
                return;
            }
            if (!line.startsWith("COMMITS")) {
                System.out.println("Unexpected response: " + line);
                return;
            }

            String[] parts = line.split(" ");
            int commitCount = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            System.out.println("Server returned " + commitCount + " commit(s)");

            List<CommitSnapshot> snapshots = new ArrayList<>();
            Path remoteRoot = repoRoot.resolve(".myvcs/remote").resolve(repo).resolve("commits");
            Files.createDirectories(remoteRoot);

            for (int i = 0; i < commitCount; i++) {
                String header = readLine(in);
                if (header == null || !header.startsWith("COMMIT")) {
                    System.out.println("Unexpected commit header: " + header);
                    return;
                }
                String[] hdr = header.split(" ");
                String pulledId = hdr.length > 1 ? hdr[1] : "";
                String state = hdr.length > 2 ? hdr[2] : "main";

                StringBuilder commitJson = new StringBuilder();
                while ((line = readLine(in)) != null && !"END".equals(line)) {
                    commitJson.append(line).append("\n");
                }

                String json = commitJson.toString();
                Map<String, String> files = parseFilesFromJson(json);
                CommitSnapshot snapshot = new CommitSnapshot(pulledId, state, json, files);
                snapshots.add(snapshot);

                Path stateDir = remoteRoot.resolve(state);
                Files.createDirectories(stateDir);
                Path commitFile = stateDir.resolve(pulledId + ".json");
                Files.writeString(commitFile, json);
                System.out.println("Saved commit JSON to " + commitFile);
            }

            // 4) Odbierz listę brakujących blobów
            line = readLine(in);
            if (line == null || !line.startsWith("MISSING")) {
                System.out.println("Unexpected response while reading missing blobs: " + line);
                return;
            }
            String[] missParts = line.split(" ");
            int missingCount = missParts.length > 1 ? Integer.parseInt(missParts[1]) : 0;
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < missingCount; i++) {
                String hash = readLine(in);
                if (hash != null && !hash.isBlank()) missing.add(hash.trim());
            }
            readLine(in); // "END"

            System.out.println("Server will send " + missing.size() + " blobs");

            // 5) Odbierz brakujące blob'y
            Path objDir = repoRoot.resolve(".myvcs/objects");
            Files.createDirectories(objDir);

            for (String expected : missing) {
                String hashLine = readLine(in);
                if (hashLine == null) throw new EOFException("Unexpected EOF while awaiting blob hash");
                String receivedHash = hashLine.trim();
                if (!receivedHash.equals(expected)) {
                    System.out.println("Warning: blob order mismatch. Expected " + expected + " but got " + receivedHash);
                }
                String sizeLine = readLine(in);
                if (sizeLine == null) throw new EOFException("Unexpected EOF while awaiting blob size for " + receivedHash);
                long size = Long.parseLong(sizeLine.trim());
                Path outFile = objDir.resolve(receivedHash);

                try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
                    long rem = size;
                    byte[] buf = new byte[8192];
                    while (rem > 0) {
                        int r = in.read(buf, 0, (int) Math.min(buf.length, rem));
                        if (r == -1) throw new EOFException("Unexpected EOF while receiving blob");
                        fos.write(buf, 0, r);
                        rem -= r;
                    }
                }
                System.out.println("Saved blob " + receivedHash);
            }

            String finalMarker = readLine(in); // spodziewamy się "END"
            if (finalMarker != null && !finalMarker.equals("END")) {
                System.out.println("Unexpected trailer after blobs: " + finalMarker);
            }

            // 6) Odtwórz pliki z połączonego stanu commitów (main + ewentualny pending)
            if (snapshots.isEmpty()) {
                System.out.println("Brak commitów do odtworzenia. Repo pozostaje bez zmian.");
            } else {
                LinkedHashMap<String, String> mergedFiles = new LinkedHashMap<>();
                for (CommitSnapshot snapshot : snapshots) {
                    for (Map.Entry<String, String> e : snapshot.files().entrySet()) {
                        mergedFiles.put(e.getKey(), e.getValue());
                    }
                }

                for (Map.Entry<String, String> e : mergedFiles.entrySet()) {
                    String rawKey = e.getKey();
                    String hash = e.getValue();

                    String cleanKey = rawKey.replace('\\', '/');
                    while (cleanKey.startsWith("./")) cleanKey = cleanKey.substring(2);
                    while (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
                    if (cleanKey.matches("^[A-Za-z]:/.*")) {
                        cleanKey = cleanKey.replaceFirst("^[A-Za-z]:", "");
                        while (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
                    }
                    if (cleanKey.isBlank()) continue;

                    Path blob = objDir.resolve(hash);
                    Path fileOut = repoRoot.resolve(cleanKey).normalize();

                    if (Files.exists(blob)) {
                        Path parent = fileOut.getParent();
                        if (parent != null) Files.createDirectories(parent);
                        Files.copy(blob, fileOut, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Restored file: " + fileOut);
                    } else {
                        System.out.println("Blob not found (cannot restore): " + hash + " for file key=" + rawKey);
                    }
                }

                CommitSnapshot last = snapshots.get(snapshots.size() - 1);
                System.out.println("Pull finished successfully. Restored state of commit " + last.id() + " (" + last.state() + ")");
            }
        }
    }

    // Pomocnicza funkcja do czytania linii z DataInputStream
    private static String readLine(DataInputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char)c);
        }
        if (c == -1 && sb.length() == 0) return null;
        return sb.toString();
    }



    private static void remoteMerge(String repo, String commitId) throws Exception {
        try (Socket s = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             DataOutputStream writer = new DataOutputStream(s.getOutputStream())) {
            writer.writeBytes("MERGE " + repo + " " + commitId + "\n");
            System.out.println(reader.readLine());
        }
    }

    private static void remoteLog(String repo) throws Exception {
        try (Socket s = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             DataOutputStream writer = new DataOutputStream(s.getOutputStream())) {
            writer.writeBytes("LOG " + repo + "\n");
            String l;
            while (!(l = reader.readLine()).equals("END")) System.out.println(l);
        }
    }

    private static void showClientLog() {
        Path commitsDir = Paths.get(".myvcs/commits");
        if (!Files.exists(commitsDir)) {
            System.out.println("No commits yet.");
            return;
        }

        List<CommitLogEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(commitsDir, "*.json")) {
            for (Path p : ds) {
                try {
                    String json = Files.readString(p);
                    String id = extractJsonValue(json, "id");
                    String msg = extractJsonValue(json, "message");
                    long ts = extractJsonTimestamp(json);
                    entries.add(new CommitLogEntry(id.isBlank() ? stripJsonName(p) : id, msg, ts));
                } catch (IOException ex) {
                    System.err.println("Failed to read " + p + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to read commits: " + ex.getMessage());
            return;
        }

        if (entries.isEmpty()) {
            System.out.println("No commits yet.");
            return;
        }

        entries.sort(Comparator.comparingLong(CommitLogEntry::timestamp).thenComparing(CommitLogEntry::id).reversed());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId zone = ZoneId.systemDefault();

        for (CommitLogEntry entry : entries) {
            String stamp = entry.timestamp() > 0
                    ? fmt.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.timestamp()), zone))
                    : "no-timestamp";
            String message = entry.message().isBlank() ? "(brak opisu)" : entry.message();
            System.out.println(entry.id() + " | " + stamp + " | " + message);
        }
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static long extractJsonTimestamp(String json) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"timestamp\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }

    private static String stripJsonName(Path p) {
        String name = p.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    // ---------- utilities ----------
    // return map hash -> path (assuming local blobs stored in .myvcs/objects/hash)
    private static Map<String, Path> getLocalBlobs(Path repoRoot) throws IOException {
        Map<String, Path> map = new HashMap<>();
        Path objDir = repoRoot.resolve(".myvcs").resolve("objects");
        if (!Files.exists(objDir)) return map;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(objDir)) {
            for (Path p : ds) if (Files.isRegularFile(p)) map.put(p.getFileName().toString(), p);
        }
        return map;
    }

    private static String sha1(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] h = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

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

    private static String getLatestLocalCommit() throws IOException {
        Path commitsDir = Paths.get(".myvcs/commits");
        if (!Files.exists(commitsDir)) return null;

        List<Path> commitFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(commitsDir, "*.json")) {
            for (Path p : ds) commitFiles.add(p);
        }

        if (commitFiles.isEmpty()) return null;

        // posortuj według nazwy pliku (UUID) i wybierz ostatni
        commitFiles.sort(Comparator.naturalOrder());
        return commitFiles.get(commitFiles.size() - 1).getFileName().toString().replace(".json", "");
    }

    private static String getLatestRemoteCommit(String repo) throws IOException {
        try (Socket s = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             DataOutputStream writer = new DataOutputStream(s.getOutputStream())) {

            writer.writeBytes("LATEST_COMMIT " + repo + "\n");
            String line = reader.readLine();
            return (line != null && !line.isBlank()) ? line.trim() : null;
        }
    }

    private record CommitSnapshot(String id, String state, String json, Map<String, String> files) {}

    private record CommitLogEntry(String id, String message, long timestamp) {}

}
