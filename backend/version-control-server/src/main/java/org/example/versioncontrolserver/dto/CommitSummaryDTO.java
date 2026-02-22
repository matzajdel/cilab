package org.example.versioncontrolserver.dto;

import org.example.versioncontrolserver.entities.CommitStatus;

public record CommitSummaryDTO(
        String id,
        String message,
        String authorEmail,
        String branchName,
        Long timestamp,
        CommitStatus status
) {}

