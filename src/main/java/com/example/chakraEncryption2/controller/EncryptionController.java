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

    @Autowired
    private ChakraCoreService chakraService;
    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private AIService aiService;



    @GetMapping("/encrypt-page")
    public String showEncryptPage(Model model) {
        model.addAttribute("encryptionSuccess", false);
        return "encrypt_page";
    }

    @PostMapping("/encrypt")
    public String encryptFile(@RequestParam("file") MultipartFile file,
                              @RequestParam("radius") double r,
                              @RequestParam("theta") int theta,
                              @RequestParam("mode") String mode,
                              Authentication auth, Model model) throws Exception {

        User owner = userRepo.findByEmail(auth.getName()).orElseThrow();

        // 1. AI Grid Optimization
        int[] optimizedGrid = aiService.getOptimizedGrid(file);

        // 2. Apply Salts
        double finalR = r + 0.55;
        int finalTheta = theta + 7;


        // 3. Encrypt
        byte[] encryptedBytes = chakraService.encrypt(file.getBytes(), optimizedGrid[0], optimizedGrid[1], finalR, finalTheta);
        String currentHash = generateHash(encryptedBytes);



        // 4. Duplicate Check
        Optional<EncryptedFile> existingFile = fileRepo.findByFileHash(currentHash);
        EncryptedFile ef = existingFile.orElse(new EncryptedFile());

        ef.setOriginalFileName(file.getOriginalFilename());
        ef.setFileType(file.getContentType());
        ef.setEncryptedData(encryptedBytes);
        ef.setFileHash(currentHash);
        ef.setServerRadius(finalR);
        ef.setServerAngle(finalTheta);
        ef.setOwner(owner);
        ef.setXl(optimizedGrid[0]);
        ef.setYl(optimizedGrid[1]);

        EncryptedFile savedFile = fileRepo.save(ef);

        // 5. UI Response Logic (Canvas Preview)
        boolean isImage = mode.equals("image") || (file.getContentType() != null && file.getContentType().startsWith("image/"));

        model.addAttribute("encryptionSuccess", true);
        model.addAttribute("newFileId", savedFile.getId());
        model.addAttribute("fileName", file.getOriginalFilename());
        model.addAttribute("isImage", isImage);

        if (isImage) {
            String base64Image = Base64.getEncoder().encodeToString(encryptedBytes);
            model.addAttribute("imagePreviewData", base64Image);
        } else {
            String base64Encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            int previewLength = Math.min(base64Encrypted.length(), 500);
            model.addAttribute("textPreview", base64Encrypted.substring(0, previewLength) + "...");
        }

        return "encrypt_page";
    }
    private String generateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return Base64.getEncoder().encodeToString(hash);
    }


    @GetMapping("/view-scrambled/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> viewScrambled(@PathVariable Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(ef.getEncryptedData());
    }

    @GetMapping("/download-scrambled/{id}")
    public ResponseEntity<byte[]> downloadScrambled(@PathVariable Long id) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        String downloadName = "scrambled_" + ef.getOriginalFileName();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType(ef.getFileType()))
                .body(ef.getEncryptedData());
    }
}
