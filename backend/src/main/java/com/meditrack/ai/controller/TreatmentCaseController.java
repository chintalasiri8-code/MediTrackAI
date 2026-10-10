package com.meditrack.ai.controller;

import com.meditrack.ai.config.UserPrincipal;
import com.meditrack.ai.dto.CreateCaseRequest;
import com.meditrack.ai.dto.JoinCaseRequest;
import com.meditrack.ai.dto.RenameCaseRequest;
import com.meditrack.ai.entity.Doctor;
import com.meditrack.ai.entity.Patient;
import com.meditrack.ai.entity.TreatmentCase;
import com.meditrack.ai.entity.User;
import com.meditrack.ai.repository.DoctorRepository;
import com.meditrack.ai.repository.PatientRepository;
import com.meditrack.ai.repository.TreatmentCaseRepository;
import com.meditrack.ai.repository.UserRepository;
import com.meditrack.ai.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class TreatmentCaseController {

    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private TreatmentService treatmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private com.meditrack.ai.service.CaseAccessValidator accessValidator;

    @PostMapping("/create")
    public ResponseEntity<?> createCase(@RequestBody CreateCaseRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }

        if (!"DOCTOR".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only doctors are authorized to initialize a new Treatment Case.");
        }

        Doctor doctor = doctorRepository.findByUserId(principal.getId()).orElse(null);
        Long doctorId = (doctor != null) ? doctor.getId() : principal.getId();
        String doctorName = principal.getUsername();
        
        Optional<User> uOpt = userRepository.findById(principal.getId());
        if (uOpt.isPresent()) doctorName = uOpt.get().getName();

        String specialty = (req.getDoctorSpecialty() != null && !req.getDoctorSpecialty().isBlank())
                ? req.getDoctorSpecialty()
                : (doctor != null ? doctor.getSpecialty() : "Dermatology");

        String treatmentName = (req.getTreatmentName() != null && !req.getTreatmentName().isBlank())
                ? req.getTreatmentName()
                : "Treatment";

        TreatmentCase tc = treatmentService.createTreatmentCase(doctorId, doctorName, specialty, treatmentName);
        return ResponseEntity.ok(tc);
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinCase(@RequestBody JoinCaseRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }

        if (!"PATIENT".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only authenticated patients can join a Treatment Case.");
        }

        Patient patient = patientRepository.findByUserId(principal.getId()).orElse(null);
        Long patientId = (patient != null) ? patient.getId() : principal.getId();

        String patientName = "Patient";
        Optional<User> uOpt = userRepository.findById(principal.getId());
        if (uOpt.isPresent()) patientName = uOpt.get().getName();

        try {
            TreatmentCase tc = treatmentService.joinTreatmentCase(req.getCaseId(), patientId, patientName);
            return ResponseEntity.ok(tc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/rename")
    public ResponseEntity<?> renameCase(@RequestBody RenameCaseRequest req) {
        if (!accessValidator.isUserAuthorizedForCase(req.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to rename this treatment case.");
        }
        try {
            TreatmentCase tc = treatmentService.renameTreatmentCase(req.getCaseId(), req.getNewLabel());
            return ResponseEntity.ok(tc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientCases(@PathVariable Long patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }
        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            Patient p = patientRepository.findByUserId(principal.getId()).orElse(null);
            Long authedPatientId = (p != null) ? p.getId() : principal.getId();
            return ResponseEntity.ok(caseRepository.findByPatientId(authedPatientId));
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = doctorRepository.findByUserId(principal.getId()).orElse(null);
            Long authedDoctorId = (d != null) ? d.getId() : principal.getId();
            List<TreatmentCase> doctorPatientCases = caseRepository.findByDoctorId(authedDoctorId).stream()
                    .filter(c -> c.getPatientId() != null && c.getPatientId().equals(patientId))
                    .toList();
            return ResponseEntity.ok(doctorPatientCases);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorCases(@PathVariable Long doctorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }
        if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = doctorRepository.findByUserId(principal.getId()).orElse(null);
            Long authedDoctorId = (d != null) ? d.getId() : principal.getId();
            return ResponseEntity.ok(caseRepository.findByDoctorId(authedDoctorId));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied for doctor cases.");
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<?> getCaseDetails(@PathVariable String caseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }
        Optional<TreatmentCase> tcOpt = caseRepository.findByCaseId(caseId.toUpperCase());
        if (tcOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TreatmentCase tc = tcOpt.get();
        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            Patient p = patientRepository.findByUserId(principal.getId()).orElse(null);
            Long pId = (p != null) ? p.getId() : principal.getId();
            if (tc.getPatientId() != null && !tc.getPatientId().equals(pId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to this treatment case.");
            }
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = doctorRepository.findByUserId(principal.getId()).orElse(null);
            Long dId = (d != null) ? d.getId() : principal.getId();
            if (tc.getDoctorId() != null && !tc.getDoctorId().equals(dId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to this treatment case.");
            }
        }
        return ResponseEntity.ok(tc);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCases(@RequestParam("query") String query) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }
        List<TreatmentCase> results = caseRepository.findByPatientNameContainingIgnoreCaseOrTreatmentNameContainingIgnoreCaseOrCaseIdContainingIgnoreCase(query, query, query);
        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            Patient p = patientRepository.findByUserId(principal.getId()).orElse(null);
            Long pId = (p != null) ? p.getId() : principal.getId();
            results = results.stream().filter(c -> c.getPatientId() != null && c.getPatientId().equals(pId)).toList();
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = doctorRepository.findByUserId(principal.getId()).orElse(null);
            Long dId = (d != null) ? d.getId() : principal.getId();
            results = results.stream().filter(c -> c.getDoctorId() != null && c.getDoctorId().equals(dId)).toList();
        }
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/{caseId}")
    public ResponseEntity<?> deleteCase(@PathVariable String caseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }

        Optional<TreatmentCase> tcOpt = caseRepository.findByCaseId(caseId.trim().toUpperCase());
        if (tcOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Treatment case not found.");
        }

        TreatmentCase tc = tcOpt.get();

        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            Patient p = patientRepository.findByUserId(principal.getId()).orElse(null);
            Long pId = (p != null) ? p.getId() : principal.getId();
            if (tc.getPatientId() == null || !tc.getPatientId().equals(pId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: You can only delete treatment cases assigned to you.");
            }
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = doctorRepository.findByUserId(principal.getId()).orElse(null);
            Long dId = (d != null) ? d.getId() : principal.getId();
            if (tc.getDoctorId() == null || !tc.getDoctorId().equals(dId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: You can only delete treatment cases you manage.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized role.");
        }

        try {
            treatmentService.deleteTreatmentCase(tc.getCaseId());
            return ResponseEntity.ok("Treatment case " + tc.getCaseId() + " deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete treatment case: " + e.getMessage());
        }
    }
}
