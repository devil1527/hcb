package com.hcb.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcb.model.dto.CheckoutForm;
import com.hcb.model.dto.CheckoutItemDto;
import com.hcb.model.entity.Order;
import com.hcb.model.entity.OrderItem;
import com.hcb.model.entity.Product;
import com.hcb.model.entity.User;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.repository.OrderItemRepository;
import com.hcb.repository.OrderRepository;
import com.hcb.repository.ProductRepository;
import com.hcb.repository.UserRepository;
import com.hcb.service.AuditService;
import com.hcb.service.OrderService;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SettingService settingService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final com.hcb.service.DeliveryService deliveryService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order createOrder(CheckoutForm form, User user) {
        // Validate Serviceable Pincode
        if (!deliveryService.isPincodeDeliverable(form.getDeliveryPincode())) {
            throw new IllegalArgumentException(deliveryService.getPincodeErrorMessage(form.getDeliveryPincode()));
        }

        // 1. Idempotency Check
        String idempotencyToken = form.getIdempotencyToken() != null ? form.getIdempotencyToken().trim() : null;
        if (idempotencyToken != null && !idempotencyToken.isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyToken(idempotencyToken);
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        // 2. Parse Items if passed as JSON string
        List<CheckoutItemDto> items = resolveItems(form);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot create an order with an empty cart.");
        }

        // 3. Pessimistic Write Lock on all ordered products
        List<Long> productIds = items.stream()
                .map(CheckoutItemDto::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findByIdInWithLock(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. Validate Product Status and Available Stock
        for (CheckoutItemDto item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new IllegalStateException("Product not found with ID: " + item.getProductId());
            }
            if (!product.isActive() || !product.isAvailable()) {
                throw new IllegalStateException("Product '" + product.getName() + "' is currently unavailable.");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for '" + product.getName() + 
                        "'. Requested: " + item.getQuantity() + ", Available: " + product.getStockQuantity());
            }
        }

        // 5. Authoritative Server-Side Calculation (Never trust client pricing)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CheckoutItemDto item : items) {
            Product product = productMap.get(item.getProductId());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal defaultShipping = settingService.getBigDecimalSetting("shipping_fee_default", new BigDecimal("100.00"));
        BigDecimal freeShippingThreshold = settingService.getBigDecimalSetting("free_shipping_threshold", new BigDecimal("499.00"));
        BigDecimal shippingFee = (subtotal.compareTo(freeShippingThreshold) >= 0) ? BigDecimal.ZERO : defaultShipping;
        BigDecimal totalAmount = subtotal.add(shippingFee);

        // 6. Build Order Entity
        Order order = Order.builder()
                .user(user)
                .idempotencyToken(idempotencyToken)
                .customerName(form.getCustomerName().trim())
                .customerEmail(form.getCustomerEmail().trim().toLowerCase())
                .customerMobile(form.getCustomerMobile().trim())
                .deliveryAddress(form.getDeliveryAddress().trim())
                .deliveryCity(form.getDeliveryCity().trim())
                .deliveryState(form.getDeliveryState().trim())
                .deliveryPincode(form.getDeliveryPincode().trim())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .status(OrderStatus.NEW)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod("UPI")
                .build();

        Order savedOrder = orderRepository.saveAndFlush(order);

        // 7. Concurrency-Safe Order Number generation from Database ID
        savedOrder.setOrderNumber("HCB-" + (1000 + savedOrder.getId()));

        // 8. Snapshot Order Items
        for (CheckoutItemDto item : items) {
            Product product = productMap.get(item.getProductId());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .productSku(product.getSku())
                    .unitPrice(product.getPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .build();

            savedOrder.addItem(orderItem);
        }

        savedOrder = orderRepository.saveAndFlush(savedOrder);

        // 9. Atomic Stock Decrement
        for (CheckoutItemDto item : items) {
            int updated = productRepository.decrementStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                throw new IllegalStateException("Failed to decrement stock for product ID: " + item.getProductId());
            }
        }

        // 10. Audit Logging
        auditService.log("ORDER_CREATED", "ORDER", savedOrder.getId(), null,
                "{\"orderNumber\":\"" + savedOrder.getOrderNumber() + "\",\"total\":" + totalAmount + "}",
                user != null ? user.getId() : null);

        return savedOrder;
    }

    private List<CheckoutItemDto> resolveItems(CheckoutForm form) {
        if (form.getItems() != null && !form.getItems().isEmpty()) {
            return form.getItems();
        }

        if (form.getCartData() != null && !form.getCartData().trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(form.getCartData());
                List<CheckoutItemDto> parsed = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Long pid = node.has("productId") ? node.get("productId").asLong() : null;
                        int qty = node.has("qty") ? node.get("qty").asInt(1) : 1;
                        if (pid != null && qty > 0) {
                            parsed.add(new CheckoutItemDto(pid, qty));
                        }
                    }
                }
                return parsed;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid cart data format.");
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Override
    public Optional<Order> findByIdempotencyToken(String token) {
        return orderRepository.findByIdempotencyToken(token);
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<Order> searchOrders(String query, OrderStatus status, PaymentStatus paymentStatus, Pageable pageable) {
        return orderRepository.searchOrders(query, status, paymentStatus, pageable);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String adminNotes, Long adminUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            order.setAdminNotes(adminNotes.trim());
        }

        if (newStatus == OrderStatus.DISPATCHED && order.getDispatchedAt() == null) {
            order.setDispatchedAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);

        auditService.log("ORDER_STATUS_CHANGED", "ORDER", saved.getId(),
                "{\"status\":\"" + oldStatus + "\"}",
                "{\"status\":\"" + newStatus + "\"}",
                adminUserId);

        return saved;
    }

    @Override
    @Transactional
    public Order verifyPayment(Long orderId, String notes, Long adminUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        User admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();

        order.setPaymentStatus(PaymentStatus.VERIFIED);
        order.setStatus(OrderStatus.PAYMENT_VERIFIED);
        order.setPaymentVerifiedBy(admin);
        order.setPaymentVerifiedAt(LocalDateTime.now());
        if (notes != null) {
            order.setPaymentNotes(notes.trim());
        }

        Order saved = orderRepository.save(order);

        auditService.log("PAYMENT_VERIFIED", "ORDER", saved.getId(),
                "{\"paymentStatus\":\"" + oldPaymentStatus + "\"}",
                "{\"paymentStatus\":\"VERIFIED\"}",
                adminUserId);

        return saved;
    }

    @Override
    @Transactional
    public Order rejectPayment(Long orderId, String notes, Long adminUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(PaymentStatus.REJECTED);
        order.setStatus(OrderStatus.PAYMENT_REJECTED);
        if (notes != null) {
            order.setPaymentNotes(notes.trim());
        }

        Order saved = orderRepository.save(order);

        auditService.log("PAYMENT_REJECTED", "ORDER", saved.getId(),
                "{\"paymentStatus\":\"" + oldPaymentStatus + "\"}",
                "{\"paymentStatus\":\"REJECTED\"}",
                adminUserId);

        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, String reason, Long cancelledByUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        Order saved = orderRepository.save(order);

        auditService.log("ORDER_CANCELLED", "ORDER", saved.getId(),
                null,
                "{\"reason\":\"" + reason + "\"}",
                cancelledByUserId);

        return saved;
    }
}
