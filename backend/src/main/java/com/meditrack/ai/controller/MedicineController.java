package com.meditrack.ai.controller;

import com.meditrack.ai.entity.Medicine;
import com.meditrack.ai.repository.MedicineRepository;
import com.meditrack.ai.service.CaseAccessValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicines")
@CrossOrigin(origins = "*")
public class MedicineController {

    @Autowired private MedicineRepository medicineRepository;
    @Autowired private CaseAccessValidator accessValidator;
    @Autowired private com.meditrack.ai.repository.TreatmentCaseRepository caseRepository;

    @GetMapping("/my")
    public ResponseEntity<?> getMyMedicines() {
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
        List<Medicine> allMeds = caseIds.stream()
                .flatMap(cId -> medicineRepository.findByCaseId(cId).stream())
                .toList();
        return ResponseEntity.ok(allMeds);
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<?> getMedicines(@PathVariable String caseId) {
        if (!accessValidator.isUserAuthorizedForCase(caseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access to medicines for this case.");
        }
        return ResponseEntity.ok(medicineRepository.findByCaseId(caseId.toUpperCase()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam("status") String status) {
        Optional<Medicine> medOpt = medicineRepository.findById(id);
        if (medOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Medicine m = medOpt.get();
        if (!accessValidator.isUserAuthorizedForCase(m.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to update medicine status.");
        }
        m.setStatus(status.toUpperCase());
        medicineRepository.save(m);
        return ResponseEntity.ok(m);
    }

    @PostMapping
    public ResponseEntity<?> addMedicine(@RequestBody Medicine medicine) {
        if (medicine.getCaseId() == null || !accessValidator.isUserAuthorizedForCase(medicine.getCaseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to add medicine for this case.");
        }
        if (medicine.getPurpose() == null || medicine.getPurpose().isBlank()) {
            medicine.setPurpose("Purpose not specified in prescription.");
        }
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }
}

