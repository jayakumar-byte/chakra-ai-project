package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.Anomaly;
import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/api/chakra")
public class DashboardController {

    @Autowired private UserRepository userRepo;
    @Autowired private AnomalyRepository anomalyRepo;

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication auth) {
        // Current logged-in user details
        User currentUser = userRepo.findByEmail(auth.getName()).orElseThrow();

        // ⭐ Filtered Alerts: Owner-ku verum Unauthorized (Red Alert) events
        // Adhuvum innum report pannaadha (Reported = False) events mattum dhaan pogum
        List<Anomaly> ownerOnlyAlerts = anomalyRepo
                .findByOwnerIdAndRedAlertTrueAndReportedFalseOrderByTimestampDesc(currentUser.getId());

        // Attributes for the dashboard
        model.addAttribute("anomalies", ownerOnlyAlerts);

        // General stats (if needed in UI)
        model.addAttribute("userCount", userRepo.count());

        return "user_dashboard";
    }
}