package org.example.versioncontrolserver.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record CommitRequestDTO(
        String commitId,
        String parentId,
        String secondParentId,
        String message,
        String authorEmail,
        String branchName,
        Long timestamp,
        Map<String, String> files
) {}
