package org.example.versioncontrolserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Webhook {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch triggerSourceBranch;

    private String triggeredPipelineId;
}
