package com.example.chakraEncryption2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "encrypted_files")
public class EncryptedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;
    private String fileType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] encryptedData;

    @Column(unique = true)
    private String fileHash;

    private double serverRadius; // This is our 'r' [cite: 2026-01-04]
    private int serverAngle;     // This is our 'theta' [cite: 2026-01-04]

    private int xl;
    private int yl;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    // ⭐ FIX: Changed HashSet to Set interface to avoid Hibernate PersistentSet mismatch [cite: 2026-01-16]
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "file_permissions",
            joinColumns = @JoinColumn(name = "file_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> allowedUsers = new HashSet<>();

    private boolean hiddenFromUser = false;

    public EncryptedFile() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- HELPER METHODS FOR SERVICE COMPATIBILITY --- [cite: 2026-01-04]

    public String getFileName() {
        return originalFileName;
    }

    public String getOwnerEmail() {
        return (owner != null) ? owner.getEmail() : "admin@chakra.com";
    }

    public double getR() {
        return serverRadius;
    }

    public int getTheta() {
        return serverAngle;
    }

    public boolean hasAccess(User user) {
        if (user == null) return false;
        return (this.owner != null && this.owner.equals(user)) || this.allowedUsers.contains(user);
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public byte[] getEncryptedData() { return encryptedData; }
    public void setEncryptedData(byte[] encryptedData) { this.encryptedData = encryptedData; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public double getServerRadius() { return serverRadius; }
    public void setServerRadius(double serverRadius) { this.serverRadius = serverRadius; }

    public int getServerAngle() { return serverAngle; }
    public void setServerAngle(int serverAngle) { this.serverAngle = serverAngle; }

    public int getXl() { return xl; }
    public void setXl(int xl) { this.xl = xl; }

    public int getYl() { return yl; }
    public void setYl(int yl) { this.yl = yl; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    // ⭐ FIX: Updated Getter/Setter to use Set interface
    public Set<User> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(Set<User> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isHiddenFromUser() { return hiddenFromUser; }
    public void setHiddenFromUser(boolean hiddenFromUser) { this.hiddenFromUser = hiddenFromUser; }
}