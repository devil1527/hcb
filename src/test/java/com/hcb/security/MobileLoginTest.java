package com.hcb.security;

import com.hcb.model.entity.User;
import com.hcb.model.entity.UserRole;
import com.hcb.model.enums.RoleType;
import com.hcb.repository.UserRepository;
import com.hcb.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MobileLoginTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("testcustomer@example.com")
                .mobile("8871921212")
                .fullName("Test Customer")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .active(true)
                .build());

        UserRole role = userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(RoleType.ROLE_CUSTOMER.name())
                .build());
        user.getRoles().add(role);
    }

    @Test
    @DisplayName("Login with exact email")
    void testLoginWithEmail() {
        UserDetails details = userDetailsService.loadUserByUsername("testcustomer@example.com");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with standard 10-digit mobile")
    void testLoginWithStandardMobile() {
        UserDetails details = userDetailsService.loadUserByUsername("8871921212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with +91 country code")
    void testLoginWithPlus91Mobile() {
        UserDetails details = userDetailsService.loadUserByUsername("+918871921212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with +91 and spaces")
    void testLoginWithPlus91AndSpacesMobile() {
        UserDetails details = userDetailsService.loadUserByUsername("+91 8871921212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with 91 prefix")
    void testLoginWith91PrefixMobile() {
        UserDetails details = userDetailsService.loadUserByUsername("918871921212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with leading 0")
    void testLoginWithZeroPrefixMobile() {
        UserDetails details = userDetailsService.loadUserByUsername("08871921212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }

    @Test
    @DisplayName("Login with hyphens and spaces")
    void testLoginWithHyphensAndSpacesMobile() {
        UserDetails details = userDetailsService.loadUserByUsername("88719-21212");
        assertNotNull(details);
        assertEquals("testcustomer@example.com", details.getUsername());
    }
}
