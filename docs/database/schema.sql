-- ============================================
-- HCB DATABASE SCHEMA v1.0
-- Healthy Choco Bytes - Chocolate Ordering Application
-- ============================================

CREATE DATABASE IF NOT EXISTS hcb 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hcb;

-- ============================================
-- USERS & AUTHENTICATION
-- ============================================

CREATE TABLE users (
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
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL, -- CUSTOMER, ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_role (user_id, role),
    INDEX idx_role (role)
) ENGINE=InnoDB;

CREATE TABLE password_reset_tokens (
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
) ENGINE=InnoDB;

-- ============================================
-- PRODUCT CATALOG
-- ============================================

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2) NULL COMMENT 'Internal cost tracking',
    stock_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT DEFAULT 5,
    active BOOLEAN DEFAULT TRUE COMMENT 'Product exists in catalog',
    available BOOLEAN DEFAULT TRUE COMMENT 'Can be temporarily unavailable',
    sort_order INT DEFAULT 0,
    image_filename VARCHAR(255) NULL,
    weight_grams INT NULL,
    sku VARCHAR(100) NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_active_available (active, available),
    INDEX idx_slug (slug),
    INDEX idx_sort (sort_order),
    INDEX idx_stock (stock_quantity),
    
    CONSTRAINT chk_price_positive CHECK (price > 0),
    CONSTRAINT chk_stock_non_negative CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

-- ============================================
-- ORDERS & ORDER ITEMS
-- ============================================

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NULL UNIQUE COMMENT 'HCB-1001 format, generated after insert from ID',
    idempotency_token VARCHAR(255) NULL COMMENT 'Prevents duplicate orders',
    user_id BIGINT NULL COMMENT 'Can be NULL for guest orders (future)',
    
    -- Customer Information (snapshot at order time)
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_mobile VARCHAR(15) NOT NULL,
    
    -- Delivery Information (snapshot at order time)
    delivery_address TEXT NOT NULL,
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(100),
    delivery_pincode VARCHAR(10),
    
    -- Order Totals (calculated at order time, immutable)
    subtotal DECIMAL(10,2) NOT NULL,
    shipping_fee DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    
    -- Order Status
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' COMMENT 'NEW, PAYMENT_PENDING, PAYMENT_SUBMITTED, PAYMENT_VERIFIED, PAYMENT_REJECTED, PREPARING, DISPATCHED, DELIVERED, CANCELLED, REFUNDED',
    
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SUBMITTED, VERIFIED, REJECTED, REFUNDED',
    
    -- Payment Details
    payment_method VARCHAR(50) DEFAULT 'UPI',
    upi_transaction_id VARCHAR(255) NULL,
    payment_screenshot_filename VARCHAR(255) NULL COMMENT 'Field exists but unused initially - WhatsApp verification preferred',
    payment_verified_by BIGINT NULL COMMENT 'Admin user ID',
    payment_verified_at TIMESTAMP NULL,
    payment_notes TEXT NULL COMMENT 'Admin notes on payment verification',
    
    -- Fulfillment
    dispatched_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason TEXT NULL,
    
    -- Admin Notes
    admin_notes TEXT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (payment_verified_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_order_number (order_number),
    UNIQUE KEY uk_idempotency_token (idempotency_token),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status),
    INDEX idx_created (created_at),
    INDEX idx_customer_email (customer_email),
    INDEX idx_customer_mobile (customer_mobile),
    INDEX idx_status_payment (status, payment_status),
    
    CONSTRAINT chk_totals_positive CHECK (subtotal >= 0 AND shipping_fee >= 0 AND total_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    
    -- Product Information (snapshot at order time, immutable)
    product_id BIGINT NULL COMMENT 'Can be NULL if product deleted later',
    product_name VARCHAR(255) NOT NULL,
    product_slug VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NULL,
    
    -- Pricing (snapshot at order time, immutable)
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    
    INDEX idx_order (order_id),
    INDEX idx_product (product_id),
    
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_prices_positive CHECK (unit_price > 0 AND line_total > 0)
) ENGINE=InnoDB;

-- ============================================
-- AUDIT LOGGING
-- ============================================

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL COMMENT 'LOGIN, PRODUCT_CREATED, PRODUCT_UPDATED, PRICE_CHANGED, STOCK_CHANGED, PAYMENT_VERIFIED, ORDER_STATUS_CHANGED, etc.',
    entity_type VARCHAR(50) NOT NULL COMMENT 'PRODUCT, ORDER, USER, SETTINGS',
    entity_id BIGINT NULL,
    old_value TEXT NULL COMMENT 'JSON of old state',
    new_value TEXT NULL COMMENT 'JSON of new state',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at),
    INDEX idx_user_action (user_id, action)
) ENGINE=InnoDB;

-- ============================================
-- SYSTEM SETTINGS
-- ============================================

CREATE TABLE settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_type VARCHAR(50) NOT NULL DEFAULT 'STRING' COMMENT 'STRING, INTEGER, BOOLEAN, JSON',
    description TEXT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_key (setting_key),
    INDEX idx_updated (updated_at)
) ENGINE=InnoDB;

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
('guest_checkout_enabled', 'false', 'BOOLEAN', 'Allow checkout without registration');

-- ============================================
-- INITIAL ADMIN USER
-- Note: Password must be set during deployment
-- Placeholder password: 'changeme123' (bcrypt hash)
-- ============================================

INSERT INTO users (email, mobile, password_hash, full_name, email_verified, active)
VALUES (
    'ashwin@ideaai.in',
    '9999999999',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIbXgqxJb2',
    'Ashwin Golani',
    TRUE,
    TRUE
);

INSERT INTO user_roles (user_id, role)
VALUES (
    (SELECT id FROM users WHERE email = 'ashwin@ideaai.in'),
    'ADMIN'
);

-- ============================================
-- VIEWS FOR CONVENIENCE
-- ============================================

-- Active products view
CREATE VIEW v_active_products AS
SELECT 
    p.id,
    p.slug,
    p.name,
    p.description,
    p.price,
    p.stock_quantity,
    p.image_filename,
    p.sort_order,
    CASE 
        WHEN p.stock_quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.stock_quantity <= p.low_stock_threshold THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM products p
WHERE p.active = TRUE AND p.available = TRUE
ORDER BY p.sort_order, p.name;

-- Pending payments view (for admin)
CREATE VIEW v_pending_payments AS
SELECT 
    o.id,
    o.order_number,
    o.customer_name,
    o.customer_email,
    o.customer_mobile,
    o.total_amount,
    o.payment_status,
    o.payment_screenshot_filename,
    o.created_at,
    TIMESTAMPDIFF(HOUR, o.created_at, NOW()) AS hours_pending
FROM orders o
WHERE o.payment_status IN ('SUBMITTED', 'PENDING')
ORDER BY o.created_at ASC;

-- Order summary view
CREATE VIEW v_order_summary AS
SELECT 
    o.id,
    o.order_number,
    o.customer_name,
    o.total_amount,
    o.status,
    o.payment_status,
    o.created_at,
    COUNT(oi.id) AS item_count,
    SUM(oi.quantity) AS total_quantity
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.order_number, o.customer_name, o.total_amount, o.status, o.payment_status, o.created_at;

-- ============================================
-- STORED PROCEDURES
-- ============================================

DELIMITER //

-- Check and decrement stock atomically
-- NOTE: Order number generation now handled in application code using database ID
CREATE PROCEDURE sp_decrement_stock(
    IN p_product_id BIGINT,
    IN p_quantity INT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
BEGIN
    DECLARE current_stock INT;
    
    -- Lock row for update
    SELECT stock_quantity INTO current_stock
    FROM products
    WHERE id = p_product_id
    FOR UPDATE;
    
    -- Check stock availability
    IF current_stock IS NULL THEN
        SET p_success = FALSE;
        SET p_message = 'Product not found';
    ELSEIF current_stock < p_quantity THEN
        SET p_success = FALSE;
        SET p_message = CONCAT('Insufficient stock. Available: ', current_stock);
    ELSE
        -- Decrement stock
        UPDATE products
        SET stock_quantity = stock_quantity - p_quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = p_product_id;
        
        SET p_success = TRUE;
        SET p_message = 'Stock decremented successfully';
    END IF;
END //

DELIMITER ;

-- ============================================
-- TRIGGERS
-- ============================================

DELIMITER //

-- Audit trigger for product price changes
CREATE TRIGGER trg_products_price_change
BEFORE UPDATE ON products
FOR EACH ROW
BEGIN
    IF OLD.price != NEW.price THEN
        INSERT INTO audit_logs (action, entity_type, entity_id, old_value, new_value)
        VALUES (
            'PRICE_CHANGED',
            'PRODUCT',
            NEW.id,
            CONCAT('{"price": ', OLD.price, '}'),
            CONCAT('{"price": ', NEW.price, '}')
        );
    END IF;
END //

-- Audit trigger for stock changes
CREATE TRIGGER trg_products_stock_change
BEFORE UPDATE ON products
FOR EACH ROW
BEGIN
    IF OLD.stock_quantity != NEW.stock_quantity THEN
        INSERT INTO audit_logs (action, entity_type, entity_id, old_value, new_value)
        VALUES (
            'STOCK_CHANGED',
            'PRODUCT',
            NEW.id,
            CONCAT('{"stock": ', OLD.stock_quantity, '}'),
            CONCAT('{"stock": ', NEW.stock_quantity, '}')
        );
    END IF;
END //

-- Audit trigger for order status changes
CREATE TRIGGER trg_orders_status_change
BEFORE UPDATE ON orders
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO audit_logs (action, entity_type, entity_id, old_value, new_value)
        VALUES (
            'ORDER_STATUS_CHANGED',
            'ORDER',
            NEW.id,
            CONCAT('{"status": "', OLD.status, '"}'),
            CONCAT('{"status": "', NEW.status, '"}')
        );
    END IF;
END //

DELIMITER ;

-- ============================================
-- DATABASE SETUP COMPLETE
-- ============================================

-- Verify tables created
SELECT 
    TABLE_NAME, 
    TABLE_ROWS,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'hcb'
ORDER BY TABLE_NAME;
