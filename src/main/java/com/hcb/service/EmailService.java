package com.hcb.service;

import com.hcb.model.entity.Order;

public interface EmailService {

    void sendOrderPlacedNotification(Order order);

    void sendPaymentVerifiedNotification(Order order);

    void sendPaymentRejectedNotification(Order order, String reason);

    void sendOrderStatusUpdateNotification(Order order);

    void sendAdminNewOrderNotification(Order order);

    void sendPasswordResetEmail(String toEmail, String resetLink);
}
