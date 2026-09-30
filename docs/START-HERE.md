# 🚀 START HERE - HCB Implementation Guide

**All architectural corrections have been applied. Ready to begin implementation.**

---

## ✅ What's Been Done

### 1. Architecture Designed
- Modular monolith (Spring Boot + Thymeleaf + MariaDB)
- Appropriate for 2GB VPS, scales to 4GB/8GB easily
- Security hardened (CSRF, XSS, IDOR protection)
- Preserves existing HCB website design

### 2. Critical Corrections Applied
- ✅ Order number generation (database auto-increment, not MAX+1)
- ✅ Stock concurrency (explicit pessimistic locking)
- ✅ Order idempotency (duplicate prevention)
- ✅ Payment workflow (WhatsApp-only, no screenshot upload initially)
- ✅ Memory allocation (configurable, not fixed)
- ✅ SMTP (provider-agnostic, not Gmail-specific)
- ✅ JAR deployment (versioned, not overwrite)
- ✅ Backup strategy (supports future offsite)
- ✅ Timeline (milestone-based, not artificial day numbers)
- ✅ Security headers (modern, removed obsolete X-XSS-Protection)
- ✅ CSRF (Spring Security default, not over-specified)

### 3. Complete Documentation Created
- Architecture specifications (3 documents, 47KB)
- Database schema (complete with indexes, triggers, views)
- Deployment guide (26KB, step-by-step VPS setup)
- Security checklist (pre-launch verification)
- Implementation roadmap (milestone-based, 15-20 days)
- Quick reference (commands and common tasks)
- Configuration summary (all values documented)

---

## 📋 Your Action Plan

### Step 1: Review Corrections (5 minutes)
Read: **`docs/CORRECTIONS-APPLIED.md`**

This summarizes all 11 corrections applied to the architecture.

### Step 2: Understand Architecture (15 minutes)
Read in order:
1. `docs/architecture/00-EXECUTIVE-SUMMARY.md` — Big picture
2. `docs/architecture/ARCHITECTURE-CORRECTIONS.md` — Critical implementation details

### Step 3: Review Database Design (10 minutes)
Review: **`docs/database/schema.sql`**

Key tables:
- `users` + `user_roles` (authentication)
- `products` (catalog)
- `orders` + `order_items` (with idempotency_token)
- `settings` (configurable business rules)
- `audit_logs` (compliance)

### Step 4: Begin Implementation (Now!)
Follow: **`docs/tasks/IMPLEMENTATION-ROADMAP.md`**

Start with **Milestone 1: Foundation + Database**

---

## 🎯 Milestone 1 Tasks (Start Here)

### Task 1.1: Create Spring Boot Project
```bash
# Using Spring Initializr or IDE
# Dependencies: Web, Thymeleaf, Data JPA, Security, Validation, Mail, Actuator
# Java 21, Maven
```

### Task 1.2: Configure pom.xml
Add dependencies:
- spring-boot-starter-web
- spring-boot-starter-thymeleaf
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-mail
- spring-boot-starter-actuator
- mariadb-java-client
- flyway-core
- flyway-mysql
- lombok

### Task 1.3: Set Up Database
```bash
# On VPS or local MariaDB:
mysql -u root -p

CREATE DATABASE hcb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hcb_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON hcb.* TO 'hcb_user'@'localhost';
GRANT CREATE, DROP, INDEX, ALTER ON hcb.* TO 'hcb_user'@'localhost';
FLUSH PRIVILEGES;

# Import schema
mysql -u hcb_user -p hcb < docs/database/schema.sql
```

### Task 1.4: Configure application.properties
```properties
server.port=10001
server.address=127.0.0.1

spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/hcb
spring.datasource.username=hcb_user
spring.datasource.password=your_password_here

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### Task 1.5: Test Application Startup
```bash
mvn spring-boot:run

# Should see:
# Started HcbApplication in X seconds
# No errors

# Test health endpoint:
curl http://localhost:10001/actuator/health
# Expected: {"status":"UP"}
```

**✅ Milestone 1 Complete:** Foundation + Database working

---

## 📚 Documentation Quick Links

### For Implementation
- **Implementation Roadmap:** `docs/tasks/IMPLEMENTATION-ROADMAP.md` (all milestones)
- **Architecture Details:** `docs/architecture/01-SYSTEM-ARCHITECTURE.md`
- **Database Schema:** `docs/database/schema.sql`

### For Deployment (Later)
- **Deployment Guide:** `docs/deployment/DEPLOYMENT-GUIDE.md` (VPS setup)
- **Security Checklist:** `docs/security/SECURITY-CHECKLIST.md` (pre-launch)

### For Reference
- **Quick Reference:** `docs/QUICK-REFERENCE.md` (commands)
- **Configuration:** `docs/PROJECT-CONFIG.md` (all values)
- **Main README:** `docs/README.md` (documentation index)

---

## ⚠️ Critical Implementation Notes

### Order Number Generation
**DO NOT USE:**
```sql
-- WRONG: Race condition
SELECT MAX(order_number) + 1 FROM orders;
```

**USE THIS:**
```java
// CORRECT: Database auto-increment
order = orderRepository.save(order);
order.setOrderNumber("HCB-" + (1000 + order.getId()));
order = orderRepository.save(order);
```

### Stock Decrement
**Always use pessimistic locking:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id IN :ids")
List<Product> findByIdInWithLock(@Param("ids") List<Long> ids);
```

**Atomic UPDATE:**
```sql
UPDATE products SET stock_quantity = stock_quantity - ?
WHERE id = ? AND stock_quantity >= ? AND active = TRUE;
```

### Order Idempotency
**Always generate and validate token:**
```java
// On checkout page load
String token = UUID.randomUUID().toString();
session.setAttribute("checkout_idempotency_token", token);

// On checkout submit
Order existing = orderRepository.findByIdempotencyToken(idempotencyToken);
if (existing != null) {
    return "redirect:/orders/" + existing.getId();
}
```

### Price Calculation
**NEVER trust client prices:**
```java
// WRONG:
BigDecimal clientPrice = request.getPrice(); // ❌

// CORRECT:
Product product = productRepository.findById(productId);
BigDecimal serverPrice = product.getPrice(); // ✅
```

---

## 🧪 Testing Requirements

### Must Test Before Launch
1. **Concurrent orders** — Two customers order last item simultaneously
2. **Idempotency** — Customer double-clicks submit button
3. **Price manipulation** — Client sends fake price in request
4. **Stock overflow** — Order more than available stock
5. **IDOR** — Customer tries to access another customer's order
6. **CSRF** — POST request without CSRF token

---

## 📞 Support

**Project Lead:** Ashwin Golani  
**Email:** ashwin@ideaai.in  
**Domain:** hcb.ideaai.in  
**Timeline:** 15-20 days to launch

**Questions?**
1. Check `docs/QUICK-REFERENCE.md` for common tasks
2. Check `docs/architecture/ARCHITECTURE-CORRECTIONS.md` for design decisions
3. Check `docs/README.md` for document index

---

## 🎉 Ready to Build!

**Architecture:** ✅ Approved  
**Corrections:** ✅ Applied  
**Documentation:** ✅ Complete  
**Database Schema:** ✅ Ready  
**Next Step:** ✅ Milestone 1

---

**Begin Milestone 1 now: Create Spring Boot project and deploy database schema.**

Good luck! 🚀🍫
