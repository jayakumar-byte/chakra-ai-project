package com.example.chakraEncryption2.service;

import com.example.chakraEncryption2.entity.*;
import com.example.chakraEncryption2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChakraCoreService {

    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private AnomalyRepository anomalyRepo;
    @Autowired private JavaMailSender mailSender;

    private static final java.util.Set<String> activeLocks = ConcurrentHashMap.newKeySet();

    // 🌀 1. ENCRYPTION
    public byte[] encrypt(byte[] data, int xl, int yl, double r, int theta) {
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = (byte) (data[i] + (int)r + theta);
        }
        return output;
    }

    // 🔓 2. DECRYPTION (With Auto-Lock and Admin-Block Check)
    public byte[] decrypt(byte[] encryptedData, int xl, int yl, double r, int theta, EncryptedFile originalFile, User currentUser) {

        // 🚨 POWER CHECK: If Admin permanently blocked the user
        if (!currentUser.isEnabled()) {
            throw new RuntimeException("Your account is permanently blocked by Admin for security reasons.");
        }

        // ⏰ AUTO-LOCK CHECK (10 Mins Logic - No Admin needed)
        if (currentUser.getDecryptDisabledUntil() != null && currentUser.getDecryptDisabledUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Too many failed attempts! Automatically locked. Try after 10 minutes.");
        }

        // 🛑 RED ALERT: Unauthorized Access
        if (!originalFile.hasAccess(currentUser)) {
            logAnomaly(originalFile, currentUser, "NO_PERMISSION");
            throw new RuntimeException("CRITICAL: Unauthorized Access! Owner notified.");
        }

        // ⚠️ YELLOW ALERT: Wrong Key Logic
        if (originalFile.getXl() != xl || originalFile.getYl() != yl || originalFile.getServerRadius() != r || originalFile.getServerAngle() != theta) {
            currentUser.setFailedAttempts(currentUser.getFailedAttempts() + 1);
            logAnomaly(originalFile, currentUser, "WRONG_KEY");

            if (currentUser.getFailedAttempts() >= 5) {
                // System Auto-locks for 10 mins
                currentUser.setDecryptDisabledUntil(LocalDateTime.now().plusMinutes(10));
                currentUser.setFailedAttempts(0);
                userRepo.save(currentUser);
                throw new RuntimeException("5 failed attempts! System locked for 10 mins.");
            }
            userRepo.save(currentUser);
            return null;
        }

        // Success: Reset failures
        currentUser.setFailedAttempts(0);
        currentUser.setDecryptDisabledUntil(null);
        userRepo.save(currentUser);

        byte[] output = new byte[encryptedData.length];
        for (int i = 0; i < encryptedData.length; i++) {
            output[i] = (byte) (encryptedData[i] - (int)r - theta);
        }
        return output;
    }

    // ⭐ 3. ANOMALY LOGGING (Categorized Warnings)
    public synchronized void logAnomaly(EncryptedFile file, User suspect, String attemptType) {
        String lockKey = suspect.getEmail() + "_" + file.getOriginalFileName() + "_" + attemptType;
        if (!activeLocks.add(lockKey)) return;

        try {
            Anomaly a = new Anomaly();
            a.setFilename(file.getOriginalFileName());
            a.setAttemptedBy(suspect.getEmail());
            a.setTimestamp(LocalDateTime.now());
            a.setOwnerId(file.getOwner().getId());
            a.setOwnerEmail(file.getOwner().getEmail());
            a.setReported(false);

            if ("NO_PERMISSION".equals(attemptType)) {
                // 🔴 RED WARNING
                a.setRedAlert(true);
                a.setYellowAlert(false);
                sendSecurityAlertToOwner(file.getOwner().getEmail(), suspect.getEmail(), file.getOriginalFileName());
            } else {
                // 🟡 YELLOW WARNING
                a.setRedAlert(false);
                a.setYellowAlert(true);
            }

            anomalyRepo.save(a);

            // Release lock after 3s to prevent mail spam
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() { activeLocks.remove(lockKey); }
            }, 3000);

        } catch (Exception e) {
            activeLocks.remove(lockKey);
            throw e;
        }
    }

    // ⭐ 4. ADMIN ACTION: Process Report
    public void processReport(Long anomalyId) {
        Anomaly a = anomalyRepo.findById(anomalyId).orElseThrow();
        a.setReported(true);
        anomalyRepo.save(a);

        sendEmail(a.getOwnerEmail(), "✅ Threat Reported Successfully",
                "Hi Owner, your report for file '" + a.getFilename() + "' has been filed. Admin is reviewing the suspect: " + a.getAttemptedBy());
    }

    private void sendSecurityAlertToOwner(String ownerEmail, String suspectEmail, String fileName) {
        String subject = "🚨 SECURITY ALERT: Unauthorized Access Attempt!";
        String body = "Hello Owner,\n\nAn unauthorized person (" + suspectEmail + ") tried to access your file: " + fileName + "\n\nPlease check Admin Dashboard to block this user if needed.";
        sendEmail(ownerEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            System.out.println(">>> Mail sent to: " + to);
        } catch (Exception e) {
            System.err.println(">>> MAIL ERROR: " + e.getMessage());
        }
    }

    public ResponseEntity<byte[]> getScrambledImage(Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ef.getFileType()))
                .body(ef.getEncryptedData());
    }
}