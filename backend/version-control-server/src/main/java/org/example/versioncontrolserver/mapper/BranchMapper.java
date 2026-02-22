package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.BranchDTO;
import org.example.versioncontrolserver.entities.Branch;
import org.springframework.stereotype.Service;

@Service
public class BranchMapper {
    public BranchDTO toDTO(Branch entity) {
        return BranchDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .headCommitId(entity.getHeadCommitId())
                .build();
    }
}
