package com.hcb.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductForm {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String name;

    private String slug;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1.00", message = "Price must be at least ₹1.00")
    private BigDecimal price;

    private BigDecimal costPrice;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private int stockQuantity = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private int lowStockThreshold = 5;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean available = true;

    @Builder.Default
    private int sortOrder = 0;

    private Integer weightGrams;

    private String sku;

    private String existingImageFilename;

    private MultipartFile imageFile;
}
