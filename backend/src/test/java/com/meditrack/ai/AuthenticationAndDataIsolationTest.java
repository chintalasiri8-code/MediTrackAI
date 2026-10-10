package com.meditrack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.dto.AuthResponse;
import com.meditrack.ai.dto.JoinCaseRequest;
import com.meditrack.ai.dto.LoginRequest;
import com.meditrack.ai.dto.RegisterRequest;
import com.meditrack.ai.entity.User;
import com.meditrack.ai.repository.UserRepository;
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
public class AuthenticationAndDataIsolationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private com.meditrack.ai.repository.PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        userRepository.findByEmail("patienta@meditrack.ai").ifPresent(user -> {
            patientRepository.findByUserId(user.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(user);
        });
        userRepository.findByEmail("patientb@meditrack.ai").ifPresent(user -> {
            patientRepository.findByUserId(user.getId()).ifPresent(patientRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    public void testCompleteAuthenticationAndDataIsolation() throws Exception {
        // 1. Verify Registration for Patient A
        RegisterRequest patientAReq = new RegisterRequest();
        patientAReq.setEmail("patientA@meditrack.ai");
        patientAReq.setPassword("secretPass123");
        patientAReq.setName("Patient A");
        patientAReq.setRole("PATIENT");
        patientAReq.setAge(30);
        patientAReq.setBloodGroup("A+");

        MvcResult regAResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientAReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authA = objectMapper.readValue(regAResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(authA.getToken());
        assertEquals("patienta@meditrack.ai", authA.getUser().getEmail());

        // 2. Verify BCrypt Password Storage
        Optional<User> userAInDb = userRepository.findByEmail("patienta@meditrack.ai");
        assertTrue(userAInDb.isPresent());
        assertNotEquals("secretPass123", userAInDb.get().getPassword());
        assertTrue(passwordEncoder.matches("secretPass123", userAInDb.get().getPassword()));

        // 3. Verify Duplicate Email Prevention
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientAReq)))
                .andExpect(status().isBadRequest());

        // 4. Verify Registration for Patient B
        RegisterRequest patientBReq = new RegisterRequest();
        patientBReq.setEmail("patientB@meditrack.ai");
        patientBReq.setPassword("secretPass456");
        patientBReq.setName("Patient B");
        patientBReq.setRole("PATIENT");

        MvcResult regBResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientBReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authB = objectMapper.readValue(regBResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(authB.getToken());

        // 5. Verify Login for Doctor (demo doctor initialized by DataInitializer)
        LoginRequest docLogin = new LoginRequest();
        docLogin.setEmail("dr.anitha@meditrack.ai");
        docLogin.setPassword("password");

        MvcResult docLoginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse docAuth = objectMapper.readValue(docLoginRes.getResponse().getContentAsString(), AuthResponse.class);
        String docToken = docAuth.getToken();

        // Doctor creates a new treatment case for Patient B
        MvcResult caseRes = mockMvc.perform(post("/api/cases/create")
                .header("Authorization", "Bearer " + docToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"treatmentName\": \"Cardiology Review\", \"doctorSpecialty\": \"Cardiology\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String createdCaseJson = caseRes.getResponse().getContentAsString();
        String caseIdForB = objectMapper.readTree(createdCaseJson).get("caseId").asText();

        // 6. Patient B joins their case
        JoinCaseRequest joinReq = new JoinCaseRequest();
        joinReq.setCaseId(caseIdForB);

        mockMvc.perform(post("/api/cases/join")
                .header("Authorization", "Bearer " + authB.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        // 7. Data Isolation Check: Patient A CANNOT access Patient B's Case
        mockMvc.perform(get("/api/treatment-space/" + caseIdForB)
                .header("Authorization", "Bearer " + authA.getToken()))
                .andExpect(status().isForbidden());

        // 8. Data Isolation Check: Patient B CAN access Patient B's Case
        mockMvc.perform(get("/api/treatment-space/" + caseIdForB)
                .header("Authorization", "Bearer " + authB.getToken()))
                .andExpect(status().isOk());

        // 9. Re-login Verification: Patient B logs in again and retrieves data cleanly
        LoginRequest bLogin = new LoginRequest();
        bLogin.setEmail("patientB@meditrack.ai");
        bLogin.setPassword("secretPass456");

        MvcResult reLoginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse reAuthB = objectMapper.readValue(reLoginRes.getResponse().getContentAsString(), AuthResponse.class);
        mockMvc.perform(get("/api/cases/patient/" + reAuthB.getPatient().getId())
                .header("Authorization", "Bearer " + reAuthB.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseId").value(caseIdForB));
    }
}
