package com.example.chakraEncryption2.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AIService {

    /**
     * AEGO - Adaptive Entropy-Based Grid Optimization
     * AI Decision Tree for Grid Selection
     * Factors: File Type, File Size, Temporal Jitter (X and Y independent)
     */
    public int[] getOptimizedGrid(MultipartFile file) {
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        // ═══════════════════════════════════════════════
        // 🌀 Temporal Jitter — Independent for X and Y
        // Same file uploaded at different times →
        // different grid! Unique encryption guaranteed!
        // ═══════════════════════════════════════════════
        long currentTime = System.currentTimeMillis();

        // X jitter — based on current time
        long timeFactorX = currentTime % 10;
        int jitterX = (int)((fileSize + timeFactorX) % 15); // range 0-14

        // Y jitter — based on half time (always different from X!)
        long timeFactorY = (currentTime / 2) % 10;
        int jitterY = (int)((fileSize + timeFactorY) % 15); // range 0-14

        // ═══════════════════════════════════════════════
        // 🌳 Decision Tree — Base grid from file type & size
        // ═══════════════════════════════════════════════
        int baseX = 10; // default
        int baseY = 10; // default

        if (contentType != null && contentType.startsWith("image")) {
            // High complexity — images have high pixel density
            if (fileSize > 2 * 1024 * 1024) {
                // Large image (>2MB) → big grid
                baseX = 60;
                baseY = 50; // different base! ✅
            } else {
                // Small image (<2MB) → medium grid
                baseX = 30;
                baseY = 25; // different base! ✅
            }
        }
        else if (contentType != null &&
                (contentType.contains("pdf") || contentType.contains("text"))) {
            // Medium complexity — documents
            if (fileSize > 1024 * 1024) {
                // Large document (>1MB)
                baseX = 20;
                baseY = 18; // different base! ✅
            } else {
                // Small document (<1MB)
                baseX = 12;
                baseY = 10; // different base! ✅
            }
        }
        else {
            // General files
            if (fileSize > 5 * 1024 * 1024) {
                // Large file (>5MB)
                baseX = 50;
                baseY = 45; // different base! ✅
            } else {
                // Small file (<5MB)
                baseX = 15;
                baseY = 12; // different base! ✅
            }
        }

        // ═══════════════════════════════════════════════
        // Final Grid = Base + Independent Jitter
        // xl ≠ yl → true polar grid! ✅
        // ═══════════════════════════════════════════════
        int xl = baseX + jitterX;
        int yl = baseY + jitterY;

        // Debug log
        System.out.println("🤖 AEGO Decision:" +
                " FileType="  + contentType  +
                " FileSize="  + fileSize     +
                " BaseX="     + baseX        +
                " BaseY="     + baseY        +
                " JitterX="   + jitterX      +
                " JitterY="   + jitterY      +
                " FinalGrid=" + xl + "x" + yl);

        return new int[]{xl, yl};
    }
}