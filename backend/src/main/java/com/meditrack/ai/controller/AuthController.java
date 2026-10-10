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
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body("Password must be at least 4 characters.");
        }
        if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            return ResponseEntity.badRequest().body("Email address is already registered.");
        }

        String role = (request.getRole() != null && request.getRole().equalsIgnoreCase("DOCTOR")) ? "DOCTOR" : "PATIENT";
        String avatar = "DOCTOR".equals(role)
                ? "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150"
                : "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150";

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getName() != null ? request.getName() : "User",
                role,
                request.getPhone() != null ? request.getPhone() : "+91 9000000000",
                avatar
        );
        User savedUser = userRepository.save(user);

        Patient patient = null;
        Doctor doctor = null;

        if ("PATIENT".equals(role)) {
            patient = new Patient(
                    savedUser.getId(),
                    request.getAge() != null ? request.getAge() : 25,
                    request.getGender() != null ? request.getGender() : "Not Specified",
                    request.getBloodGroup() != null ? request.getBloodGroup() : "O+",
                    request.getMedicalHistory() != null ? request.getMedicalHistory() : "None"
            );
            patient = patientRepository.save(patient);
        } else {
            doctor = new Doctor(
                    savedUser.getId(),
                    request.getSpecialty() != null ? request.getSpecialty() : "General Practice",
                    request.getHospital() != null ? request.getHospital() : "MediTrack Health Center",
                    request.getLicenseNumber() != null ? request.getLicenseNumber() : "MED-" + System.currentTimeMillis() % 100000,
                    request.getExperienceYears() != null ? request.getExperienceYears() : 5
            );
            doctor = doctorRepository.save(doctor);
        }

        String token = jwtTokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());
        return ResponseEntity.ok(new AuthResponse(token, savedUser, patient, doctor));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Email and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }

        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
        Doctor doctor = doctorRepository.findByUserId(user.getId()).orElse(null);

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
