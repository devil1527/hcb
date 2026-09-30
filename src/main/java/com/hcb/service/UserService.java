package com.hcb.service;

import com.hcb.model.dto.RegisterRequest;
import com.hcb.model.entity.User;

import java.util.Optional;

public interface UserService {

    User register(RegisterRequest request);

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    String createPasswordResetToken(String email);

    boolean validatePasswordResetToken(String token);

    void resetPassword(String token, String newPassword);
}
