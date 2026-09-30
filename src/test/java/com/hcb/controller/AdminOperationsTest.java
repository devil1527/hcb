package com.hcb.controller;

import com.hcb.model.entity.Order;
import com.hcb.model.entity.OrderItem;
import com.hcb.model.entity.Product;
import com.hcb.model.entity.User;
import com.hcb.model.entity.UserRole;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.model.enums.RoleType;
import com.hcb.repository.*;
import com.hcb.security.HcbUserDetails;
import com.hcb.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AdminOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SettingService settingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User customerUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        // Admin User
        adminUser = userRepository.save(User.builder()
                .email("admin@ideaai.in")
                .mobile("9876543201")
                .fullName("Ashwin Admin")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .active(true)
                .build());
        UserRole adminRole = userRoleRepository.save(UserRole.builder().user(adminUser).role(RoleType.ROLE_ADMIN.name()).build());
        adminUser.getRoles().add(adminRole);

        // Customer User
        customerUser = userRepository.save(User.builder()
                .email("cust@example.com")
                .mobile("9876543202")
                .fullName("Customer Test")
                .passwordHash(passwordEncoder.encode("CustPass123!"))
                .active(true)
                .build());
        UserRole custRole = userRoleRepository.save(UserRole.builder().user(customerUser).role(RoleType.ROLE_CUSTOMER.name()).build());
        customerUser.getRoles().add(custRole);

        // Product with stock = 10
        testProduct = productRepository.save(Product.builder()
                .name("Almond Crunch Deluxe")
                .slug("almond-crunch-deluxe")
                .description("Rich dark chocolate roasted almond bar.")
                .price(new BigDecimal("199.00"))
                .stockQuantity(10)
                .lowStockThreshold(5)
                .active(true)
                .available(true)
                .build());

        // Order
        testOrder = Order.builder()
                .user(customerUser)
                .orderNumber("HCB-1001")
                .customerName(customerUser.getFullName())
                .customerEmail(customerUser.getEmail())
                .customerMobile(customerUser.getMobile())
                .deliveryAddress("402 Royal Palms")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .subtotal(new BigDecimal("398.00"))
                .shippingFee(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("498.00"))
                .status(OrderStatus.NEW)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyToken(UUID.randomUUID().toString())
                .build();
        testOrder = orderRepository.save(testOrder);

        OrderItem item = OrderItem.builder()
                .order(testOrder)
                .product(testProduct)
                .productName(testProduct.getName())
                .productSlug(testProduct.getSlug())
                .productSku(testProduct.getSku())
                .unitPrice(testProduct.getPrice())
                .quantity(2)
                .lineTotal(new BigDecimal("398.00"))
                .build();
        item = orderItemRepository.save(item);
        testOrder.getItems().add(item);
    }

    @Test
    @DisplayName("Admin dashboard requires ROLE_ADMIN; Customer receives 403 Forbidden")
    void testAdminSecurity() throws Exception {
        // Anonymous -> Redirect to login
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Customer -> 403 Forbidden
        mockMvc.perform(get("/admin")
                        .with(user(new HcbUserDetails(customerUser))))
                .andExpect(status().isForbidden());

        // Admin -> 200 OK
        mockMvc.perform(get("/admin")
                        .with(user(new HcbUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Dashboard Overview")))
                .andExpect(content().string(containsString("Total Orders")));
    }

    @Test
    @DisplayName("Admin can search and list orders")
    void testAdminOrdersList() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .with(user(new HcbUserDetails(adminUser)))
                        .param("query", "HCB-1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders/list"))
                .andExpect(model().attributeExists("ordersPage"))
                .andExpect(content().string(containsString("HCB-1001")))
                .andExpect(content().string(containsString("Customer Test")));
    }

    @Test
    @DisplayName("Admin can view order details")
    void testAdminOrderDetail() throws Exception {
        mockMvc.perform(get("/admin/orders/" + testOrder.getId())
                        .with(user(new HcbUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders/detail"))
                .andExpect(content().string(containsString("HCB-1001")))
                .andExpect(content().string(containsString("Almond Crunch Deluxe")));
    }

    @Test
    @DisplayName("Admin verifies payment -> PaymentStatus becomes VERIFIED, OrderStatus becomes PAYMENT_VERIFIED")
    void testAdminVerifyPayment() throws Exception {
        mockMvc.perform(post("/admin/orders/" + testOrder.getId() + "/verify-payment")
                        .with(csrf())
                        .with(user(new HcbUserDetails(adminUser)))
                        .param("notes", "Payment verified via Paytm UTR 982371982739"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/" + testOrder.getId()));

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(PaymentStatus.VERIFIED, updated.getPaymentStatus());
        assertEquals(OrderStatus.PAYMENT_VERIFIED, updated.getStatus());
        assertTrue(updated.getPaymentNotes().contains("Paytm UTR"));
    }

    @Test
    @DisplayName("Admin rejects payment -> PaymentStatus becomes FAILED, OrderStatus becomes PAYMENT_REJECTED")
    void testAdminRejectPayment() throws Exception {
        mockMvc.perform(post("/admin/orders/" + testOrder.getId() + "/reject-payment")
                        .with(csrf())
                        .with(user(new HcbUserDetails(adminUser)))
                        .param("notes", "Invalid screenshot submitted"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/" + testOrder.getId()));

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(PaymentStatus.REJECTED, updated.getPaymentStatus());
        assertEquals(OrderStatus.PAYMENT_REJECTED, updated.getStatus());
    }

    @Test
    @DisplayName("Admin cancels order -> Status becomes CANCELLED and stock is restored")
    void testAdminCancelOrderRestoresStock() throws Exception {
        // Initial product stock was 10.
        mockMvc.perform(post("/admin/orders/" + testOrder.getId() + "/update-status")
                        .with(csrf())
                        .with(user(new HcbUserDetails(adminUser)))
                        .param("status", "CANCELLED")
                        .param("notes", "Customer requested cancellation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/" + testOrder.getId()));

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updated.getStatus());

        // Ordered item qty was 2; stock must be restored from 10 -> 12
        Product restoredProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(12, restoredProduct.getStockQuantity());
    }

    @Test
    @DisplayName("Admin can view and update system settings")
    void testAdminSettings() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .with(user(new HcbUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/list"))
                .andExpect(content().string(containsString("Store Configuration")));

        mockMvc.perform(post("/admin/settings")
                        .with(csrf())
                        .with(user(new HcbUserDetails(adminUser)))
                        .param("free_shipping_threshold", "599")
                        .param("shipping_fee_default", "120")
                        .param("upi_id", "newvpa@icici")
                        .param("customer_registration_enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));

        assertEquals(599, settingService.getIntSetting("free_shipping_threshold", 499));
        assertEquals(120, settingService.getIntSetting("shipping_fee_default", 100));
        assertEquals("newvpa@icici", settingService.getSetting("upi_id", ""));
    }

    @Test
    @DisplayName("Admin can view immutable audit logs")
    void testAdminAuditLogs() throws Exception {
        mockMvc.perform(get("/admin/audit")
                        .with(user(new HcbUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit/list"))
                .andExpect(content().string(containsString("System Audit Logs")));
    }
}
