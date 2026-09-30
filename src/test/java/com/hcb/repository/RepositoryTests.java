package com.hcb.repository;

import com.hcb.model.entity.*;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class RepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("User creation and role verification")
    void testUserCreation() {
        User user = User.builder()
                .email("customer@example.com")
                .mobile("9876543210")
                .passwordHash("$2a$12$dummyhash")
                .fullName("Test Customer")
                .active(true)
                .build();
        user.addRole("ROLE_CUSTOMER");

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        Optional<User> found = userRepository.findByEmail("customer@example.com");
        assertTrue(found.isPresent());
        assertEquals("Test Customer", found.get().getFullName());
        assertTrue(found.get().hasRole("ROLE_CUSTOMER"));
        assertFalse(found.get().hasRole("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Product stock decrement atomic behavior")
    void testProductStockDecrement() {
        Product product = Product.builder()
                .name("Test Chocolate Box")
                .slug("test-chocolate-box")
                .price(new BigDecimal("99.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .active(true)
                .available(true)
                .build();

        Product saved = productRepository.saveAndFlush(product);

        // Decrement by 3
        int updated = productRepository.decrementStock(saved.getId(), 3);
        assertEquals(1, updated);

        Product refreshed = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals(7, refreshed.getStockQuantity());

        // Try to decrement by more than available (8 > 7) -> should update 0 rows
        int failedUpdate = productRepository.decrementStock(saved.getId(), 8);
        assertEquals(0, failedUpdate);

        // Stock remains 7
        refreshed = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals(7, refreshed.getStockQuantity());
    }

    @Test
    @DisplayName("Order creation with snapshot price and order items")
    void testOrderCreation() {
        Product product = Product.builder()
                .name("Single Protein Bar")
                .slug("single-protein-bar")
                .price(new BigDecimal("69.00"))
                .stockQuantity(50)
                .active(true)
                .available(true)
                .build();
        product = productRepository.saveAndFlush(product);

        Order order = Order.builder()
                .customerName("Ashwin Golani")
                .customerEmail("ashwin@ideaai.in")
                .customerMobile("918871921212")
                .deliveryAddress("Legacy Vista, Pune, Maharashtra")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .subtotal(new BigDecimal("69.00"))
                .shippingFee(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("169.00"))
                .status(OrderStatus.NEW)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyToken("token-test-12345")
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .productName(product.getName())
                .productSlug(product.getSlug())
                .unitPrice(product.getPrice())
                .quantity(1)
                .lineTotal(new BigDecimal("69.00"))
                .build();

        order.addItem(item);
        Order savedOrder = orderRepository.saveAndFlush(order);

        // Generate database-ID based order number: HCB-1001 + id
        savedOrder.setOrderNumber("HCB-" + (1000 + savedOrder.getId()));
        savedOrder = orderRepository.saveAndFlush(savedOrder);

        assertNotNull(savedOrder.getId());
        assertTrue(savedOrder.getOrderNumber().startsWith("HCB-"));
        assertEquals(1, savedOrder.getItems().size());
        assertEquals("Single Protein Bar", savedOrder.getItems().get(0).getProductName());

        // Price snapshot check: altering product price does NOT alter order item price
        product.setPrice(new BigDecimal("99.00"));
        productRepository.saveAndFlush(product);

        Order reloadedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(new BigDecimal("69.00"), reloadedOrder.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("Setting and AuditLog persistence")
    void testSettingAndAuditLog() {
        Setting setting = Setting.builder()
                .settingKey("test_key")
                .settingValue("test_value")
                .settingType("STRING")
                .description("Test Description")
                .build();
        settingRepository.save(setting);

        Optional<Setting> foundSetting = settingRepository.findBySettingKey("test_key");
        assertTrue(foundSetting.isPresent());
        assertEquals("test_value", foundSetting.get().getSettingValue());

        AuditLog log = AuditLog.builder()
                .action("TEST_ACTION")
                .entityType("SETTING")
                .entityId(foundSetting.get().getId())
                .oldValue(null)
                .newValue("test_value")
                .build();
        AuditLog savedLog = auditLogRepository.save(log);
        assertNotNull(savedLog.getId());
    }
}
