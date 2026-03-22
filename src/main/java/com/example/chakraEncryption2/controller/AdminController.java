package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.*;
import com.example.chakraEncryption2.repository.*;
import com.example.chakraEncryption2.service.ChakraCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/api/chakra/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private FileRepository fileRepo;
    @Autowired private AnomalyRepository anomalyRepo;
    @Autowired private ChakraCoreService chakraService;

    // 📊 1. ADMIN DASHBOARD (Security Logs - Enna aanalum delete aagadhu)
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        long totalUsers = userRepo.count();
        long totalFiles = fileRepo.count();
        long redAlerts = anomalyRepo.countByRedAlertTrue();
        long yellowAlerts = anomalyRepo.countByYellowAlertTrue();
        long totalThreats = redAlerts + yellowAlerts;

        // Inga namma dismiss aana records-aiyum sethu thaan kaatuvom (Full Audit Trail)
        List<Anomaly> allAnomalies = anomalyRepo.findAllByOrderByTimestampDesc();
        List<User> allUsers = userRepo.findAll();

        model.addAttribute("userCount", totalUsers);
        model.addAttribute("fileCount", totalFiles);
        model.addAttribute("detectedCount", totalThreats);
        model.addAttribute("redCount", redAlerts);
        model.addAttribute("yellowCount", yellowAlerts);
        model.addAttribute("anomalies", allAnomalies);
        model.addAttribute("users", allUsers);

        return "admin_dashboard";
    }

    // 🚫 2. TOGGLE BLOCK (Same logic)
    @PostMapping("/user/toggle-block/{id}")
    public String toggleUserBlock(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepo.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepo.save(user);
        String status = user.isEnabled() ? "Permanently Unblocked" : "Permanently Blocked";
        ra.addFlashAttribute("success", "User " + user.getEmail() + " has been " + status + "!");
        return "redirect:/api/chakra/admin/dashboard";
    }

    // 📩 3. USER REQUESTS (Filter applied: vanish logic inga thaan work aagum)
    @GetMapping("/requests")
    public String viewReports(Model model) {
        // Repository-la namma create panna puthu method-ah use panrom
        List<Anomaly> reports = anomalyRepo.findByReportedTrueAndDismissedInReportFalse();
        model.addAttribute("reportedAnomalies", reports);
        return "admin_requests";
    }

    // 🛑 4. BLOCK BY EMAIL (Same logic)
    @PostMapping("/user/block-by-email")
    public String blockUserByEmail(@RequestParam("email") String email, RedirectAttributes ra) {
        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null) {
            user.setEnabled(false);
            userRepo.save(user);
            ra.addFlashAttribute("success", "User " + email + " has been permanently blocked!");
        } else {
            ra.addFlashAttribute("error", "User not found!");
        }
        return "redirect:/api/chakra/admin/requests";
    }

    // ✅ 5. DISMISS REPORT (Soft Dismiss - Vanish from requests ONLY)
    @PostMapping("/request/dismiss/{id}")
    public String dismissReport(@PathVariable Long id, RedirectAttributes ra) {
        // deleteById-ku badhula ippo update panrom
        Anomaly anomaly = anomalyRepo.findById(id).orElseThrow();

        // Flag-ah true panrom, so requests-la thiriyaadhu
        anomaly.setDismissedInReport(true);
        anomalyRepo.save(anomaly);

        ra.addFlashAttribute("success", "Report dismissed from requests. Log saved in Security Dashboard.");
        return "redirect:/api/chakra/admin/requests";
    }
}