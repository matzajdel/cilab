package org.example.versioncontrolserver.dto;

import java.util.List;

public record PushRequestDTO(
        String branchName,
        String commitId,
//        boolean reviewMode,
        CommitRequestDTO commitData,
        List<String> objects
) {}

