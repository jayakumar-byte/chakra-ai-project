package com.example.chakraEncryption2.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AIService {

    // Size Constraints in Bytes
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long MAX_TEXT_SIZE = 5 * 1024 * 1024;  // 5MB

    /**
     * ⭐ NEW: Validates file metadata based on content type and customized size limits
     * Image must be < 2MB, Text file must be < 5MB
     */
    public boolean validateFileMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        long fileSize = file.getSize();

        // 1. Image Validation Logic (< 2MB)
        if (contentType != null && contentType.startsWith("image/")) {
            return fileSize <= MAX_IMAGE_SIZE;
        }

        // 2. Text File Validation Logic (< 5MB)
        if (contentType != null && (contentType.startsWith("text/") || contentType.equals("application/json") || contentType.equals("application/pdf"))) {
            return fileSize <= MAX_TEXT_SIZE;
        }

        // 3. Alternate files handling fallback (If needed, else default to true/false)
        return false;
    }

    /**
     * EXISTING: Core AEGO Grid Optimization Engine (Kept intact)
     */
    public int[] getOptimizedGrid(MultipartFile file) {
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        long currentTime = System.currentTimeMillis();

        // 1. Fundamental Capacity (sqrt + buffer)
        int minSide = (int) Math.ceil(Math.sqrt(fileSize)) + 5;

        // 2. Independent Jitters
        int jitterX = (int)((currentTime % 10) + (fileSize % 5));
        int jitterY = (int)(((currentTime / 2) % 10) + (fileSize % 7));

        int xl = minSide + jitterX;
        int yl = minSide + jitterY;

        // 3. Aspect Ratio Adjustments
        if (contentType != null && contentType.startsWith("image")) {
            xl += 5;
        } else if (contentType != null && contentType.contains("pdf")) {
            yl += 5;
        }

        // 4. THE GOLDEN RULE: Area >= fileSize
        while ((long) xl * yl < fileSize) {
            xl++;
            yl++;
        }

        System.out.println("🤖 AEGO Final Decision: Size=" + fileSize + " Grid=" + xl + "x" + yl);
        return new int[]{xl, yl};
    }
}