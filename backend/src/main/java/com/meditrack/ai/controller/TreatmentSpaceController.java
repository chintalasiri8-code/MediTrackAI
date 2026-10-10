package com.meditrack.ai.controller;

import com.meditrack.ai.config.UserPrincipal;
import com.meditrack.ai.entity.*;
import com.meditrack.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/treatment-space")
@CrossOrigin(origins = "*")
public class TreatmentSpaceController {

    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private OCRResultRepository ocrResultRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private WeeklyFeedbackRepository feedbackRepository;
    @Autowired private AISummaryRepository aiSummaryRepository;
    @Autowired private TimelineEventRepository timelineRepository;
    @Autowired private DoctorNoteRepository noteRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;

    @Autowired private com.meditrack.ai.service.CaseAccessValidator accessValidator;

    @GetMapping("/{caseId}")
    public ResponseEntity<?> getTreatmentSpaceData(@PathVariable String caseId) {
        String formattedCaseId = caseId.toUpperCase();
        Optional<TreatmentCase> caseOpt = caseRepository.findByCaseId(formattedCaseId);
        if (caseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Treatment Case ID not found.");
        }

        if (!accessValidator.isUserAuthorizedForCase(formattedCaseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to this treatment space.");
        }

        TreatmentCase tc = caseOpt.get();

        Map<String, Object> data = new HashMap<>();
        data.put("treatmentCase", tc);
        data.put("prescriptions", prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(formattedCaseId));
        data.put("ocrResult", ocrResultRepository.findFirstByCaseIdOrderByProcessedAtDesc(formattedCaseId).orElse(null));
        data.put("documents", documentRepository.findByCaseIdOrderByUploadedAtDesc(formattedCaseId));
        data.put("medicines", medicineRepository.findByCaseId(formattedCaseId));
        data.put("reminders", reminderRepository.findByCaseId(formattedCaseId));
        data.put("feedbacks", feedbackRepository.findByCaseIdOrderByWeekNumberAsc(formattedCaseId));
        data.put("aiSummaries", aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(formattedCaseId));
        data.put("timeline", timelineRepository.findByCaseIdOrderByTimestampAsc(formattedCaseId));
        data.put("doctorNotes", noteRepository.findByCaseIdOrderByCreatedDateDesc(formattedCaseId));

        return ResponseEntity.ok(data);
    }
}
