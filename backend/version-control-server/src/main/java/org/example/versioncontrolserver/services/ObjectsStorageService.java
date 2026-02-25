package org.example.versioncontrolserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.versioncontrolserver.dto.CommitFileDTO;
import org.example.versioncontrolserver.dto.CommitRequestDTO;
import org.example.versioncontrolserver.dto.FileNodeDTO;
import org.example.versioncontrolserver.dto.PullManifestDTO;
import org.example.versioncontrolserver.entities.Branch;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.repositories.BranchRepository;
import org.example.versioncontrolserver.repositories.CommitRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectsStorageService {
    private static final Path GLOBAL_OBJECTS_DIR = Paths.get("server-storage/global-objects");
    private final ObjectMapper objectMapper;

    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(GLOBAL_OBJECTS_DIR);
    }

    public byte[] getFileContent(String blobHash) {
        Path blobPath = GLOBAL_OBJECTS_DIR.resolve(blobHash);

        if (!Files.exists(blobPath)) {
            throw new EntityNotFoundException("Blob not found: " + blobHash);
        }

        try {
            return Files.readAllBytes(blobPath);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading files");
        }
    }

    public void saveObjectsFromStream(InputStream zipStream) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String hash = entry.getName();

                // 1. Validation: correct SHA-1? (40 signs hex)
                if (!hash.matches("^[a-f0-9]{40}$")) {
                    log.warn("Zignorowano plik o nieprawidłowej nazwie (to nie jest hash): {}", hash);
                    zis.closeEntry();
                    continue;
                }

                // 2. Validation "Zip Slip"
                Path targetPath = GLOBAL_OBJECTS_DIR.resolve(hash).normalize();
                if (!targetPath.startsWith(GLOBAL_OBJECTS_DIR.normalize())) {
                    throw new SecurityException("Wykryto próbę ataku Zip Slip! Nieprawidłowa ścieżka: " + hash);
                }

                // Direct stream save
                if (!Files.exists(targetPath)) {
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Zapisano nowy obiekt: {}", hash);
                }
                zis.closeEntry();
            }
        }
    }

    public void streamPullDataAsZip(PullManifestDTO manifest, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            if (manifest.commits().isEmpty()) return;

            // 1. Write the newest commit
            zos.putNextEntry(new ZipEntry("commit-metadata.json"));
            CommitRequestDTO topCommit = manifest.commits().get(0);
            zos.write(objectMapper.writeValueAsBytes(topCommit));
            zos.closeEntry();

            // 2. Write all commits
            for (CommitRequestDTO commit : manifest.commits()) {
                zos.putNextEntry(new ZipEntry("commits/" + commit.commitId() + ".json"));
                zos.write(objectMapper.writeValueAsBytes(commit));
                zos.closeEntry();
            }

            // 3. Write objects
            for (String blobHash : manifest.blobHashes()) {
                Path blobPath = GLOBAL_OBJECTS_DIR.resolve(blobHash);

                if (Files.exists(blobPath)) {
                    zos.putNextEntry(new ZipEntry("objects/" + blobHash));
                    Files.copy(blobPath, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    public List<FileNodeDTO> getBranchTree(String branchId) {
        Branch branch = branchRepository.findById(Long.parseLong(branchId))
                .orElseThrow(() -> new EntityNotFoundException("Branch with id: " + branchId + "not found"));

        String headCommitId = branch.getHeadCommitId();

        Commit commit = commitRepository.findById(headCommitId)
                .orElseThrow(() -> new EntityNotFoundException("Head commit for branch does not exist"));

        Map<String, String> flatFiles = commit.getFileMap();

        return buildTree(flatFiles);
    }

    public List<FileNodeDTO> buildTree(Map<String, String> flatFiles) {
        // Tu będziemy trzymać pliki i foldery z samego "korzenia" (root) repozytorium
        List<FileNodeDTO> rootNodes = new ArrayList<>();

        // Mapa pomocnicza, żebyśmy wiedzieli, czy dany folder już istnieje w drzewie
        // Kluczem jest pełna ścieżka folderu (np. "src/main/java")
        Map<String, FileNodeDTO> directoryCache = new HashMap<>();

        for (Map.Entry<String, String> entry : flatFiles.entrySet()) {
            String fullPath = entry.getKey();
            String blobHash = entry.getValue();

            // Ujednolicamy ukośniki na wypadek, gdyby klient używał Windowsa (\)
            String normalizedPath = fullPath.replace("\\", "/");
            String[] parts = normalizedPath.split("/");

            FileNodeDTO parentDir = null;
            String currentDirPath = "";

            // Przechodzimy przez każdy segment ścieżki (np. ["src", "main", "Main.java"])
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isLastPart = (i == parts.length - 1); // Jeśli to ostatni element, to jest to plik

                if (isLastPart) {
                    // --- TO JEST PLIK ---
                    FileNodeDTO fileNode = new FileNodeDTO(part, normalizedPath, blobHash);

                    if (parentDir != null) {
                        parentDir.getChildren().add(fileNode); // Wrzucamy plik do folderu
                    } else {
                        rootNodes.add(fileNode); // Plik leży w głównym katalogu repozytorium
                    }
                } else {
                    // --- TO JEST FOLDER ---
                    // Budujemy ścieżkę tego konkretnego folderu
                    currentDirPath = currentDirPath.isEmpty() ? part : currentDirPath + "/" + part;

                    // Sprawdzamy, czy już wcześniej nie utworzyliśmy tego folderu
                    FileNodeDTO dirNode = directoryCache.get(currentDirPath);

                    if (dirNode == null) {
                        // Tworzymy nowy folder
                        dirNode = new FileNodeDTO(part, currentDirPath);
                        directoryCache.put(currentDirPath, dirNode);

                        // Podpinamy go pod rodzica (lub do korzenia)
                        if (parentDir != null) {
                            parentDir.getChildren().add(dirNode);
                        } else {
                            rootNodes.add(dirNode);
                        }
                    }

                    // Ten folder staje się "rodzicem" dla następnego segmentu w pętli
                    parentDir = dirNode;
                }
            }
        }

        return rootNodes;
    }
}
