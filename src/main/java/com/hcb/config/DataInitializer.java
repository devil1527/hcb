package com.hcb.config;

import com.hcb.model.entity.User;
import com.hcb.model.entity.UserRole;
import com.hcb.repository.UserRepository;
import com.hcb.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Ensures the default administrator account automatically exists on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "ashwin@ideaai.in";
        Optional<User> adminOpt = userRepository.findByEmail(adminEmail);

        User admin;
        if (adminOpt.isEmpty()) {
            log.info("Creating default administrator account: {}", adminEmail);
            admin = User.builder()
                    .email(adminEmail)
                    .mobile("9999999999")
                    .fullName("Ashwin Golani")
                    .passwordHash(passwordEncoder.encode("changeme123"))
                    .emailVerified(true)
                    .active(true)
                    .build();
            admin = userRepository.save(admin);
            log.info("Created administrator user with ID: {}", admin.getId());
        } else {
            admin = adminOpt.get();
        }

        // Verify and ensure ROLE_ADMIN role is attached
        List<UserRole> existingRoles = userRoleRepository.findByUserId(admin.getId());
        boolean hasAdminRole = existingRoles.stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRole()));

        if (!hasAdminRole) {
            log.info("Assigning ROLE_ADMIN to user: {}", adminEmail);
            UserRole adminRole = UserRole.builder()
                    .user(admin)
                    .role("ROLE_ADMIN")
                    .build();
            userRoleRepository.save(adminRole);
            log.info("ROLE_ADMIN assigned successfully.");
        }
    }
}
