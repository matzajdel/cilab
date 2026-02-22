package org.example.versioncontrolserver.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String headCommitId;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "repository_id")
    private Repo repo;

    public Branch(Repo repo, String name) {
        this.repo = repo;
        this.name = name;
    }
}
