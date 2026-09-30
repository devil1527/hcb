# HCB Project Configuration

**Critical configuration values for deployment**

---

## Project Information

| Key | Value |
|-----|-------|
| **Project Name** | HCB - Healthy Choco Bytes |
| **Domain** | hcb.ideaai.in |
| **Type** | Chocolate Ordering Application |
| **Framework** | Spring Boot 3.3.4 |
| **Java Version** | OpenJDK 21 |
| **Build Tool** | Maven |
| **Target Launch** | Mid-October 2026 (15-20 days) |

---

## VPS Environment

| Component | Value |
|-----------|-------|
| **OS** | Ubuntu 24.04 LTS |
| **RAM** | 2GB (upgradeable to 4GB/8GB) |
| **Disk** | 30GB (19GB free) |
| **CPU** | Current VPS spec (upgradeable) |
| **IP Address** | [Add your VPS IP here] |

---

## Network Configuration

| Service | Host | Port | Access |
|---------|------|------|--------|
| **HCB Application** | 127.0.0.1 | 10001 | localhost only |
| **MariaDB** | 127.0.0.1 | 3306 | localhost only |
| **Apache HTTP** | 0.0.0.0 | 80 | public |
| **Apache HTTPS** | 0.0.0.0 | 443 | public |
| **IdeaAI App** | 127.0.0.1 | 9999 | localhost only (DO NOT TOUCH) |
| **Ollama** | 127.0.0.1 | 11434 | localhost only (DO NOT TOUCH) |
| **SSH** | 0.0.0.0 | 22 | public (firewall protected) |

---

## Database Configuration

| Key | Value |
|-----|-------|
| **Database Name** | hcb |
| **Database User** | hcb_user |
| **Database Password** | [SET DURING DEPLOYMENT] |
| **Character Set** | utf8mb4 |
| **Collation** | utf8mb4_unicode_ci |
| **Engine** | InnoDB |
| **Connection Pool Size** | 10 (max), 5 (min idle) |

### Database Tables
- `users` — Customer accounts
- `user_roles` — User roles (CUSTOMER, ADMIN)
- `password_reset_tokens` — Password reset tokens
- `products` — Product catalog
- `orders` — Customer orders
- `order_items` — Order line items
- `settings` — Configurable settings
- `audit_logs` — Audit trail

---

## Application Configuration

| Key | Value |
|-----|-------|
| **Application Port** | 10001 |
| **Bind Address** | 127.0.0.1 (localhost only) |
| **Context Path** | / (root) |
| **Session Timeout** | 30 minutes |
| **JVM Initial Heap** | 256MB (-Xms256m) |
| **JVM Max Heap** | 512MB (-Xmx512m) |
| **Max Metaspace** | 128MB |
| **GC Algorithm** | G1GC |

---

## Email Configuration

| Key | Value |
|-----|-------|
| **Provider** | SMTP (provider-agnostic) |
| **SMTP Host** | Configurable via ${SMTP_HOST} |
| **SMTP Port** | Configurable via ${SMTP_PORT} |
| **SMTP Username** | Configurable via ${SMTP_USERNAME} |
| **SMTP Password** | Configurable via ${SMTP_PASSWORD} |
| **From Email** | Configurable via ${SMTP_FROM} |
| **Admin Email** | ashwin@ideaai.in |
| **Example Providers** | Gmail (smtp.gmail.com:587), SendGrid (smtp.sendgrid.net:587), AWS SES |

### Email Templates Required
- Order confirmation
- Payment verified
- Payment rejected
- Order dispatched
- Order delivered
- Password reset
- Welcome email
- Admin new order notification

---

## Payment Configuration

| Key | Value |
|-----|-------|
| **Payment Method** | Manual UPI verification |
| **UPI ID** | [Set during deployment or via admin settings] |
| **UPI Name** | Healthy Choco Bytes (or real name to display) |
| **Payment Gateway** | None (manual verification) |
| **Payment Verification** | Manual via WhatsApp (no screenshot upload in V1) |

---

## Business Configuration

| Setting | Default Value | Configurable? |
|---------|---------------|---------------|
| **Shipping Fee** | ₹100 | Yes (admin settings) |
| **Free Shipping Threshold** | ₹499 | Yes (admin settings) |
| **WhatsApp Number** | 918871921212 | Yes (admin settings) |
| **Business Address** | Legacy Vista, Pune, Maharashtra, India | Yes (admin settings) |
| **Max Order Quantity** | 50 per product | Yes (admin settings) |
| **Low Stock Threshold** | 5 | Yes (admin settings) |
| **Guest Checkout** | Disabled | Yes (admin settings) |

---

## File Storage Configuration

| Type | Location | Max Size | Allowed Types |
|------|----------|----------|---------------|
| **Product Images** | /var/lib/hcb/uploads/products/ | 5MB | image/jpeg, image/png, image/webp |
| **Application JAR** | /opt/hcb/ | ~45MB | N/A |
| **Application Logs** | /var/log/hcb/ | 100MB per file | N/A |
| **Database Backups** | /var/backups/hcb/database/ | Variable | .sql.gz |
| **File Backups** | /var/backups/hcb/files/ | Variable | .tar.gz |

---

## Backup Configuration

| Backup Type | Frequency | Retention | Location |
|-------------|-----------|-----------|----------|
| **Database** | Daily (2 AM) | 30 days | /var/backups/hcb/database/ |
| **Files (uploads)** | Weekly (Sunday 3 AM) | 90 days | /var/backups/hcb/files/ |
| **Application JAR** | On deployment | Last 5 versions | /opt/hcb/backups/ |

### Backup Scripts
- `/opt/hcb/backup-database.sh` — Database backup
- `/opt/hcb/backup-files.sh` — File backup
- Scheduled via cron (hcb user)

---

## Security Configuration

| Setting | Value |
|---------|-------|
| **Password Hashing** | bcrypt (strength 12) |
| **CSRF Protection** | Enabled (Spring Security) |
| **XSS Protection** | Enabled (Thymeleaf auto-escaping) |
| **HTTPS Enforced** | Yes (HTTP → HTTPS redirect) |
| **HSTS Header** | Enabled (max-age=31536000) |
| **SSL Certificate** | Let's Encrypt (auto-renewal) |
| **Session Cookie Flags** | HttpOnly, Secure, SameSite=Strict |
| **Rate Limiting** | 5 login attempts per 15 minutes |
| **Account Lockout** | 10 failed attempts |
| **Content Security Policy** | Configured (see Apache config) |

### Security Headers
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`

---

## Logging Configuration

| Log Type | Location | Max Size | Retention |
|----------|----------|----------|-----------|
| **Application** | /var/log/hcb/application.log | 100MB | 30 days |
| **Audit** | /var/log/hcb/audit.log | 100MB | 90 days |
| **Error** | /var/log/hcb/error.log | 100MB | 30 days |
| **Backup** | /var/log/hcb/backup.log | 50MB | 30 days |
| **Apache Access** | /var/log/apache2/hcb-ssl-access.log | Managed by logrotate | 14 days |
| **Apache Error** | /var/log/apache2/hcb-ssl-error.log | Managed by logrotate | 14 days |

### Log Levels
- Production: `INFO` (application), `WARN` (root)
- Development: `DEBUG` (application), `INFO` (root)

---

## Monitoring Configuration

| Endpoint | URL | Access |
|----------|-----|--------|
| **Health Check** | /actuator/health | Public |
| **Application Info** | /actuator/info | Public |
| **Metrics** | /actuator/metrics | Admin only |

### Health Check Schedule
- Cron job every 5 minutes
- Alert on failure (email to ashwin@ideaai.in)
- Auto-restart on failure (systemd policy)

---

## System Users & Permissions

### Application User
```
Username: hcb
UID: 999 (system user)
Home: /opt/hcb
Shell: /bin/bash
Group: hcb (GID: 999)
```

### File Permissions
```
/opt/hcb/                      → 755 (hcb:hcb)
/opt/hcb/application.properties → 600 (hcb:hcb)
/opt/hcb/*.jar                 → 755 (hcb:hcb)
/opt/hcb/backups/              → 700 (hcb:hcb)
/var/lib/hcb/                  → 755 (hcb:hcb)
/var/log/hcb/                  → 755 (hcb:hcb)
/var/backups/hcb/              → 700 (hcb:hcb)
```

---

## Apache Configuration

### Virtual Host
**File:** `/etc/apache2/sites-available/hcb.conf`

**Key Settings:**
- ServerName: `hcb.ideaai.in`
- ServerAdmin: `ashwin@ideaai.in`
- ProxyPass: `http://127.0.0.1:10001/`
- SSL Certificate: Let's Encrypt (auto-renewal via Certbot)

### Required Modules
- mod_proxy
- mod_proxy_http
- mod_rewrite
- mod_ssl
- mod_headers

---

## systemd Service Configuration

**File:** `/etc/systemd/system/hcb.service`

**Key Settings:**
- User: `hcb`
- ExecStart: Java with memory limits (-Xms256m -Xmx512m)
- Restart Policy: `on-failure` (max 3 attempts)
- Auto-start: Enabled (`systemctl enable hcb`)

---

## DNS Configuration (Cloudflare)

| Record Type | Name | Value | Proxy Status |
|-------------|------|-------|--------------|
| **A** | hcb | [VPS IP Address] | Proxied (orange cloud) |

**TTL:** Auto  
**SSL/TLS Mode:** Full (strict) or Flexible

---

## Initial Admin Account

| Field | Value |
|-------|-------|
| **Email** | ashwin@ideaai.in |
| **Mobile** | 9999999999 (placeholder) |
| **Password** | changeme123 (MUST CHANGE IMMEDIATELY) |
| **Full Name** | Ashwin Golani |
| **Role** | ADMIN |
| **Email Verified** | TRUE |
| **Active** | TRUE |

**⚠️ CRITICAL:** Change admin password immediately after first login!

---

## Environment Variables Template

**Create file:** `/opt/hcb/.env` (not committed to Git)

```bash
# Database
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=hcb
DB_USERNAME=hcb_user
DB_PASSWORD=<SECURE_PASSWORD_HERE>

# Email (Provider-Agnostic SMTP)
# Example for Gmail: smtp.gmail.com:587
# Example for SendGrid: smtp.sendgrid.net:587
SMTP_HOST=<SMTP_HOST_HERE>
SMTP_PORT=<SMTP_PORT_HERE>
SMTP_USERNAME=<SMTP_USERNAME_HERE>
SMTP_PASSWORD=<SMTP_PASSWORD_HERE>
SMTP_FROM=noreply@healthychocobytes.in

# Admin
ADMIN_EMAIL=ashwin@ideaai.in

# Payment
UPI_ID=<UPI_ID_HERE>
UPI_NAME=<REAL_NAME_HERE>

# Application
SERVER_PORT=10001
UPLOAD_DIR=/var/lib/hcb/uploads
```

**Permissions:** `chmod 600 /opt/hcb/.env` (readable only by hcb user)

---

## Maven Dependencies (pom.xml)

### Key Dependencies
- `spring-boot-starter-web` — Web MVC
- `spring-boot-starter-thymeleaf` — Template engine
- `spring-boot-starter-data-jpa` — Database access
- `spring-boot-starter-security` — Authentication/authorization
- `spring-boot-starter-validation` — Bean validation
- `spring-boot-starter-mail` — Email notifications
- `spring-boot-starter-actuator` — Health checks/metrics
- `mariadb-java-client` — Database driver
- `flyway-core` — Database migrations
- `flyway-mysql` — Flyway MariaDB support
- `lombok` — Boilerplate reduction

### Testing Dependencies
- `spring-boot-starter-test` — Testing framework
- `spring-security-test` — Security testing

---

## Product Catalog (Initial)

**2 products from existing website:**

1. **Healthy Protein Bar**
   - Slug: `healthy-protein-bar`
   - Price: ₹69
   - Description: "Packed with pea protein, rich dry fruits, and unsweetened peanut butter for pure plant energy. (1 Pc)"
   - Image: `banner.png` (from existing site)

2. **Protein Chocolate Box**
   - Slug: `protein-chocolate-box`
   - Price: ₹69
   - Description: "Made with 55% Morde dark chocolate compound, ragi murmura crunch, and wholesome protein. (Box of 4 Pcs)"
   - Image: `chocolate.png` (from existing site)

**Note:** Admin can add more products via Admin UI after launch.

---

## Order States

```
NEW 
  → PAYMENT_PENDING 
  → PAYMENT_SUBMITTED 
  → PAYMENT_VERIFIED 
  → PREPARING 
  → DISPATCHED 
  → DELIVERED
```

**Alternative paths:**
- `PAYMENT_SUBMITTED` → `PAYMENT_REJECTED`
- `Any state` → `CANCELLED`
- `After payment` → `REFUNDED`

---

## Performance Targets

| Metric | Target | Max Acceptable |
|--------|--------|----------------|
| **Application Memory** | 350MB | 512MB |
| **Database Memory** | 200MB | 300MB |
| **Home Page Load** | < 500ms | < 1s |
| **Checkout Time** | < 2s | < 5s |
| **Order Creation** | < 1s | < 3s |
| **Concurrent Users** | 20 | 50 |
| **Orders per Day** | 200 | 500 |

**If exceeding limits:** Consider 4GB VPS upgrade

---

## Capacity Planning

### Current (2GB VPS)
- Concurrent users: 10-50
- Orders per day: 100-500
- Product catalog: 50-100 products
- Orders in database: 10,000-50,000

### Upgrade to 4GB VPS When:
- Concurrent users > 50
- Memory usage consistently > 80%
- Response times degrading

### Upgrade to 8GB VPS When:
- Concurrent users > 200
- Orders per day > 2,000
- Disk space low (> 80% used)

---

## Pre-Launch Checklist

**Configuration:**
- [ ] Database created and schema deployed
- [ ] Database user created with proper privileges
- [ ] Admin password changed from default
- [ ] SMTP credentials configured
- [ ] UPI ID and name configured
- [ ] WhatsApp number configured
- [ ] Business address configured
- [ ] Admin email configured

**Deployment:**
- [ ] Application JAR deployed to /opt/hcb/
- [ ] systemd service created and enabled
- [ ] Apache virtual host configured
- [ ] SSL certificate obtained (Let's Encrypt)
- [ ] DNS pointing to VPS (Cloudflare)
- [ ] Firewall configured (ufw)
- [ ] Backup scripts scheduled (cron)
- [ ] Health check scheduled (cron)

**Testing:**
- [ ] Customer registration works
- [ ] Customer login works
- [ ] Product browsing works
- [ ] Add to cart works
- [ ] Checkout flow works
- [ ] Order creation works
- [ ] Stock decrements correctly
- [ ] Email notifications work
- [ ] Admin login works
- [ ] Admin can manage products
- [ ] Admin can verify payments
- [ ] Admin can update order status
- [ ] Security checklist completed

---

## Post-Launch Monitoring

**Daily:**
- [ ] Check application status
- [ ] Review error logs
- [ ] Monitor disk space
- [ ] Monitor memory usage

**Weekly:**
- [ ] Review audit logs
- [ ] Check backup success
- [ ] Review Apache logs

**Monthly:**
- [ ] System updates
- [ ] Dependency updates
- [ ] Test backup restoration

**Quarterly:**
- [ ] Security audit
- [ ] Performance review
- [ ] Capacity planning

---

## Support & Escalation

**Level 1 (Self-service):**
- Check logs: `/var/log/hcb/application.log`
- Check service status: `systemctl status hcb`
- Review QUICK-REFERENCE.md troubleshooting section

**Level 2 (Developer - You):**
- SSH into VPS
- Review application logs
- Check database
- Restart services if needed

**Level 3 (Escalation):**
- VPS provider support (hardware/network issues)
- Cloudflare support (DNS/DDoS issues)
- Spring Boot community forums (framework issues)

---

## Important URLs

**Production:**
- Website: https://hcb.ideaai.in
- Admin: https://hcb.ideaai.in/admin/login
- Health: https://hcb.ideaai.in/actuator/health

**Development Resources:**
- Spring Boot Docs: https://docs.spring.io/spring-boot/
- Thymeleaf Docs: https://www.thymeleaf.org/
- MariaDB Docs: https://mariadb.com/kb/
- Let's Encrypt: https://letsencrypt.org/
- Certbot: https://certbot.eff.org/

---

**Configuration document created:** September 28, 2026  
**Last reviewed:** September 28, 2026  
**Next review:** Before deployment (Day 19)

---

**Ready to deploy? Follow [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md) step-by-step!**
