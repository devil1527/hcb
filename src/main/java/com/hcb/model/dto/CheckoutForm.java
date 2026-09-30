package com.hcb.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutForm {

    @NotBlank(message = "Idempotency token is required")
    private String idempotencyToken;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String customerName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    private String customerEmail;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter a valid 10-digit Indian mobile number")
    private String customerMobile;

    @NotBlank(message = "Delivery address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String deliveryAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    private String deliveryCity;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State name must be between 2 and 100 characters")
    private String deliveryState;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Please enter a valid 6-digit Indian postal pincode")
    private String deliveryPincode;

    @Builder.Default
    private List<CheckoutItemDto> items = new ArrayList<>();

    // Hidden form input holding JSON cart items from client
    private String cartData;
}
