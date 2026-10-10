package com.meditrack.ai.repository;

import com.meditrack.ai.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByCaseIdOrderByTimestampAsc(String caseId);
    void deleteByCaseId(String caseId);
}
