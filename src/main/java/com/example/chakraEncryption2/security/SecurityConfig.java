package com.example.chakraEncryption2.security;

import com.example.chakraEncryption2.entity.User;
import com.example.chakraEncryption2.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepo;

    public SecurityConfig(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        //for login and sign up
                        .requestMatchers("/register", "/login", "/css/**", "/js/**").permitAll()
                        // for Aadmin access
                        .requestMatchers("/api/chakra/admin/**").hasRole("ADMIN")
                        //for user access
                        .requestMatchers("/api/chakra/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            String email = authentication.getName();

                            // 1. Check if the user is ADMIN (Manual Check)
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                            if (isAdmin) {
                                response.sendRedirect("/api/chakra/admin/dashboard");
                                return;
                            }

                            // 2. For Normal Users, check Block status from DB
                            // Use Optional safe-aa handle panna 'orElse(null)' use panroam
                            User user = userRepo.findByEmail(email).orElse(null);

                            if (user != null && user.isBlocked()) {
                                request.getSession().invalidate();
                                response.sendRedirect("/login?error=suspended");
                                return;
                            }

                            // 3. If everything is fine, go to User Dashboard
                            response.sendRedirect("/api/chakra/dashboard");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}