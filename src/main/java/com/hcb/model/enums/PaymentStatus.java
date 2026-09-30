package com.hcb.model.enums;

public enum PaymentStatus {
    PENDING("Payment Pending"),
    SUBMITTED("Payment Submitted"),
    VERIFIED("Payment Verified"),
    REJECTED("Payment Rejected"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
