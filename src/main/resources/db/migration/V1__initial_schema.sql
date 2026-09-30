-- ============================================
-- HCB DATABASE SCHEMA v1.0
-- Healthy Choco Bytes - Flyway Initial Schema
-- ============================================

-- USERS & AUTHENTICATION
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile VARCHAR(15) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    mobile_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    account_locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_mobile (mobile),
    INDEX idx_active (active),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_role (user_id, role),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_expires (expires_at),
    INDEX idx_user_used (user_id, used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PRODUCT CATALOG
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2) NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT DEFAULT 5,
    active BOOLEAN DEFAULT TRUE,
    available BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    image_filename VARCHAR(255) NULL,
    weight_grams INT NULL,
    sku VARCHAR(100) NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_active_available (active, available),
    INDEX idx_slug (slug),
    INDEX idx_sort (sort_order),
    INDEX idx_stock (stock_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ORDERS & ORDER ITEMS
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NULL UNIQUE,
    idempotency_token VARCHAR(255) NULL UNIQUE,
    user_id BIGINT NULL,
    
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_mobile VARCHAR(15) NOT NULL,
    
    delivery_address TEXT NOT NULL,
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(100),
    delivery_pincode VARCHAR(10),
    
    subtotal DECIMAL(10,2) NOT NULL,
    shipping_fee DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) DEFAULT 'UPI',
    upi_transaction_id VARCHAR(255) NULL,
    payment_screenshot_filename VARCHAR(255) NULL,
    payment_verified_by BIGINT NULL,
    payment_verified_at TIMESTAMP NULL,
    payment_notes TEXT NULL,
    
    dispatched_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason TEXT NULL,
    admin_notes TEXT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (payment_verified_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_order_number (order_number),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status),
    INDEX idx_created (created_at),
    INDEX idx_customer_email (customer_email),
    INDEX idx_customer_mobile (customer_mobile),
    INDEX idx_status_payment (status, payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    
    product_id BIGINT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_slug VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NULL,
    
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    
    INDEX idx_order (order_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AUDIT LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SYSTEM SETTINGS
CREATE TABLE IF NOT EXISTS settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    description TEXT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- INITIAL SETTINGS DATA
-- ============================================
INSERT INTO settings (setting_key, setting_value, setting_type, description) VALUES
('shipping_fee_default', '100', 'INTEGER', 'Default shipping fee in rupees'),
('free_shipping_threshold', '499', 'INTEGER', 'Order subtotal for free shipping'),
('whatsapp_number', '918871921212', 'STRING', 'Business WhatsApp number'),
('business_email', 'contact@healthychocobytes.in', 'STRING', 'Business contact email'),
('business_address', 'Legacy Vista, Pune, Maharashtra, India', 'STRING', 'Business address'),
('upi_id', 'healthychocobytes@paytm', 'STRING', 'UPI ID for payments'),
('upi_name', 'Healthy Choco Bytes', 'STRING', 'UPI payee name'),
('order_notification_emails', 'ashwin@ideaai.in', 'STRING', 'Comma-separated emails for order notifications'),
('site_maintenance_mode', 'false', 'BOOLEAN', 'Site maintenance mode flag'),
('max_order_quantity_per_product', '50', 'INTEGER', 'Maximum quantity per product in single order'),
('low_stock_alert_threshold', '5', 'INTEGER', 'Alert admin when stock falls below this'),
('admin_notification_enabled', 'true', 'BOOLEAN', 'Send admin email notifications'),
('customer_registration_enabled', 'true', 'BOOLEAN', 'Allow new customer registrations'),
('guest_checkout_enabled', 'false', 'BOOLEAN', 'Allow checkout without registration')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- ============================================
-- INITIAL ADMIN USER
-- Password hash for 'changeme123'
-- ============================================
INSERT INTO users (email, mobile, password_hash, full_name, email_verified, active)
VALUES (
    'ashwin@ideaai.in',
    '9999999999',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIbXgqxJb2',
    'Ashwin Golani',
    TRUE,
    TRUE
)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE email = 'ashwin@ideaai.in'
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- ============================================
-- INITIAL PRODUCTS SEED
-- ============================================
INSERT INTO products (slug, name, description, price, stock_quantity, low_stock_threshold, active, available, sort_order, image_filename, sku)
VALUES
('healthy-protein-bar', 'Healthy Protein Bar', 'Packed with pea protein, rich dry fruits, and unsweetened peanut butter for pure plant energy. (1 Pc)', 69.00, 100, 5, TRUE, TRUE, 1, 'banner.png', 'HCB-BAR-01'),
('protein-chocolate-box', 'Protein Chocolate Box', 'Made with 55% Morde dark chocolate compound, ragi murmura crunch, and wholesome protein. (Box of 4 Pcs)', 69.00, 100, 5, TRUE, TRUE, 2, 'chocolate.png', 'HCB-BOX-01')
ON DUPLICATE KEY UPDATE name = VALUES(name);
