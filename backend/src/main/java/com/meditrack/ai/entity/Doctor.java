package com.meditrack.ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String specialty;
    private String hospital;
    private String licenseNumber;
    private Integer experienceYears;

    public Doctor() {}

    public Doctor(Long userId, String specialty, String hospital, String licenseNumber, Integer experienceYears) {
        this.userId = userId;
        this.specialty = specialty;
        this.hospital = hospital;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
}
