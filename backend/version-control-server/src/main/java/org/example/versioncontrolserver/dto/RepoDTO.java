package org.example.versioncontrolserver.dto;

import jakarta.persistence.Column;
import lombok.Builder;

@Builder
public record RepoDTO(
    Long id,
    String name
) {
}
