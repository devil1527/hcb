package com.hcb.controller;

import com.hcb.model.dto.ForgotPasswordRequest;
import com.hcb.model.dto.RegisterRequest;
import com.hcb.model.dto.ResetPasswordRequest;
import com.hcb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "reset", required = false) String reset,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email/mobile or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been safely logged out.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! Please log in.");
        }
        if (reset != null) {
            model.addAttribute("successMessage", "Password has been successfully updated. You may now log in.");
        }
        return "auth/login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage(@RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "logout", required = false) String logout,
                                 @RequestParam(value = "accessDenied", required = false) String accessDenied,
                                 Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid administrator credentials. Please check your username/password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out of the Admin Portal.");
        }
        if (accessDenied != null) {
            model.addAttribute("errorMessage", "Access Denied: Administrator privileges are required to access this console.");
        }
        return "admin/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.registerRequest", "Passwords do not match");
        }

        if (userService.existsByEmail(request.getEmail())) {
            bindingResult.rejectValue("email", "error.registerRequest", "An account with this email address already exists");
        }

        if (userService.existsByMobile(request.getMobile())) {
            bindingResult.rejectValue("mobile", "error.registerRequest", "An account with this mobile number already exists");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please log in.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            String token = userService.createPasswordResetToken(request.getEmail());
            // In development / demo, we display the reset link; when SMTP is configured, this also sends email
            model.addAttribute("resetToken", token);
            model.addAttribute("successMessage", "Password reset instructions have been generated. Check your email or use the link below to reset your password.");
        } catch (IllegalArgumentException e) {
            // Protect against email enumeration by showing same message or gentle notice
            model.addAttribute("successMessage", "If an account exists with that email address, password reset instructions have been generated.");
        }

        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        if (token == null || !userService.validatePasswordResetToken(token)) {
            model.addAttribute("errorMessage", "Invalid or expired password reset link. Please request a new one.");
            return "auth/forgot-password";
        }

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        model.addAttribute("resetPasswordRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (!request.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.resetPasswordRequest", "Passwords do not match");
        }

        if (!userService.validatePasswordResetToken(request.getToken())) {
            bindingResult.rejectValue("token", "error.resetPasswordRequest", "Reset token is invalid or expired");
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Your password has been successfully reset! Please log in.");
            return "redirect:/login?reset=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }
}
