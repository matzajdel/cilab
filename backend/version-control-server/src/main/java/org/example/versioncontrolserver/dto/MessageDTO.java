package org.example.versioncontrolserver.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private String text;
    private String authorEmail;
    private String commitId;
}
