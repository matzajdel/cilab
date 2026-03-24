package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.entities.Branch;
import org.example.versioncontrolserver.entities.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    List<Webhook> findByTriggerSourceBranch(Branch branch);
}
