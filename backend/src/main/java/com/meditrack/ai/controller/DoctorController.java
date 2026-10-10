package com.meditrack.ai.controller;

import com.meditrack.ai.dto.DoctorNoteRequest;
import com.meditrack.ai.entity.Doctor;
import com.meditrack.ai.entity.DoctorNote;
import com.meditrack.ai.entity.TreatmentCase;
import com.meditrack.ai.entity.User;
import com.meditrack.ai.repository.DoctorNoteRepository;
import com.meditrack.ai.repository.TreatmentCaseRepository;
import com.meditrack.ai.repository.UserRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import com.meditrack.ai.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private DoctorNoteRepository noteRepository;
    @Autowired private TreatmentService treatmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private CaseAccessValidator accessValidator;

    @GetMapping("/cases/{doctorId}")
    public ResponseEntity<?> getDoctorCases(@PathVariable Long doctorId) {
        Doctor authedDoctor = accessValidator.getAuthenticatedDoctor();
        if (authedDoctor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only doctors are authorized to access doctor cases.");
        }
        return ResponseEntity.ok(caseRepository.findByDoctorId(authedDoctor.getId()));
    }

    @PostMapping("/note")
    public ResponseEntity<?> addDoctorNote(@RequestBody DoctorNoteRequest req) {
        Doctor authedDoctor = accessValidator.getAuthenticatedDoctor();
        if (authedDoctor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only doctors can add doctor notes.");
        }
        if (!accessValidator.isUserAuthorizedForCase(req.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to add note to this case.");
        }

        String doctorName = "Dr. Anitha";
        Optional<User> uOpt = userRepository.findById(accessValidator.getAuthenticatedPrincipal().getId());
        if (uOpt.isPresent()) {
            doctorName = uOpt.get().getName();
        }

        DoctorNote note = treatmentService.addDoctorNote(
            req.getCaseId().toUpperCase(),
            authedDoctor.getId(),
            doctorName,
            req.getNoteText()
        );
        return ResponseEntity.ok(note);
    }

    @GetMapping("/notes/{caseId}")
    public ResponseEntity<?> getDoctorNotes(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to doctor notes.");
        }
        return ResponseEntity.ok(noteRepository.findByCaseIdOrderByCreatedDateDesc(caseId.toUpperCase()));
    }
}

