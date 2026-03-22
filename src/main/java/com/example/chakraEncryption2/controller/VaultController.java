package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.FileRepository;
import com.example.chakraEncryption2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/api/chakra")
public class VaultController {


    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;


    @GetMapping("/history")
    public String showHistory(Model model, Authentication auth) {
        User currentUser = userRepo.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("files", fileRepo.findByOwnerAndHiddenFromUserFalse(currentUser));
        return "history_page";
    }

    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        EncryptedFile ef = fileRepo.findById(id).orElseThrow();
        User currentUser = userRepo.findByEmail(auth.getName()).orElseThrow();

        if (ef.getOwner().getId().equals(currentUser.getId())) {
            fileRepo.delete(ef);
            ra.addFlashAttribute("message", "File deleted successfully!");
        } else {
            ra.addFlashAttribute("error", "Unauthorized!");
        }
        return "redirect:/api/chakra/history";
    }
}
