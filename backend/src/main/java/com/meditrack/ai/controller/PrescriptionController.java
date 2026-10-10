package com.meditrack.ai.controller;

import com.meditrack.ai.entity.OCRResult;
import com.meditrack.ai.entity.Prescription;
import com.meditrack.ai.repository.OCRResultRepository;
import com.meditrack.ai.repository.PrescriptionRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import com.meditrack.ai.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Autowired private TreatmentService treatmentService;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private OCRResultRepository ocrResultRepository;
    @Autowired private CaseAccessValidator accessValidator;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPrescription(
            @RequestParam("caseId") String caseId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("file") MultipartFile file) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to upload prescription for this case.");
        }
        try {
            Prescription p = treatmentService.uploadPrescription(caseId.toUpperCase(), title, file);
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Prescription upload error: " + e.getMessage());
        }
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<?> getPrescriptionsByCase(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to prescriptions for this case.");
        }
        return ResponseEntity.ok(prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(caseId.toUpperCase()));
    }

    @GetMapping("/ocr/{prescriptionId}")
    public ResponseEntity<?> getOCRResult(@PathVariable Long prescriptionId) {
        Optional<Prescription> pOpt = prescriptionRepository.findById(prescriptionId);
        if (pOpt.isEmpty() || !accessValidator.isUserAuthorizedForCase(pOpt.get().getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to OCR result.");
        }
        return ocrResultRepository.findByPrescriptionId(prescriptionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

