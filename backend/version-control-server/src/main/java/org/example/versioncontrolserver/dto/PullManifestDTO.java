package org.example.versioncontrolserver.dto;

import java.util.List;
import java.util.Set;

public record PullManifestDTO(
        List<CommitRequestDTO> commits,
        Set<String> blobHashes
) {}
