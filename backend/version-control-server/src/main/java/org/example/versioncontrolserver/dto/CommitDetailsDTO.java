package org.example.versioncontrolserver.dto;

import lombok.Builder;
import org.example.versioncontrolserver.entities.CommitStatus;

import java.util.List;

@Builder
public record CommitDetailsDTO (
        String id,
        String message,
        String authorEmail,
        String branchName,
        Long timestamp,
        CommitStatus status,
        List<MessageDTO> messages,
        List<LabelDTO> labels
) {
}
