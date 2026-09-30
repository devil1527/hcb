package com.hcb.security;

import com.hcb.model.entity.User;
import com.hcb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username/Email/Mobile cannot be empty");
        }

        String raw = username.trim();
        String emailIdentifier = raw.toLowerCase();

        // 1. Direct lookup by email or exact entered mobile string
        java.util.Optional<User> userOpt = userRepository.findByEmailOrMobile(emailIdentifier, raw);

        // 2. If not found, attempt flexible mobile normalization
        if (userOpt.isEmpty()) {
            String digitsOnly = raw.replaceAll("\\D+", "");
            if (!digitsOnly.isEmpty()) {
                String tenDigit = digitsOnly.length() >= 10
                        ? digitsOnly.substring(digitsOnly.length() - 10)
                        : digitsOnly;

                // Try finding by core 10-digit mobile
                userOpt = userRepository.findByMobile(tenDigit);

                // Try with 91 prefix
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByMobile("91" + tenDigit);
                }

                // Try with +91 prefix
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByMobile("+91" + tenDigit);
                }

                // Try with 0 prefix
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByMobile("0" + tenDigit);
                }

                // Fuzzy match ending in tenDigit
                if (userOpt.isEmpty() && tenDigit.length() >= 7) {
                    userOpt = userRepository.findByMobileFuzzy(tenDigit);
                }
            }
        }

        User user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("No user account found with: " + username));

        return new HcbUserDetails(user);
    }
}
