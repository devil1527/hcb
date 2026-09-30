package com.hcb.controller.admin;

import com.hcb.model.entity.Order;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.security.HcbUserDetails;
import com.hcb.service.EmailService;
import com.hcb.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final EmailService emailService;

    @GetMapping
    public String listOrders(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "status", required = false) OrderStatus status,
                             @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "15") int size,
                             Model model) {
        Page<Order> ordersPage = orderService.searchOrders(query, status, paymentStatus, PageRequest.of(page, size));

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());

        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + id));

        model.addAttribute("order", order);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/verify-payment")
    public String verifyPayment(@PathVariable("id") Long id,
                                @RequestParam(value = "notes", required = false) String notes,
                                @AuthenticationPrincipal HcbUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        Long adminId = userDetails != null ? userDetails.getId() : null;
        Order order = orderService.verifyPayment(id, notes, adminId);
        emailService.sendPaymentVerifiedNotification(order);

        redirectAttributes.addFlashAttribute("successMessage", "Payment has been successfully verified for order " + order.getOrderNumber());
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/reject-payment")
    public String rejectPayment(@PathVariable("id") Long id,
                                @RequestParam(value = "notes", required = false) String notes,
                                @AuthenticationPrincipal HcbUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        Long adminId = userDetails != null ? userDetails.getId() : null;
        Order order = orderService.rejectPayment(id, notes, adminId);
        emailService.sendPaymentRejectedNotification(order, notes);

        redirectAttributes.addFlashAttribute("successMessage", "Payment marked as rejected for order " + order.getOrderNumber());
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OrderStatus newStatus,
                               @RequestParam(value = "notes", required = false) String notes,
                               @AuthenticationPrincipal HcbUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Long adminId = userDetails != null ? userDetails.getId() : null;
        Order order;
        if (newStatus == OrderStatus.CANCELLED) {
            order = orderService.cancelOrder(id, notes, adminId);
        } else {
            order = orderService.updateOrderStatus(id, newStatus, notes, adminId);
        }

        emailService.sendOrderStatusUpdateNotification(order);

        redirectAttributes.addFlashAttribute("successMessage", "Order status successfully updated to " + newStatus.getDisplayName());
        return "redirect:/admin/orders/" + id;
    }
}
