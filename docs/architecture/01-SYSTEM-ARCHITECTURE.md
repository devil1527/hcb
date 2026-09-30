# HCB System Architecture

## Architecture Pattern: Modular Monolith

### Why Monolith?

**Advantages for HCB:**
1. **Resource Constraints** — 2GB VPS comfortable for single JVM process
2. **Operational Simplicity** — One deployment unit, one log stream, one health check
3. **Development Speed** — No network boundaries, no service coordination
4. **Transaction Integrity** — Database transactions span entire order flow
5. **Easy Debugging** — Single stack trace, no distributed tracing needed
6. **Scaling Path** — Can extract services later if genuinely needed

**Why NOT Microservices:**
- 2GB RAM insufficient for multiple JVMs (each needs 300-500MB)
- Operational complexity (service discovery, API gateway, message broker)
- Network latency between services
- Distributed transaction complexity
- Debugging difficulty
- Overkill for 2-product catalog

### Modular Structure

Despite being a monolith, code is organized in clean modules:

```
com.hcb
├── config/           # Spring configuration
├── controller/       # Web controllers
│   ├── customer/     # Customer-facing pages
│   ├── admin/        # Admin panel
│   └── api/          # REST APIs (if needed)
├── service/          # Business logic
│   ├── order/
│   ├── product/
│   ├── payment/
│   └── email/
├── repository/       # Data access
├── model/            # Domain entities
│   ├── entity/       # JPA entities
│   ├── dto/          # Data transfer objects
│   └── enums/        # Order status, payment status, etc.
├── security/         # Authentication & authorization
├── validation/       # Custom validators
└── util/             # Utilities
```

**Module Boundaries:**
- Controllers never access repositories directly (always through services)
- Services are transactional boundaries
- No circular dependencies between services
- Clear separation: customer vs admin controllers

## Technology Stack

### Backend Framework: Spring Boot 3.3.4

**Components Used:**
- **Spring Web** — Web MVC, REST APIs
- **Spring Data JPA** — Database access, repository pattern
- **Spring Security** — Authentication, authorization, CSRF protection
- **Spring Boot Mail** — Email notifications
- **Spring Boot Actuator** — Health checks, metrics
- **Spring Boot Validation** — Bean validation

**Why Spring Boot 3.3.4:**
- Stable release compatible with Java 21 (already on VPS)
- Already running successfully on VPS (IdeaAI application)
- Mature ecosystem
- Excellent documentation
- Built-in production features (Actuator)
- Easy to deploy as standalone JAR
- You're already a Java developer

### Template Engine: Thymeleaf

**Why Thymeleaf:**
- Server-side rendering (SEO-friendly)
- Secure by default (auto-escaping)
- Can reuse existing HTML/CSS
- No separate frontend build step
- Natural templating (valid HTML)
- Spring Boot integration

**Alternative Rejected:**
- React/Vue SPA — Adds build complexity, security burden, no real benefit
- JSP — Legacy, worse than Thymeleaf
- FreeMarker — Thymeleaf more Spring-idiomatic

### Database: MariaDB 10.11

**Why MariaDB:**
- Already installed on VPS
- Excellent for transactional workloads
- ACID compliant
- Foreign key support (InnoDB)
- Great performance for HCB's scale
- MySQL-compatible

**Alternative Rejected:**
- PostgreSQL — MariaDB already installed, no compelling reason to change
- MongoDB — No need for NoSQL, transactions are critical
- H2/SQLite — Not production-grade for multi-user

### Build Tool: Maven

**Why Maven:**
- Java ecosystem standard
- Dependency management
- Consistent builds
- Plugin ecosystem (for testing, code quality)

**pom.xml dependencies:**
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
    </dependency>
    
    <!-- Database Migration -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## System Components

### 1. Web Layer (Controllers)

**Customer Controllers:**
```
/                   → HomeController.home()
/products           → ProductController.list()
/products/{slug}    → ProductController.detail()
/cart               → CartController.view()
/checkout           → CheckoutController.checkout()
/orders             → OrderController.myOrders()
/orders/{id}        → OrderController.viewOrder()
/login              → AuthController.login()
/register           → AuthController.register()
/forgot-password    → AuthController.forgotPassword()
```

**Admin Controllers:**
```
/admin/login        → AdminController.login()
/admin/dashboard    → AdminController.dashboard()
/admin/products     → AdminProductController.list()
/admin/products/new → AdminProductController.create()
/admin/products/{id}/edit → AdminProductController.edit()
/admin/orders       → AdminOrderController.list()
/admin/orders/{id}  → AdminOrderController.view()
/admin/payments     → AdminPaymentController.pending()
/admin/settings     → AdminSettingsController.view()
```

### 2. Service Layer (Business Logic)

**OrderService:**
- `createOrder(OrderRequest)` — Create order with stock validation
- `calculateTotal(List<CartItem>)` — Calculate subtotal + shipping
- `updateOrderStatus(Long orderId, OrderStatus newStatus)` — State machine
- `getCustomerOrders(Long userId)` — Order history

**ProductService:**
- `findActiveProducts()` — List visible products
- `findBySlug(String slug)` — Product detail
- `createProduct(ProductRequest)` — Admin creates product
- `updateProduct(Long id, ProductRequest)` — Admin edits product
- `checkStock(Long productId, int quantity)` — Availability check
- `decrementStock(Long productId, int quantity)` — Atomic decrement

**PaymentService:**
- `submitPayment(Long orderId, PaymentDetails)` — Customer submits payment
- `verifyPayment(Long orderId, Long adminId)` — Admin verifies
- `rejectPayment(Long orderId, String reason, Long adminId)` — Admin rejects
- `generateUpiQr(String upiId, BigDecimal amount, String orderNumber)` — QR code

**UserService:**
- `register(RegistrationRequest)` — Create customer account
- `authenticate(String emailOrMobile, String password)` — Login
- `resetPassword(String email)` — Send reset link
- `changePassword(Long userId, String newPassword)` — Update password

**EmailService:**
- `sendOrderConfirmation(Order order)` — Async email
- `sendPaymentVerified(Order order)` — Async email
- `sendOrderDispatched(Order order)` — Async email
- `sendPasswordReset(User user, String token)` — Async email

**AuditService:**
- `logAdminAction(String action, String entityType, Long entityId, User admin)` — Audit log

### 3. Repository Layer (Data Access)

**JPA Repositories:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobile(String mobile);
    Optional<User> findByEmailOrMobile(String email, String mobile);
}

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByActiveAndAvailableOrderBySortOrder(boolean active, boolean available);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.id = :id AND p.stockQuantity >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdempotencyToken(String idempotencyToken);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}

public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Optional<Settings> findBySettingKey(String key);
}

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
```

### 4. Security Layer

**Spring Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/products/**", "/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/orders/**", "/checkout/**").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            )
            .csrf(csrf -> csrf
                // Spring Security default CSRF for Thymeleaf
            )
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().enable()
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**Admin Authentication:**
- Separate login page: `/admin/login`
- Requires `ADMIN` role
- All admin actions audit logged
- Session timeout: 30 minutes idle

**Customer Authentication:**
- Login page: `/login`
- Requires `CUSTOMER` role
- Remember me: 7 days vs 1 day
- Session timeout: 30 minutes idle

## Request Flow Examples

### Customer Places Order

```
1. Customer adds products to cart (client-side, localStorage)
2. Customer clicks "Checkout"
3. Browser POST /checkout with cart items + delivery info
4. CheckoutController receives request
5. CheckoutController validates CSRF token (Spring Security)
6. CheckoutController calls OrderService.createOrder()
7. OrderService starts database transaction
8. OrderService locks products for update (pessimistic lock)
9. OrderService validates stock availability
10. OrderService calculates total (server-side, ignores client prices)
11. OrderService creates Order entity
12. OrderService creates OrderItem entities (snapshot prices)
13. OrderService decrements stock atomically
14. OrderService commits transaction
15. OrderService calls EmailService.sendOrderConfirmation() (async)
16. CheckoutController redirects to /orders/{id} with success message
17. EmailService sends confirmation email (background thread)
```

### Admin Verifies Payment

```
1. Admin logs in at /admin/login
2. Admin navigates to /admin/payments (pending payments list)
3. Admin clicks order to view details
4. Admin clicks "Verify Payment" button
6. Browser POST /admin/payments/{orderId}/verify with CSRF token
7. AdminPaymentController validates admin role
8. AdminPaymentController calls PaymentService.verifyPayment()
9. PaymentService starts transaction
10. PaymentService updates order.paymentStatus = VERIFIED
11. PaymentService updates order.status = PAYMENT_VERIFIED
12. PaymentService records payment_verified_by (admin user ID)
13. PaymentService logs audit entry
14. PaymentService commits transaction
15. PaymentService calls EmailService.sendPaymentVerified() (async)
16. AdminPaymentController redirects with success message
17. EmailService sends payment verified email to customer
```

## State Management

### Session State
- User authentication stored in HTTP session
- Cart stored client-side (localStorage)
- CSRF token stored in cookie

### Database State
- Products: Current price, stock, availability
- Orders: Immutable after creation (snapshot)
- Order Items: Immutable (preserve historical data)
- Users: Mutable (profile updates allowed)
- Settings: Mutable (admin can change)

### Cache Strategy
- **No caching initially** (premature optimization)
- Future: Cache active products (5-minute TTL)
- Future: Cache settings (evict on update)

## Error Handling

### Exception Hierarchy
```
BusinessException (base)
├── InsufficientStockException
├── ProductNotFoundException
├── OrderNotFoundException
├── UnauthorizedException
├── InvalidPaymentException
└── InvalidStateTransitionException
```

### Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientStockException.class)
    public String handleInsufficientStock(InsufficientStockException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/stock-unavailable";
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex) {
        return "error/403";
    }
    
    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again.");
        return "error/500";
    }
}
```

## Logging Strategy

### Log Levels
- **ERROR** — Application errors, exceptions
- **WARN** — Business warnings (low stock, payment issues)
- **INFO** — Important business events (order created, payment verified)
- **DEBUG** — Service method entries/exits (development only)

### Log Locations
- Application logs: `/var/log/hcb/application.log`
- Audit logs: `/var/log/hcb/audit.log`
- Error logs: `/var/log/hcb/error.log` (ERROR level only)

### What to Log
- All exceptions
- All admin actions
- Order creation
- Payment verification
- Stock depletion
- Email send success/failure
- Authentication success/failure

### What NOT to Log
- Passwords
- Session tokens
- Database credentials
- Full credit card numbers (we don't handle these)
- Customer PII in INFO level (only in DEBUG)

## Performance Considerations

### Database Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Query Optimization
- Indexes on foreign keys
- Indexes on frequently queried columns (email, mobile, order_number)
- Avoid N+1 queries (use JOIN FETCH)
- Pagination on list queries

### File Upload
- Max file size: 5MB
- Accepted types: image/jpeg, image/png, image/webp
- Storage: `/var/lib/hcb/uploads/` (local filesystem)
- Future: Move to object storage if VPS disk fills

### Async Processing
- Email sending (non-blocking)
- Audit logging (async appender)
- Future: Background jobs (stock alerts, reports)

## Monitoring & Health

### Spring Boot Actuator Endpoints
```
/actuator/health     — Application health check (public)
/actuator/metrics    — JVM metrics (admin only)
/actuator/info       — Build info (public)
```

### Health Indicators
- Database connectivity
- Disk space (warn if < 1GB free)
- Memory usage (warn if > 90%)

### Metrics to Track
- Order creation rate
- Payment verification latency
- Email delivery success rate
- Database query performance
- JVM heap usage
- HTTP response times

## Deployment Architecture

### Production Stack
```
[Internet]
    ↓
[Cloudflare DNS + DDoS Protection]
    ↓
[Apache 2.4 HTTPS :443]
    ↓ (reverse proxy)
[Spring Boot :10001] ← localhost only
    ↓
[MariaDB :3306] ← localhost only
```

### File System Layout
```
/opt/hcb/
    hcb-1.0.0.jar         # Current release
    hcb-0.9.0.jar         # Previous release (rollback)
    application.properties # Environment-specific config
    backups/              # Old JAR backups

/var/lib/hcb/
    uploads/
        products/         # Product images
        
/var/log/hcb/
    application.log       # Application logs
    audit.log             # Audit logs
    error.log             # Error-only logs
    
/etc/systemd/system/
    hcb.service           # systemd service file
```

### Process Management
- Service: `hcb.service`
- User: `hcb` (non-root)
- Auto-start: Yes (enabled in systemd)
- Restart policy: On failure (max 3 retries)
- Graceful shutdown: SIGTERM (Spring Boot handles)

## Configuration Management

### Environment Variables
```bash
# Database
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=hcb
DB_USERNAME=hcb_user
DB_PASSWORD=<secure-password>

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=<gmail-address>
SMTP_PASSWORD=<app-password>
SMTP_FROM=noreply@healthychocobytes.in

# Admin
ADMIN_EMAIL=ashwin@ideaai.in

# Payment
UPI_ID=<upi-id>
UPI_NAME=<real-name>

# Application
SERVER_PORT=10001
UPLOAD_DIR=/var/lib/hcb/uploads
```

### application.properties
```properties
# Server
server.port=${SERVER_PORT:10001}
server.address=127.0.0.1

# Database
spring.datasource.url=jdbc:mariadb://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Email
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# File Upload
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
hcb.upload.directory=${UPLOAD_DIR}

# Security
spring.security.session.timeout=30m

# Logging
logging.level.root=WARN
logging.level.com.hcb=INFO
logging.file.name=/var/log/hcb/application.log

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

## Summary

This modular monolith architecture:
- ✅ Fits comfortably on 2GB VPS
- ✅ Preserves existing visual design (Thymeleaf)
- ✅ Addresses all security concerns
- ✅ Prevents data loss (transactions, snapshots)
- ✅ Scales vertically (4GB/8GB VPS)
- ✅ Separates concerns (controller/service/repository)
- ✅ Audit logged (compliance)
- ✅ Easy to deploy (single JAR)
- ✅ Easy to maintain (Java developer friendly)

**Next:** Review database schema design in `02-DATABASE-SCHEMA.md`
