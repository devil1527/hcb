package com.hcb.controller;

import com.hcb.model.entity.Order;
import com.hcb.model.entity.OrderItem;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class OrderDetailRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testRenderOrderDetailThroughWeb() throws Exception {
        User user = userRepository.save(User.builder()
                .email("dollypanjwani123@gmail.com")
                .fullName("Ashwin Golani")
                .mobile("8871921212")
                .passwordHash("password")
                .active(true)
                .build());
        UserRole role = userRoleRepository.save(UserRole.builder().user(user).role(RoleType.ROLE_CUSTOMER.name()).build());
        user.getRoles().add(role);

        Product p = productRepository.save(Product.builder()
                .name("Healthy Protein Bar")
                .slug("healthy-protein-bar")
                .description("Desc")
                .price(new BigDecimal("69.00"))
                .active(true)
                .available(true)
                .stockQuantity(100)
                .build());

        Order order = Order.builder()
                .orderNumber("HCB-1008")
                .user(user)
                .customerName("Ashwin Golani")
                .customerEmail("dollypanjwani123@gmail.com")
                .customerMobile("8871921212")
                .deliveryAddress("dghgfrjfhjgf")
                .deliveryCity("pune")
                .deliveryState("maharastra")
                .deliveryPincode("411017")
                .subtotal(new BigDecimal("690.00"))
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("690.00"))
                .status(OrderStatus.NEW)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .product(p)
                .productName("Healthy Protein Bar")
                .productSlug("healthy-protein-bar")
                .productSku("HPB-1")
                .quantity(10)
                .unitPrice(new BigDecimal("69.00"))
                .lineTotal(new BigDecimal("690.00"))
                .build();
        order.addItem(item);

        Order saved = orderRepository.save(order);

        MvcResult result = mockMvc.perform(get("/orders/" + saved.getId())
                        .flashAttr("orderPlacedSuccess", true)
                        .with(user(new HcbUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        System.out.println("RENDERED HTML LENGTH: " + html.length());
        System.out.println("LAST 500 CHARS OF HTML:\n" + html.substring(Math.max(0, html.length() - 500)));
        assertFalse(html.contains("Something Went Wrong"), "Should not contain error page!");
    }
}
