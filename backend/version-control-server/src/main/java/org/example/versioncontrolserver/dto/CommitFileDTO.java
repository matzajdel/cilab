package org.example.versioncontrolserver.dto;

import jakarta.persistence.Column;
import lombok.Builder;

@Builder
public record CommitFileDTO(
        Long id,
        String path,
        String blobHash
) {
}
