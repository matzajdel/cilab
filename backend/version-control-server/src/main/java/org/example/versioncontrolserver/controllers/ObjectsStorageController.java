package org.example.versioncontrolserver.controllers;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.FileNodeDTO;
import org.example.versioncontrolserver.services.ObjectsStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class ObjectsStorageController {
    private final ObjectsStorageService service;

//    @GetMapping("/files/{blobHash}")
//    public ResponseEntity<byte[]> getFileContent(@PathVariable String blobHash) {
//        byte[] content = service.getFileContent(blobHash);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(content);
//    }

    @GetMapping("/files/{blobHash}")
    public ResponseEntity<byte[]> getFileContent(@PathVariable String blobHash) {
        byte[] content = service.getFileContent(blobHash);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/branches/{branchId}/file-tree")
    public ResponseEntity<List<FileNodeDTO>> getBranchTree(@PathVariable String branchId) {
        return ResponseEntity.ok(service.getBranchTree(branchId));
    }
}
