package com.meditrack.ai.repository;

import com.meditrack.ai.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByCaseId(String caseId);
    List<Reminder> findByCaseIdAndActiveTrue(String caseId);
    void deleteByCaseId(String caseId);
}
