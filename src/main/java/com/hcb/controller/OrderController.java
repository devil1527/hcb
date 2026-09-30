package com.hcb.controller;

import com.hcb.model.dto.CheckoutForm;
import com.hcb.model.entity.Order;
import com.hcb.model.entity.User;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.security.HcbUserDetails;
import com.hcb.service.DeliveryService;
import com.hcb.service.EmailService;
import com.hcb.service.OrderService;
import com.hcb.service.SettingService;
import com.hcb.service.UserService;
import com.hcb.util.UpiQrUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final SettingService settingService;
    private final EmailService emailService;
    private final DeliveryService deliveryService;

    @GetMapping("/checkout")
    public String checkoutPage(Model model,
                               HttpSession session,
                               @AuthenticationPrincipal HcbUserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin?error=admin_cannot_order";
        }

        String token = UUID.randomUUID().toString();
        session.setAttribute("checkout_idempotency_token", token);

        CheckoutForm form = CheckoutForm.builder()
                .idempotencyToken(token)
                .build();

        if (userDetails != null) {
            form.setCustomerName(userDetails.getFullName());
            form.setCustomerEmail(userDetails.getEmail());
            form.setCustomerMobile(userDetails.getMobile());
        }

        model.addAttribute("checkoutForm", form);
        model.addAttribute("freeShippingThreshold", settingService.getIntSetting("free_shipping_threshold", 499));
        model.addAttribute("defaultShippingFee", settingService.getIntSetting("shipping_fee_default", 100));
        model.addAttribute("pincodeRestrictionEnabled", deliveryService.isRestrictionEnabled());
        model.addAttribute("deliveryMinPincode", deliveryService.getMinPincode());
        model.addAttribute("deliveryMaxPincode", deliveryService.getMaxPincode());
        model.addAttribute("deliveryRegionName", deliveryService.getDeliveryRegionName());
        return "cart/checkout";
    }

    @PostMapping("/checkout")
    public String handleCheckout(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal HcbUserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators cannot place customer orders. Please use a customer account.");
        }

        // Validate Serviceable Pincode
        if (form.getDeliveryPincode() != null && !deliveryService.isPincodeDeliverable(form.getDeliveryPincode())) {
            bindingResult.rejectValue("deliveryPincode", "error.deliveryPincode", deliveryService.getPincodeErrorMessage(form.getDeliveryPincode()));
        }

        // Validate Idempotency Token
        String sessionToken = (String) session.getAttribute("checkout_idempotency_token");
        String formToken = form.getIdempotencyToken();

        // Check if an order was already created with this idempotency token
        if (formToken != null && !formToken.trim().isEmpty()) {
            Optional<Order> existingOrder = orderService.findByIdempotencyToken(formToken.trim());
            if (existingOrder.isPresent()) {
                session.removeAttribute("checkout_idempotency_token");
                return "redirect:/orders/" + existingOrder.get().getId();
            }
        }

        if (sessionToken == null || !sessionToken.equals(formToken)) {
            bindingResult.reject("error.checkoutForm", "Your checkout session expired or was already submitted. Please review your cart.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("freeShippingThreshold", settingService.getIntSetting("free_shipping_threshold", 499));
            model.addAttribute("defaultShippingFee", settingService.getIntSetting("shipping_fee_default", 100));
            model.addAttribute("pincodeRestrictionEnabled", deliveryService.isRestrictionEnabled());
            model.addAttribute("deliveryMinPincode", deliveryService.getMinPincode());
            model.addAttribute("deliveryMaxPincode", deliveryService.getMaxPincode());
            model.addAttribute("deliveryRegionName", deliveryService.getDeliveryRegionName());
            return "cart/checkout";
        }

        // Invalidate session idempotency token to prevent double submissions
        session.removeAttribute("checkout_idempotency_token");

        User user = null;
        if (userDetails != null) {
            user = userService.findById(userDetails.getId()).orElse(null);
        }

        try {
            Order order = orderService.createOrder(form, user);
            emailService.sendOrderPlacedNotification(order);
            emailService.sendAdminNewOrderNotification(order);
            redirectAttributes.addFlashAttribute("orderPlacedSuccess", true);
            return "redirect:/orders/" + order.getId();
        } catch (Exception e) {
            // Restore token if transaction failed before database commit
            session.setAttribute("checkout_idempotency_token", formToken);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("freeShippingThreshold", settingService.getIntSetting("free_shipping_threshold", 499));
            model.addAttribute("defaultShippingFee", settingService.getIntSetting("shipping_fee_default", 100));
            model.addAttribute("pincodeRestrictionEnabled", deliveryService.isRestrictionEnabled());
            model.addAttribute("deliveryMinPincode", deliveryService.getMinPincode());
            model.addAttribute("deliveryMaxPincode", deliveryService.getMaxPincode());
            model.addAttribute("deliveryRegionName", deliveryService.getDeliveryRegionName());
            return "cart/checkout";
        }
    }

    @GetMapping("/orders")
    public String myOrders(Model model, @AuthenticationPrincipal HcbUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/orders";
        }

        List<Order> orders = orderService.getOrdersByUser(userDetails.getId());
        model.addAttribute("orders", orders);
        return "customer/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Long id,
                              Model model,
                              @AuthenticationPrincipal HcbUserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/orders/" + id;
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // IDOR Protection: Registered user orders require authentication and owner/admin check
        if (order.getUser() != null) {
            if (userDetails == null) {
                return "redirect:/login";
            }
            if (!order.getUser().getId().equals(userDetails.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to order details");
            }
        } else {
            if (userDetails == null) {
                return "redirect:/login";
            }
            boolean isEmailOwner = userDetails.getEmail() != null && userDetails.getEmail().equalsIgnoreCase(order.getCustomerEmail());
            boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isEmailOwner && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to order details");
            }
        }

        model.addAttribute("order", order);

        // Generate UPI QR Code and payment details if payment is PENDING
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            String upiId = settingService.getSetting("upi_id", "healthychocobytes@paytm");
            String upiName = settingService.getSetting("upi_name", "Healthy Choco Bytes");
            String paymentNumber = settingService.getSetting("payment_number", "918871921212");
            String customQrImage = settingService.getSetting("qr_code_image", "");

            String upiUrl = UpiQrUtil.buildUpiUrl(upiId, upiName, order.getTotalAmount(), order.getOrderNumber());
            String qrCodeBase64 = UpiQrUtil.generateQrBase64(upiUrl, 260, 260);

            model.addAttribute("upiId", upiId);
            model.addAttribute("upiName", upiName);
            model.addAttribute("paymentNumber", paymentNumber);
            model.addAttribute("customQrImage", customQrImage);
            model.addAttribute("upiUrl", upiUrl);
            model.addAttribute("qrCodeBase64", qrCodeBase64);

            // WhatsApp verification message
            String whatsappNumber = settingService.getSetting("whatsapp_number", "918871921212");
            String message = "Hello Healthy Choco Bytes, I have completed the payment of ₹" +
                    order.getTotalAmount() + " for Order " + order.getOrderNumber() +
                    ". Here is my payment screenshot.";
            String whatsappUrl = "https://wa.me/" + whatsappNumber + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            model.addAttribute("whatsappPaymentUrl", whatsappUrl);
        }

        return "customer/order-detail";
    }
}
