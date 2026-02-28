package org.example.versioncontrolserver.repositories;

import org.example.versioncontrolserver.dto.CommitSummaryDTO;
import org.example.versioncontrolserver.entities.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommitRepository extends JpaRepository<Commit, String> {

    @Query(value = """
        WITH RECURSIVE ancestry AS (
            -- Punkt startowy (Twój nowy commit)
            SELECT id, parent_id 
            FROM commit
            WHERE id = :descendantId
            
            UNION ALL
            
            -- Krok rekurencyjny (Idziemy w górę po rodzicach)
            SELECT c.id, c.parent_id 
            FROM commit c
            INNER JOIN ancestry a ON c.id = a.parent_id
        )
        -- Sprawdzamy, czy w zebranej historii jest nasz ancestorId
        SELECT COUNT(*) > 0 
        FROM ancestry 
        WHERE id = :ancestorId
        """, nativeQuery = true)
    boolean isAncestor(@Param("descendantId") String descendantId, @Param("ancestorId") String ancestorId);

    @Query(value = """
        WITH RECURSIVE ancestor_search AS (
            SELECT id, parent_id, status FROM commit WHERE id = :startId
            UNION ALL

            SELECT c.id, c.parent_id, c.status
            FROM commit c
            INNER JOIN ancestor_search a ON c.id = a.parent_id

            WHERE a.status NOT IN ('MERGED', 'IN_REVIEW') 
        )
        SELECT COUNT(*) FROM ancestor_search WHERE status = 'IN_REVIEW'
    """, nativeQuery = true)
    int countInReviewAncestors(@Param("startId") String startId);

    List<CommitSummaryDTO> findAllByRepo_IdOrderByTimestampDesc(Long id);

    Optional<CommitSummaryDTO> findProjectedById(String id);

    List<CommitSummaryDTO> findFirst6AllByAuthorEmailOrderByTimestampDesc(String authorEmail);
}
