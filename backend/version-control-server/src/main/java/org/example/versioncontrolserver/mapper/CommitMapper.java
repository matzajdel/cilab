package org.example.versioncontrolserver.mapper;

import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.CommitDetailsDTO;
import org.example.versioncontrolserver.dto.CommitRequestDTO;
import org.example.versioncontrolserver.dto.LabelDTO;
import org.example.versioncontrolserver.dto.MessageDTO;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.CommitFile;
import org.example.versioncontrolserver.entities.CommitStatus;
import org.example.versioncontrolserver.entities.Repo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommitMapper {
    private final MessageMapper messageMapper;
    private final LabelMapper labelMapper;

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
                .build();
    }

    public CommitDetailsDTO toCommitDetailsDTO(Commit entity) {
        return CommitDetailsDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .authorEmail(entity.getAuthorEmail())
                .branchName(entity.getBranch().getName())
                .timestamp(entity.getTimestamp())
                .status(entity.getStatus())
                .messages(entity.getMessages().stream()
                        .map(messageMapper::toDTO)
                        .toList()
                )
                .labels(entity.getLabels().stream()
                        .map(labelMapper::toDTO)
                        .toList()
                )
                .build();
    }
}
