package org.example.versioncontrolserver.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Commit {
    @Id
    private String id;

    private String message;
    private Long timestamp;
    private String parentId;
    private String secondParentId;
    private String authorEmail;
    private String branchName;

    @Enumerated(EnumType.STRING)
    private CommitStatus status;

    @ManyToOne
    @JoinColumn(name = "repository_id")
    private Repo repo;

    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommitFile> files = new ArrayList<>();

    public void addFile(String path, String hash) {
        CommitFile file = CommitFile.builder()
                .path(path)
                .blobHash(hash)
                .commit(this)
                .build();

        this.files.add(file);
    }

    public Map<String, String> getFileMap() {
        return files.stream()
                .collect(Collectors.toMap(CommitFile::getPath, CommitFile::getBlobHash));
    }
}
