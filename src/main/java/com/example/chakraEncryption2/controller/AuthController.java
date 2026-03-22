package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // LOGIN PAGE logic with custom error message
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            // Requirement 2: Login fail aana indha message pogum
            model.addAttribute("loginError", "Either E-mail Or Password Is Wrong");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        // model-la "user" attribute illa-na register page error varum, so safe-aa check pannuvom
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        // Service call panni result-ah vaangurom (Email check & Password check inga nadakkum)
        String result = userService.saveUser(user);

        if (result.equals("SUCCESS")) {
            return "redirect:/login?success";
        } else {
            // Requirement 1 & 3: Error message-ah thirumba register page-ku anupurom
            redirectAttributes.addFlashAttribute("errorMessage", result);
            redirectAttributes.addFlashAttribute("user", user); // Enter panna data-ah thirumba anupurom
            return "redirect:/register";
        }
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin_dashboard";
    }
}