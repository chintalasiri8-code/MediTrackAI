package com.meditrack.ai.repository;

import com.meditrack.ai.entity.AISummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AISummaryRepository extends JpaRepository<AISummary, Long> {
    List<AISummary> findByCaseIdOrderByWeekNumberAsc(String caseId);
    Optional<AISummary> findByCaseIdAndWeekNumber(String caseId, Integer weekNumber);
    void deleteByCaseId(String caseId);
}
