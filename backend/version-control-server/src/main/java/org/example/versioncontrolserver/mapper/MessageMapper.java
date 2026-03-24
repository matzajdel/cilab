package org.example.versioncontrolserver.mapper;

import org.example.versioncontrolserver.dto.MessageDTO;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.entities.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageMapper {
    public Message toEntity(MessageDTO dto, Commit commit) {
        return Message.builder()
                .text(dto.getText())
                .authorEmail(dto.getAuthorEmail())
                .commit(commit)
                .build();
    }

    public MessageDTO toDTO(Message entity) {
        return MessageDTO.builder()
                .text(entity.getText())
                .authorEmail(entity.getAuthorEmail())
                .commitId(entity.getCommit().getId())
                .build();
    }
}
