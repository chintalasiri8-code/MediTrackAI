package com.meditrack.ai.repository;

import com.meditrack.ai.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByCaseIdOrderByUploadedAtDesc(String caseId);
    void deleteByCaseId(String caseId);
}
