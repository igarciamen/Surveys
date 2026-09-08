package com.igarciamen.surveys.repository;

import com.igarciamen.surveys.model.Survey;
import com.igarciamen.surveys.model.SurveyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    // Public catalog search: only PUBLISHED surveys, same LOWER/LIKE style as ProductRepository.
    @Query("SELECT s FROM Survey s " +
            "WHERE s.status = com.igarciamen.surveys.model.SurveyStatus.PUBLISHED " +
            "  AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "    OR LOWER(s.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Survey> searchPublished(@Param("q") String query, Pageable pageable);

    Page<Survey> findByStatus(SurveyStatus status, Pageable pageable);

    // The designer's own dashboard (any status).
    Page<Survey> findByOwnerUserId(Long ownerUserId, Pageable pageable);

    // Used by the @Scheduled auto-close job.
    List<Survey> findByStatusAndClosesAtBefore(SurveyStatus status, Instant cutoff);
}
