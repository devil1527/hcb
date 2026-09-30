package com.hcb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcb.model.entity.Order;
import com.hcb.model.entity.Product;
import com.hcb.model.entity.User;
import com.hcb.model.entity.UserRole;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.model.enums.RoleType;
import com.hcb.repository.OrderRepository;
import com.hcb.repository.ProductRepository;
import com.hcb.repository.UserRepository;
import com.hcb.repository.UserRoleRepository;
import com.hcb.security.HcbUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
class OrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct1;
    private Product testProduct2;
    private User customerA;
    private User customerB;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        // Seed products
        testProduct1 = productRepository.save(Product.builder()
                .name("Almond Rock Bites")
                .slug("almond-rock-bites")
                .description("Handmade roasted almonds in rich dark chocolate.")
                .price(new BigDecimal("299.00"))
                .stockQuantity(25)
                .active(true)
                .available(true)
                .build());

        testProduct2 = productRepository.save(Product.builder()
                .name("Hazelnut Dark Mini")
                .slug("hazelnut-dark-mini")
                .description("Crunchy single portion treat.")
                .price(new BigDecimal("79.00"))
                .stockQuantity(10)
                .active(true)
                .available(true)
                .build());

        // Seed Customer A
        customerA = userRepository.save(User.builder()
                .email("custA@example.com")
                .mobile("9876543201")
                .fullName("Customer Alpha")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .active(true)
                .build());
        UserRole custARole = userRoleRepository.save(UserRole.builder().user(customerA).role(RoleType.ROLE_CUSTOMER.name()).build());
        customerA.getRoles().add(custARole);

        // Seed Customer B
        customerB = userRepository.save(User.builder()
                .email("custB@example.com")
                .mobile("9876543202")
                .fullName("Customer Beta")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .active(true)
                .build());
        UserRole custBRole = userRoleRepository.save(UserRole.builder().user(customerB).role(RoleType.ROLE_CUSTOMER.name()).build());
        customerB.getRoles().add(custBRole);

        // Seed Admin
        adminUser = userRepository.save(User.builder()
                .email("admin@ideaai.in")
                .mobile("9876543203")
                .fullName("Ashwin Admin")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .active(true)
                .build());
        UserRole adminRole = userRoleRepository.save(UserRole.builder().user(adminUser).role(RoleType.ROLE_ADMIN.name()).build());
        adminUser.getRoles().add(adminRole);
    }

    @Test
    @DisplayName("GET /checkout redirects unauthenticated user to login")
    void testCheckoutRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /checkout loads with an idempotency token for authenticated customer")
    void testCheckoutPageLoadsForCustomer() throws Exception {
        MvcResult result = mockMvc.perform(get("/checkout")
                        .with(user(new HcbUserDetails(customerA))))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/checkout"))
                .andExpect(model().attributeExists("checkoutForm"))
                .andReturn();

        String sessionToken = (String) result.getRequest().getSession().getAttribute("checkout_idempotency_token");
        assertNotNull(sessionToken);
        assertFalse(sessionToken.trim().isEmpty());
    }

    @Test
    @DisplayName("POST /checkout creates order, decrements stock atomically, and applies free shipping over ₹499")
    void testSuccessfulCheckout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = UUID.randomUUID().toString();
        session.setAttribute("checkout_idempotency_token", token);

        String cartJson = "[{\"productId\":" + testProduct1.getId() + ",\"qty\":2}]";

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .with(csrf())
                        .with(user(new HcbUserDetails(customerA)))
                        .param("idempotencyToken", token)
                        .param("cartData", cartJson)
                        .param("customerName", "Customer Alpha")
                        .param("customerEmail", "custA@example.com")
                        .param("customerMobile", "9876543201")
                        .param("deliveryAddress", "Flat 101, Chocolate Hills")
                        .param("deliveryCity", "Pune")
                        .param("deliveryState", "Maharashtra")
                        .param("deliveryPincode", "411001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));

        // Verify Order
        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        Order order = orders.get(0);

        assertTrue(order.getOrderNumber().startsWith("HCB-"));
        assertEquals(new BigDecimal("598.00"), order.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), order.getShippingFee().setScale(2)); // Free shipping over 499
        assertEquals(new BigDecimal("598.00"), order.getTotalAmount());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(customerA.getId(), order.getUser().getId());

        // Verify Product stock decremented from 25 to 23
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertEquals(23, updatedProduct.getStockQuantity());
    }

    @Test
    @DisplayName("Subtotal under ₹499 charges default ₹100 shipping fee")
    void testShippingFeeUnderThreshold() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = UUID.randomUUID().toString();
        session.setAttribute("checkout_idempotency_token", token);

        String cartJson = "[{\"productId\":" + testProduct2.getId() + ",\"qty\":1}]";

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .with(csrf())
                        .with(user(new HcbUserDetails(customerA)))
                        .param("idempotencyToken", token)
                        .param("cartData", cartJson)
                        .param("customerName", "Customer Alpha")
                        .param("customerEmail", "custA@example.com")
                        .param("customerMobile", "9876543201")
                        .param("deliveryAddress", "Plot 99, Sweet Street")
                        .param("deliveryCity", "Pune")
                        .param("deliveryState", "Maharashtra")
                        .param("deliveryPincode", "411002"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));

        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        Order order = orders.get(0);

        assertEquals(new BigDecimal("79.00"), order.getSubtotal());
        assertEquals(new BigDecimal("100.00"), order.getShippingFee());
        assertEquals(new BigDecimal("179.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("Reject checkout when delivery pincode is outside serviceable Pune range (411001 – 411090)")
    void testRejectNonServiceablePincode() throws Exception {
        String token = UUID.randomUUID().toString();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("checkout_idempotency_token", token);

        String cartJson = "[{\"id\":" + testProduct1.getId() + ",\"name\":\"Protein Bar\",\"price\":79.00,\"qty\":1}]";

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .with(csrf())
                        .with(user(new HcbUserDetails(customerA)))
                        .param("idempotencyToken", token)
                        .param("cartData", cartJson)
                        .param("customerName", "Outstation Customer")
                        .param("customerEmail", "outstation@example.com")
                        .param("customerMobile", "9876543201")
                        .param("deliveryAddress", "MG Road")
                        .param("deliveryCity", "Mumbai")
                        .param("deliveryState", "Maharashtra")
                        .param("deliveryPincode", "400001"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "deliveryPincode"))
                .andExpect(view().name("cart/checkout"));
    }

    @Test
    @DisplayName("Idempotency token prevents duplicate orders on re-submission")
    void testIdempotentDuplicateSubmission() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = UUID.randomUUID().toString();
        session.setAttribute("checkout_idempotency_token", token);

        String cartJson = "[{\"productId\":" + testProduct1.getId() + ",\"qty\":1}]";

        // First submission creates the order
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .with(csrf())
                        .with(user(new HcbUserDetails(customerA)))
                        .param("idempotencyToken", token)
                        .param("cartData", cartJson)
                        .param("customerName", "Customer Alpha")
                        .param("customerEmail", "custA@example.com")
                        .param("customerMobile", "9876543201")
                        .param("deliveryAddress", "Street 1")
                        .param("deliveryCity", "Pune")
                        .param("deliveryState", "Maharashtra")
                        .param("deliveryPincode", "411001"))
                .andExpect(status().is3xxRedirection());

        assertEquals(1, orderRepository.count());
        Long createdOrderId = orderRepository.findAll().get(0).getId();

        // Second submission with same token should redirect to created order without creating another order
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .with(csrf())
                        .with(user(new HcbUserDetails(customerA)))
                        .param("idempotencyToken", token)
                        .param("cartData", cartJson)
                        .param("customerName", "Customer Alpha")
                        .param("customerEmail", "custA@example.com")
                        .param("customerMobile", "9876543201")
                        .param("deliveryAddress", "Street 1")
                        .param("deliveryCity", "Pune")
                        .param("deliveryState", "Maharashtra")
                        .param("deliveryPincode", "411001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + createdOrderId));

        assertEquals(1, orderRepository.count());
    }

    @Test
    @DisplayName("IDOR Security: Customer B cannot view Customer A's order (403 Forbidden)")
    void testIdorProtectionForbiddenForOtherUser() throws Exception {
        // Create an order belonging to Customer A
        Order order = Order.builder()
                .user(customerA)
                .orderNumber("HCB-1001")
                .customerName(customerA.getFullName())
                .customerEmail(customerA.getEmail())
                .customerMobile(customerA.getMobile())
                .deliveryAddress("123 Street")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .subtotal(new BigDecimal("299.00"))
                .shippingFee(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("399.00"))
                .status(OrderStatus.NEW)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyToken(UUID.randomUUID().toString())
                .build();
        order = orderRepository.save(order);

        // Customer B tries to view Customer A's order -> 403 Forbidden
        mockMvc.perform(get("/orders/" + order.getId())
                        .with(user(new HcbUserDetails(customerB))))
                .andExpect(status().isForbidden());

        // Anonymous user tries to view Customer A's order -> redirect to login
        mockMvc.perform(get("/orders/" + order.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Customer A views their own order -> 200 OK
        mockMvc.perform(get("/orders/" + order.getId())
                        .with(user(new HcbUserDetails(customerA))))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/order-detail"))
                .andExpect(content().string(containsString("HCB-1001")))
                .andExpect(content().string(containsString("healthychocobytes@paytm")))
                .andExpect(content().string(containsString("payment-methods-layout")))
                .andExpect(content().string(containsString("Scan & Pay with Any UPI App")))
                .andExpect(content().string(containsString("Option 2: Pay to UPI ID (VPA)")))
                .andExpect(content().string(containsString("Option 3: Pay to Mobile Number")))
                .andExpect(content().string(containsString("whatsapp-strip")))
                .andExpect(content().string(containsString("Send WhatsApp Confirmation")))
                .andExpect(content().string(containsString("order-details-grid")));

        // Admin views Customer A's order -> redirected to secure admin order management
        mockMvc.perform(get("/orders/" + order.getId())
                        .with(user(new HcbUserDetails(adminUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/" + order.getId()));
    }
}
