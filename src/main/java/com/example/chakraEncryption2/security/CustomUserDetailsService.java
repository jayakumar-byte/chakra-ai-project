package com.example.chakraEncryption2.security;

import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Check for Constant Admin
        if ("admin@chakra.com".equals(email)) {
            return org.springframework.security.core.userdetails.User.withUsername("admin@chakra.com")
                    .password(new BCryptPasswordEncoder().encode("admin123"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    .build();
        }

        // 2. Check for Normal User in Database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // .enabled() method resolve aagala-na, indha direct Constructor logic use pannunga macha:
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),      // Enabled (True-na login aagum)
                true,                  // Account Non-Expired
                true,                  // Credentials Non-Expired
                true,                  // Account Non-Locked
                Collections.singleton(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}