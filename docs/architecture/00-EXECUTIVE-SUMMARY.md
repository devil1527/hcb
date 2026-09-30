# HCB System Architecture - Executive Summary

**Project:** Healthy Choco Bytes (HCB) - Chocolate Ordering Application  
**Target Domain:** hcb.ideaai.in  
**Timeline:** 15-20 days to launch  
**Architecture Date:** September 28, 2026  

## Current State

**Existing Website:**
- Single-page static HTML site
- 2 products (Healthy Protein Bar, Protein Chocolate Box)
- Client-side cart management
- WhatsApp checkout integration
- Premium chocolate brand aesthetic
- ~20MB assets (images, videos)

**Critical Issues:**
- ❌ No server validation
- ❌ No order persistence
- ❌ No customer accounts
- ❌ No admin panel
- ❌ No inventory management
- ❌ Price/quantity can be manipulated

## Target State

**Production Architecture:**
```
Cloudflare
    ↓
Apache HTTPS (:443)
    ↓
Spring Boot HCB (127.0.0.1:10001)
    ↓
MariaDB (127.0.0.1:3306)
```

**Technology Stack:**
- **Framework:** Spring Boot 3.3.4
- **Template Engine:** Thymeleaf (preserves existing HTML/CSS)
- **Database:** MariaDB 10.11 (already installed)
- **Security:** Spring Security 6
- **Deployment:** systemd service + Apache reverse proxy
- **JVM:** OpenJDK 21 (already installed)

## VPS Environment

**Current Resources:**
- RAM: 2GB (HCB allocation: 512MB max heap)
- Disk: 30GB (19GB free)
- OS: Ubuntu 24.04 LTS
- Java: OpenJDK 21

**Existing Services (DO NOT TOUCH):**
- Apache 2.4 (:80, :443)
- MariaDB 10.11 (:3306)
- IdeaAI Java app (:9999, ~400MB RAM)
- Ollama (:11434)
- SSH (:22)

**HCB Application:**
- Port: 127.0.0.1:10001 (internal only)
- Memory: 512MB max heap (256MB initial)
- User: `hcb` (dedicated system user)
- Service: `hcb.service` (systemd)

## Key Architectural Decisions

### ✅ What We're Doing

1. **Modular Monolith** — Right size for 2GB VPS, easy to maintain
2. **Thymeleaf Templates** — Preserves existing HTML/CSS, secure by default
3. **Server-Side Validation** — All pricing/stock calculated on server
4. **Pessimistic Locking** — Prevents stock overselling under concurrent orders
5. **Snapshot Pattern** — Order items preserve historical prices/names
6. **Manual Payment Verification** — Matches current business process, admin verifies UPI payments
7. **Audit Logging** — All sensitive actions logged for compliance
8. **Separate Admin Auth** — Admin login isolated from customer login

### ❌ What We're NOT Doing (And Why)

1. **No Microservices** — Overkill for 2GB VPS and small catalog
2. **No React/Vue SPA** — Unnecessary complexity, Thymeleaf sufficient
3. **No Payment Gateway** — Manual verification works, saves transaction fees
4. **No Redis** — Premature optimization for current scale
5. **No Kubernetes** — Enterprise overkill
6. **No OTP Login** — SMS costs, email reset sufficient
7. **No Cloud Services** — VPS sufficient, avoid vendor lock-in

## Business Configuration

**Email Notifications:**
- Provider: SMTP (provider-agnostic - can use Gmail, SendGrid, AWS SES, etc.)
- Admin notifications: ashwin@ideaai.in
- Templates: Order confirmation, payment verified, dispatched, etc.
- Initial setup: Gmail SMTP recommended (simple, free tier available)

**Payment Configuration:**
- Method: Manual UPI payment
- Admin verifies payments manually via WhatsApp (customer sends screenshot there)
- Display: UPI ID + QR code + Real name on checkout

**Product Catalog:**
- Initial: 2 products (Healthy Protein Bar, Protein Chocolate Box)
- Admin can add/edit products without developer
- Images stored: `/var/lib/hcb/uploads/products/`

**Shipping Rules:**
- Default shipping: ₹100
- Free shipping threshold: ₹499+
- Configurable via admin settings (no code changes)

## Scale Path

### Current (2GB VPS)
- Capacity: 10-50 concurrent users
- Orders: 100-500/day
- Memory: Comfortable fit with 850MB buffer

### Stage 2 (4GB VPS)
- Upgrade trigger: 200+ concurrent users, response time degradation
- Changes: Increase JVM heap to 1GB, tune connection pool
- No code changes required

### Stage 3 (8GB VPS)
- Upgrade trigger: 500+ concurrent users, disk space issues
- Changes: Increase heap to 2GB, separate file storage
- No architecture changes required

### Stage 4 (Horizontal Scaling)
- Upgrade trigger: National expansion, 1000+ concurrent users
- Changes: Load balancer, multiple app servers, database replication
- Architecture refactor: Extract background jobs

## Security Posture

**Authentication:**
- Customer: Email/mobile + bcrypt password (strength 12)
- Admin: Separate admin login, audit logged
- Session: HttpOnly, Secure, SameSite=Strict cookies
- Rate limiting: 5 failed attempts per 15 minutes

**Authorization:**
- Role-based: CUSTOMER, ADMIN
- IDOR protection: Users can only access own orders
- Admin actions: Require admin role verification on server

**Input Validation:**
- SQL Injection: Parameterized queries (JPA)
- XSS: Thymeleaf auto-escaping, CSP headers
- CSRF: Spring Security tokens on all mutating requests
- File Upload: MIME validation, UUID filenames, size limits

**Business Logic:**
- Price Manipulation: Server-side price lookup always
- Stock Race Conditions: Pessimistic locking on order creation
- Payment Status: Only admin can verify payments
- Order Status: State machine enforced on server

## Data Protection

**Backup Strategy:**
- Database: Daily full backup at 2 AM (30-day retention)
- Files: Weekly backup of uploads directory
- Location: `/var/backups/hcb/`
- Verification: Automated backup integrity checks

**Data Integrity:**
- Foreign key constraints
- Transaction boundaries
- Audit logs for all critical actions
- Soft deletes (products never physically deleted)
- Immutable order history (snapshot pattern)

## Implementation Phases

**Phase 1 (Week 1-2):** Foundation  
- Spring Boot project setup
- Database schema + migrations
- User authentication
- Product CRUD
- Admin panel foundation

**Phase 2 (Week 3):** Customer Frontend  
- Migrate existing HTML/CSS to Thymeleaf
- Dynamic product listing
- Customer registration/login
- Responsive design preserved

**Phase 3 (Week 4):** Ordering System  
- Checkout flow
- Order creation with stock validation
- UPI QR generation
- WhatsApp integration
- Order history

**Phase 4 (Week 5):** Admin Operations  
- Product management (upload images)
- Order management
- Payment verification interface
- Stock management
- Settings panel

**Phase 5 (Week 6):** Email & Notifications  
- SMTP configuration
- Transactional email templates
- Order confirmation emails
- Admin notifications

**Phase 6 (Week 7):** Testing & Hardening  
- Unit tests
- Integration tests
- Security testing (CSRF, XSS, IDOR)
- Load testing
- Penetration testing

**Phase 7 (Week 8):** Deployment  
- VPS setup (user, directories)
- systemd service configuration
- Apache virtual host + SSL
- Backup scripts
- Monitoring setup
- DNS cutover

**Phase 8 (Ongoing):** Post-Launch  
- Monitor performance
- Security updates
- Feature enhancements based on business needs

## Documentation Index

All architecture documentation is in `docs/architecture/`:

1. **00-EXECUTIVE-SUMMARY.md** — This document
2. **01-SYSTEM-ARCHITECTURE.md** — Detailed technical architecture
3. **02-TECHNOLOGY-STACK.md** — Technology choices and justifications
4. **03-SECURITY-ARCHITECTURE.md** — Security design and threat model
5. **04-SCALE-STRATEGY.md** — VPS upgrade path and scaling plan

Database documentation is in `docs/database/`:

1. **schema.sql** — Complete database schema
2. **SCHEMA-DESIGN.md** — Design decisions and rationale
3. **ENTITY-RELATIONSHIPS.md** — ER diagrams and relationships
4. **MIGRATION-STRATEGY.md** — Schema migration approach

Deployment documentation is in `docs/deployment/`:

1. **VPS-SETUP.md** — Server setup instructions
2. **DEPLOYMENT-GUIDE.md** — Deployment procedure
3. **APACHE-CONFIG.md** — Apache virtual host configuration
4. **SYSTEMD-SERVICE.md** — systemd service setup
5. **BACKUP-STRATEGY.md** — Backup and restore procedures
6. **MONITORING.md** — Monitoring and alerting setup

Implementation tasks are in `docs/tasks/`:

1. **PHASE-1-FOUNDATION.md** — Week 1-2 tasks
2. **PHASE-2-FRONTEND.md** — Week 3 tasks
3. **PHASE-3-ORDERING.md** — Week 4 tasks
4. **PHASE-4-ADMIN.md** — Week 5 tasks
5. **PHASE-5-EMAIL.md** — Week 6 tasks
6. **PHASE-6-TESTING.md** — Week 7 tasks
7. **PHASE-7-DEPLOYMENT.md** — Week 8 tasks
8. **PHASE-8-POSTLAUNCH.md** — Ongoing tasks

Security documentation is in `docs/security/`:

1. **THREAT-MODEL.md** — Security threats and mitigations
2. **AUTHENTICATION.md** — Authentication design
3. **AUTHORIZATION.md** — Authorization model
4. **SECURITY-CHECKLIST.md** — Pre-launch security checklist

## Developer Notes

**You mentioned:**
- ✅ Senior Java developer — Good, you can handle deployment
- ✅ SSH access to VPS — You'll deploy directly
- ✅ Gmail SMTP ready — We'll configure in application.properties
- ✅ Admin email: ashwin@ideaai.in
- ✅ Product catalog ready by Phase 4 — Timeline aligns

**Critical Reminders:**
1. **DO NOT touch existing IdeaAI application**
2. **Create dedicated `hcb` database and user**
3. **Run HCB as separate systemd service**
4. **Use port 10001 (verify availability first)**
5. **Test on staging before production cutover**

## Success Criteria

**Launch Checklist:**
- [ ] Customers can browse products
- [ ] Customers can create accounts and log in
- [ ] Customers can place orders
- [ ] Orders persist in database
- [ ] Admin can verify payments
- [ ] Admin can manage products without code changes
- [ ] Admin can update order statuses
- [ ] Email notifications work
- [ ] Stock deduction is safe under concurrent orders
- [ ] All prices calculated server-side
- [ ] Security testing passed
- [ ] Backup scripts configured and tested
- [ ] Apache reverse proxy working
- [ ] SSL certificate valid
- [ ] DNS pointing to VPS
- [ ] Application auto-starts on reboot

## Contact & Questions

**Project Lead:** Ashwin Golani  
**Admin Email:** ashwin@ideaai.in  
**Domain:** hcb.ideaai.in  
**Deployment Target:** 15-20 days

**Next Action:** Review all documentation in `docs/` folder and approve to begin Phase 1 implementation.
