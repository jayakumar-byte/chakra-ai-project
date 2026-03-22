package com.example.chakraEncryption2.service;

import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    // 🛡️ Password Regex: 8 chars, 1 Special, 1 Number, 1 Alphabet
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    public String saveUser(User user) {
        // 1. Password Strength Check (Requirement 3)
        // Regex: 8 chars, 1 Special, 1 Number, 1 Alphabet
        if (!user.getPassword().matches(PASSWORD_PATTERN)) {
            return "Password must consist of 8 letters, one special symbol, one number and one alphabet";
        }

        // 2. Email Existence Check (Requirement 1)
        // Optional handle panna isPresent() dhaan use pannanum
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "Entered E-mail Is Not Available";
        }

        // 3. Success Flow
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            userRepository.save(user);

            sendWelcomeEmail(user.getEmail(), user.getName());
            return "SUCCESS";
        } catch (Exception e) {
            return "Registration failed: " + e.getMessage();
        }
    }

    private void sendWelcomeEmail(String to, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Chakra Encryption! 🛡️");
            message.setText("Hi " + name + ",\n\n" +
                    "Your account has been successfully created. Your files are now protected with Chakra logic.\n" +
                    "Stay Secure,\nChakra Team");
            mailSender.send(message);
            System.out.println(">>> Welcome mail sent to: " + to);
        } catch (Exception e) {
            System.err.println(">>> WELCOME MAIL ERROR: " + e.getMessage());
        }
    }
}