package com.example.chakraEncryption2.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "file_permissions")
public class FilePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private EncryptedFile file;

    @ManyToOne
    @JoinColumn(name = "user_id") // DB-la 'user_id' nu create aagaradhala namma ingaye mathiduvom
    private User allowedUser;

    @ManyToOne
    @JoinColumn(name = "granted_by_id")
    private User grantedBy;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EncryptedFile getFile() { return file; }
    public void setFile(EncryptedFile file) { this.file = file; }

    public User getAllowedUser() { return allowedUser; }
    public void setAllowedUser(User allowedUser) { this.allowedUser = allowedUser; }

    public User getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(User grantedBy) {
        this.grantedBy = grantedBy;
    }
}