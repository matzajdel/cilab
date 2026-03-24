package org.example.versioncontrolserver.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.versioncontrolserver.entities.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}
