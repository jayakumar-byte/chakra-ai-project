package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.FileRepository;
import com.example.chakraEncryption2.repository.UserRepository;
import com.example.chakraEncryption2.service.AIService;
import com.example.chakraEncryption2.service.ChakraCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequestMapping("/api/chakra")
public class EncryptionController {

    @Autowired private ChakraCoreService chakraService;
    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private AIService aiService;

    // ═══════════════════════════════════════════════════════
    // 📄 Encrypt Page
    // ═══════════════════════════════════════════════════════
    @GetMapping("/encrypt-page")
    public String showEncryptPage(Model model) {
        model.addAttribute("encryptionSuccess", false);
        return "encrypt_page";
    }

    // ═══════════════════════════════════════════════════════
    // 🔒 Encrypt File
    // ═══════════════════════════════════════════════════════
    @PostMapping("/encrypt")
    public String encryptFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("radius") double r,
            @RequestParam("theta") int theta,
            @RequestParam("mode") String mode,
            Authentication auth,
            Model model) throws Exception {

        User owner = userRepo.findByEmail(auth.getName()).orElseThrow();

        // Step 1: AI Grid Optimization (AEGO)
        int[] optimizedGrid = aiService.getOptimizedGrid(file);
        int xl = optimizedGrid[0];
        int yl = optimizedGrid[1];

        // Step 2: Apply Server-Side Salt (Hidden constants)
        double finalR     = r + 0.55;
        int    finalTheta = theta + 7;

        // Step 3: Encrypt using real polar geometry
        byte[] encryptedBytes = chakraService.encrypt(
                file.getBytes(), xl, yl, finalR, finalTheta
        );

        // Step 4: SHA-256 hash for integrity
        String currentHash = generateHash(encryptedBytes);

        // Step 5: Duplicate check — update if same file
        Optional<EncryptedFile> existingFile = fileRepo.findByFileHash(currentHash);
        EncryptedFile ef = existingFile.orElse(new EncryptedFile());

        ef.setOriginalFileName(file.getOriginalFilename());
        ef.setFileType(file.getContentType());
        ef.setEncryptedData(encryptedBytes);
        ef.setFileHash(currentHash);
        ef.setServerRadius(finalR);
        ef.setServerAngle(finalTheta);
        ef.setOwner(owner);
        ef.setXl(xl);
        ef.setYl(yl);

        EncryptedFile savedFile = fileRepo.save(ef);

        // Step 6: UI Response (Canvas Preview)
        boolean isImage = mode.equals("image") ||
                (file.getContentType() != null &&
                        file.getContentType().startsWith("image/"));

        model.addAttribute("encryptionSuccess", true);
        model.addAttribute("newFileId", savedFile.getId());
        model.addAttribute("fileName", file.getOriginalFilename());
        model.addAttribute("gridSize", xl + " x " + yl);   // show grid info
        model.addAttribute("isImage", isImage);

        if (isImage) {
            String base64Image = Base64.getEncoder().encodeToString(encryptedBytes);
            model.addAttribute("imagePreviewData", base64Image);
        } else {
            String base64Encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            int previewLength = Math.min(base64Encrypted.length(), 500);
            model.addAttribute("textPreview",
                    base64Encrypted.substring(0, previewLength) + "...");
        }

        return "encrypt_page";
    }

    // ═══════════════════════════════════════════════════════
    // 🔑 SHA-256 Hash Generator
    // ═══════════════════════════════════════════════════════
    private String generateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return Base64.getEncoder().encodeToString(hash);
    }

    // ═══════════════════════════════════════════════════════
    // 🖼️ View Scrambled Image (Canvas Preview)
    // ═══════════════════════════════════════════════════════
    @GetMapping("/view-scrambled/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> viewScrambled(@PathVariable Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(ef.getEncryptedData());
    }

    // ═══════════════════════════════════════════════════════
    // 📥 Download Scrambled File
    // ═══════════════════════════════════════════════════════
    @GetMapping("/download-scrambled/{id}")
    public ResponseEntity<byte[]> downloadScrambled(@PathVariable Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        String downloadName = "scrambled_" + ef.getOriginalFileName();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType(ef.getFileType()))
                .body(ef.getEncryptedData());
    }
}