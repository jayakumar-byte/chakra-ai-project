package com.example.chakraEncryption2.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AIService {

    /**
     * AI Decision Tree for Grid Optimization
     * Factors: File Type, File Size, and Temporal Jitter (Time factor)
     */
    public int[] getOptimizedGrid(MultipartFile file) {
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        // 🌀 AI Temporal Jitter
        // System time-a use panni random-ness add panrom.
        // Idhunala same file-a upload pannaalum unique grid kidaikkum.
        long timeFactor = System.currentTimeMillis() % 10;
        int jitter = (int) ((fileSize + timeFactor) % 15); // Jitter range 0-14

        int base = 10; // Default base for unknown files

        // 🌳 Decision Tree Logic
        if (contentType != null && contentType.startsWith("image")) {
            // High complexity for images due to pixel density
            base = (fileSize > 2 * 1024 * 1024) ? 60 : 30;
        }
        else if (contentType != null && (contentType.contains("pdf") || contentType.contains("text"))) {
            // Medium complexity for documents
            base = (fileSize > 1024 * 1024) ? 20 : 12;
        }
        else {
            // General files
            base = (fileSize > 5 * 1024 * 1024) ? 50 : 15;
        }

        // Final AI Optimized Grid = Base + Jitter
        int xl = base + jitter;
        int yl = base + jitter;

        // Logging the AI decision for debugging (Optional)
        System.out.println("🤖 AI Decision: FileType=" + contentType +
                ", Base=" + base + ", Jitter=" + jitter +
                ", Final Grid=" + xl + "x" + yl);

        return new int[]{xl, yl};
    }



}