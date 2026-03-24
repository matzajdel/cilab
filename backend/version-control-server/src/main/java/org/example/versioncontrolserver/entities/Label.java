package org.example.versioncontrolserver.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int value = 0;

    private String authorEmail;

    @ManyToOne
    @JoinColumn(name = "commit_id")
    private Commit commit;
}
