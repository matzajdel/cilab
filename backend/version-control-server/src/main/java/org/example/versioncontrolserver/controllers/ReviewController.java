package org.example.versioncontrolserver.controllers;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.exception.SubmitRejectedException;
import org.example.versioncontrolserver.services.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vcs")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping(value = "/submit/{commitId}")
    public ResponseEntity submit(
            @PathVariable String commitId,
            @RequestHeader("X-User-Email") String authorEmail
    ) throws SubmitRejectedException {
        try {
            service.submitReview(commitId, authorEmail);
            return ResponseEntity.ok("Review submitted successfully.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/rebase/{commitId}")
    public ResponseEntity<String> rebase(
            @PathVariable String commitId,
            @RequestParam(defaultValue = "master") String branch,
            @RequestHeader("X-User-Email") String authorEmail
    ) {
        return ResponseEntity.ok(service.rebase(commitId, branch, authorEmail));
    }
}
