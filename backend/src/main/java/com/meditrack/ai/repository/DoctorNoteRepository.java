package com.meditrack.ai.repository;

import com.meditrack.ai.entity.DoctorNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorNoteRepository extends JpaRepository<DoctorNote, Long> {
    List<DoctorNote> findByCaseIdOrderByCreatedDateDesc(String caseId);
    void deleteByCaseId(String caseId);
}
