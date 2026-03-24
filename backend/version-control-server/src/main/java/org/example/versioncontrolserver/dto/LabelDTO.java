package org.example.versioncontrolserver.dto;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder
@NoArgsConstructor
public class LabelDTO {
    private String name;
    private int value;
    private String commitId;
}
