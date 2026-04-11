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

    // ═══════════════════════════════════════════════════════
    // 🌀 1. ENCRYPTION — Real Polar Geometry Based
    // ═══════════════════════════════════════════════════════
    public byte[] encrypt(byte[] data, int xl, int yl, double r, int theta) {
        byte[] output = new byte[data.length];

        // Grid center point
        double centerX = xl / 2.0;
        double centerY = yl / 2.0;

        for (int i = 0; i < data.length; i++) {

            // Step 1: Find grid position of this byte
            int row = i / xl;
            int col = i % xl;

            // Step 2: Calculate polar coordinates from center
            // R = distance from grid center to this cell
            double cellR = Math.sqrt(
                    Math.pow(col - centerX, 2) +
                            Math.pow(row - centerY, 2)
            );

            // Theta = angle of this cell from center (in radians → degrees)
            double cellTheta = Math.toDegrees(
                    Math.atan2(row - centerY, col - centerX)
            );

            // Step 3: Apply user key + server salt to polar values
            // Each byte gets UNIQUE shift based on its position in grid!
            int shift = (int)((r * cellR) + (theta * cellTheta / 360.0));

            // Step 4: Encrypt byte with unique polar shift
            output[i] = (byte)(data[i] + shift);
        }

        return output;
    }

    // ═══════════════════════════════════════════════════════
    // 🔓 2. DECRYPTION — Reverse Polar Shift
    //       (With Auto-Lock + Admin-Block Check)
    // ═══════════════════════════════════════════════════════
    public byte[] decrypt(byte[] encryptedData, int xl, int yl,
                          double r, int theta,
                          EncryptedFile originalFile, User currentUser) {

        // 🚨 Admin permanently blocked this user
        if (!currentUser.isEnabled()) {
            throw new RuntimeException(
                    "Your account is permanently blocked by Admin for security reasons."
            );
        }

        // ⏰ Auto-lock check (10 min lockout)
        if (currentUser.getDecryptDisabledUntil() != null &&
                currentUser.getDecryptDisabledUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Too many failed attempts! Automatically locked. Try after 10 minutes."
            );
        }

        // 🔴 RED ALERT: User has no permission for this file
        if (!originalFile.hasAccess(currentUser)) {
            logAnomaly(originalFile, currentUser, "NO_PERMISSION");
            throw new RuntimeException("CRITICAL: Unauthorized Access! Owner notified.");
        }

        // 🟡 YELLOW ALERT: Wrong key submitted
        if (originalFile.getXl() != xl ||
                originalFile.getYl() != yl ||
                originalFile.getServerRadius() != r ||
                originalFile.getServerAngle() != theta) {

            currentUser.setFailedAttempts(currentUser.getFailedAttempts() + 1);
            logAnomaly(originalFile, currentUser, "WRONG_KEY");

            if (currentUser.getFailedAttempts() >= 5) {
                // Auto-lock for 10 minutes
                currentUser.setDecryptDisabledUntil(LocalDateTime.now().plusMinutes(10));
                currentUser.setFailedAttempts(0);
                userRepo.save(currentUser);
                throw new RuntimeException("5 failed attempts! System locked for 10 mins.");
            }
            userRepo.save(currentUser);
            return null;
        }

        // ✅ Correct key — reset failure count
        currentUser.setFailedAttempts(0);
        currentUser.setDecryptDisabledUntil(null);
        userRepo.save(currentUser);

        // Reverse the polar shift (exact reverse of encrypt)
        byte[] output = new byte[encryptedData.length];

        double centerX = xl / 2.0;
        double centerY = yl / 2.0;

        for (int i = 0; i < encryptedData.length; i++) {

            int row = i / xl;
            int col = i % xl;

            double cellR = Math.sqrt(
                    Math.pow(col - centerX, 2) +
                            Math.pow(row - centerY, 2)
            );

            double cellTheta = Math.toDegrees(
                    Math.atan2(row - centerY, col - centerX)
            );

            // Same shift calculation — subtract instead of add
            int shift = (int)((r * cellR) + (theta * cellTheta / 360.0));

            output[i] = (byte)(encryptedData[i] - shift);
        }

        return output;
    }

    // ═══════════════════════════════════════════════════════
    // ⭐ 3. ANOMALY LOGGING — Red & Yellow Alerts
    // ═══════════════════════════════════════════════════════
    public synchronized void logAnomaly(EncryptedFile file, User suspect, String attemptType) {

        String lockKey = suspect.getEmail() + "_" +
                file.getOriginalFileName() + "_" + attemptType;
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
                // 🔴 RED ALERT — Unauthorized user
                a.setRedAlert(true);
                a.setYellowAlert(false);
                sendSecurityAlertToOwner(
                        file.getOwner().getEmail(),
                        suspect.getEmail(),
                        file.getOriginalFileName()
                );
            } else {
                // 🟡 YELLOW ALERT — Wrong key
                a.setRedAlert(false);
                a.setYellowAlert(true);
            }

            anomalyRepo.save(a);

            // Release duplicate-guard lock after 3 seconds
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() { activeLocks.remove(lockKey); }
            }, 3000);

        } catch (Exception e) {
            activeLocks.remove(lockKey);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════
    // ⭐ 4. ADMIN ACTION — Process / Dismiss Report
    // ═══════════════════════════════════════════════════════
    public void processReport(Long anomalyId) {
        Anomaly a = anomalyRepo.findById(anomalyId).orElseThrow();
        a.setReported(true);
        anomalyRepo.save(a);

        sendEmail(
                a.getOwnerEmail(),
                "✅ Threat Reported Successfully",
                "Hi Owner,\n\nYour report for file '" + a.getFilename() +
                        "' has been filed.\nAdmin is reviewing the suspect: " + a.getAttemptedBy()
        );
    }

    // ═══════════════════════════════════════════════════════
    // 📧 MAIL HELPERS
    // ═══════════════════════════════════════════════════════
    private void sendSecurityAlertToOwner(String ownerEmail,
                                          String suspectEmail,
                                          String fileName) {
        String subject = "🚨 SECURITY ALERT: Unauthorized Access Attempt!";
        String body = "Hello Owner,\n\n" +
                "An unauthorized person (" + suspectEmail + ") " +
                "tried to access your file: " + fileName + "\n\n" +
                "Please check Admin Dashboard to block this user if needed.";
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

    // ═══════════════════════════════════════════════════════
    // 🖼️ SCRAMBLED IMAGE VIEW
    // ═══════════════════════════════════════════════════════
    public ResponseEntity<byte[]> getScrambledImage(Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ef.getFileType()))
                .body(ef.getEncryptedData());
    }
}