package com.meditrack.ai.repository;

import com.meditrack.ai.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByCaseId(String caseId);
    List<Medicine> findByCaseIdAndTimeOfDay(String caseId, String timeOfDay);
    void deleteByCaseId(String caseId);
}
