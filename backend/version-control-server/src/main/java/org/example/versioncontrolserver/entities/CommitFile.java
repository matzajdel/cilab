package org.example.versioncontrolserver.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "commit_files", indexes = {
        @Index(name = "idx_commit_file_path", columnList = "path"),
        @Index(name = "idx_blob_hash", columnList = "blobHash")
})
public class CommitFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String blobHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private Commit commit;

    public CommitFile(String path, String blobHash, Commit commit) {
        this.path = path;
        this.blobHash = blobHash;
        this.commit = commit;
    }
}
