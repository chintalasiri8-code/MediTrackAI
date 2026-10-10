package com.meditrack.ai.repository;

import com.meditrack.ai.entity.WeeklyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WeeklyFeedbackRepository extends JpaRepository<WeeklyFeedback, Long> {
    List<WeeklyFeedback> findByCaseIdOrderByWeekNumberAsc(String caseId);
    Optional<WeeklyFeedback> findByCaseIdAndWeekNumber(String caseId, Integer weekNumber);
    void deleteByCaseId(String caseId);
}
