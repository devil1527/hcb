package com.hcb.model.enums;

public enum OrderStatus {
    NEW("New Order"),
    PAYMENT_PENDING("Payment Pending"),
    PAYMENT_SUBMITTED("Payment Submitted"),
    PAYMENT_VERIFIED("Payment Verified"),
    PAYMENT_REJECTED("Payment Rejected"),
    PREPARING("Preparing"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
