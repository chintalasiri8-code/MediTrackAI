package com.meditrack.ai.controller;

import com.meditrack.ai.entity.Reminder;
import com.meditrack.ai.repository.ReminderRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    @Autowired private ReminderRepository reminderRepository;
    @Autowired private CaseAccessValidator accessValidator;
    @Autowired private com.meditrack.ai.repository.TreatmentCaseRepository caseRepository;

    @GetMapping("/my")
    public ResponseEntity<?> getMyReminders() {
        com.meditrack.ai.config.UserPrincipal principal = accessValidator.getAuthenticatedPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthenticated");
        }
        List<String> caseIds;
        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            com.meditrack.ai.entity.Patient p = accessValidator.getAuthenticatedPatient();
            Long pId = (p != null) ? p.getId() : principal.getId();
            caseIds = caseRepository.findByPatientId(pId).stream().map(com.meditrack.ai.entity.TreatmentCase::getCaseId).toList();
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            com.meditrack.ai.entity.Doctor d = accessValidator.getAuthenticatedDoctor();
            Long dId = (d != null) ? d.getId() : principal.getId();
            caseIds = caseRepository.findByDoctorId(dId).stream().map(com.meditrack.ai.entity.TreatmentCase::getCaseId).toList();
        } else {
            caseIds = List.of();
        }
        List<Reminder> allReminders = caseIds.stream()
                .flatMap(cId -> reminderRepository.findByCaseId(cId).stream())
                .toList();
        return ResponseEntity.ok(allReminders);
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<?> getReminders(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to reminders for this case.");
        }
        return ResponseEntity.ok(reminderRepository.findByCaseId(caseId.toUpperCase()));
    }

    @PostMapping
    public ResponseEntity<?> createReminder(@RequestBody Reminder reminder) {
        if (reminder.getCaseId() == null || reminder.getCaseId().isBlank()) {
            com.meditrack.ai.config.UserPrincipal principal = accessValidator.getAuthenticatedPrincipal();
            if (principal != null) {
                if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
                    com.meditrack.ai.entity.Patient p = accessValidator.getAuthenticatedPatient();
                    Long pId = (p != null) ? p.getId() : principal.getId();
                    List<com.meditrack.ai.entity.TreatmentCase> pCases = caseRepository.findByPatientId(pId);
                    if (!pCases.isEmpty()) {
                        reminder.setCaseId(pCases.get(0).getCaseId());
                    }
                } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
                    com.meditrack.ai.entity.Doctor d = accessValidator.getAuthenticatedDoctor();
                    Long dId = (d != null) ? d.getId() : principal.getId();
                    List<com.meditrack.ai.entity.TreatmentCase> dCases = caseRepository.findByDoctorId(dId);
                    if (!dCases.isEmpty()) {
                        reminder.setCaseId(dCases.get(0).getCaseId());
                    }
                }
            }
        }
        if (reminder.getCaseId() == null || !accessValidator.isUserAuthorizedForCase(reminder.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to create reminder for this case.");
        }
        reminder.setCaseId(reminder.getCaseId().toUpperCase());
        return ResponseEntity.ok(reminderRepository.save(reminder));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateReminderStatus(@PathVariable Long id, @RequestParam("status") String status) {
        Optional<Reminder> reminderOpt = reminderRepository.findById(id);
        if (reminderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Reminder r = reminderOpt.get();
        if (!accessValidator.isUserAuthorizedForCase(r.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to update reminder.");
        }
        r.setStatus(status.toUpperCase());
        reminderRepository.save(r);
        return ResponseEntity.ok(r);
    }
}

