package com.hcb.controller;

import com.hcb.model.entity.Product;
import com.hcb.service.ProductService;
import com.hcb.service.SettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private SettingService settingService;

    @Test
    @DisplayName("GET / returns index view with dynamic products and branding")
    void testHomePageRendering() throws Exception {
        // Seed a product
        Product bar = Product.builder()
                .name("Healthy Protein Bar")
                .slug("healthy-protein-bar")
                .description("Packed with pea protein, rich dry fruits, and unsweetened peanut butter.")
                .price(new BigDecimal("69.00"))
                .stockQuantity(50)
                .active(true)
                .available(true)
                .sortOrder(1)
                .imageFilename("banner.png")
                .build();
        productService.saveProduct(bar);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("businessWhatsapp"))
                .andExpect(content().string(containsString("Healthy Choco Bytes")))
                .andExpect(content().string(containsString("Healthy Protein Bar")))
                .andExpect(content().string(containsString("₹69.00")))
                .andExpect(content().string(containsString("Add to Cart")));
    }
}
