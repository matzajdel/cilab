package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.LabelDTO;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.Label;
import org.springframework.stereotype.Service;

@Service
public class LabelMapper {
    public Label toEntity(LabelDTO dto, Commit commit, String authorEmail) {
        return Label.builder()
                .name(dto.getName())
                .value(dto.getValue())
                .authorEmail(authorEmail)
                .commit(commit)
                .build();
    }

    public LabelDTO toDTO (Label entity) {
        return LabelDTO.builder()
                .name(entity.getName())
                .value(entity.getValue())
                .commitId(entity.getCommit().getId())
                .build();
    }
}
