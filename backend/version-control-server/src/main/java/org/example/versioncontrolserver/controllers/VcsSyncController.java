package org.example.versioncontrolserver.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.PullManifestDTO;
import org.example.versioncontrolserver.dto.PushRequestDTO;
import org.example.versioncontrolserver.services.ObjectsStorageService;
import org.example.versioncontrolserver.services.VcsSyncService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class VcsSyncController {
    private final VcsSyncService syncService;
    private final ObjectsStorageService objectsService;

    @PostMapping("/repo/{repoName}/push-init")
    public List<String> pushInit(@PathVariable String repoName, @RequestBody PushRequestDTO request) {
        return syncService.handlePushInit(repoName, request);
    }

    @PostMapping(value = "/repo/{repoName}/push-objects", consumes = "application/zip")
    public void pushObjects(@PathVariable String repoName, HttpServletRequest request) throws IOException {
//        service.handlePushObjectStream(inputStream);
        objectsService.saveObjectsFromStream(request.getInputStream());
    }

    @GetMapping("/repo/{repoName}/pull")
    public ResponseEntity<StreamingResponseBody> pull(
        @PathVariable String repoName,
        @RequestParam("target") String target
    ) throws IOException {
        PullManifestDTO manifest = syncService.preparePullManifest(repoName, target);

        StreamingResponseBody stream = outputStream -> {
            objectsService.streamPullDataAsZip(manifest, outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pull-data.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

//    @GetMapping(value = "/repo/{repoName}/pull", produces = "application/zip")
//    public void pull(@PathVariable String repoName, @RequestParam String target, HttpServletResponse response) throws IOException {
//        response.setHeader("Content-Disposition", "attachment; filename=repo.zip");
//        service.handlePullStream(repoName, target, response.getOutputStream());
//    }

}
