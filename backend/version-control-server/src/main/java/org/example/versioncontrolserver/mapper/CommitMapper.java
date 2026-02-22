package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.CommitRequestDTO;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.CommitFile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommitMapper {
    public CommitRequestDTO toDTO(Commit entity) {
        Map<String, String> files = entity.getFiles().stream()
                .collect(Collectors.toMap(CommitFile::getPath, CommitFile::getBlobHash));

        return CommitRequestDTO.builder()
                .commitId(entity.getId())
                .parentId(entity.getParentId())
                .secondParentId(entity.getSecondParentId())
                .message(entity.getMessage())
                .timestamp(entity.getTimestamp())
                .files(files)
                .authorEmail(entity.getAuthorEmail())
                .build();
    }

    public Commit toEntity(CommitRequestDTO dto) {
        return Commit.builder()
                .id(dto.commitId())
                .message(dto.message())
                .timestamp(dto.timestamp())
                .parentId(dto.parentId())
                .secondParentId(dto.secondParentId())
                .authorEmail(dto.authorEmail())
                .branchName(dto.branchName())
                .build();
    }
}
