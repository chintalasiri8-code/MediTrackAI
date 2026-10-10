package com.meditrack.ai.controller;

import com.meditrack.ai.config.JwtTokenProvider;
import com.meditrack.ai.dto.AuthResponse;
import com.meditrack.ai.dto.LoginRequest;
import com.meditrack.ai.dto.RegisterRequest;
import com.meditrack.ai.entity.Doctor;
import com.meditrack.ai.entity.Patient;
import com.meditrack.ai.entity.User;
import com.meditrack.ai.repository.DoctorRepository;
import com.meditrack.ai.repository.PatientRepository;
import com.meditrack.ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
            }
            if (request.getPassword() == null || request.getPassword().length() < 4) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 4 characters."));
            }

            String cleanEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.findByEmail(cleanEmail).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email address is already registered."));
            }

            String role = (request.getRole() != null && request.getRole().equalsIgnoreCase("DOCTOR")) ? "DOCTOR" : "PATIENT";
            String avatar = "DOCTOR".equals(role)
                    ? "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150"
                    : "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150";

            String name = (request.getName() != null && !request.getName().isBlank()) ? request.getName().trim() : "User";
            String phone = (request.getPhone() != null && !request.getPhone().isBlank()) ? request.getPhone().trim() : "+91 9000000000";

            User user = new User(
                    cleanEmail,
                    passwordEncoder.encode(request.getPassword()),
                    name,
                    role,
                    phone,
                    avatar
            );
            User savedUser = userRepository.save(user);

            Patient patient = null;
            Doctor doctor = null;

            if ("PATIENT".equals(role)) {
                patient = new Patient(
                        savedUser.getId(),
                        (request.getAge() != null && request.getAge() > 0) ? request.getAge() : 25,
                        (request.getGender() != null && !request.getGender().isBlank()) ? request.getGender().trim() : "Not Specified",
                        (request.getBloodGroup() != null && !request.getBloodGroup().isBlank()) ? request.getBloodGroup().trim() : "O+",
                        (request.getMedicalHistory() != null && !request.getMedicalHistory().isBlank()) ? request.getMedicalHistory().trim() : "None"
                );
                patient = patientRepository.save(patient);
            } else {
                doctor = new Doctor(
                        savedUser.getId(),
                        (request.getSpecialty() != null && !request.getSpecialty().isBlank()) ? request.getSpecialty().trim() : "General Practice",
                        (request.getHospital() != null && !request.getHospital().isBlank()) ? request.getHospital().trim() : "MediTrack Health Center",
                        (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) ? request.getLicenseNumber().trim() : "MED-" + (System.currentTimeMillis() % 100000),
                        (request.getExperienceYears() != null && request.getExperienceYears() >= 0) ? request.getExperienceYears() : 5
                );
                doctor = doctorRepository.save(doctor);
            }

            String token = jwtTokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());
            return ResponseEntity.ok(new AuthResponse(token, savedUser, patient, doctor));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is already registered."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed: " + ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password."));
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password."));
        }

        Patient patient = null;
        Doctor doctor = null;
        if ("PATIENT".equals(user.getRole())) {
            patient = patientRepository.findByUserId(user.getId()).orElse(null);
        } else {
            doctor = doctorRepository.findByUserId(user.getId()).orElse(null);
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user, patient, doctor));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthenticated");
        }

        String email = auth.getName();
        Optional<User> optionalUser = userRepository.findByEmail(email.toLowerCase());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = optionalUser.get();
        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
        Doctor doctor = doctorRepository.findByUserId(user.getId()).orElse(null);

        return ResponseEntity.ok(new AuthResponse(null, user, patient, doctor));
    }
}
