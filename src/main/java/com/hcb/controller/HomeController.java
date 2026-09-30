package com.hcb.controller;

import com.hcb.model.entity.Product;
import com.hcb.service.ProductService;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final SettingService settingService;

    @GetMapping("/")
    public String index(Model model) {
        List<Product> products = productService.getActiveProducts();
        model.addAttribute("products", products);
        model.addAttribute("businessWhatsapp", settingService.getSetting("whatsapp_number", "918871921212"));
        model.addAttribute("freeShippingThreshold", settingService.getIntSetting("free_shipping_threshold", 499));
        return "index";
    }
}
