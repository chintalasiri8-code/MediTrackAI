package com.meditrack.ai.config;

import com.meditrack.ai.entity.*;
import com.meditrack.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
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
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Already initialized
        }

        // 1. Create Demo Users with BCrypt Hashed Passwords
        String encodedPassword = passwordEncoder.encode("password");
        User siriUser = new User("siri@meditrack.ai", encodedPassword, "Siri", "PATIENT", "+91 9123456789", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");
        userRepository.save(siriUser);
        Patient siriPatient = new Patient(siriUser.getId(), 24, "Female", "O+", "No prior chronic drug allergies. Occasional facial acne breakouts.");
        patientRepository.save(siriPatient);

        User doctorUser = new User("dr.anitha@meditrack.ai", encodedPassword, "Dr. Anitha", "DOCTOR", "+91 9876543210", "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150");
        userRepository.save(doctorUser);
        Doctor anithaDoctor = new Doctor(doctorUser.getId(), "Dermatology & Skin Care", "Apollo Skin & Wellness Center", "MED-884920", 12);
        doctorRepository.save(anithaDoctor);

        // 2. Create Primary Demo Case CASE-7F92K
        String caseId = "CASE-7F92K";
        TreatmentCase demoCase = new TreatmentCase(
            caseId,
            siriPatient.getId(),
            "Siri",
            anithaDoctor.getId(),
            "Dr. Anitha",
            "Dermatologist",
            "Acne Skin Treatment",
            LocalDate.of(2026, 10, 4)
        );
        demoCase.setPatientDisplayLabel("Skin Treatment");
        caseRepository.save(demoCase);

        // Additional Case 2 for Siri to demonstrate multiple cases
        String caseId2 = "CASE-9X41B";
        TreatmentCase kneeCase = new TreatmentCase(
            caseId2,
            siriPatient.getId(),
            "Siri",
            anithaDoctor.getId(),
            "Dr. Rajesh Kumar",
            "Orthopedic Specialist",
            "Knee Strain Recovery",
            LocalDate.of(2026, 9, 15)
        );
        kneeCase.setPatientDisplayLabel("Orthopedic Treatment");
        caseRepository.save(kneeCase);

        // 3. Create Prescription for CASE-7F92K
        Prescription rx = new Prescription(caseId, "Dermatology Prescription - Dr. Anitha", "/uploads/prescription_siri_skin.pdf", "Prescription_Skin_Oct2026.pdf");
        prescriptionRepository.save(rx);

        // 4. Create OCR Result for CASE-7F92K
        String rawText = "APOLLO SKIN & WELLNESS CLINIC\n" +
                         "Dr. Anitha, M.D. Dermatology | Reg: MED-884920\n" +
                         "Patient: Siri | Date: 04 Oct 2026 | Case ID: CASE-7F92K\n\n" +
                         "Clinical Findings:\n" +
                         "• Facial Acne Vulgaris (Grade II) with inflammatory erythema\n" +
                         "• Localized cheek papules and comedones\n\n" +
                         "Rx Medications:\n" +
                         "1. Doxycycline Hyclate 100 mg - 1 tab Morning - 14 Days (After food)\n" +
                         "2. Clindamycin Gel 1% - Apply thin layer Night - 21 Days\n" +
                         "3. Gentle Hydrating Cleanser 100 ml - Wash face Morning - 30 Days\n" +
                         "4. Nicotinamide & Zinc 500 mg - 1 tab Afternoon - 14 Days";

        String findingsJson = "[\"Facial Acne Vulgaris (Grade II) with inflammatory erythema\",\"Localized cheek papules and comedones noted during dermatological consultation\"]";
        String medicinesJson = "[" +
                               "{\"name\":\"Doxycycline Hyclate\",\"dosage\":\"100 mg\",\"timeOfDay\":\"Morning\",\"foodRelation\":\"After food\",\"durationDays\":14,\"purpose\":\"Purpose not specified in prescription.\"}," +
                               "{\"name\":\"Clindamycin Gel 1%\",\"dosage\":\"15 g\",\"timeOfDay\":\"Night\",\"foodRelation\":\"After food\",\"durationDays\":21,\"purpose\":\"Purpose not specified in prescription.\"}," +
                               "{\"name\":\"Gentle Hydrating Cleanser\",\"dosage\":\"100 ml\",\"timeOfDay\":\"Morning\",\"foodRelation\":\"Before food\",\"durationDays\":30,\"purpose\":\"Purpose not specified in prescription.\"}," +
                               "{\"name\":\"Nicotinamide & Zinc Tablet\",\"dosage\":\"500 mg\",\"timeOfDay\":\"Afternoon\",\"foodRelation\":\"After food\",\"durationDays\":14,\"purpose\":\"Purpose not specified in prescription.\"}" +
                               "]";

        OCRResult ocr = new OCRResult(caseId, rx.getId(), rawText, findingsJson, medicinesJson);
        ocrResultRepository.save(ocr);

        // 5. Create Medicines
        Medicine m1 = new Medicine(caseId, rx.getId(), "Doxycycline Hyclate", "100 mg", "mg", "Morning", "After food", 14, "Purpose not specified in prescription.", "TAKEN");
        Medicine m2 = new Medicine(caseId, rx.getId(), "Clindamycin Gel 1%", "15 g", "g", "Night", "After food", 21, "Purpose not specified in prescription.", "PENDING");
        Medicine m3 = new Medicine(caseId, rx.getId(), "Gentle Hydrating Cleanser", "100 ml", "ml", "Morning", "Before food", 30, "Purpose not specified in prescription.", "TAKEN");
        Medicine m4 = new Medicine(caseId, rx.getId(), "Nicotinamide & Zinc Tablet", "500 mg", "mg", "Afternoon", "After food", 14, "Purpose not specified in prescription.", "PENDING");
        medicineRepository.save(m1);
        medicineRepository.save(m2);
        medicineRepository.save(m3);
        medicineRepository.save(m4);

        // 6. Create Smart Reminders
        Reminder r1 = new Reminder(caseId, "Doxycycline Hyclate", "08:00 AM", "After breakfast", "Daily", LocalDate.now(), LocalDate.now().plusDays(14));
        r1.setStatus("TAKEN");
        Reminder r2 = new Reminder(caseId, "Nicotinamide & Zinc Tablet", "01:30 PM", "After lunch", "Daily", LocalDate.now(), LocalDate.now().plusDays(14));
        r2.setStatus("PENDING");
        Reminder r3 = new Reminder(caseId, "Clindamycin Gel 1%", "09:00 PM", "Before sleep", "Daily", LocalDate.now(), LocalDate.now().plusDays(21));
        r3.setStatus("PENDING");
        reminderRepository.save(r1);
        reminderRepository.save(r2);
        reminderRepository.save(r3);

        // 7. Create Documents
        documentRepository.save(new DocumentEntity(caseId, "Prescription_Skin_Oct2026.pdf", "application/pdf", "/uploads/prescription_siri_skin.pdf", "DOCTOR"));
        documentRepository.save(new DocumentEntity(caseId, "Blood_Report.pdf", "application/pdf", "/uploads/blood_report_siri.pdf", "PATIENT"));
        documentRepository.save(new DocumentEntity(caseId, "Scan_Report.pdf", "application/pdf", "/uploads/scan_report_siri.pdf", "PATIENT"));
        documentRepository.save(new DocumentEntity(caseId, "Followup_Report.pdf", "application/pdf", "/uploads/followup_report_siri.pdf", "PATIENT"));

        // 8. Create Historical Weekly Feedback & Saved AI Summaries (Week 1, Week 2, Week 3)
        // Week 1
        WeeklyFeedback wf1 = new WeeklyFeedback();
        wf1.setCaseId(caseId);
        wf1.setWeekNumber(1);
        wf1.setQ1CurrentSymptoms("Facial redness and slight swelling around cheek region.");
        wf1.setQ2SymptomsImproved("Yes, redness has slightly decreased.");
        wf1.setQ3NewSymptoms("None");
        wf1.setQ4SymptomsWorse("No");
        wf1.setQ5SideEffects("Mild stomach discomfort after morning dose.");
        wf1.setQ6FollowedSchedule("Yes, followed daily.");
        wf1.setQ7MissedMedicines("No missed medicines.");
        wf1.setQ8ImprovementsNoticed("Active acne pimples are flattening.");
        wf1.setQ9ChallengesFaced("Remembering afternoon pill while at office.");
        wf1.setQ10DoctorNotes("Cleanser feels soothing.");
        wf1.setSubmissionDate(LocalDateTime.now().minusWeeks(3));
        feedbackRepository.save(wf1);

        AISummary ai1 = new AISummary();
        ai1.setCaseId(caseId);
        ai1.setWeeklyFeedbackId(wf1.getId());
        ai1.setWeekNumber(1);
        ai1.setOverallStatus("Improving");
        ai1.setImprovements("• Facial redness reduced\n• Active papules flattening nicely");
        ai1.setMedicationAdherence("• 100% adherence. Zero missed doses.");
        ai1.setNewObservations("• Mild stomach discomfort reported post-morning tablet");
        ai1.setDoctorAttention("• Monitor stomach tolerance for Doxycycline; ensure taken with full glass of water");
        ai1.setSummaryDate(LocalDateTime.now().minusWeeks(3));
        ai1.setFullRawSummary("WEEKLY RECOVERY SUMMARY - WEEK 1\n\nOverall: Improving\n\nImprovements:\n• Facial redness reduced\n• Active papules flattening nicely\n\nMedication Adherence:\n• 100% adherence. Zero missed doses.\n\nNew Observations:\n• Mild stomach discomfort reported post-morning tablet\n\nDoctor Attention:\n• Monitor stomach tolerance for Doxycycline");
        aiSummaryRepository.save(ai1);

        // Week 2
        WeeklyFeedback wf2 = new WeeklyFeedback();
        wf2.setCaseId(caseId);
        wf2.setWeekNumber(2);
        wf2.setQ1CurrentSymptoms("Very mild redness remaining. Skin texture improving.");
        wf2.setQ2SymptomsImproved("Yes, significant reduction in acne spots.");
        wf2.setQ3NewSymptoms("None");
        wf2.setQ4SymptomsWorse("No");
        wf2.setQ5SideEffects("None");
        wf2.setQ6FollowedSchedule("Yes");
        wf2.setQ7MissedMedicines("Missed 1 afternoon dose on Thursday.");
        wf2.setQ8ImprovementsNoticed("No new breakouts in the past 7 days.");
        wf2.setQ9ChallengesFaced("None");
        wf2.setQ10DoctorNotes("Overall feeling very confident.");
        wf2.setSubmissionDate(LocalDateTime.now().minusWeeks(2));
        feedbackRepository.save(wf2);

        AISummary ai2 = new AISummary();
        ai2.setCaseId(caseId);
        ai2.setWeeklyFeedbackId(wf2.getId());
        ai2.setWeekNumber(2);
        ai2.setOverallStatus("Improving");
        ai2.setImprovements("• Significant reduction in acne spots\n• No new breakouts in past 7 days");
        ai2.setMedicationAdherence("• One afternoon dose missed on Thursday.");
        ai2.setNewObservations("• Stomach discomfort resolved completely.");
        ai2.setDoctorAttention("• Continue topical gel application as scheduled.");
        ai2.setSummaryDate(LocalDateTime.now().minusWeeks(2));
        ai2.setFullRawSummary("WEEKLY RECOVERY SUMMARY - WEEK 2\n\nOverall: Improving\n\nImprovements:\n• Significant reduction in acne spots\n• No new breakouts in past 7 days\n\nMedication Adherence:\n• One afternoon dose missed on Thursday.\n\nNew Observations:\n• Stomach discomfort resolved completely.\n\nDoctor Attention:\n• Continue topical gel application as scheduled.");
        aiSummaryRepository.save(ai2);

        // Week 3
        WeeklyFeedback wf3 = new WeeklyFeedback();
        wf3.setCaseId(caseId);
        wf3.setWeekNumber(3);
        wf3.setQ1CurrentSymptoms("Skin clearer, but mild localized itching on jawline area.");
        wf3.setQ2SymptomsImproved("Yes, acne 80% cleared.");
        wf3.setQ3NewSymptoms("Mild itching reported on jawline after gel application.");
        wf3.setQ4SymptomsWorse("No");
        wf3.setQ5SideEffects("Mild localized dryness/itching.");
        wf3.setQ6FollowedSchedule("Yes");
        wf3.setQ7MissedMedicines("Zero missed doses.");
        wf3.setQ8ImprovementsNoticed("Skin tone evened out considerably.");
        wf3.setQ9ChallengesFaced("Dryness during cold evening air.");
        wf3.setQ10DoctorNotes("Please suggest if moisturizer frequency should be increased.");
        wf3.setSubmissionDate(LocalDateTime.now().minusWeeks(1));
        feedbackRepository.save(wf3);

        AISummary ai3 = new AISummary();
        ai3.setCaseId(caseId);
        ai3.setWeeklyFeedbackId(wf3.getId());
        ai3.setWeekNumber(3);
        ai3.setOverallStatus("Attention Needed");
        ai3.setImprovements("• Skin 80% cleared of active lesions\n• Tone evened out significantly");
        ai3.setMedicationAdherence("• 100% adherence logged.");
        ai3.setNewObservations("• Mild localized itching & dryness reported on jawline.");
        ai3.setDoctorAttention("• Doctor should review jawline dryness during next follow-up and adjust moisturizer recommendation.");
        ai3.setSummaryDate(LocalDateTime.now().minusWeeks(1));
        ai3.setFullRawSummary("WEEKLY RECOVERY SUMMARY - WEEK 3\n\nOverall: Attention Needed\n\nImprovements:\n• Skin 80% cleared of active lesions\n• Tone evened out significantly\n\nMedication Adherence:\n• 100% adherence logged.\n\nNew Observations:\n• Mild localized itching & dryness reported on jawline.\n\nDoctor Attention:\n• Doctor should review jawline dryness during next follow-up.");
        aiSummaryRepository.save(ai3);

        // 9. Timeline Events
        timelineRepository.save(new TimelineEvent(caseId, "CONSULTATION", "Initial Consultation Completed", "Patient Siri met Dr. Anitha for skin evaluation.", "CLINICAL"));
        timelineRepository.save(new TimelineEvent(caseId, "PRESCRIPTION_UPLOADED", "Prescription Uploaded", "Prescription_Skin_Oct2026.pdf added to treatment space.", "CLINICAL"));
        timelineRepository.save(new TimelineEvent(caseId, "OCR_COMPLETED", "OCR Processing Completed", "Extracted text, findings and 4 prescribed medications.", "SYSTEM"));
        timelineRepository.save(new TimelineEvent(caseId, "DIGITAL_PRESCRIPTION_CREATED", "Digital Prescription Created", "Findings & structured dosage details generated.", "CLINICAL"));
        timelineRepository.save(new TimelineEvent(caseId, "MEDICINE_SCHEDULE_CREATED", "Medicine Schedule Created", "Scheduled Morning, Afternoon, and Night routines.", "MEDICATION"));
        timelineRepository.save(new TimelineEvent(caseId, "SMART_REMINDER_ACTIVATED", "Smart Reminders Activated", "3 daily smart reminder alerts configured.", "MEDICATION"));
        timelineRepository.save(new TimelineEvent(caseId, "WEEKLY_FEEDBACK_SUBMITTED", "Week 1 Check-in Submitted", "Patient logged week 1 recovery feedback.", "RECOVERY"));
        timelineRepository.save(new TimelineEvent(caseId, "AI_SUMMARY_GENERATED", "Week 1 AI Summary Generated", "Gemini structured Week 1 recovery progress.", "RECOVERY"));
        timelineRepository.save(new TimelineEvent(caseId, "WEEKLY_FEEDBACK_SUBMITTED", "Week 2 Check-in Submitted", "Patient logged week 2 recovery feedback.", "RECOVERY"));
        timelineRepository.save(new TimelineEvent(caseId, "AI_SUMMARY_GENERATED", "Week 2 AI Summary Generated", "Gemini structured Week 2 recovery progress.", "RECOVERY"));
        timelineRepository.save(new TimelineEvent(caseId, "WEEKLY_FEEDBACK_SUBMITTED", "Week 3 Check-in Submitted", "Patient logged week 3 recovery feedback.", "RECOVERY"));
        timelineRepository.save(new TimelineEvent(caseId, "AI_SUMMARY_GENERATED", "Week 3 AI Summary Generated", "Gemini flagged jawline dryness for doctor review.", "RECOVERY"));

        // 10. Doctor Notes
        noteRepository.save(new DoctorNote(caseId, anithaDoctor.getId(), "Dr. Anitha", "Patient is responding very well to Doxycycline and Clindamycin gel. For Week 3 jawline dryness, recommend applying non-comedogenic hydrating moisturizer 15 mins after gel application. Schedule follow-up consultation next week."));
        timelineRepository.save(new TimelineEvent(caseId, "DOCTOR_REVIEWED", "Doctor Clinical Review", "Dr. Anitha reviewed treatment journey and added guidance note.", "CLINICAL"));
    }
}
