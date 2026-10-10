package com.meditrack.ai.controller;

import com.meditrack.ai.entity.DocumentEntity;
import com.meditrack.ai.repository.DocumentRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import com.meditrack.ai.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired private TreatmentService treatmentService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private CaseAccessValidator accessValidator;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("caseId") String caseId,
            @RequestParam(value = "role", defaultValue = "PATIENT") String role,
            @RequestParam("file") MultipartFile file) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to upload document for this case.");
        }
        try {
            DocumentEntity doc = treatmentService.uploadDocument(caseId.toUpperCase(), role, file);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Document upload error: " + e.getMessage());
        }
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<?> getDocumentsByCase(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to documents for this case.");
        }
        return ResponseEntity.ok(documentRepository.findByCaseIdOrderByUploadedAtDesc(caseId.toUpperCase()));
    }
}

