package com.hcb.controller;

import com.hcb.model.dto.RegisterRequest;
import com.hcb.model.entity.User;
import com.hcb.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("GET /login and GET /register load successfully")
    void testAuthPagesLoad() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Welcome Back")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Join HCB")));
    }

    @Test
    @DisplayName("POST /register successfully registers a new customer")
    void testSuccessfulRegistration() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Priya Sharma")
                .param("email", "priya@example.com")
                .param("mobile", "9811223344")
                .param("password", "chocolate123")
                .param("confirmPassword", "chocolate123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        User user = userService.findByEmail("priya@example.com").orElse(null);
        assertNotNull(user);
        assertEquals("Priya Sharma", user.getFullName());
        assertEquals("9811223344", user.getMobile());
        assertTrue(user.hasRole("ROLE_CUSTOMER"));
        assertFalse(user.hasRole("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("POST /register fails with duplicate email or mobile")
    void testDuplicateRegistration() throws Exception {
        RegisterRequest initial = RegisterRequest.builder()
                .fullName("Original User")
                .email("duplicate@example.com")
                .mobile("9112233445")
                .password("password123")
                .confirmPassword("password123")
                .build();
        userService.register(initial);

        // Try same email
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Another User")
                .param("email", "duplicate@example.com")
                .param("mobile", "9998887776")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());

        // Try same mobile
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Another User")
                .param("email", "newunique@example.com")
                .param("mobile", "9112233445")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Login with valid email and valid mobile number")
    void testLoginFlow() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Login Test User")
                .email("logintest@example.com")
                .mobile("9876543219")
                .password("Secret1234")
                .confirmPassword("Secret1234")
                .build();
        userService.register(request);

        // 1. Login with email
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "logintest@example.com")
                .param("password", "Secret1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 2. Login with mobile
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "9876543219")
                .param("password", "Secret1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 3. Login with wrong password
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "logintest@example.com")
                .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Password recovery and reset flow")
    void testPasswordRecoveryAndReset() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Recovery User")
                .email("recover@example.com")
                .mobile("9123456780")
                .password("OldPass123")
                .confirmPassword("OldPass123")
                .build();
        userService.register(request);

        // Request reset
        mockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("email", "recover@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("resetToken"));

        String token = userService.createPasswordResetToken("recover@example.com");

        // Submit new password
        mockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", token)
                .param("newPassword", "NewPass456")
                .param("confirmPassword", "NewPass456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset=true"));

        // Verify login with new password succeeds
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "recover@example.com")
                .param("password", "NewPass456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Customer cannot access /admin (403 Forbidden)")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void testCustomerCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user accessing /admin is redirected to /login")
    void testAnonymousAccessAdminRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
