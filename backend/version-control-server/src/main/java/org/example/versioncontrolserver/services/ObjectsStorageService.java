package org.example.versioncontrolserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.versioncontrolserver.dto.CommitRequestDTO;
import org.example.versioncontrolserver.dto.PullManifestDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectsStorageService {
    private static final Path GLOBAL_OBJECTS_DIR = Paths.get("server-storage/global-objects");
    private final ObjectMapper objectMapper;

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
}
