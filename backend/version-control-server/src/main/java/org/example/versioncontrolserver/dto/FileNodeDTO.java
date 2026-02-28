package org.example.versioncontrolserver.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FileNodeDTO {
    private String name;
    private String path;
    private NodeType type;
    private String blobHash;
    private List<FileNodeDTO> children;

    public FileNodeDTO(String name, String path, String blobHash) {
        this.name = name;
        this.path = path;
        this.type = NodeType.FILE;
        this.blobHash = blobHash;
    }

    public FileNodeDTO(String name, String path) {
        this.name = name;
        this.path = path;
        this.type = NodeType.DIRECTORY;
        this.children = new ArrayList<>();
    }
}
