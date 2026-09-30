package com.hcb.controller;

import com.hcb.model.entity.Product;
import com.hcb.service.ProductService;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SettingService settingService;

    @GetMapping("/products")
    public String listProducts() {
        return "redirect:/#products";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable("slug") String slug, Model model) {
        Product product = productService.getProductBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is currently unavailable");
        }

        model.addAttribute("product", product);
        model.addAttribute("businessWhatsapp", settingService.getSetting("whatsapp_number", "918871921212"));
        return "products/detail";
    }
}
