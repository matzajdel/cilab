package org.example.versioncontrolserver.dto;

import lombok.Builder;

@Builder
public record BranchDTO(
        Long id,
        String name,
        String headCommitId
) {
}
