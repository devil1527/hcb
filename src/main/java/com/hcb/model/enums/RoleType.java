package com.hcb.model.enums;

public enum RoleType {
    ROLE_CUSTOMER,
    ROLE_ADMIN;

    public static RoleType fromString(String role) {
        if (role == null) return ROLE_CUSTOMER;
        if (role.startsWith("ROLE_")) {
            return RoleType.valueOf(role);
        }
        return RoleType.valueOf("ROLE_" + role);
    }
}
