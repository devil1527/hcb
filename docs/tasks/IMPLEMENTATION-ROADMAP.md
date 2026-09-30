# HCB Implementation Roadmap

**Timeline:** 15-20 days to launch  
**Target Date:** Mid-October 2026  
**Developer:** Ashwin Golani (Senior Java Developer)  
**Architecture:** Milestone-Based Phases (no artificial daily deadlines)

---

## Implementation Philosophy

This roadmap is organized by **deliverable milestones**, not calendar days. Each milestone represents a working, testable piece of functionality. Complete each milestone fully before moving to the next.

**Timeline flexibility:** 15-20 days total is the target, but milestones may take more or less time depending on complexity discovered during implementation.

---

## Milestone 1: Foundation + Database

**Goal:** Runnable Spring Boot application with database connection verified.

**Tasks:**
- [ ] Create Spring Boot 3.3.4 project (Spring Initializer or manual)
- [ ] Configure `pom.xml` with all required dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-thymeleaf
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-validation
  - spring-boot-starter-mail
  - spring-boot-starter-actuator
  - mariadb-java-client
  - flyway-core, flyway-mysql
  - lombok
  - spring-boot-starter-test, spring-security-test
- [ ] Set up project directory structure:
  ```
  com.hcb
  ├── config/
  ├── controller/ (customer/, admin/, api/)
  ├── service/ (order/, product/, payment/, email/)
  ├── repository/
  ├── model/ (entity/, dto/, enums/)
  ├── security/
  ├── validation/
  └── util/
  ```
- [ ] Configure `application.properties` for local development
- [ ] Set up Git repository (if not already)
- [ ] Create `.gitignore` (exclude passwords, IDE files, target/)
- [ ] Create database on VPS MariaDB: `CREATE DATABASE hcb;`
- [ ] Create `hcb_user` database user with proper privileges
- [ ] Deploy `schema.sql` to create all tables
- [ ] Verify all 8 tables created: users, user_roles, products, orders, order_items, settings, audit_logs, password_reset_tokens
- [ ] Configure Flyway for future migrations
- [ ] Test database connectivity from local dev machine
- [ ] Set up logging configuration (logback.xml)

**Deliverable:** Empty Spring Boot 3.3.4 app starts successfully, connects to database, health check responds.

**Verification:**
```bash
mvn clean package
java -jar target/hcb-1.0.0.jar
curl http://localhost:10001/actuator/health
# Expected: {"status":"UP"}
```

---

## Milestone 2: Customer Authentication + Products

**Goal:** Customers can register, log in, and browse products from database.

**Tasks:**

### Authentication
- [ ] Create `User` JPA entity (with email, mobile, password_hash, full_name, etc.)
- [ ] Create `UserRole` JPA entity (CUSTOMER, ADMIN)
- [ ] Create `UserRepository` interface
  - findByEmail()
  - findByMobile()
  - findByEmailOrMobile()
- [ ] Create `UserService` (register, authenticate, findUser)
- [ ] Implement bcrypt password hashing (strength 12)
- [ ] Create `PasswordResetToken` entity and repository
- [ ] Create Spring Security configuration:
  - Configure login/logout URLs
  - Configure authorization rules (admin vs customer)
  - Enable CSRF protection (Spring Security default)
  - Configure session management (30-minute timeout)
- [ ] Create Thymeleaf layout template (`layout.html`)
- [ ] Create login page (`/login`)
- [ ] Create registration page (`/register`)
- [ ] Implement server-side validation for registration form
- [ ] Test authentication flow (register → login → logout)

### Products
- [ ] Create `Product` JPA entity
- [ ] Create `ProductRepository` interface:
  - findBySlug()
  - findByActiveAndAvailableOrderBySortOrder()
  - findByIdForUpdate() with @Lock(PESSIMISTIC_WRITE)
  - decrementStock() with @Modifying atomic UPDATE
- [ ] Create `ProductService` (CRUD operations, stock management)
- [ ] Implement pessimistic locking for stock
- [ ] Create home page controller (`HomeController`)
- [ ] Port existing home page HTML to Thymeleaf (hero, features, ingredients)
- [ ] Create product listing page (`/products`)
- [ ] Fetch active products from database
- [ ] Display product cards (image, name, price, stock status)
- [ ] Create product detail page (`/products/{slug}`)
- [ ] Add "Out of Stock" badge when stock = 0
- [ ] Test product browsing flow

**Deliverable:** Customers can register, log in, browse products. All products come from database.

**Verification:**
- [ ] Register new customer account
- [ ] Log in with credentials
- [ ] View product listing (shows database products)
- [ ] View product detail page
- [ ] Log out

---

## Milestone 3: Cart + Checkout + Orders

**Goal:** End-to-end customer order flow working with stock validation and email confirmation.

**Tasks:**

### Cart (Client-Side)
- [ ] Port existing cart JavaScript (localStorage)
- [ ] Create cart modal/drawer (Thymeleaf partial)
- [ ] Display cart items with quantities
- [ ] Implement "Update Quantity" and "Remove" buttons
- [ ] Calculate subtotal client-side (display only, not trusted)
- [ ] Display shipping fee based on subtotal
- [ ] Calculate total (subtotal + shipping)

### Checkout
- [ ] Create checkout page (`/checkout`)
- [ ] Create checkout form (name, phone, address, city, state, pincode)
- [ ] Implement client-side form validation
- [ ] Create `Order` JPA entity
- [ ] Create `OrderItem` JPA entity
- [ ] Create `OrderRepository` and `OrderItemRepository`:
  - findByOrderNumber()
  - findByIdempotencyToken()
  - findByUserIdOrderByCreatedAtDesc()
  - findByStatusOrderByCreatedAtDesc()
- [ ] Create `OrderService`:
  - createOrder(OrderRequest) — with transaction
  - calculateTotal() — server-side, ignore client prices
  - updateOrderStatus()
  - getCustomerOrders()
- [ ] Implement checkout POST handler:
  - Validate CSRF token (automatic)
  - Validate delivery info (server-side)
  - Check idempotency token (prevent double-submit)
  - Start database transaction
  - Lock products for update (pessimistic lock)
  - Validate stock availability
  - Calculate total from database prices (ignore client)
  - Create Order entity
  - Create OrderItem entities (snapshot prices from database)
  - Decrement stock atomically
  - Generate order number: `HCB-` + (1000 + order.getId())
  - Commit transaction
  - Trigger email confirmation (async)
- [ ] Create order confirmation page (`/orders/{id}`)
- [ ] Display order details with UPI payment instructions
- [ ] Create "My Orders" page (`/orders`)
- [ ] Display customer's order history
- [ ] Test concurrent order creation (2 users ordering last item)

**Deliverable:** Customers can complete full checkout flow. Stock decrements safely. Order confirmation displayed.

**Verification:**
- [ ] Add products to cart
- [ ] Proceed to checkout
- [ ] Submit order with delivery info
- [ ] Verify stock decremented in database
- [ ] View order confirmation
- [ ] Verify order appears in "My Orders"
- [ ] Test: Two users ordering last item → one succeeds, one fails

---

## Milestone 4: UPI Payment + WhatsApp

**Goal:** Payment verification workflow complete (WhatsApp-only, no screenshot upload).

**Tasks:**

### UPI Integration
- [ ] Create `PaymentService`:
  - submitPayment(orderId, paymentDetails)
  - verifyPayment(orderId, adminId)
  - rejectPayment(orderId, reason, adminId)
  - generateUpiQr(upiId, amount, orderNumber)
- [ ] Implement UPI QR code generation
- [ ] Display UPI QR code on order confirmation page
- [ ] Display UPI ID (text) below QR code
- [ ] Display real name for UPI transfer
- [ ] Add WhatsApp button with pre-filled message:
  ```
  "Hi, I placed order HCB-1057 for ₹199. I've completed UPI payment. Please verify."
  ```
- [ ] Configure WhatsApp number in settings (or environment variable)

### Payment Status Updates
- [ ] Update order status when payment submitted: PAYMENT_PENDING → PAYMENT_SUBMITTED
- [ ] Implement payment status tracking in orders table (payment_status column)
- [ ] Add payment_verified_at, payment_verified_by columns
- [ ] Test payment submission flow

**Deliverable:** Order confirmation shows UPI QR + WhatsApp button. Payment workflow ready for admin verification (Milestone 5).

**Verification:**
- [ ] Place order
- [ ] View order confirmation page
- [ ] Verify UPI QR code displayed
- [ ] Verify UPI ID and name displayed
- [ ] Click WhatsApp button → opens WhatsApp with pre-filled message

---

## Milestone 5: Admin Panel

**Goal:** Admin can log in and manage products, orders, and payments.

**Tasks:**

### Admin Authentication
- [ ] Create admin login page (`/admin/login`)
- [ ] Configure separate admin authentication (same Spring Security, different role)
- [ ] Require `ADMIN` role for `/admin/**` URLs
- [ ] Create admin dashboard (`/admin/dashboard`)
- [ ] Display key metrics on dashboard:
  - Total orders today
  - Pending payments count
  - Low stock products count
  - Recent orders (last 10)
- [ ] Create admin navigation menu (products, orders, payments, settings)
- [ ] Update admin password from default (if not already done)

### Admin Product Management
- [ ] Create product list page (`/admin/products`)
- [ ] Display all products (active + inactive)
- [ ] Create "Add Product" page (`/admin/products/new`)
- [ ] Implement product creation form:
  - Name, slug, description, price, stock, image upload, sort order
- [ ] Create "Edit Product" page (`/admin/products/{id}/edit`)
- [ ] Implement product update form
- [ ] Implement image upload validation (MIME type, size < 5MB)
- [ ] Store images in `/var/lib/hcb/uploads/products/`
- [ ] Implement "Deactivate Product" button
- [ ] Implement stock update form
- [ ] Test product CRUD operations

### Admin Order Management
- [ ] Create order list page (`/admin/orders`)
- [ ] Implement order filtering (status, date range, customer search)
- [ ] Display order summary table (order number, customer, total, status)
- [ ] Create order detail page (`/admin/orders/{id}`)
- [ ] Display full order details:
  - Customer info
  - Delivery address
  - Order items with snapshot prices
  - Payment status
  - Order status
  - Timeline (created, payment verified, dispatched, etc.)
- [ ] Implement "Update Order Status" form
- [ ] Enforce state machine transitions (e.g., can't go from NEW to DELIVERED)
- [ ] Create `AuditLog` entity and `AuditLogRepository`
- [ ] Create `AuditService` to log all admin actions
- [ ] Log all order status changes to audit log
- [ ] Test order status updates

### Admin Payment Verification
- [ ] Create payment verification page (`/admin/payments`)
- [ ] List orders with PAYMENT_SUBMITTED status
- [ ] Display order number, customer, amount, submission time
- [ ] Implement "Verify Payment" button:
  - Update payment_status = VERIFIED
  - Update order_status = PAYMENT_VERIFIED
  - Record payment_verified_by (admin user ID)
  - Record payment_verified_at (timestamp)
  - Log to audit log
  - Trigger "payment verified" email (async)
- [ ] Implement "Reject Payment" button with reason field:
  - Update payment_status = REJECTED
  - Update order_status = PAYMENT_REJECTED
  - Record rejection_reason
  - Log to audit log
  - Trigger "payment rejected" email with reason
- [ ] Test payment verification workflow

### Admin Settings Management
- [ ] Create `Settings` entity and repository
- [ ] Create settings management page (`/admin/settings`)
- [ ] Display all configurable settings:
  - Shipping fee (₹)
  - Free shipping threshold (₹)
  - WhatsApp number
  - UPI ID
  - UPI name (real name for transfers)
  - Admin notification email
  - Low stock threshold
- [ ] Implement settings update form
- [ ] Log settings changes to audit log
- [ ] Test settings updates (verify changes take effect immediately)

**Deliverable:** Admin panel fully functional. Admin can manage products, orders, payments, and settings.

**Verification:**
- [ ] Admin logs in at `/admin/login`
- [ ] View dashboard with metrics
- [ ] Create new product
- [ ] Edit existing product
- [ ] Upload product image
- [ ] View order list with filters
- [ ] View order details
- [ ] Update order status (e.g., PREPARING → DISPATCHED)
- [ ] Verify payment for pending order
- [ ] Reject payment with reason
- [ ] Update settings (shipping fee, UPI ID)

---

## Milestone 6: Email Notifications

**Goal:** All transactional emails sent automatically.

**Tasks:**

### Email Configuration
- [ ] Configure SMTP in `application.properties`:
  ```properties
  spring.mail.host=${SMTP_HOST}
  spring.mail.port=${SMTP_PORT}
  spring.mail.username=${SMTP_USERNAME}
  spring.mail.password=${SMTP_PASSWORD}
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  ```
- [ ] Set environment variables:
  - SMTP_HOST (e.g., smtp.gmail.com for Gmail)
  - SMTP_PORT (e.g., 587)
  - SMTP_USERNAME (SMTP account username)
  - SMTP_PASSWORD (SMTP account password or app-specific password)
  - SMTP_FROM (e.g., noreply@healthychocobytes.in)

### Email Service
- [ ] Create `EmailService` with async methods:
  - sendOrderConfirmation(Order)
  - sendPaymentVerified(Order)
  - sendPaymentRejected(Order, reason)
  - sendOrderDispatched(Order)
  - sendOrderDelivered(Order)
  - sendPasswordReset(User, token)
  - sendWelcomeEmail(User)
  - sendAdminOrderNotification(Order)
- [ ] Implement async email sending (use @Async)
- [ ] Implement email retry logic (3 attempts with exponential backoff)
- [ ] Log email send success/failure

### Email Templates
- [ ] Create Thymeleaf email templates:
  - `email/order-confirmation.html` — Order details + UPI QR + WhatsApp link
  - `email/payment-verified.html` — Payment confirmed, order preparing
  - `email/payment-rejected.html` — Payment issue, reason, re-submit instructions
  - `email/order-dispatched.html` — Tracking info (if available)
  - `email/order-delivered.html` — Thank you message
  - `email/password-reset.html` — Reset link
  - `email/welcome.html` — Welcome new customer
  - `email/admin-new-order.html` — Admin notification with order summary
- [ ] Add company branding to all email templates (logo, colors)
- [ ] Test all email templates with sample data

### Email Integration
- [ ] Integrate email sending into order creation (order confirmation)
- [ ] Integrate email sending into payment verification (payment verified)
- [ ] Integrate email sending into payment rejection (payment rejected)
- [ ] Integrate email sending into order status updates (dispatched, delivered)
- [ ] Send admin notification on new order (to ashwin@ideaai.in)
- [ ] Integrate email sending into password reset flow

### Testing
- [ ] Test order confirmation email
- [ ] Test payment verified email
- [ ] Test payment rejected email
- [ ] Test order dispatched email
- [ ] Test password reset email
- [ ] Test welcome email
- [ ] Test admin notification email
- [ ] Verify all emails delivered within SMTP daily limit

**Deliverable:** All transactional emails sent automatically. Email templates branded and tested.

**Verification:**
- [ ] Place order → receive order confirmation email
- [ ] Admin verifies payment → customer receives payment verified email
- [ ] Admin rejects payment → customer receives rejection email with reason
- [ ] Admin updates order to DISPATCHED → customer receives dispatched email
- [ ] Admin receives notification email on new order
- [ ] Request password reset → receive reset link email

---

## Milestone 7: Security + Testing

**Goal:** Application hardened and fully tested before production deployment.

**Tasks:**

### Security Testing
- [ ] Test CSRF protection:
  - Try POST /checkout without CSRF token (should fail)
- [ ] Test IDOR protection:
  - Try accessing another user's order (should fail with 403)
  - Try accessing admin URLs as customer (should fail with 403)
- [ ] Test price manipulation:
  - Send fake prices in checkout request
  - Verify server uses database prices (not client prices)
- [ ] Test quantity manipulation:
  - Send negative quantity (should fail validation)
  - Send huge quantity (should fail validation)
- [ ] Test SQL injection:
  - Try SQL injection in product search
  - Try SQL injection in order filter
  - Verify parameterized queries prevent injection
- [ ] Test XSS:
  - Try XSS in product description
  - Try XSS in customer name
  - Verify Thymeleaf auto-escaping prevents XSS
- [ ] Test file upload security:
  - Try uploading .exe file (should reject)
  - Try uploading .php file (should reject)
  - Try uploading oversized file (should reject)
  - Verify only image/jpeg, image/png, image/webp accepted
- [ ] Test session timeout:
  - Log in, wait 30 minutes, try action (should redirect to login)
- [ ] Test concurrent ordering:
  - Stock = 1
  - Thread A: order product (quantity=1)
  - Thread B: order product (quantity=1)
  - Verify: One succeeds, one fails (stock never goes negative)
- [ ] Review all Spring Security headers present:
  - X-Frame-Options: DENY
  - X-Content-Type-Options: nosniff
  - Strict-Transport-Security (after HTTPS enabled)
  - Content-Security-Policy
- [ ] Fix any security issues discovered

### Integration Testing
- [ ] Test complete customer journey:
  1. Register account
  2. Log in
  3. Browse products
  4. Add to cart
  5. Checkout (submit order)
  6. View order confirmation
  7. Receive order confirmation email
  8. View order in "My Orders"
- [ ] Test complete admin journey:
  1. Admin logs in
  2. Create new product
  3. Receive new order notification email
  4. View pending payment
  5. Verify payment
  6. Update order status (PREPARING → DISPATCHED)
  7. Customer receives dispatched email
  8. Update settings (shipping fee)
- [ ] Test order state machine:
  - Verify valid transitions allowed (NEW → PAYMENT_PENDING → PAYMENT_VERIFIED → ...)
  - Verify invalid transitions blocked (NEW → DELIVERED should fail)
- [ ] Test idempotency:
  - Submit order with idempotency token abc123 → creates HCB-1057
  - Submit same order again with token abc123 → redirects to HCB-1057 (no duplicate)
- [ ] Test email delivery under load:
  - Create 5 orders simultaneously
  - Verify all 5 order confirmation emails sent
- [ ] Test database rollback on error:
  - Trigger error during order creation (e.g., invalid product ID)
  - Verify transaction rolled back (no partial order created)
- [ ] Test stock restoration on cancellation:
  - Create order (stock decrements)
  - Cancel order
  - Verify stock restored

### Load Testing
- [ ] Load test: 20 concurrent users browsing products
- [ ] Load test: 10 concurrent users placing orders
- [ ] Load test: 100 products in catalog (check query performance)
- [ ] Load test: 1000 orders in database (check pagination, filtering)
- [ ] Monitor application memory usage during load tests:
  - Verify memory < 512MB under load
  - Check for memory leaks (memory should stabilize, not grow indefinitely)
- [ ] Monitor database query performance (slow query log)
- [ ] Fix any performance issues discovered

**Deliverable:** Application hardened. All security tests pass. Integration tests pass. Load tests pass.

**Verification:**
- [ ] All security tests documented and passing
- [ ] All integration scenarios work end-to-end
- [ ] Concurrent orders safe (verified with test)
- [ ] Idempotency working (verified with test)
- [ ] Application memory stable under load
- [ ] No critical performance issues

---

## Milestone 8: Production Deployment

**Goal:** Application live on production VPS at https://hcb.ideaai.in

**Tasks:**

### Pre-Deployment
- [ ] Backup current database (on VPS)
- [ ] Backup current JAR (if upgrading existing deployment)
- [ ] Review deployment checklist in DEPLOYMENT-GUIDE.md
- [ ] Update production `application.properties` with correct values
- [ ] Set all environment variables in `/opt/hcb/.env`:
  - DB_PASSWORD (secure password)
  - SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD
  - ADMIN_EMAIL (ashwin@ideaai.in)
  - UPI_ID, UPI_NAME
- [ ] Build production JAR: `mvn clean package -DskipTests`
- [ ] Verify JAR size reasonable (~45MB)

### VPS Setup (if first deployment)
- [ ] SSH into VPS
- [ ] Verify port 10001 available: `netstat -tuln | grep 10001`
- [ ] Create `hcb` system user: `sudo useradd -r -s /bin/bash hcb`
- [ ] Create directories:
  - `/opt/hcb/`
  - `/var/lib/hcb/uploads/products/`
  - `/var/log/hcb/`
  - `/var/backups/hcb/database/`
  - `/var/backups/hcb/files/`
- [ ] Set directory permissions: `chown -R hcb:hcb /opt/hcb /var/lib/hcb /var/log/hcb /var/backups/hcb`
- [ ] Copy production `application.properties` to `/opt/hcb/`
- [ ] Secure permissions: `chmod 600 /opt/hcb/application.properties`

### Deploy Application
- [ ] Copy JAR to VPS: `scp target/hcb-1.0.0.jar user@vps:/tmp/`
- [ ] Move JAR: `sudo mv /tmp/hcb-1.0.0.jar /opt/hcb/`
- [ ] Set ownership: `sudo chown hcb:hcb /opt/hcb/hcb-1.0.0.jar`
- [ ] Create symlink: `sudo ln -sf /opt/hcb/hcb-1.0.0.jar /opt/hcb/hcb-current.jar`
- [ ] Create systemd service file: `/etc/systemd/system/hcb.service`
- [ ] Configure JVM memory limits: `-Xms256m -Xmx512m`
- [ ] Enable service: `sudo systemctl enable hcb`
- [ ] Start service: `sudo systemctl start hcb`
- [ ] Verify service running: `sudo systemctl status hcb`
- [ ] Check logs: `tail -f /var/log/hcb/application.log`
- [ ] Test health check: `curl http://localhost:10001/actuator/health` (should return `{"status":"UP"}`)

### Apache Configuration
- [ ] Create Apache virtual host file: `/etc/apache2/sites-available/hcb.conf`
- [ ] Configure reverse proxy to `http://127.0.0.1:10001/`
- [ ] Configure security headers (X-Frame-Options, CSP, etc.)
- [ ] Enable required Apache modules: `proxy`, `proxy_http`, `rewrite`, `ssl`, `headers`
- [ ] Enable site: `sudo a2ensite hcb.conf`
- [ ] Test Apache config: `sudo apache2ctl configtest` (should return "Syntax OK")
- [ ] Reload Apache: `sudo systemctl reload apache2`

### SSL Configuration
- [ ] Install Certbot: `sudo apt install certbot python3-certbot-apache`
- [ ] Obtain SSL certificate: `sudo certbot --apache -d hcb.ideaai.in`
- [ ] Verify HTTPS working: `curl -I https://hcb.ideaai.in`
- [ ] Test auto-renewal: `sudo certbot renew --dry-run`

### DNS Configuration
- [ ] Update Cloudflare DNS: Point `hcb.ideaai.in` A record to VPS IP
- [ ] Enable Cloudflare proxy (orange cloud) for DDoS protection
- [ ] Wait for DNS propagation (5-30 minutes)
- [ ] Test from external network: `curl -I https://hcb.ideaai.in`

### Backup Configuration
- [ ] Copy backup scripts to VPS:
  - `/opt/hcb/backup-database.sh`
  - `/opt/hcb/backup-files.sh`
- [ ] Make scripts executable: `chmod +x /opt/hcb/backup-*.sh`
- [ ] Set ownership: `chown hcb:hcb /opt/hcb/backup-*.sh`
- [ ] Schedule backups in crontab:
  - Database: Daily at 2 AM
  - Files: Weekly on Sunday at 3 AM
- [ ] Test backup scripts manually:
  - `sudo -u hcb /opt/hcb/backup-database.sh`
  - `sudo -u hcb /opt/hcb/backup-files.sh`
- [ ] Verify backups created in `/var/backups/hcb/`

### Health Check Configuration
- [ ] Create health check script: `/opt/hcb/healthcheck.sh`
- [ ] Make executable: `chmod +x /opt/hcb/healthcheck.sh`
- [ ] Schedule health check in crontab: Every 5 minutes
- [ ] Test health check script manually

### Production Smoke Test
- [ ] Test customer registration (create test account)
- [ ] Test customer login
- [ ] Test product browsing
- [ ] Test add to cart
- [ ] Test checkout flow (place test order)
- [ ] Verify order confirmation displayed
- [ ] Verify order confirmation email received
- [ ] Test admin login
- [ ] Verify admin can view pending payments
- [ ] Test payment verification
- [ ] Verify payment verified email received
- [ ] Test order status update (PREPARING → DISPATCHED)
- [ ] Verify order dispatched email received
- [ ] Test admin product creation
- [ ] Test admin settings update

### Monitoring
- [ ] Monitor logs for first 2 hours:
  - Application logs: `tail -f /var/log/hcb/application.log`
  - Apache access: `tail -f /var/log/apache2/hcb-ssl-access.log`
  - Apache error: `tail -f /var/log/apache2/hcb-ssl-error.log`
- [ ] Check resource usage:
  - Memory: `free -h`
  - Disk: `df -h`
  - CPU: `htop`
- [ ] Verify application memory < 512MB: `ps aux | grep java | grep hcb`
- [ ] Check for errors: `grep -i error /var/log/hcb/application.log`

**Deliverable:** Live production system at https://hcb.ideaai.in. All smoke tests passing. Monitoring active.

**Verification:**
- [ ] Website accessible from external network
- [ ] HTTPS working (SSL certificate valid)
- [ ] Customer can register and place order
- [ ] Admin can log in and manage orders
- [ ] All emails sending successfully
- [ ] Backups scheduled and running
- [ ] Health check running
- [ ] No critical errors in logs
- [ ] Resource usage within limits

---

## Post-Launch Tasks

**These can be done after launch, not blocking:**

### Immediate Post-Launch (Week 1)
- [ ] Monitor logs daily for errors
- [ ] Monitor server resources (CPU, RAM, disk)
- [ ] Respond to customer feedback
- [ ] Fix any production bugs discovered
- [ ] Update product catalog with real products/images (if not already done)
- [ ] Fine-tune email templates based on customer response
- [ ] Review security logs for suspicious activity

### Month 1
- [ ] Collect metrics:
  - Orders per day
  - Average order value
  - Conversion rate (visitors → orders)
  - Payment verification time (admin workflow)
  - Email delivery success rate
- [ ] Review slow queries (if any)
- [ ] Optimize images (compress, resize) if needed
- [ ] Update dependencies: `mvn versions:display-dependency-updates`
- [ ] Review audit logs

### Quarter 1
- [ ] Full security audit
- [ ] Consider penetration testing
- [ ] Performance review (response times, database query performance)
- [ ] Capacity planning:
  - Is 2GB VPS sufficient?
  - Do we need to upgrade to 4GB?
  - Are SMTP limits sufficient (100/day for Gmail)?
- [ ] Test backup restoration (quarterly drill)
- [ ] Plan feature enhancements based on usage:
  - Bulk product import (CSV)
  - Product variants (size, flavor)
  - Discount codes
  - Customer reviews
  - Invoice generation (PDF)
  - SMS notifications

---

## Critical Path Items

**Must finish before launch:**
1. ✅ Database schema deployed
2. ✅ Authentication working (customer + admin)
3. ✅ Order creation with stock validation
4. ✅ Payment verification workflow
5. ✅ Email notifications configured
6. ✅ Security testing passed
7. ✅ Apache reverse proxy configured
8. ✅ SSL certificate obtained
9. ✅ Backup scripts scheduled
10. ✅ Production smoke test passed

**Can be added post-launch:**
- Product variants (size, flavor)
- Discount codes / coupon system
- Customer reviews
- Advanced analytics dashboard
- Bulk product import
- SMS notifications
- Invoice PDF generation
- Advanced reporting
- Customer loyalty program

---

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|---------------------|
| **SMTP limit exceeded (100/day for Gmail)** | Monitor daily send count. Plan upgrade to SendGrid or AWS SES if orders exceed 80/day. |
| **VPS out of memory** | Monitor with `free -h` and `htop`. Alert if > 90%. Upgrade to 4GB VPS if consistently high. |
| **Database corruption** | Daily backups + quarterly restore drill. Keep 30 days of backups. |
| **Concurrent stock overselling** | Pessimistic locking + atomic UPDATE tested in Milestone 7. |
| **SSL certificate expiry** | Certbot auto-renewal enabled. Test with `certbot renew --dry-run`. |
| **Apache reverse proxy fails** | Monitor Apache logs. systemd restart policy for application. Keep rollback JAR. |
| **Application crash on startup** | systemd restart policy (max 3 attempts). Logs to diagnose. Rollback procedure documented. |
| **Payment verification backlog** | Admin receives email on new order. WhatsApp notifications. Monitor pending payments count on dashboard. |

---

## Progress Tracking

**Recommended approach:**

At the end of each work session, record progress:

```
Date: YYYY-MM-DD
Milestone: [Current Milestone]

Completed:
- [x] Task 1
- [x] Task 2

In Progress:
- [ ] Task 3 (estimated 50% complete)

Blocked:
- None / [Issue description if blocked]

Next Session Plan:
- [ ] Task 4
- [ ] Task 5

Notes:
- [Any important findings, decisions, or issues]
```

---

## Pre-Launch Checklist

**Before going live, confirm:**

### Configuration
- [ ] Database created, schema deployed, indexes verified
- [ ] Database user has least privilege (no global privileges)
- [ ] Admin password changed from default
- [ ] SMTP credentials configured and tested
- [ ] UPI ID and name configured
- [ ] WhatsApp number configured
- [ ] Admin email configured (ashwin@ideaai.in)
- [ ] Shipping fee and threshold configured

### Security
- [ ] All passwords changed from defaults
- [ ] CSRF protection enabled and tested
- [ ] XSS protection verified (Thymeleaf auto-escaping)
- [ ] HTTPS enforced (HTTP → HTTPS redirect)
- [ ] Security headers configured
- [ ] File upload validation working
- [ ] Admin-only URLs protected (tested with non-admin user)
- [ ] Port 10001 NOT exposed to internet (localhost only)
- [ ] Application runs as non-root user (`hcb`)
- [ ] File permissions secure (`application.properties` is 600)

### Functionality
- [ ] Customer registration works
- [ ] Customer login works
- [ ] Product browsing works
- [ ] Add to cart works
- [ ] Checkout flow works
- [ ] Order creation works
- [ ] Stock decrements correctly
- [ ] Concurrent orders safe (tested)
- [ ] Idempotency working (tested)
- [ ] UPI QR code displays
- [ ] WhatsApp link works
- [ ] Admin login works
- [ ] Admin can create/edit products
- [ ] Admin can verify payments
- [ ] Admin can update order status
- [ ] Admin can change settings
- [ ] Email notifications work (all templates tested)
- [ ] Order history displays correctly

### Operations
- [ ] Application auto-starts on reboot (systemd enabled)
- [ ] Application restarts on failure (systemd policy)
- [ ] Logs rotate properly (logback + logrotate)
- [ ] Backup scripts scheduled (cron)
- [ ] Backup scripts tested manually
- [ ] Backup restoration tested
- [ ] Health check endpoint responds
- [ ] Health check scheduled (cron)
- [ ] Apache reverse proxy working
- [ ] SSL certificate valid (Let's Encrypt)
- [ ] SSL auto-renewal tested (`certbot renew --dry-run`)
- [ ] DNS resolves correctly (Cloudflare)
- [ ] Firewall configured (ufw: allow 22, 80, 443)

### Performance
- [ ] Application memory < 512MB under load
- [ ] Database queries indexed (explain plan reviewed)
- [ ] Home page load time < 2 seconds
- [ ] Checkout completes in < 5 seconds
- [ ] Email sends in < 10 seconds
- [ ] 20 concurrent users tested

---

## Success Metrics (First Month)

### Technical Metrics
- **Uptime:** > 99% (< 7 hours downtime per month)
- **Response Time:** Average < 500ms (home page)
- **Security Incidents:** Zero critical
- **Data Loss:** Zero incidents
- **Email Delivery:** > 95% success rate

### Business Metrics
- **Orders Processed:** Track actual count
- **Customers Registered:** Track actual count
- **Average Order Value:** Track actual ₹ amount
- **Customer Satisfaction:** Informal feedback, monitor complaints

---

## Emergency Contacts

**Project Lead:** Ashwin Golani  
**Admin Email:** ashwin@ideaai.in  
**Domain:** hcb.ideaai.in  
**VPS:** [VPS IP Address]  
**Database:** MariaDB 10.11 on localhost:3306  
**Application Port:** 127.0.0.1:10001

---

## Documentation References

- **Architecture:** `docs/architecture/`
- **Database Schema:** `docs/database/schema.sql`
- **Deployment Guide:** `docs/deployment/DEPLOYMENT-GUIDE.md` (step-by-step)
- **Security Checklist:** `docs/security/SECURITY-CHECKLIST.md`
- **Quick Reference:** `docs/QUICK-REFERENCE.md` (commands, troubleshooting)
- **Project Config:** `docs/PROJECT-CONFIG.md` (configuration values)

---

## External Resources

- **Spring Boot Docs:** https://docs.spring.io/spring-boot/docs/current/reference/html/
- **Thymeleaf Docs:** https://www.thymeleaf.org/documentation.html
- **MariaDB Docs:** https://mariadb.com/kb/en/documentation/
- **Spring Security Docs:** https://docs.spring.io/spring-security/reference/
- **Let's Encrypt:** https://letsencrypt.org/
- **Certbot:** https://certbot.eff.org/

---

**Last Updated:** September 28, 2026  
**Architecture Version:** Spring Boot 3.3.4  
**Next Action:** Begin Milestone 1 — Foundation + Database

