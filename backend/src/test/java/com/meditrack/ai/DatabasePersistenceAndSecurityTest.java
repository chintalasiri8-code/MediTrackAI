package com.meditrack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.dto.AuthResponse;
import com.meditrack.ai.dto.CreateCaseRequest;
import com.meditrack.ai.dto.JoinCaseRequest;
import com.meditrack.ai.dto.LoginRequest;
import com.meditrack.ai.dto.RegisterRequest;
import com.meditrack.ai.entity.User;
import com.meditrack.ai.repository.DoctorRepository;
import com.meditrack.ai.repository.PatientRepository;
import com.meditrack.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DatabasePersistenceAndSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        userRepository.findByEmail("dr.test@meditrack.ai").ifPresent(user -> {
            doctorRepository.findByUserId(user.getId()).ifPresent(doctorRepository::delete);
            userRepository.delete(user);
        });
        userRepository.findByEmail("patient.test@meditrack.ai").ifPresent(user -> {
            patientRepository.findByUserId(user.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    public void testPersistenceSecurityAndAccessIsolation() throws Exception {
        // 1. Doctor Registration & Password Hashing check
        RegisterRequest doctorReq = new RegisterRequest();
        doctorReq.setEmail("dr.test@meditrack.ai");
        doctorReq.setPassword("docSecret123");
        doctorReq.setName("Dr. Test");
        doctorReq.setRole("DOCTOR");
        doctorReq.setSpecialty("Cardiology");

        MvcResult docRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doctorReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse docAuth = objectMapper.readValue(docRes.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(docAuth.getToken());

        // Verify password is encrypted in database with BCrypt
        Optional<User> docInDb = userRepository.findByEmail("dr.test@meditrack.ai");
        assertTrue(docInDb.isPresent());
        assertNotEquals("docSecret123", docInDb.get().getPassword());
        assertTrue(passwordEncoder.matches("docSecret123", docInDb.get().getPassword()));

        // Verify duplicate email is blocked
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doctorReq)))
                .andExpect(status().isBadRequest());

        // 2. Doctor creates a treatment case
        CreateCaseRequest caseReq = new CreateCaseRequest();
        caseReq.setTreatmentName("Heart Health Care");
        caseReq.setDoctorSpecialty("Cardiology");

        MvcResult caseRes = mockMvc.perform(post("/api/cases/create")
                .header("Authorization", "Bearer " + docAuth.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isOk())
                .andReturn();

        String caseId = objectMapper.readTree(caseRes.getResponse().getContentAsString()).get("caseId").asText();
        assertNotNull(caseId);

        // 3. Register Patient Test User
        RegisterRequest patientReq = new RegisterRequest();
        patientReq.setEmail("patient.test@meditrack.ai");
        patientReq.setPassword("patientPass789");
        patientReq.setName("Patient Test");
        patientReq.setRole("PATIENT");

        MvcResult patRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse patAuth = objectMapper.readValue(patRes.getResponse().getContentAsString(), AuthResponse.class);

        // 4. Patient joins case
        JoinCaseRequest joinReq = new JoinCaseRequest();
        joinReq.setCaseId(caseId);

        mockMvc.perform(post("/api/cases/join")
                .header("Authorization", "Bearer " + patAuth.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        // Verify patient can fetch treatment space data
        mockMvc.perform(get("/api/treatment-space/" + caseId)
                .header("Authorization", "Bearer " + patAuth.getToken()))
                .andExpect(status().isOk());

        // 5. Re-authentication & Persistence Check
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("patient.test@meditrack.ai");
        loginReq.setPassword("patientPass789");

        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse rePatAuth = objectMapper.readValue(loginRes.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(rePatAuth.getToken());
        assertEquals("patient.test@meditrack.ai", rePatAuth.getUser().getEmail());

        // Ensure patient retrieves previously joined case cleanly
        mockMvc.perform(get("/api/cases/patient/" + rePatAuth.getPatient().getId())
                .header("Authorization", "Bearer " + rePatAuth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].treatmentName").value("Heart Health Care"));
    }
}
