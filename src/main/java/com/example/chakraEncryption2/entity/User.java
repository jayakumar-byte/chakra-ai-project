package com.example.chakraEncryption2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String role; // ROLE_USER or ROLE_ADMIN

    @Column(nullable = false)
    private boolean isBlocked = false; // Admin-led hard block [cite: 2026-01-04]


    // 🕒 Security & Lockout Fields
    private int failedAttempts = 0; // Tracks consecutive wrong keys [cite: 2026-01-04]
    private LocalDateTime decryptDisabledUntil; // Decryption lock timer [cite: 2026-01-04]

    private boolean enabled = true; // By default user active-aa irupaanga

    // Indha Setter dhaan ippo missing:
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    // New Getters/Setters for Lockout Logic [cite: 2026-01-04]
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getDecryptDisabledUntil() { return decryptDisabledUntil; }
    public void setDecryptDisabledUntil(LocalDateTime decryptDisabledUntil) {
        this.decryptDisabledUntil = decryptDisabledUntil;
    }
}