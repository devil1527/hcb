package com.hcb.service.impl;

import com.hcb.model.dto.RegisterRequest;
import com.hcb.model.entity.PasswordResetToken;
import com.hcb.model.entity.User;
import com.hcb.repository.PasswordResetTokenRepository;
import com.hcb.repository.UserRepository;
import com.hcb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        String cleanMobile = request.getMobile().trim();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }
        if (userRepository.existsByMobile(cleanMobile)) {
            throw new IllegalArgumentException("An account with this mobile number already exists.");
        }
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = User.builder()
                .email(cleanEmail)
                .mobile(cleanMobile)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .emailVerified(false)
                .mobileVerified(false)
                .build();

        user.addRole("ROLE_CUSTOMER");
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public Optional<User> findByMobile(String mobile) {
        if (mobile == null) return Optional.empty();
        return userRepository.findByMobile(mobile.trim());
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return mobile != null && userRepository.existsByMobile(mobile.trim());
    }

    @Override
    @Transactional
    public String createPasswordResetToken(String email) {
        User user = findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email address."));

        // Invalidate any existing unused tokens for this user
        passwordResetTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    passwordResetTokenRepository.save(existing);
                });

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && !t.isExpired())
                .isPresent();
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new IllegalArgumentException("This password reset link has expired or already been used.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
