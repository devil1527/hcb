package com.hcb.controller.admin;

import com.hcb.model.dto.ProductForm;
import com.hcb.model.entity.Product;
import com.hcb.service.FileStorageService;
import com.hcb.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductForm());
        }
        return "admin/products/form";
    }

    @PostMapping("/new")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Auto-generate slug if missing
        if (form.getSlug() == null || form.getSlug().trim().isEmpty()) {
            form.setSlug(toSlug(form.getName()));
        } else {
            form.setSlug(toSlug(form.getSlug()));
        }

        if (productService.getProductBySlug(form.getSlug()).isPresent()) {
            bindingResult.rejectValue("slug", "error.productForm", "A product with this URL slug already exists.");
        }

        // Clean & handle SKU
        String rawSku = form.getSku() != null ? form.getSku().trim() : null;
        String sku = (rawSku != null && !rawSku.isEmpty()) ? rawSku.toUpperCase() : null;
        if (sku != null) {
            if (productService.getProductBySku(sku).isPresent()) {
                bindingResult.rejectValue("sku", "error.productForm", "A product with SKU '" + sku + "' already exists.");
            }
        } else {
            // Auto-generate clean unique SKU if left blank by admin
            String baseCode = form.getSlug().replace("-", "").toUpperCase();
            if (baseCode.length() > 6) baseCode = baseCode.substring(0, 6);
            sku = "HCB-" + baseCode + "-" + String.format("%03d", (int)(Math.random() * 900 + 100));
        }

        if (bindingResult.hasErrors()) {
            return "admin/products/form";
        }

        String imageFilename = null;
        if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
            try {
                imageFilename = fileStorageService.storeProductImage(form.getImageFile());
            } catch (Exception e) {
                bindingResult.rejectValue("imageFile", "error.productForm", e.getMessage());
                return "admin/products/form";
            }
        }

        Product product = Product.builder()
                .name(form.getName().trim())
                .slug(form.getSlug())
                .description(form.getDescription())
                .price(form.getPrice())
                .costPrice(form.getCostPrice())
                .stockQuantity(form.getStockQuantity())
                .lowStockThreshold(form.getLowStockThreshold())
                .active(form.isActive())
                .available(form.isAvailable())
                .sortOrder(form.getSortOrder())
                .weightGrams(form.getWeightGrams())
                .sku(sku)
                .imageFilename(imageFilename != null ? imageFilename : "banner.png")
                .build();

        try {
            productService.saveProduct(product);
        } catch (Exception e) {
            bindingResult.reject("error.productForm", "Failed to save product: " + e.getMessage());
            return "admin/products/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Product '" + product.getName() + "' created successfully!");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id: " + id));

        ProductForm form = ProductForm.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .active(product.isActive())
                .available(product.isAvailable())
                .sortOrder(product.getSortOrder())
                .weightGrams(product.getWeightGrams())
                .sku(product.getSku())
                .existingImageFilename(product.getImageFilename())
                .build();

        model.addAttribute("productForm", form);
        return "admin/products/form";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String rawSku = form.getSku() != null ? form.getSku().trim() : null;
        String sku = (rawSku != null && !rawSku.isEmpty()) ? rawSku.toUpperCase() : null;
        if (sku != null) {
            java.util.Optional<Product> existingWithSku = productService.getProductBySku(sku);
            if (existingWithSku.isPresent() && !existingWithSku.get().getId().equals(id)) {
                bindingResult.rejectValue("sku", "error.productForm", "A product with SKU '" + sku + "' already exists.");
            }
        }

        if (bindingResult.hasErrors()) {
            return "admin/products/form";
        }

        String imageFilename = form.getExistingImageFilename();
        if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
            try {
                imageFilename = fileStorageService.storeProductImage(form.getImageFile());
            } catch (Exception e) {
                bindingResult.rejectValue("imageFile", "error.productForm", e.getMessage());
                return "admin/products/form";
            }
        }

        Product productDetails = Product.builder()
                .name(form.getName().trim())
                .description(form.getDescription())
                .price(form.getPrice())
                .costPrice(form.getCostPrice())
                .stockQuantity(form.getStockQuantity())
                .lowStockThreshold(form.getLowStockThreshold())
                .active(form.isActive())
                .available(form.isAvailable())
                .sortOrder(form.getSortOrder())
                .weightGrams(form.getWeightGrams())
                .sku(sku)
                .imageFilename(imageFilename)
                .build();

        try {
            productService.updateProduct(id, productDetails);
        } catch (Exception e) {
            bindingResult.reject("error.productForm", "Failed to update product: " + e.getMessage());
            return "admin/products/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        productService.toggleActive(id);
        Product product = productService.getProductById(id).orElse(null);
        if (product != null && !product.isActive()) {
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + product.getName() + "' removed from website. Customers will no longer see it.");
        } else if (product != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + product.getName() + "' is now live on the website!");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Product visibility updated.");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-available")
    public String toggleAvailable(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        productService.toggleAvailable(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product availability updated.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/update-stock")
    public String updateStock(@PathVariable("id") Long id,
                              @RequestParam("stockQuantity") int stockQuantity,
                              RedirectAttributes redirectAttributes) {
        productService.updateStock(id, stockQuantity);
        redirectAttributes.addFlashAttribute("successMessage", "Stock updated successfully.");
        return "redirect:/admin/products";
    }

    private String toSlug(String input) {
        if (input == null) return "";
        return input.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
