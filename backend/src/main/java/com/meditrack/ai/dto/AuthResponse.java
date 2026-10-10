package com.meditrack.ai.dto;

import com.meditrack.ai.entity.Doctor;
import com.meditrack.ai.entity.Patient;
import com.meditrack.ai.entity.User;

public class AuthResponse {
    private String token;
    private User user;
    private Patient patient;
    private Doctor doctor;

    public AuthResponse(String token, User user, Patient patient, Doctor doctor) {
        this.token = token;
        this.user = user;
        this.patient = patient;
        this.doctor = doctor;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}
