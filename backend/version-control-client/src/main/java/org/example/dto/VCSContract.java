package org.example.dto;

import java.util.List;
import java.util.Map;

public class VCSContract {
    public record CommitDTO(
            String commitId,
            String parentId,
            String secondParentId,
            String message,
            String authorEmail,
            String branchName,
            long timestamp,
            Map<String, String> files
    ) {}

    public record PushRequest(
            String branchName,
            String commitId,
            CommitDTO commitData,
            List<String> objects
    ) {}

    public record MissingObjectsRequest(
        List<String> missingHashes,
        Map<String, String> payload
    ) {}

    public record PullResponse(
        String headCommitId,
        List<CommitDTO> history,
        Map<String, String> objects
    ) {}

    public record FetchResult(
            String fetchedCommitId,
            int commitsCount,
            int objectsCount
    ) {}
}