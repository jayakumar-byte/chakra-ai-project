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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChakraCoreService {

    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private AnomalyRepository anomalyRepo;
    @Autowired private JavaMailSender mailSender;

    private static final java.util.Set<String> activeLocks = ConcurrentHashMap.newKeySet();

    /**
     * ⭐ NEW: Input Validation Constraints Check Layer
     * Conditions: radius <= 15 (and > 0), theta > 1 and <= 360
     */
    public boolean validateGeometricParameters(double radius, int theta) {
        if (radius <= 0 || radius > 15.0) {
            return false;
        }
        if (theta <= 1 || theta > 360) {
            return false;
        }
        return true;
    }

    // BIJECTIVE PERMUTATION
    private static class PixelMapping implements Comparable<PixelMapping> {
        int originalIndex;
        double rotY;
        double rotX;

        public PixelMapping(int id, double ry, double rx) {
            this.originalIndex = id;
            this.rotY = ry;
            this.rotX = rx;
        }

        @Override
        public int compareTo(PixelMapping o) {
            if (Math.abs(this.rotY - o.rotY) > 0.000001) {
                return Double.compare(this.rotY, o.rotY);
            }
            return Double.compare(this.rotX, o.rotX);
        }
    }

    private int[] generateChakraPermutation(int xl, int yl, int theta) {
        double cx = xl / 2.0;
        double cy = yl / 2.0;
        double thetaRad = Math.toRadians(theta);
        int total = xl * yl;

        List<PixelMapping> list = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int c = i % xl;
            int r = i / xl;
            // Original Chakra Math
            double rotX = Math.cos(thetaRad) * (c - cx) - Math.sin(thetaRad) * (r - cy) + cx;
            double rotY = Math.sin(thetaRad) * (c - cx) + Math.cos(thetaRad) * (r - cy) + cy;
            list.add(new PixelMapping(i, rotY, rotX));
        }

        // Sorting guarantees ZERO collisions (1-to-1 Mapping)
        Collections.sort(list);

        int[] permutation = new int[total];
        for (int i = 0; i < total; i++) {
            permutation[i] = list.get(i).originalIndex;
        }
        return permutation;
    }

    // ENCRYPTION
    public byte[] encrypt(byte[] data, int xl, int yl, double r, int theta) {
        int totalCapacity = xl * yl;
        byte[] paddedGrid = new byte[totalCapacity];

        // 🚨 STORE EXACT ORIGINAL SIZE IN FIRST 4 BYTES
        paddedGrid[0] = (byte) (data.length >> 24);
        paddedGrid[1] = (byte) (data.length >> 16);
        paddedGrid[2] = (byte) (data.length >> 8);
        paddedGrid[3] = (byte) (data.length);

        // Copy actual file data after the 4-byte header
        System.arraycopy(data, 0, paddedGrid, 4, data.length);

        // Fill the rest with seeded noise
        Random rand = new Random(42);
        for(int i = 4 + data.length; i < totalCapacity; i++) {
            paddedGrid[i] = (byte) rand.nextInt(256);
        }

        // Apply Bijective Rotation & Radial Shift
        double cx = xl / 2.0;
        double cy = yl / 2.0;
        int[] perm = generateChakraPermutation(xl, yl, theta);
        byte[] output = new byte[totalCapacity];

        for (int i = 0; i < totalCapacity; i++) {
            int originalIdx = perm[i];
            byte val = paddedGrid[originalIdx];

            // Shift based on NEW location (i)
            int r_coord = i / xl;
            int c_coord = i % xl;
            double cellR = Math.sqrt(Math.pow(c_coord - cx, 2) + Math.pow(r_coord - cy, 2));

            output[i] = (byte) (val + (int)(r * cellR));
        }
        return output;
    }

    // DECRYPTION
    public byte[] decrypt(byte[] encryptedData, int xl, int yl, double r, int theta, EncryptedFile originalFile, User currentUser) {
        // --- Security Checks ---
        if (!currentUser.isEnabled()) throw new RuntimeException("Account Blocked.");
        if (currentUser.getDecryptDisabledUntil() != null && currentUser.getDecryptDisabledUntil().isAfter(LocalDateTime.now()))
            throw new RuntimeException("Locked! Try later.");
        if (!originalFile.hasAccess(currentUser)) { logAnomaly(originalFile, currentUser, "NO_PERMISSION"); throw new RuntimeException("Unauthorized!"); }

        if (originalFile.getXl() != xl || originalFile.getYl() != yl || originalFile.getServerRadius() != r || originalFile.getServerAngle() != theta) {
            currentUser.setFailedAttempts(currentUser.getFailedAttempts() + 1); logAnomaly(originalFile, currentUser, "WRONG_KEY");
            if (currentUser.getFailedAttempts() >= 5) { currentUser.setDecryptDisabledUntil(LocalDateTime.now().plusMinutes(10)); currentUser.setFailedAttempts(0); }
            userRepo.save(currentUser); return null;
        }
        currentUser.setFailedAttempts(0); userRepo.save(currentUser);

        int totalCapacity = xl * yl;
        double cx = xl / 2.0;
        double cy = yl / 2.0;

        int[] perm = generateChakraPermutation(xl, yl, theta);
        byte[] paddedGrid = new byte[totalCapacity];

        // Reverse the mapping and shift
        for (int i = 0; i < totalCapacity; i++) {
            int originalIdx = perm[i];

            int r_coord = i / xl;
            int c_coord = i % xl;
            double cellR = Math.sqrt(Math.pow(c_coord - cx, 2) + Math.pow(r_coord - cy, 2));

            paddedGrid[originalIdx] = (byte) (encryptedData[i] - (int)(r * cellR));
        }

        // 🚨 RECOVER THE EXACT ORIGINAL FILE SIZE
        int originalLength = ((paddedGrid[0] & 0xFF) << 24) |
                ((paddedGrid[1] & 0xFF) << 16) |
                ((paddedGrid[2] & 0xFF) << 8) |
                (paddedGrid[3] & 0xFF);

        // Security check for corrupted arrays
        if (originalLength <= 0 || originalLength > totalCapacity - 4) {
            originalLength = totalCapacity - 4; // Fallback
        }

        // Extract ONLY the exact original bytes
        byte[] finalOutput = new byte[originalLength];
        System.arraycopy(paddedGrid, 4, finalOutput, 0, originalLength);

        return finalOutput;
    }

    // ANOMALY & ADMIN HELPERS
    public void processReport(Long anomalyId) {
        Anomaly a = anomalyRepo.findById(anomalyId).orElseThrow();
        a.setReported(true);
        anomalyRepo.save(a);
        sendEmail(a.getOwnerEmail(), "✅ Threat Reported", "Report filed for: " + a.getFilename());
    }

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
            if ("NO_PERMISSION".equals(attemptType)) { a.setRedAlert(true); sendSecurityAlert(file.getOwner().getEmail(), suspect.getEmail(), file.getOriginalFileName()); }
            else { a.setYellowAlert(true); }
            anomalyRepo.save(a);
            new java.util.Timer().schedule(new java.util.TimerTask() { @Override public void run() { activeLocks.remove(lockKey); } }, 3000);
        } catch (Exception e) { activeLocks.remove(lockKey); }
    }

    private void sendSecurityAlert(String owner, String suspect, String file) {
        sendEmail(owner, "🚨 Security Alert", suspect + " tried to access your file: " + file);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage(); msg.setTo(to); msg.setSubject(subject); msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) { System.err.println("Mail Error: " + e.getMessage()); }
    }

    // FIXED: Changed ef.FileType() to ef.getFileType()
    public ResponseEntity<byte[]> getScrambledImage(Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ef.getFileType()))
                .body(ef.getEncryptedData());
    }
}