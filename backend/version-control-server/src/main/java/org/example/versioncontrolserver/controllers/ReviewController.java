package org.example.versioncontrolserver.controllers;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.services.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping(value = "/repo/{repoName}/submit/{commitId}")
    public ResponseEntity submit(@PathVariable String repoName, @PathVariable String commitId) {
        try {
            service.submitReview(repoName, commitId);
            return ResponseEntity.ok("Review submitted successfully.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/repo/{repoName}/rebase/{commitId}")
    public ResponseEntity<String> rebase(
            @PathVariable String repoName,
            @PathVariable String commitId,
            @RequestParam(defaultValue = "master") String branch
    ) {
        return ResponseEntity.ok(service.rebase(repoName, commitId, branch));
    }
}
