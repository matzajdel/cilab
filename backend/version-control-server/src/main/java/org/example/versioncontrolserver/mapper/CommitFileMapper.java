package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.CommitFileDTO;
import org.example.versioncontrolserver.entities.CommitFile;
import org.springframework.stereotype.Service;

@Service
public class CommitFileMapper {
    public CommitFileDTO toDTO(CommitFile entity) {
        return CommitFileDTO.builder()
                .id(entity.getId())
                .path(entity.getPath())
                .blobHash(entity.getBlobHash())
                .build();
    }
}
