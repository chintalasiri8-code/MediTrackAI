package com.meditrack.ai.service;

import com.meditrack.ai.entity.*;
import com.meditrack.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TreatmentService {

    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private OCRResultRepository ocrResultRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private WeeklyFeedbackRepository feedbackRepository;
    @Autowired private AISummaryRepository aiSummaryRepository;
    @Autowired private TimelineEventRepository timelineRepository;
    @Autowired private DoctorNoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;

    @Autowired private StorageService storageService;
    @Autowired private OCRService ocrService;
    @Autowired private GeminiAIService geminiAIService;

    // Generate Collision-Resistant Case ID like CASE-7F92K
    public String generateUniqueCaseId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String caseId;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("CASE-");
            for (int i = 0; i < 5; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            caseId = sb.toString();
            attempts++;
            if (attempts > 100) {
                caseId = "CASE-" + System.currentTimeMillis() % 100000;
            }
        } while (caseRepository.findByCaseId(caseId).isPresent());

        return caseId;
    }

    @Transactional
    public TreatmentCase createTreatmentCase(Long doctorId, String doctorName, String doctorSpecialty, String treatmentName) {
        String caseId = generateUniqueCaseId();
        
        // Retrieve Doctor entity if present
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        String finalDoctorName = doctorName;
        String finalSpecialty = doctorSpecialty;

        if (doctor != null) {
            User doctorUser = userRepository.findById(doctor.getUserId()).orElse(null);
            if (doctorUser != null) finalDoctorName = doctorUser.getName();
            if (doctor.getSpecialty() != null && !doctor.getSpecialty().isBlank()) {
                finalSpecialty = doctor.getSpecialty();
            }
        }

        TreatmentCase c = new TreatmentCase();
        c.setCaseId(caseId);
        c.setDoctorId(doctorId);
        c.setDoctorName(finalDoctorName != null ? finalDoctorName : "Dr. Anitha");
        c.setDoctorSpecialty(finalSpecialty != null ? finalSpecialty : "Dermatology");
        c.setTreatmentName(treatmentName != null ? treatmentName : "Treatment");
        c.setPatientDisplayLabel(treatmentName != null ? treatmentName : "Treatment");
        c.setPatientId(null);
        c.setPatientName("Awaiting Patient");
        c.setStartDate(LocalDate.now());
        c.setStatus("AWAITING_PATIENT");

        TreatmentCase saved = caseRepository.save(c);

        logTimelineEvent(caseId, "CONSULTATION", "Treatment Case Initialized", "Doctor " + finalDoctorName + " created new treatment case ID " + caseId, "CLINICAL");
        return saved;
    }

    @Transactional
    public TreatmentCase joinTreatmentCase(String caseId, Long patientId, String patientName) {
        String formattedCaseId = caseId.trim().toUpperCase();
        TreatmentCase c = caseRepository.findByCaseId(formattedCaseId)
                .orElseThrow(() -> new RuntimeException("Invalid or non-existent Treatment Case ID: " + formattedCaseId));

        if (c.getPatientId() != null) {
            if (c.getPatientId().equals(patientId)) {
                throw new RuntimeException("You are already connected to Treatment Case " + formattedCaseId + ".");
            } else {
                throw new RuntimeException("This Treatment Case ID (" + formattedCaseId + ") is already connected to another patient.");
            }
        }

        c.setPatientId(patientId);
        if (patientName != null && !patientName.isBlank()) {
            c.setPatientName(patientName);
        }
        c.setStatus("ACTIVE");
        c.setLastUpdate(LocalDateTime.now());

        TreatmentCase saved = caseRepository.save(c);

        logTimelineEvent(formattedCaseId, "CONSULTATION", "Patient Connected", "Patient " + (patientName != null ? patientName : "") + " successfully joined Treatment Case " + formattedCaseId, "CLINICAL");
        return saved;
    }

    @Transactional
    public TreatmentCase renameTreatmentCase(String caseId, String newLabel) {
        TreatmentCase c = caseRepository.findByCaseId(caseId.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        c.setPatientDisplayLabel(newLabel);
        return caseRepository.save(c);
    }

    @Transactional
    public Prescription uploadPrescription(String caseId, String title, MultipartFile file) {
        String formattedCaseId = caseId.toUpperCase();
        String fileUrl = storageService.storeFile(file);
        Prescription p = new Prescription(formattedCaseId, title != null ? title : file.getOriginalFilename(), fileUrl, file.getOriginalFilename());
        Prescription savedP = prescriptionRepository.save(p);

        logTimelineEvent(formattedCaseId, "PRESCRIPTION_UPLOADED", "Prescription Uploaded", "File " + file.getOriginalFilename() + " added to case", "CLINICAL");

        try {
            File targetFile = new File("." + fileUrl);
            OCRResult ocr = ocrService.processPrescriptionOCR(formattedCaseId, savedP.getId(), targetFile, file.getOriginalFilename());
            ocrResultRepository.save(ocr);

            logTimelineEvent(formattedCaseId, "OCR_COMPLETED", "OCR Processing Completed", "Extracted findings and medicines from prescription", "SYSTEM");
            logTimelineEvent(formattedCaseId, "DIGITAL_PRESCRIPTION_CREATED", "Digital Prescription Created", "Digital prescription generated from OCR results", "CLINICAL");

            List<Medicine> parsedMeds = ocrService.parseMedicinesFromText(formattedCaseId, savedP.getId(), ocr.getRawText());
            medicineRepository.saveAll(parsedMeds);
            logTimelineEvent(formattedCaseId, "MEDICINE_SCHEDULE_CREATED", "Medicine Schedule Created", parsedMeds.size() + " medications scheduled", "MEDICATION");

            for (Medicine m : parsedMeds) {
                Reminder r = new Reminder(formattedCaseId, m.getName(), m.getTimeOfDay().equals("Morning") ? "8:00 AM" : "9:00 PM", m.getFoodRelation(), "Daily", LocalDate.now(), LocalDate.now().plusDays(m.getDurationDays()));
                reminderRepository.save(r);
            }
            logTimelineEvent(formattedCaseId, "SMART_REMINDER_ACTIVATED", "Smart Reminders Activated", "Automatic smart reminders scheduled for active medications", "MEDICATION");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedP;
    }

    @Transactional
    public DocumentEntity uploadDocument(String caseId, String role, MultipartFile file) {
        String formattedCaseId = caseId.toUpperCase();
        String fileUrl = storageService.storeFile(file);
        DocumentEntity doc = new DocumentEntity(formattedCaseId, file.getOriginalFilename(), file.getContentType(), fileUrl, role);
        DocumentEntity saved = documentRepository.save(doc);

        logTimelineEvent(formattedCaseId, "DOCUMENT_UPLOADED", "Medical Document Added", file.getOriginalFilename() + " uploaded by " + role, "CLINICAL");
        return saved;
    }

    @Transactional
    public WeeklyFeedback saveFeedbackDraft(WeeklyFeedback feedback) {
        String formattedCaseId = feedback.getCaseId().toUpperCase();
        feedback.setCaseId(formattedCaseId);
        feedback.setStatus("DRAFT");
        feedback.setSubmissionDate(LocalDateTime.now());

        Optional<WeeklyFeedback> existingOpt = feedbackRepository.findByCaseIdAndWeekNumber(formattedCaseId, feedback.getWeekNumber());
        if (existingOpt.isPresent()) {
            WeeklyFeedback existing = existingOpt.get();
            existing.setQ1CurrentSymptoms(feedback.getQ1CurrentSymptoms());
            existing.setQ2SymptomsImproved(feedback.getQ2SymptomsImproved());
            existing.setQ3NewSymptoms(feedback.getQ3NewSymptoms());
            existing.setQ4SymptomsWorse(feedback.getQ4SymptomsWorse());
            existing.setQ5SideEffects(feedback.getQ5SideEffects());
            existing.setQ6FollowedSchedule(feedback.getQ6FollowedSchedule());
            existing.setQ7MissedMedicines(feedback.getQ7MissedMedicines());
            existing.setQ8ImprovementsNoticed(feedback.getQ8ImprovementsNoticed());
            existing.setQ9ChallengesFaced(feedback.getQ9ChallengesFaced());
            existing.setQ10DoctorNotes(feedback.getQ10DoctorNotes());
            existing.setStatus("DRAFT");
            return feedbackRepository.save(existing);
        }
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public AISummary submitWeeklyFeedbackAndSaveSummary(WeeklyFeedback feedback) {
        String formattedCaseId = feedback.getCaseId().toUpperCase();
        feedback.setCaseId(formattedCaseId);
        feedback.setStatus("SUBMITTED");
        feedback.setSubmissionDate(LocalDateTime.now());

        // Update existing feedback or save new
        WeeklyFeedback savedFeedback;
        Optional<WeeklyFeedback> existingOpt = feedbackRepository.findByCaseIdAndWeekNumber(formattedCaseId, feedback.getWeekNumber());
        if (existingOpt.isPresent()) {
            WeeklyFeedback existing = existingOpt.get();
            existing.setQ1CurrentSymptoms(feedback.getQ1CurrentSymptoms());
            existing.setQ2SymptomsImproved(feedback.getQ2SymptomsImproved());
            existing.setQ3NewSymptoms(feedback.getQ3NewSymptoms());
            existing.setQ4SymptomsWorse(feedback.getQ4SymptomsWorse());
            existing.setQ5SideEffects(feedback.getQ5SideEffects());
            existing.setQ6FollowedSchedule(feedback.getQ6FollowedSchedule());
            existing.setQ7MissedMedicines(feedback.getQ7MissedMedicines());
            existing.setQ8ImprovementsNoticed(feedback.getQ8ImprovementsNoticed());
            existing.setQ9ChallengesFaced(feedback.getQ9ChallengesFaced());
            existing.setQ10DoctorNotes(feedback.getQ10DoctorNotes());
            existing.setStatus("SUBMITTED");
            existing.setSubmissionDate(LocalDateTime.now());
            savedFeedback = feedbackRepository.save(existing);
        } else {
            savedFeedback = feedbackRepository.save(feedback);
        }

        logTimelineEvent(formattedCaseId, "WEEKLY_FEEDBACK_SUBMITTED", "Week " + feedback.getWeekNumber() + " Recovery Check-in Saved", "Patient submitted week " + feedback.getWeekNumber() + " recovery feedback", "RECOVERY");

        // Generate AI Summary with Gemini
        AISummary newSummary = geminiAIService.generateWeeklySummary(savedFeedback);
        
        // Update existing AISummary for that week if present, preserving chronological history
        Optional<AISummary> existingSummaryOpt = aiSummaryRepository.findByCaseIdAndWeekNumber(formattedCaseId, feedback.getWeekNumber());
        AISummary savedSummary;
        if (existingSummaryOpt.isPresent()) {
            AISummary existingSummary = existingSummaryOpt.get();
            existingSummary.setOverallStatus(newSummary.getOverallStatus());
            existingSummary.setImprovements(newSummary.getImprovements());
            existingSummary.setMedicationAdherence(newSummary.getMedicationAdherence());
            existingSummary.setNewObservations(newSummary.getNewObservations());
            existingSummary.setDoctorAttention(newSummary.getDoctorAttention());
            existingSummary.setFullRawSummary(newSummary.getFullRawSummary());
            existingSummary.setSummaryDate(LocalDateTime.now());
            savedSummary = aiSummaryRepository.save(existingSummary);
        } else {
            savedSummary = aiSummaryRepository.save(newSummary);
        }

        logTimelineEvent(formattedCaseId, "AI_SUMMARY_GENERATED", "Week " + feedback.getWeekNumber() + " AI Summary Saved", "Gemini recovery summary saved to treatment space", "RECOVERY");
        return savedSummary;
    }

    public String generateOverallSummary(String caseId) {
        String formattedCaseId = caseId.toUpperCase();
        List<AISummary> summaries = aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(formattedCaseId);
        return geminiAIService.generateOverallProgressSummary(summaries);
    }

    @Transactional
    public DoctorNote addDoctorNote(String caseId, Long doctorId, String doctorName, String noteText) {
        String formattedCaseId = caseId.toUpperCase();
        DoctorNote note = new DoctorNote(formattedCaseId, doctorId, doctorName, noteText);
        DoctorNote saved = noteRepository.save(note);

        logTimelineEvent(formattedCaseId, "DOCTOR_REVIEWED", "Doctor Clinical Review", "Dr. " + doctorName + " reviewed treatment space and added clinical notes", "CLINICAL");
        return saved;
    }

    public void logTimelineEvent(String caseId, String eventType, String title, String description, String category) {
        TimelineEvent event = new TimelineEvent(caseId, eventType, title, description, category);
        timelineRepository.save(event);
    }

    @Transactional
    public void deleteTreatmentCase(String caseId) {
        String formattedCaseId = caseId.trim().toUpperCase();
        TreatmentCase tc = caseRepository.findByCaseId(formattedCaseId)
                .orElseThrow(() -> new RuntimeException("Treatment case not found: " + formattedCaseId));

        // Clean up associated physical files if present
        List<Prescription> prescriptions = prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(formattedCaseId);
        for (Prescription p : prescriptions) {
            if (p.getFileUrl() != null) {
                try {
                    File f = new File("." + p.getFileUrl());
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
            }
        }
        List<DocumentEntity> docs = documentRepository.findByCaseIdOrderByUploadedAtDesc(formattedCaseId);
        for (DocumentEntity d : docs) {
            if (d.getFileUrl() != null) {
                try {
                    File f = new File("." + d.getFileUrl());
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
            }
        }

        // Delete all associated database records to avoid orphaned records
        ocrResultRepository.deleteByCaseId(formattedCaseId);
        medicineRepository.deleteByCaseId(formattedCaseId);
        reminderRepository.deleteByCaseId(formattedCaseId);
        documentRepository.deleteByCaseId(formattedCaseId);
        feedbackRepository.deleteByCaseId(formattedCaseId);
        aiSummaryRepository.deleteByCaseId(formattedCaseId);
        timelineRepository.deleteByCaseId(formattedCaseId);
        noteRepository.deleteByCaseId(formattedCaseId);
        prescriptionRepository.deleteByCaseId(formattedCaseId);

        // Delete the TreatmentCase itself
        caseRepository.delete(tc);
    }
}
