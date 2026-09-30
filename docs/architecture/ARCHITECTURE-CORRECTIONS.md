# HCB Architecture Corrections

**Critical corrections applied based on architectural review**

---

## Summary of Changes

The core architecture was correct. These corrections address specific implementation details before coding begins.

### ✅ What Remains Unchanged (Approved)
- Modular monolith pattern (Spring Boot + Thymeleaf + MariaDB)
- Apache reverse proxy configuration
- Server-side price calculation
- Historical order snapshots (immutable pricing)
- Separate admin/customer authentication
- Audit logging
- systemd deployment
- Backup strategy

### ✅ What Has Been Corrected
1. Timeline reorganized (milestone-based, not artificial day numbers)
2. Order number generation (database ID-based, not MAX+1)
3. Stock transaction design (explicit, not vague)
4. Payment screenshot handling (WhatsApp-only initially)
5. CSRF implementation (Spring Security default, not over-specified)
6. Security headers (removed obsolete X-XSS-Protection)
7. Memory allocation (configuration-driven, not fixed)
8. SMTP configuration (provider-agnostic, not Gmail-specific)
9. Backup design (supports future offsite, not VPS-only)
10. JAR deployment (versioned files, not overwrite)
11. Order idempotency (explicit duplicate prevention)

---

## Correction 1: Timeline Reorganization

### Problem
Executive summary: "15-20 days"  
Implementation roadmap: "6-8 weeks" with artificial "Day 1, Day 2..."

### Solution: Milestone-Based Implementation

**8 Working Milestones:**

1. **Foundation + Database**
   - Spring Boot project
   - Database schema deployed
   - Flyway migrations
   - Connection verified

2. **Customer Login + Products**
   - User registration/login/password reset
   - Product browsing (dynamic from DB)
   - Product detail pages

3. **Cart + Checkout + Orders**
   - Client-side cart (localStorage)
   - Checkout form
   - Order creation (with transaction)
   - Stock decrement (atomic)
   - Order history

4. **UPI + WhatsApp**
   - UPI QR generation
   - Order confirmation page
   - WhatsApp pre-filled message

5. **Admin Panel**
   - Admin login (separate)
   - Product CRUD
   - Order management
   - Payment verification
   - Settings management

6. **Email Notifications**
   - SMTP config (provider-agnostic)
   - Email templates
   - Transactional emails

7. **Security + Testing**
   - CSRF/IDOR/XSS testing
   - Concurrent order testing
   - Idempotency testing
   - Security checklist

8. **VPS Deployment**
   - Server setup
   - Database deployment
   - Application deployment
   - Apache/SSL config
   - DNS cutover

**Timeline:** 15-20 days for focused developer working through milestones sequentially.

---

## Correction 2: Order Number Generation

### Problem
Original used `MAX(order_number) + 1` stored procedure:
```sql
SELECT MAX(CAST(SUBSTRING(order_number, 5) AS UNSIGNED)) + 1 FROM orders;
```
**Race condition:** Two concurrent orders can calculate same next number.

### Solution: Database Auto-Increment

**Database ID is source of truth:**
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- Database guarantees uniqueness
    order_number VARCHAR(50) NOT NULL UNIQUE,
    ...
);
```

**Generate display number from database ID:**
```java
@Transactional
public Order createOrder(OrderRequest request) {
    Order order = new Order();
    // ... set fields
    
    // Save to get database-generated ID
    order = orderRepository.save(order);
    
    // Generate display number (concurrency-safe)
    order.setOrderNumber("HCB-" + (1000 + order.getId()));
    order = orderRepository.save(order);
    
    return order;
}
```

**Examples:**
- DB ID 1 → HCB-1001
- DB ID 2 → HCB-1002
- DB ID 57 → HCB-1057

**Stored procedure removed from schema.sql**

---

## Correction 3: Stock Transaction Design

### Problem
Plan said "use pessimistic locking" but didn't specify exact transaction flow.

### Solution: Explicit Transaction Design

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public Order createOrder(OrderRequest request, User user) {
    
    // 1. Lock product rows (pessimistic write lock)
    List<Product> products = productRepository.findByIdInWithLock(productIds);
    
    // 2. Validate products exist, active, available
    for (OrderItemRequest itemRequest : request.getItems()) {
        Product product = findProduct(products, itemRequest.getProductId());
        if (!product.isActive() || !product.isAvailable()) {
            throw new ProductUnavailableException(product.getName());
        }
        
        // 3. Validate stock availability
        if (product.getStockQuantity() < itemRequest.getQuantity()) {
            throw new InsufficientStockException(product.getName());
        }
    }
    
    // 4. Calculate authoritative total (server-side, never trust client)
    BigDecimal subtotal = BigDecimal.ZERO;
    for (OrderItemRequest itemRequest : request.getItems()) {
        Product product = findProduct(products, itemRequest.getProductId());
        // CRITICAL: Use database price, not client price
        BigDecimal lineTotal = product.getPrice()
            .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
        subtotal = subtotal.add(lineTotal);
    }
    
    // 5. Calculate shipping (from settings)
    BigDecimal shippingFee = calculateShipping(subtotal);
    BigDecimal totalAmount = subtotal.add(shippingFee);
    
    // 6. Create order
    Order order = new Order();
    // ... set fields
    order.setSubtotal(subtotal);
    order.setShippingFee(shippingFee);
    order.setTotalAmount(totalAmount);
    order = orderRepository.save(order);
    
    // Generate order number
    order.setOrderNumber("HCB-" + (1000 + order.getId()));
    order = orderRepository.save(order);
    
    // 7. Create order items (snapshot prices)
    for (OrderItemRequest itemRequest : request.getItems()) {
        Product product = findProduct(products, itemRequest.getProductId());
        OrderItem item = new OrderItem();
        item.setProductName(product.getName());  // Snapshot
        item.setUnitPrice(product.getPrice());   // Snapshot
        item.setQuantity(itemRequest.getQuantity());
        orderItemRepository.save(item);
    }
    
    // 8. Decrement stock atomically
    for (OrderItemRequest itemRequest : request.getItems()) {
        int rowsUpdated = productRepository.decrementStock(
            itemRequest.getProductId(), 
            itemRequest.getQuantity()
        );
        if (rowsUpdated == 0) {
            throw new StockDecrementFailedException();
        }
    }
    
    // 9. Commit transaction
    return order;
}
```

**Repository methods:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id IN :ids")
List<Product> findByIdInWithLock(@Param("ids") List<Long> ids);

@Modifying
@Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
       "WHERE p.id = :productId AND p.stockQuantity >= :quantity " +
       "AND p.active = true AND p.available = true")
int decrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
```

**Concurrent order test required:**
```
Stock = 1
Customer A: buy 1
Customer B: buy 1

Expected: One succeeds, one fails with InsufficientStockException
Never: Both succeed (stock = -1)
```

---

## Correction 4: Payment Screenshot Handling

### Problem
Plan inconsistent: sometimes stored screenshots, sometimes WhatsApp-only.

### Solution: WhatsApp-Only Workflow

**Primary workflow:**
```
Customer places order
    ↓
UPI QR displayed
    ↓
Customer pays
    ↓
Customer clicks WhatsApp button
    ↓
WhatsApp pre-filled: "Hi HCB, payment for Order HCB-1057. Amount ₹399."
    ↓
Customer attaches screenshot manually in WhatsApp
    ↓
You receive WhatsApp message
    ↓
You verify payment manually
    ↓
Admin panel → Mark Payment Verified
    ↓
Customer receives email
```

**What HCB stores:**
- Order number
- Total amount
- Payment status (PENDING, SUBMITTED, VERIFIED, REJECTED)
- Payment method (UPI)
- UPI transaction ID (optional)
- Admin who verified
- Verification timestamp
- Admin notes

**What HCB does NOT store initially:**
- Payment screenshot image file

**Database field kept for future:**
```sql
payment_screenshot_filename VARCHAR(255) NULL  -- Field exists but unused
```

**Benefits:**
- No file upload security
- No storage needed
- No privacy exposure
- You already use WhatsApp

**Future enhancement:** Can add screenshot upload later if needed.

---

## Correction 5: CSRF Implementation

### Problem
Plan over-specified: "CSRF token in cookie (CookieCsrfTokenRepository)"

### Solution: Spring Security Default

**Requirement (not implementation):**
- All POST/PUT/DELETE must have CSRF protection
- Tokens validated server-side
- Invalid tokens rejected (403)

**Implementation:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            // Spring Security default - works with Thymeleaf
        );
        return http.build();
    }
}
```

**Thymeleaf auto-adds CSRF token:**
```html
<form method="post" th:action="@{/checkout}">
    <!-- CSRF token auto-added by Spring Security -->
    <button type="submit">Place Order</button>
</form>
```

**Do not over-specify unless there's a reason.**

---

## Correction 6: Security Headers

### Problem
Plan required obsolete `X-XSS-Protection` header.

### Solution: Modern Security Headers

**Required headers:**
```apache
Header always set X-Frame-Options "DENY"
Header always set X-Content-Type-Options "nosniff"
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains"
Header always set Referrer-Policy "strict-origin-when-cross-origin"
Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' fonts.googleapis.com; font-src 'self' fonts.gstatic.com; img-src 'self' data:;"
```

**Removed (obsolete):**
```apache
# DO NOT ADD:
# Header always set X-XSS-Protection "1; mode=block"
```

**Why:** Deprecated, can introduce vulnerabilities. Use `Content-Security-Policy` instead.

---

## Correction 7: Memory Allocation

### Problem
Fixed at `-Xmx512m` without considering other services.

### Solution: Configuration-Driven Memory

**Measure actual capacity first:**
```bash
# Stop IdeaAI
sudo systemctl stop ideaai
sleep 30
free -h
```

**Configure based on measurement:**

If available ~1.2GB: `-Xmx768m`  
If available ~1.0GB: `-Xmx512m`  
If available ~800MB: `-Xmx384m`

**systemd service:**
```ini
[Service]
Environment="JVM_MIN_HEAP=256m"
Environment="JVM_MAX_HEAP=512m"

ExecStart=/usr/bin/java \
  -Xms${JVM_MIN_HEAP} \
  -Xmx${JVM_MAX_HEAP} \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseG1GC \
  -jar /opt/hcb/hcb-current.jar
```

**Easy to adjust without editing service file.**

---

## Correction 8: SMTP Configuration

### Problem
Gmail-specific configuration.

### Solution: Provider-Agnostic SMTP

**application.properties:**
```properties
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

hcb.email.from=${SMTP_FROM}
hcb.email.admin=${ADMIN_EMAIL}
```

**Environment variables:**
```bash
# Gmail (initial)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-gmail@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=your-gmail@gmail.com
ADMIN_EMAIL=ashwin@ideaai.in

# To switch to SendGrid later:
# SMTP_HOST=smtp.sendgrid.net
# SMTP_USERNAME=apikey
# SMTP_PASSWORD=sendgrid-api-key

# To switch to AWS SES later:
# SMTP_HOST=email-smtp.us-east-1.amazonaws.com
# SMTP_USERNAME=ses-username
# SMTP_PASSWORD=ses-password
```

**Application code never mentions "Gmail".**

---

## Correction 9: Backup Design

### Problem
Backups only on VPS (doesn't protect against VPS loss).

### Solution: Designed for Offsite Extension

**backup-database.sh:**
```bash
#!/bin/bash
set -e

# Local backup
BACKUP_DIR="/var/backups/hcb/database"
BACKUP_FILE="$BACKUP_DIR/hcb-db-$(date +%Y%m%d-%H%M%S).sql.gz"

mysqldump ... | gzip > "$BACKUP_FILE"

# Offsite backup (optional, configure when ready)
OFFSITE_ENABLED="${OFFSITE_BACKUP_ENABLED:-false}"
OFFSITE_DEST="${OFFSITE_BACKUP_DEST:-}"

if [ "$OFFSITE_ENABLED" = "true" ] && [ -n "$OFFSITE_DEST" ]; then
    # Example: rsync remote-server:/backups/hcb/
    # Example: rclone remote:hcb-backups/
    # Example: aws s3 cp ... s3://bucket/hcb/
    
    # Uncomment appropriate command when ready
    # rsync -avz "$BACKUP_FILE" "$OFFSITE_DEST/"
fi

# Clean old backups
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +30 -delete
```

**To enable offsite later:**
```bash
export OFFSITE_BACKUP_ENABLED=true
export OFFSITE_BACKUP_DEST="remote-server:/backups/hcb/"
```

**No code changes needed.**

---

## Correction 10: JAR Deployment

### Problem
Plan sometimes suggested overwriting JAR.

### Solution: Versioned JARs + Symlink

**Directory structure:**
```
/opt/hcb/
├── hcb-1.0.0.jar
├── hcb-1.0.1.jar
├── hcb-1.0.2.jar
├── hcb-current.jar  → symlink to active version
```

**Deployment:**
```bash
sudo cp /tmp/hcb-1.1.0.jar /opt/hcb/
sudo systemctl stop hcb
sudo ln -sf /opt/hcb/hcb-1.1.0.jar /opt/hcb/hcb-current.jar
sudo systemctl start hcb
```

**Rollback:**
```bash
sudo systemctl stop hcb
sudo ln -sf /opt/hcb/hcb-1.0.2.jar /opt/hcb/hcb-current.jar
sudo systemctl start hcb
```

**systemd always points to symlink:**
```ini
ExecStart=/usr/bin/java ... -jar /opt/hcb/hcb-current.jar
```

---

## Correction 11: Order Idempotency

### Problem
No duplicate order prevention.

### Solution: Idempotency Token

**Problem scenario:**
```
Customer clicks "Place Order"
    ↓
Slow network
    ↓
Customer clicks again
    ↓
Two requests sent
    ↓
Without protection: HCB-1057 and HCB-1058 both created
```

**Solution:**

**1. Generate token on checkout page load:**
```java
@GetMapping("/checkout")
public String checkoutPage(Model model, HttpSession session) {
    String token = UUID.randomUUID().toString();
    session.setAttribute("checkout_idempotency_token", token);
    model.addAttribute("idempotencyToken", token);
    return "checkout";
}
```

**2. Include in form:**
```html
<form method="post" th:action="@{/checkout}">
    <input type="hidden" name="idempotencyToken" th:value="${idempotencyToken}" />
    <button type="submit">Place Order</button>
</form>
```

**3. Validate on submission:**
```java
@PostMapping("/checkout")
public String checkout(@RequestParam String idempotencyToken, HttpSession session) {
    // Validate token
    String sessionToken = (String) session.getAttribute("checkout_idempotency_token");
    if (!sessionToken.equals(idempotencyToken)) {
        throw new InvalidCheckoutSessionException();
    }
    
    // Check if already created
    Order existing = orderRepository.findByIdempotencyToken(idempotencyToken);
    if (existing != null) {
        return "redirect:/orders/" + existing.getId();
    }
    
    // Remove token (single use)
    session.removeAttribute("checkout_idempotency_token");
    
    // Create order
    Order order = orderService.createOrder(request, user);
    order.setIdempotencyToken(idempotencyToken);
    orderRepository.save(order);
    
    return "redirect:/orders/" + order.getId();
}
```

**4. Database support:**
```sql
ALTER TABLE orders 
ADD COLUMN idempotency_token VARCHAR(255) NULL,
ADD INDEX idx_idempotency_token (idempotency_token);
```

**Prevents:**
- ✅ Double-click submit
- ✅ Back button issues
- ✅ Multiple tabs
- ✅ Network retry

---

## Pre-Implementation Review Checklist

Before coding begins, verify:

- [ ] **Concurrency:** Transaction design explicit, stock locking tested
- [ ] **Order numbering:** Database ID-based (no MAX+1)
- [ ] **Idempotency:** Token-based duplicate prevention
- [ ] **Payment workflow:** WhatsApp-only (no screenshot upload initially)
- [ ] **Backup:** Local + designed for offsite
- [ ] **Deployment:** Versioned JARs + symlink rollback
- [ ] **Memory:** Configuration-driven (not fixed)
- [ ] **SMTP:** Provider-agnostic

---

## Summary Table

| Issue | Before | After |
|-------|--------|-------|
| Timeline | 6-8 weeks, day-based | Milestone-based, 15-20 days |
| Order number | MAX()+1 race condition | DB auto-increment |
| Stock locking | Vague | Explicit transaction |
| Payment screenshot | Inconsistent | WhatsApp-only |
| CSRF | Over-specified | Spring default |
| Security headers | Included X-XSS-Protection | Removed (obsolete) |
| Memory | Fixed 512MB | Configuration-driven |
| SMTP | Gmail-specific | Provider-agnostic |
| Backups | VPS-only | Supports offsite |
| JAR deployment | Overwrite | Versioned + symlink |
| Order idempotency | Missing | Token-based |

---

**All corrections applied. Architecture approved for implementation.**

**Next:** Begin Milestone 1 (Foundation + Database)

---

**Last Updated:** September 28, 2026  
**Status:** ✅ Ready for implementation
