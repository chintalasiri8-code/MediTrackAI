package com.meditrack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.dto.*;
import com.meditrack.ai.entity.*;
import com.meditrack.ai.repository.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullEndToEndMySQLPersistenceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private WeeklyFeedbackRepository feedbackRepository;
    @Autowired private AISummaryRepository aiSummaryRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String patientAToken;
    private static String patientBToken;
    private static String doctorToken;
    private static String doctorBToken;
    private static String createdCaseId;

    @Test
    @Order(1)
    public void test1_RegisterAndDuplicateEmailPrevention() throws Exception {
        // Cleanup test emails if exist
        userRepository.findByEmail("e2e.patienta@meditrack.ai").ifPresent(u -> {
            patientRepository.findByUserId(u.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail("e2e.patientb@meditrack.ai").ifPresent(u -> {
            patientRepository.findByUserId(u.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail("e2e.doctora@meditrack.ai").ifPresent(u -> {
            doctorRepository.findByUserId(u.getId()).ifPresent(doctorRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail("e2e.doctorb@meditrack.ai").ifPresent(u -> {
            doctorRepository.findByUserId(u.getId()).ifPresent(doctorRepository::delete);
            userRepository.delete(u);
        });

        // Register Patient A
        RegisterRequest pAReq = new RegisterRequest();
        pAReq.setEmail("e2e.patienta@meditrack.ai");
        pAReq.setPassword("PassPatientA123");
        pAReq.setName("E2E Patient A");
        pAReq.setRole("PATIENT");
        pAReq.setAge(28);
        pAReq.setBloodGroup("B+");

        MvcResult pARes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pAReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse pAAuth = objectMapper.readValue(pARes.getResponse().getContentAsString(), AuthResponse.class);
        patientAToken = pAAuth.getToken();
        assertNotNull(patientAToken);
        assertEquals("PATIENT", pAAuth.getUser().getRole());

        // Register Patient B
        RegisterRequest pBReq = new RegisterRequest();
        pBReq.setEmail("e2e.patientb@meditrack.ai");
        pBReq.setPassword("PassPatientB456");
        pBReq.setName("E2E Patient B");
        pBReq.setRole("PATIENT");

        MvcResult pBRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pBReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse pBAuth = objectMapper.readValue(pBRes.getResponse().getContentAsString(), AuthResponse.class);
        patientBToken = pBAuth.getToken();
        assertNotNull(patientBToken);

        // Register Doctor A
        RegisterRequest docReq = new RegisterRequest();
        docReq.setEmail("e2e.doctora@meditrack.ai");
        docReq.setPassword("PassDoctorA123");
        docReq.setName("E2E Dr. Alpha");
        docReq.setRole("DOCTOR");
        docReq.setSpecialty("Dermatology");

        MvcResult docRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse docAuth = objectMapper.readValue(docRes.getResponse().getContentAsString(), AuthResponse.class);
        doctorToken = docAuth.getToken();
        assertNotNull(doctorToken);
        assertEquals("DOCTOR", docAuth.getUser().getRole());

        // Register Doctor B (unauthorized doctor for isolation test)
        RegisterRequest docBReq = new RegisterRequest();
        docBReq.setEmail("e2e.doctorb@meditrack.ai");
        docBReq.setPassword("PassDoctorB123");
        docBReq.setName("E2E Dr. Beta");
        docBReq.setRole("DOCTOR");
        docBReq.setSpecialty("Neurology");

        MvcResult docBRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docBReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse docBAuth = objectMapper.readValue(docBRes.getResponse().getContentAsString(), AuthResponse.class);
        doctorBToken = docBAuth.getToken();
        assertNotNull(doctorBToken);

        // Verify duplicate email is strictly rejected with 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pAReq)))
                .andExpect(status().isBadRequest());

        // Verify password hashes are BCrypt in MySQL
        Optional<User> uA = userRepository.findByEmail("e2e.patienta@meditrack.ai");
        assertTrue(uA.isPresent());
        assertTrue(uA.get().getPassword().startsWith("$2a$") || uA.get().getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("PassPatientA123", uA.get().getPassword()));
    }

    @Test
    @Order(2)
    public void test2_LoginValidationAgainstDatabase() throws Exception {
        LoginRequest validLogin = new LoginRequest();
        validLogin.setEmail("e2e.patienta@meditrack.ai");
        validLogin.setPassword("PassPatientA123");

        MvcResult res = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(auth.getToken());
        assertEquals("e2e.patienta@meditrack.ai", auth.getUser().getEmail());

        // Invalid password must fail with 401
        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setEmail("e2e.patienta@meditrack.ai");
        invalidLogin.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    public void test3_CreateTreatmentCaseAndSaveRecords() throws Exception {
        // Doctor creates case
        CreateCaseRequest caseReq = new CreateCaseRequest();
        caseReq.setTreatmentName("E2E Skin Recovery");
        caseReq.setDoctorSpecialty("Dermatology");

        MvcResult caseRes = mockMvc.perform(post("/api/cases/create")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isOk())
                .andReturn();

        createdCaseId = objectMapper.readTree(caseRes.getResponse().getContentAsString()).get("caseId").asText();
        assertNotNull(createdCaseId);

        // Patient A connects to case
        JoinCaseRequest joinReq = new JoinCaseRequest();
        joinReq.setCaseId(createdCaseId);

        mockMvc.perform(post("/api/cases/join")
                .header("Authorization", "Bearer " + patientAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        // Upload prescription file
        MockMultipartFile file = new MockMultipartFile("file", "test_rx.pdf", "application/pdf", "Dummy Rx Content".getBytes());
        mockMvc.perform(multipart("/api/prescriptions/upload")
                .file(file)
                .param("caseId", createdCaseId)
                .param("title", "Initial Skin Rx")
                .header("Authorization", "Bearer " + patientAToken))
                .andExpect(status().isOk());

        // Save Weekly Feedback & AI Summary
        WeeklyFeedbackRequest fbReq = new WeeklyFeedbackRequest();
        fbReq.setCaseId(createdCaseId);
        fbReq.setWeekNumber(1);
        fbReq.setQ1CurrentSymptoms("Mild redness");
        fbReq.setQ2SymptomsImproved("Yes");
        fbReq.setQ6FollowedSchedule("Yes");
        fbReq.setQ7MissedMedicines("None");
        fbReq.setQ8ImprovementsNoticed("Redness fading");

        mockMvc.perform(post("/api/recovery/feedback/save")
                .header("Authorization", "Bearer " + patientAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fbReq)))
                .andExpect(status().isOk());

        // Save a Medicine and a Reminder
        Medicine med = new Medicine(createdCaseId, null, "Amoxicillin 500mg", "500 mg", "mg", "Morning", "After food", 7, "Antibiotic", "PENDING");
        mockMvc.perform(post("/api/medicines")
                .header("Authorization", "Bearer " + patientAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(med)))
                .andExpect(status().isOk());

        Reminder rem = new Reminder(createdCaseId, "Amoxicillin 500mg", "09:00 AM", "After breakfast", "Daily", java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(7));
        mockMvc.perform(post("/api/reminders")
                .header("Authorization", "Bearer " + patientAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rem)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    public void test4_DataIsolationBetweenPatientAAndPatientB() throws Exception {
        // Patient A CAN access their own case
        mockMvc.perform(get("/api/treatment-space/" + createdCaseId)
                .header("Authorization", "Bearer " + patientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatmentCase.caseId").value(createdCaseId))
                .andExpect(jsonPath("$.medicines").isArray());

        // Patient B CANNOT access Patient A's case (403 Forbidden)
        mockMvc.perform(get("/api/treatment-space/" + createdCaseId)
                .header("Authorization", "Bearer " + patientBToken))
                .andExpect(status().isForbidden());

        // Patient B CANNOT access Patient A's prescriptions
        mockMvc.perform(get("/api/prescriptions/case/" + createdCaseId)
                .header("Authorization", "Bearer " + patientBToken))
                .andExpect(status().isForbidden());

        // Patient B CANNOT access Patient A's recovery history
        mockMvc.perform(get("/api/recovery/history/" + createdCaseId)
                .header("Authorization", "Bearer " + patientBToken))
                .andExpect(status().isForbidden());

        // Patient B CANNOT access Patient A's medicines
        mockMvc.perform(get("/api/medicines/case/" + createdCaseId)
                .header("Authorization", "Bearer " + patientBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    public void test5_DoctorAccessControlAndUnauthorizedDoctorBlocked() throws Exception {
        // Authorized Doctor (Doctor A) CAN access the case
        mockMvc.perform(get("/api/treatment-space/" + createdCaseId)
                .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        // Unauthorized Doctor (Doctor B) CANNOT access the case (403 Forbidden)
        mockMvc.perform(get("/api/treatment-space/" + createdCaseId)
                .header("Authorization", "Bearer " + doctorBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    public void test6_ReLoginAndPersistenceVerification() throws Exception {
        // Re-login Patient A
        LoginRequest reLogin = new LoginRequest();
        reLogin.setEmail("e2e.patienta@meditrack.ai");
        reLogin.setPassword("PassPatientA123");

        MvcResult reLoginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse reAuth = objectMapper.readValue(reLoginRes.getResponse().getContentAsString(), AuthResponse.class);
        String freshToken = reAuth.getToken();
        assertNotNull(freshToken);

        // Fetch user data via fresh token
        mockMvc.perform(get("/api/cases/patient/" + reAuth.getPatient().getId())
                .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseId").value(createdCaseId));

        // Confirm database records exist directly in repositories connected to MySQL
        Optional<TreatmentCase> tcInDb = caseRepository.findByCaseId(createdCaseId);
        assertTrue(tcInDb.isPresent());
        assertEquals("E2E Skin Recovery", tcInDb.get().getTreatmentName());

        assertFalse(prescriptionRepository.findByCaseIdOrderByUploadedAtDesc(createdCaseId).isEmpty());
        assertFalse(feedbackRepository.findByCaseIdOrderByWeekNumberAsc(createdCaseId).isEmpty());
        assertFalse(aiSummaryRepository.findByCaseIdOrderByWeekNumberAsc(createdCaseId).isEmpty());
        assertFalse(medicineRepository.findByCaseId(createdCaseId).isEmpty());
        assertFalse(reminderRepository.findByCaseId(createdCaseId).isEmpty());
    }
}
