package com.hcb.controller;

import com.hcb.model.entity.Product;
import com.hcb.repository.ProductRepository;
import com.hcb.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("GET /products/{slug} displays product details")
    void testProductDetailView() throws Exception {
        Product product = Product.builder()
                .name("Keto Hazelnut Crunch")
                .slug("keto-hazelnut-crunch")
                .description("Guilt-free dark chocolate with roasted hazelnuts.")
                .price(new BigDecimal("79.00"))
                .stockQuantity(15)
                .active(true)
                .available(true)
                .build();
        product = productService.saveProduct(product);

        mockMvc.perform(get("/products/keto-hazelnut-crunch"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(content().string(containsString("Keto Hazelnut Crunch")))
                .andExpect(content().string(containsString("₹79.00")));
    }

    @Test
    @DisplayName("GET /products/unknown returns 404")
    void testProductDetailNotFound() throws Exception {
        mockMvc.perform(get("/products/non-existent-treat"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin product management: list, create, and update stock")
    @WithMockUser(username = "ashwin@ideaai.in", roles = {"ADMIN"})
    void testAdminProductFlow() throws Exception {
        // 1. List products
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"));

        // 2. Create product via multipart form
        MockMultipartFile image = new MockMultipartFile(
                "imageFile", "test-box.png", "image/png", "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/admin/products/new")
                .file(image)
                .with(csrf())
                .param("name", "Cashew Bliss Bar")
                .param("slug", "cashew-bliss-bar")
                .param("description", "Handmade cashew bites.")
                .param("price", "89.00")
                .param("stockQuantity", "25")
                .param("active", "true")
                .param("available", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product created = productService.getProductBySlug("cashew-bliss-bar").orElse(null);
        assertNotNull(created);
        assertEquals("Cashew Bliss Bar", created.getName());
        assertEquals(25, created.getStockQuantity());

        // 3. Update stock directly
        mockMvc.perform(post("/admin/products/" + created.getId() + "/update-stock")
                .with(csrf())
                .param("stockQuantity", "40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        Product refreshed = productService.getProductById(created.getId()).orElseThrow();
        assertEquals(40, refreshed.getStockQuantity());
    }

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Stock concurrency: when stock=1, two simultaneous decrements yield exactly 1 success")
    void testConcurrentStockDecrement() throws InterruptedException {
        Product product = Product.builder()
                .name("Limited Edition Truffle")
                .slug("limited-truffle-" + System.currentTimeMillis())
                .price(new BigDecimal("120.00"))
                .stockQuantity(1) // Exactly 1 item in stock
                .active(true)
                .available(true)
                .build();
        final Product saved = productRepository.saveAndFlush(product);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // wait for simultaneous launch
                    Integer rows = transactionTemplate.execute(status -> 
                        productRepository.decrementStock(saved.getId(), 1)
                    );
                    if (rows != null && rows == 1) {
                        successes.incrementAndGet();
                    } else {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // fire simultaneously
        boolean completed = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within 5 seconds");
        assertEquals(1, successes.get(), "Exactly one concurrent thread must successfully consume the unit");
        assertEquals(1, failures.get(), "The other concurrent thread must fail to decrement");

        Product finalProduct = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, finalProduct.getStockQuantity(), "Final stock must be 0, never negative");
    }
}
