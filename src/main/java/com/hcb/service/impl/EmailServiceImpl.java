package com.hcb.service.impl;

import com.hcb.model.entity.Order;
import com.hcb.service.EmailService;
import com.hcb.service.SettingService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SettingService settingService;

    @Override
    @Async
    public void sendOrderPlacedNotification(Order order) {
        boolean enabled = settingService.getBooleanSetting("admin_notification_enabled", true);
        if (!enabled) {
            log.info("Email notifications disabled in settings. Skipping order placed email for {}", order.getOrderNumber());
            return;
        }

        String to = order.getCustomerEmail();
        String subject = "Order Confirmation - " + order.getOrderNumber() + " | Healthy Choco Bytes";
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2 style='color: #3E2415;'>Thank You for Your Order!</h2>" +
                "<p>Hi " + escape(order.getCustomerName()) + ",</p>" +
                "<p>We have received your order <strong>" + order.getOrderNumber() + "</strong>. Our kitchen is preparing to handcraft your treats.</p>" +
                "<p><strong>Order Summary:</strong><br>" +
                "Total Amount: ₹" + order.getTotalAmount() + "<br>" +
                "Delivery To: " + escape(order.getDeliveryAddress()) + ", " + escape(order.getDeliveryCity()) + "</p>" +
                "<p>Please complete your payment via UPI and share the screenshot on WhatsApp to verify your order.</p>" +
                "<p>Warm regards,<br>Healthy Choco Bytes Team</p>" +
                "</div>";

        sendHtmlEmail(to, subject, body);
    }

    @Override
    @Async
    public void sendPaymentVerifiedNotification(Order order) {
        String to = order.getCustomerEmail();
        String subject = "Payment Verified - " + order.getOrderNumber() + " | Healthy Choco Bytes";
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2 style='color: #28a745;'>Payment Confirmed!</h2>" +
                "<p>Hi " + escape(order.getCustomerName()) + ",</p>" +
                "<p>Your payment of <strong>₹" + order.getTotalAmount() + "</strong> for order <strong>" + order.getOrderNumber() + "</strong> has been successfully verified.</p>" +
                "<p>Your handmade chocolates are now being freshly prepared and safely packaged.</p>" +
                "<p>Warm regards,<br>Healthy Choco Bytes Team</p>" +
                "</div>";

        sendHtmlEmail(to, subject, body);
    }

    @Override
    @Async
    public void sendPaymentRejectedNotification(Order order, String reason) {
        String to = order.getCustomerEmail();
        String subject = "Payment Update for Order " + order.getOrderNumber() + " | Healthy Choco Bytes";
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2 style='color: #dc3545;'>Payment Verification Update</h2>" +
                "<p>Hi " + escape(order.getCustomerName()) + ",</p>" +
                "<p>We were unable to verify the UPI payment for your order <strong>" + order.getOrderNumber() + "</strong>.</p>" +
                (reason != null && !reason.trim().isEmpty() ? "<p><strong>Reason provided by team:</strong> " + escape(reason) + "</p>" : "") +
                "<p>Please contact our support team on WhatsApp or re-submit your transaction reference so we can prepare your order without delay.</p>" +
                "<p>Warm regards,<br>Healthy Choco Bytes Team</p>" +
                "</div>";

        sendHtmlEmail(to, subject, body);
    }

    @Override
    @Async
    public void sendAdminNewOrderNotification(Order order) {
        boolean enabled = settingService.getBooleanSetting("admin_notification_enabled", true);
        if (!enabled) {
            return;
        }

        String adminEmails = settingService.getSetting("order_notification_emails", "ashwin@ideaai.in");
        String subject = "[NEW ORDER] " + order.getOrderNumber() + " - ₹" + order.getTotalAmount();
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2>New Customer Order Received!</h2>" +
                "<p><strong>Order Number:</strong> " + order.getOrderNumber() + "</p>" +
                "<p><strong>Customer:</strong> " + escape(order.getCustomerName()) + " (" + escape(order.getCustomerMobile()) + ")</p>" +
                "<p><strong>Email:</strong> " + escape(order.getCustomerEmail()) + "</p>" +
                "<p><strong>Delivery City:</strong> " + escape(order.getDeliveryCity()) + "</p>" +
                "<p><strong>Total Amount:</strong> ₹" + order.getTotalAmount() + "</p>" +
                "<p><strong>Payment Method:</strong> " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")</p>" +
                "<p><a href='https://hcb.ideaai.in/admin/orders/" + order.getId() + "' style='background: #3E2415; color: white; padding: 8px 16px; text-decoration: none; border-radius: 4px;'>View in Admin Panel</a></p>" +
                "</div>";

        // Admin email list might contain multiple comma-separated emails
        String[] recipients = adminEmails.split(",");
        for (String recipient : recipients) {
            String cleanEmail = recipient.trim();
            if (!cleanEmail.isEmpty()) {
                sendHtmlEmail(cleanEmail, subject, body);
            }
        }
    }

    @Override
    @Async
    public void sendOrderStatusUpdateNotification(Order order) {
        String to = order.getCustomerEmail();
        String subject = "Order Status Update - " + order.getOrderNumber() + " | Healthy Choco Bytes";
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2 style='color: #3E2415;'>Order Update: " + order.getStatus().getDisplayName() + "</h2>" +
                "<p>Hi " + escape(order.getCustomerName()) + ",</p>" +
                "<p>Your order <strong>" + order.getOrderNumber() + "</strong> status has been updated to: <strong>" + order.getStatus().getDisplayName() + "</strong>.</p>" +
                "<p>Warm regards,<br>Healthy Choco Bytes Team</p>" +
                "</div>";

        sendHtmlEmail(to, subject, body);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Password Reset Request | Healthy Choco Bytes";
        String body = "<div style='font-family: Arial, sans-serif; color: #3E2415; max-width: 600px;'>" +
                "<h2 style='color: #3E2415;'>Password Reset Request</h2>" +
                "<p>We received a request to reset the password for your account.</p>" +
                "<p>Click the link below to set a new password. This link is valid for 1 hour:</p>" +
                "<p><a href='" + resetLink + "' style='background: #3E2415; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Reset My Password</a></p>" +
                "<p>If you did not request this, please disregard this email.</p>" +
                "<p>Warm regards,<br>Healthy Choco Bytes Team</p>" +
                "</div>";

        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("JavaMailSender is not configured. (Simulated email to: '{}' | Subject: '{}')", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            
            String from = settingService.getSetting("business_email", "contact@healthychocobytes.in");
            String senderName = settingService.getSetting("business_name", "Healthy Choco Bytes");
            
            helper.setFrom(from, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Notification email successfully sent to '{}' with subject: '{}'", to, subject);
        } catch (Exception e) {
            // Never fail caller's database transaction due to SMTP connection failure
            log.error("Failed to send email to '{}' (Subject: '{}'): {}", to, subject, e.getMessage());
        }
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
