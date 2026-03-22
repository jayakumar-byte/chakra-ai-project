package com.example.chakraEncryption2.controller;

import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.FilePermission;
import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.FileRepository;
import com.example.chakraEncryption2.repository.PermissionRepository;
import com.example.chakraEncryption2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/api/chakra")
public class ShareController {

    @Autowired private FileRepository fileRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PermissionRepository permissionRepo;


    @GetMapping("/share-management")
    public String shareManagementPage(Model model, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("myFiles", fileRepo.findAllByOwner(user));
        model.addAttribute("username", user.getName());
        return "share_management";
    }

    @PostMapping("/grant-permission/{fileId}")
    public String grantPermission(@PathVariable Long fileId, @RequestParam String email,
                                  Authentication auth, RedirectAttributes ra) {
        EncryptedFile file = fileRepo.findById(fileId).orElseThrow();
        User userToGrant = userRepo.findByEmail(email).orElse(null);
        User grantor = userRepo.findByEmail(auth.getName()).orElseThrow();

        if (userToGrant == null) {
            ra.addFlashAttribute("error", "🚨 User account not found!");
            return "redirect:/api/chakra/share-management";
        }
        if (userToGrant.getId().equals(grantor.getId())) {
            ra.addFlashAttribute("error", "You cannot share with yourself.");
            return "redirect:/api/chakra/share-management";
        }
        if (permissionRepo.existsByFileAndAllowedUser(file, userToGrant)) {
            ra.addFlashAttribute("error", "User already has access.");
            return "redirect:/api/chakra/share-management";
        }

        FilePermission p = new FilePermission();
        p.setFile(file);
        p.setAllowedUser(userToGrant);
        p.setGrantedBy(grantor);
        permissionRepo.save(p);

        ra.addFlashAttribute("success", "Permission granted to " + email);
        return "redirect:/api/chakra/share-management";
    }

    @GetMapping("/file-permissions/{fileId}")
    public String getFilePermissions(@PathVariable Long fileId, Model model) {
        EncryptedFile file = fileRepo.findById(fileId).orElseThrow();
        List<FilePermission> permissions = permissionRepo.findAllByFile(file);
        model.addAttribute("permissions", permissions);
        return "fragments/permission_list :: permissionTable";
    }

    @PostMapping("/revoke-permission/{permissionId}")
    public String revokePermission(@PathVariable Long permissionId, RedirectAttributes ra) {
        permissionRepo.deleteById(permissionId);
        ra.addFlashAttribute("message", "Access revoked successfully!");
        return "redirect:/api/chakra/share-management";
    }
}
