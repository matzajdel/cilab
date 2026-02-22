package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.RepoDTO;
import org.example.versioncontrolserver.entities.Repo;
import org.springframework.stereotype.Service;

@Service
public class RepoMapper {
    public RepoDTO toDTO(Repo entity) {
        return RepoDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
