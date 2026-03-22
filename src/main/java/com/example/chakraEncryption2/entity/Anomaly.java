package com.example.chakraEncryption2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomalies")
public class Anomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐ IDHU THAAN MISSING: Database columns-ah Java fields-kooda map pannudhu
    @Column(name = "attempted_by")
    private String attemptedBy; // [cite: 2026-01-16]

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "is_reported")
    private boolean reported;

    private String filename;
    // Anomaly.java-la
    private boolean redAlert;    // true-na Unauthorized Person
    private boolean yellowAlert; // true-na Wrong Key
    private LocalDateTime timestamp;

    // Anomaly.java
    private boolean dismissedInReport = false; // Default-aa false-nu irukkum

    public Anomaly() {}

    // --- GETTERS AND SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getAttemptedBy() { return attemptedBy; }
    public void setAttemptedBy(String attemptedBy) { this.attemptedBy = attemptedBy; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isReported() {
        return reported;
    }

    public void setReported(boolean reported) {
        this.reported = reported;
    }

    public boolean isRedAlert() {
        return redAlert;
    }

    public void setRedAlert(boolean redAlert) {
        this.redAlert = redAlert;
    }

    public boolean isYellowAlert() {
        return yellowAlert;
    }

    public void setYellowAlert(boolean yellowAlert) {
        this.yellowAlert = yellowAlert;
    }

    public boolean isDismissedInReport() {
        return dismissedInReport;
    }

    public void setDismissedInReport(boolean dismissedInReport) {
        this.dismissedInReport = dismissedInReport;
    }
}