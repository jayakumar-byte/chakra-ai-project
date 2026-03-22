package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.Anomaly;
import com.example.chakraEncryption2.repository.AnomalyRepository;
import com.example.chakraEncryption2.service.ChakraCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/chakra")
public class AnomalyController {

    @Autowired private AnomalyRepository anomalyRepo;
    @Autowired private ChakraCoreService chakraService;

    @PostMapping("/report-threat/{anomalyId}")
    public String reportThreat(@PathVariable Long anomalyId, RedirectAttributes ra) {
        try {
            // 1. Database-la flag-ah update panrom
            Anomaly anomaly = anomalyRepo.findById(anomalyId).orElseThrow();
            anomaly.setReported(true);
            anomalyRepo.save(anomaly); // <--- DB update happens here

            // 2. Triggers Email & Other logic in service
            chakraService.processReport(anomalyId);

            ra.addFlashAttribute("message", "✅ Threat reported! Admin has been notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Reporting failed: " + e.getMessage());
        }
        return "redirect:/api/chakra/dashboard";
    }

    @GetMapping("/view-attempt/{id}")
    @ResponseBody
    public java.util.Map<String, Object> viewAttempt(@PathVariable Long id) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        anomalyRepo.findById(id).ifPresentOrElse(anomaly -> {
            response.put("suspectEmail", anomaly.getAttemptedBy());
            response.put("fileName", anomaly.getFilename());
            response.put("time", anomaly.getTimestamp().toString());
            response.put("reported", anomaly.isReported()); // Flag-ahum anupuvom to toggle UI
        }, () -> {
            response.put("error", "Details not found");
        });
        return response;
    }
}