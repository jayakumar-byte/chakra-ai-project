package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.FileActivity;
import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.*;
import com.example.chakraEncryption2.service.ChakraCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequestMapping("/api/chakra")
public class DecryptionController {

    @Autowired private ChakraCoreService chakraService;
    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private FileActivityRepository activityRepo;
    @Autowired private PermissionRepository permissionRepo;

    @GetMapping("/decrypt-hub")
    public String showDecryptHub() {
        return "decrypt_page";
    }

    // ⭐ STEP 1: FILE IDENTIFICATION & AUTHORIZATION
    @PostMapping("/decrypt-upload")
    public String identifyFile(@RequestParam("file") MultipartFile file,
                               Authentication auth, RedirectAttributes ra) {
        try {
            String uploadHash = generateHash(file.getBytes());
            Optional<EncryptedFile> matchedFile = fileRepo.findByFileHash(uploadHash);

            if (matchedFile.isEmpty()) {
                ra.addFlashAttribute("error", "🚨 File verification failed! Unknown File.");
                return "redirect:/api/chakra/decrypt-hub";
            }

            EncryptedFile ef = matchedFile.get();
            User currentUser = userRepo.findByEmail(auth.getName()).orElseThrow();

            // ⭐ RED ALERT LOGIC: Use Service for Mail Alert
            if (!checkAccessSafe(ef, currentUser)) {
                // ✅ Calling Service Method (This sends the Owner Alert Mail)
                chakraService.logAnomaly(ef, currentUser, "NO_PERMISSION");

                ra.addFlashAttribute("error", "🚨 ACCESS DENIED: Unauthorized person detected. Owner Notified!");
                return "redirect:/api/chakra/decrypt-hub";
            }

            ra.addFlashAttribute("fileId", ef.getId());
            ra.addFlashAttribute("fileName", ef.getOriginalFileName());
            ra.addFlashAttribute("verificationSuccess", true);

            return "redirect:/api/chakra/decrypt-hub";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/api/chakra/decrypt-hub";
        }
    }

    // ⭐ STEP 2: DECRYPTION ATTEMPT
    @PostMapping("/decrypt/{id}")
    public ResponseEntity<?> decryptFile(@PathVariable Long id,
                                         @RequestParam("radius") double r,
                                         @RequestParam("theta") int theta,
                                         Authentication auth) {

        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        User currentUser = userRepo.findByEmail(auth.getName()).orElseThrow();

        if (!checkAccessSafe(ef, currentUser)) {
            // Re-check for security
            chakraService.logAnomaly(ef, currentUser, "NO_PERMISSION");
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Unauthorized access."));
        }

        // Salt addition as per project logic
        double saltedR = r + 0.55;
        int saltedTheta = theta + 7;

        try {
            // ⭐ Decrypt call: Service handles WRONG_KEY anomaly inside
            byte[] decryptedBytes = chakraService.decrypt(ef.getEncryptedData(), ef.getXl(), ef.getYl(),
                    saltedR, saltedTheta, ef, currentUser);

            if (decryptedBytes == null) {
                return ResponseEntity.status(401)
                        .body(java.util.Map.of("error", "Invalid Credentials! Key combination is wrong."));
            }

            logActivity(ef.getOriginalFileName(), auth.getName(), "DECRYPTION_SUCCESS");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"decrypted_" + ef.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(ef.getFileType()))
                    .body(decryptedBytes);

        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private String generateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(data));
    }

    private void logActivity(String filename, String email, String action) {
        FileActivity log = new FileActivity();
        log.setFilename(filename);
        log.setAccessedBy(email);
        log.setAccessTime(LocalDateTime.now());
        log.setAction(action);
        activityRepo.save(log);
    }

    private boolean checkAccessSafe(EncryptedFile file, User user) {
        boolean isOwnerByEmail = file.getOwner().getEmail().equalsIgnoreCase(user.getEmail());
        boolean hasSharedPermission = permissionRepo.existsByFileAndAllowedUser(file, user);
        return isOwnerByEmail || hasSharedPermission;
    }
}