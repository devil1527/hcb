package com.hcb.service;

import com.hcb.model.dto.CheckoutForm;
import com.hcb.model.entity.Order;
import com.hcb.model.entity.User;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(CheckoutForm form, User user);

    Optional<Order> getOrderById(Long id);

    Optional<Order> getOrderByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyToken(String token);

    List<Order> getOrdersByUser(Long userId);

    Page<Order> getOrdersByUser(Long userId, Pageable pageable);

    Page<Order> searchOrders(String query, OrderStatus status, PaymentStatus paymentStatus, Pageable pageable);

    Order updateOrderStatus(Long orderId, OrderStatus newStatus, String adminNotes, Long adminUserId);

    Order verifyPayment(Long orderId, String notes, Long adminUserId);

    Order rejectPayment(Long orderId, String notes, Long adminUserId);

    Order cancelOrder(Long orderId, String reason, Long cancelledByUserId);
}
