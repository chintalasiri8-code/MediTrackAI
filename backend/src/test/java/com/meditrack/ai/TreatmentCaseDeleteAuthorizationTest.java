package com.meditrack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.dto.AuthResponse;
import com.meditrack.ai.dto.CreateCaseRequest;
import com.meditrack.ai.dto.JoinCaseRequest;
import com.meditrack.ai.dto.RegisterRequest;
import com.meditrack.ai.entity.*;
import com.meditrack.ai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TreatmentCaseDeleteAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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

    private String doctorToken;
    private String patientToken;
    private String otherPatientToken;

    @BeforeEach
    public void setup() throws Exception {
        // Cleanup test users if present
        userRepository.findByEmail("doc.deltest@meditrack.ai").ifPresent(u -> {
            doctorRepository.findByUserId(u.getId()).ifPresent(doctorRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail("patient.deltest@meditrack.ai").ifPresent(u -> {
            patientRepository.findByUserId(u.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail("patient.other@meditrack.ai").ifPresent(u -> {
            patientRepository.findByUserId(u.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(u);
        });

        // 1. Register Doctor
        RegisterRequest docReq = new RegisterRequest();
        docReq.setEmail("doc.deltest@meditrack.ai");
        docReq.setPassword("docPass123");
        docReq.setName("Dr. Delete Test");
        docReq.setRole("DOCTOR");
        docReq.setSpecialty("Dermatology");
        MvcResult docRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isOk())
                .andReturn();
        doctorToken = objectMapper.readValue(docRes.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 2. Register Patient
        RegisterRequest patReq = new RegisterRequest();
        patReq.setEmail("patient.deltest@meditrack.ai");
        patReq.setPassword("patPass123");
        patReq.setName("Patient Delete Test");
        patReq.setRole("PATIENT");
        MvcResult patRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patReq)))
                .andExpect(status().isOk())
                .andReturn();
        patientToken = objectMapper.readValue(patRes.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 3. Register Other Patient (for authorization isolation testing)
        RegisterRequest otherPatReq = new RegisterRequest();
        otherPatReq.setEmail("patient.other@meditrack.ai");
        otherPatReq.setPassword("otherPass123");
        otherPatReq.setName("Other Patient");
        otherPatReq.setRole("PATIENT");
        MvcResult otherPatRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherPatReq)))
                .andExpect(status().isOk())
                .andReturn();
        otherPatientToken = objectMapper.readValue(otherPatRes.getResponse().getContentAsString(), AuthResponse.class).getToken();
    }

    @Test
    public void testCompleteDeleteOperationAndAuthorization() throws Exception {
        // 1. Doctor creates a treatment case
        CreateCaseRequest createReq = new CreateCaseRequest();
        createReq.setTreatmentName("Skin Rash Care");
        createReq.setDoctorSpecialty("Dermatology");
        MvcResult createRes = mockMvc.perform(post("/api/cases/create")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        String caseId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("caseId").asText();
        assertNotNull(caseId);

        // 2. Patient joins the case
        JoinCaseRequest joinReq = new JoinCaseRequest();
        joinReq.setCaseId(caseId);
        mockMvc.perform(post("/api/cases/join")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        // 3. Insert associated child records across all related tables
        Prescription prescription = prescriptionRepository.save(new Prescription(caseId, "Rash Rx", "/uploads/test.jpg", "test.jpg"));
        ocrResultRepository.save(new OCRResult(caseId, prescription.getId(), "Raw text OCR", "{}", "[]"));
        medicineRepository.save(new Medicine(caseId, prescription.getId(), "Hydrocortisone", "10 mg", "mg", "Morning", "After food", 7, "Anti-inflammatory", "PENDING"));
        reminderRepository.save(new Reminder(caseId, "Hydrocortisone", "08:00 AM", "After breakfast", "Daily", LocalDate.now(), LocalDate.now().plusDays(7)));
        documentRepository.save(new DocumentEntity(caseId, "LabReport.pdf", "application/pdf", "/uploads/lab.pdf", "PATIENT"));
        WeeklyFeedback feedback = new WeeklyFeedback();
        feedback.setCaseId(caseId);
        feedback.setWeekNumber(1);
        feedback.setQ1CurrentSymptoms("Mild rash");
        feedback.setStatus("SUBMITTED");
        feedbackRepository.save(feedback);
        AISummary summary = new AISummary();
        summary.setCaseId(caseId);
        summary.setWeekNumber(1);
        summary.setOverallStatus("IMPROVING");
        summary.setFullRawSummary("Full summary");
        aiSummaryRepository.save(summary);
        timelineRepository.save(new TimelineEvent(caseId, "TEST_EVENT", "Test Event", "Description", "CLINICAL"));
        noteRepository.save(new DoctorNote(caseId, 1L, "Dr. Delete Test", "Follow-up needed."));

        // Verify child records exist in MySQL database
        assertFalse(prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(caseId).isEmpty());
        assertFalse(medicineRepository.findByCaseId(caseId).isEmpty());
        assertFalse(reminderRepository.findByCaseId(caseId).isEmpty());
        assertFalse(documentRepository.findByCaseIdOrderByUploadedAtDesc(caseId).isEmpty());
        assertFalse(feedbackRepository.findByCaseIdOrderByWeekNumberAsc(caseId).isEmpty());
        assertFalse(aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(caseId).isEmpty());
        assertFalse(timelineRepository.findByCaseIdOrderByTimestampAsc(caseId).isEmpty());
        assertFalse(noteRepository.findByCaseIdOrderByCreatedDateDesc(caseId).isEmpty());

        // 4. Test: Unauthenticated deletion is blocked with 403 Access Denied
        mockMvc.perform(delete("/api/cases/" + caseId))
                .andExpect(status().isForbidden());

        // 5. Test: Unauthorized patient (other patient) deletion is blocked with 403
        mockMvc.perform(delete("/api/cases/" + caseId)
                .header("Authorization", "Bearer " + otherPatientToken))
                .andExpect(status().isForbidden());

        // Verify the case and all records still exist after unauthorized attempts
        assertTrue(caseRepository.findByCaseId(caseId).isPresent());
        assertFalse(medicineRepository.findByCaseId(caseId).isEmpty());

        // 6. Test: Non-existent case deletion returns 404
        mockMvc.perform(delete("/api/cases/CASE-NONEXIST")
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isNotFound());

        // 7. Test: Authorized Patient deletes the case
        mockMvc.perform(delete("/api/cases/" + caseId)
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        // 8. Verify the case itself is deleted from MySQL database
        assertTrue(caseRepository.findByCaseId(caseId).isEmpty());

        // 9. Verify ALL associated child records are completely removed with no orphaned records
        assertTrue(prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(caseId).isEmpty());
        assertTrue(ocrResultRepository.findFirstByCaseIdOrderByProcessedAtDesc(caseId).isEmpty());
        assertTrue(medicineRepository.findByCaseId(caseId).isEmpty());
        assertTrue(reminderRepository.findByCaseId(caseId).isEmpty());
        assertTrue(documentRepository.findByCaseIdOrderByUploadedAtDesc(caseId).isEmpty());
        assertTrue(feedbackRepository.findByCaseIdOrderByWeekNumberAsc(caseId).isEmpty());
        assertTrue(aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(caseId).isEmpty());
        assertTrue(timelineRepository.findByCaseIdOrderByTimestampAsc(caseId).isEmpty());
        assertTrue(noteRepository.findByCaseIdOrderByCreatedDateDesc(caseId).isEmpty());
    }

    @Test
    public void testDoctorCanDeleteCaseTheyManage() throws Exception {
        // Doctor creates a treatment case
        CreateCaseRequest createReq = new CreateCaseRequest();
        createReq.setTreatmentName("Acne Followup");
        createReq.setDoctorSpecialty("Dermatology");
        MvcResult createRes = mockMvc.perform(post("/api/cases/create")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        String caseId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("caseId").asText();
        assertNotNull(caseId);

        // Doctor deletes the case they created (even before patient joins)
        mockMvc.perform(delete("/api/cases/" + caseId)
                .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        assertTrue(caseRepository.findByCaseId(caseId).isEmpty());
    }
}
