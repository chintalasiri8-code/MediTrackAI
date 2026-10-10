package com.meditrack.ai.controller;

import com.meditrack.ai.dto.WeeklyFeedbackRequest;
import com.meditrack.ai.entity.AISummary;
import com.meditrack.ai.entity.WeeklyFeedback;
import com.meditrack.ai.repository.AISummaryRepository;
import com.meditrack.ai.repository.WeeklyFeedbackRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import com.meditrack.ai.service.GeminiAIService;
import com.meditrack.ai.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recovery")
@CrossOrigin(origins = "*")
public class RecoveryController {

    @Autowired private WeeklyFeedbackRepository feedbackRepository;
    @Autowired private AISummaryRepository aiSummaryRepository;
    @Autowired private TreatmentService treatmentService;
    @Autowired private GeminiAIService geminiAIService;
    @Autowired private CaseAccessValidator accessValidator;

    @PostMapping("/feedback/draft")
    public ResponseEntity<?> saveDraft(@RequestBody WeeklyFeedback feedback) {
        if (feedback.getCaseId() == null || !accessValidator.isUserAuthorizedForCase(feedback.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to save draft for this case.");
        }
        try {
            WeeklyFeedback saved = treatmentService.saveFeedbackDraft(feedback);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving draft: " + e.getMessage());
        }
    }

    @PostMapping("/feedback/summarize")
    public ResponseEntity<?> summarizeWeeklyFeedback(@RequestBody WeeklyFeedback feedback) {
        if (feedback.getCaseId() == null || !accessValidator.isUserAuthorizedForCase(feedback.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to summarize feedback for this case.");
        }
        feedback.setCaseId(feedback.getCaseId().toUpperCase());
        AISummary summary = geminiAIService.generateWeeklySummary(feedback);
        
        Map<String, Object> response = new HashMap<>();
        response.put("feedback", feedback);
        response.put("aiSummary", summary);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback/save")
    public ResponseEntity<?> submitAndSaveSummary(@RequestBody WeeklyFeedbackRequest req) {
        if (req.getCaseId() == null || !accessValidator.isUserAuthorizedForCase(req.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to save weekly summary for this case.");
        }
        try {
            WeeklyFeedback fb = new WeeklyFeedback();
            fb.setCaseId(req.getCaseId().toUpperCase());
            fb.setWeekNumber(req.getWeekNumber() != null ? req.getWeekNumber() : 1);
            fb.setQ1CurrentSymptoms(req.getQ1CurrentSymptoms());
            fb.setQ2SymptomsImproved(req.getQ2SymptomsImproved());
            fb.setQ3NewSymptoms(req.getQ3NewSymptoms());
            fb.setQ4SymptomsWorse(req.getQ4SymptomsWorse());
            fb.setQ5SideEffects(req.getQ5SideEffects());
            fb.setQ6FollowedSchedule(req.getQ6FollowedSchedule());
            fb.setQ7MissedMedicines(req.getQ7MissedMedicines());
            fb.setQ8ImprovementsNoticed(req.getQ8ImprovementsNoticed());
            fb.setQ9ChallengesFaced(req.getQ9ChallengesFaced());
            fb.setQ10DoctorNotes(req.getQ10DoctorNotes());

            AISummary savedSummary = treatmentService.submitWeeklyFeedbackAndSaveSummary(fb);
            return ResponseEntity.ok(savedSummary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving weekly summary: " + e.getMessage());
        }
    }

    @GetMapping("/history/{caseId}")
    public ResponseEntity<?> getRecoveryHistory(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to recovery history.");
        }
        return ResponseEntity.ok(aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(caseId.toUpperCase()));
    }

    @GetMapping("/overall-progress/{caseId}")
    public ResponseEntity<?> getOverallProgressSummary(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to overall progress.");
        }
        try {
            String overallText = treatmentService.generateOverallSummary(caseId.toUpperCase());
            Map<String, String> res = new HashMap<>();
            res.put("overallProgressSummary", overallText);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating overall progress summary: " + e.getMessage());
        }
    }
}

