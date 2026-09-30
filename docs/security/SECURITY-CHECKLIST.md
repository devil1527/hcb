# HCB Security Checklist

**Use this checklist before launch and quarterly thereafter**

---

## Authentication & Session Security

### Password Security
- [ ] All passwords use bcrypt with strength 12 or higher
- [ ] Default admin password changed from placeholder
- [ ] No passwords stored in plaintext anywhere
- [ ] No passwords in Git repository
- [ ] No passwords in log files
- [ ] Password reset tokens expire after 1 hour
- [ ] Password reset tokens single-use only
- [ ] Password complexity requirements enforced (min 8 chars)

### Session Management
- [ ] Sessions use secure cookies (HttpOnly=true)
- [ ] Sessions use Secure flag in production (HTTPS only)
- [ ] Sessions use SameSite=Strict
- [ ] Session timeout configured (30 minutes idle)
- [ ] Session regeneration after login
- [ ] Session invalidation on logout
- [ ] Concurrent session limit enforced (1 session per user)
- [ ] Session fixation protection enabled (Spring Security default)

### Login Protection
- [ ] Rate limiting on login endpoint (5 attempts per 15 min)
- [ ] Account lockout after 10 failed attempts
- [ ] Failed login attempts logged
- [ ] Successful login logged with IP/user-agent
- [ ] No account enumeration (same error for invalid email/password)
- [ ] CAPTCHA on password reset (optional, but recommended)

---

## Authorization & Access Control

### Role-Based Access Control
- [ ] Admin role required for `/admin/**` URLs
- [ ] Customer role required for `/orders/**` URLs
- [ ] Authorization checked on server-side (not just UI)
- [ ] `@PreAuthorize` annotations on sensitive methods
- [ ] Authorization failures logged

### IDOR Protection
- [ ] Users can only access their own orders
- [ ] Order ID access validated against user session
- [ ] Product IDs validated (exist and active)
- [ ] Admin-only actions require admin role verification
- [ ] No direct object references in URLs where possible

### Admin Security
- [ ] Separate admin authentication (not shared with customers)
- [ ] All admin actions audit logged
- [ ] Admin login failures alerted
- [ ] Admin account cannot be deleted via UI
- [ ] Admin password reset requires email verification

---

## Input Validation & Injection Protection

### SQL Injection
- [ ] All database queries use parameterized statements (JPA/JDBC)
- [ ] No string concatenation in SQL queries
- [ ] No raw SQL executed with user input
- [ ] Product search uses safe query methods
- [ ] Order filtering uses safe query methods

### XSS (Cross-Site Scripting)
- [ ] Thymeleaf auto-escaping enabled (default)
- [ ] No `th:utext` used with user input
- [ ] Content-Security-Policy header configured
- [ ] User input sanitized before storage
- [ ] Product descriptions HTML-escaped on display
- [ ] Customer names HTML-escaped on display

### CSRF (Cross-Site Request Forgery)
- [ ] Spring Security CSRF protection enabled
- [ ] CSRF tokens on all POST/PUT/DELETE forms
- [ ] CSRF tokens validated on server
- [ ] CSRF token auto-added by Spring Security (Thymeleaf default)
- [ ] CSRF failures logged

### Path Traversal
- [ ] File uploads use UUID-based filenames (not user-provided)
- [ ] No user-controlled file paths
- [ ] Upload directory outside web root
- [ ] Symbolic links not followed in upload directory

---

## File Upload Security

### Upload Validation
- [ ] File size limit enforced (5MB for images)
- [ ] MIME type validation (accept only image/jpeg, image/png, image/webp)
- [ ] Magic byte verification (not just file extension)
- [ ] Filename sanitization (UUID-based storage)
- [ ] No executable files allowed (.exe, .sh, .php, etc.)
- [ ] Upload directory has no execute permission

### Storage Security
- [ ] Uploads stored outside application directory
- [ ] Uploads directory: `/var/lib/hcb/uploads/`
- [ ] Uploads served through controlled endpoint (not direct file access)
- [ ] No directory listing enabled
- [ ] Proper file permissions (644 for files, 755 for directories)

---

## Business Logic Security

### Price Manipulation
- [ ] All prices fetched from database (never trusted from client)
- [ ] Order total calculated server-side
- [ ] Shipping fee calculated server-side
- [ ] Client-sent prices ignored completely
- [ ] Price changes audit logged

### Quantity Manipulation
- [ ] Quantity validated server-side (1-50 per product)
- [ ] Negative quantities rejected
- [ ] Zero quantities rejected
- [ ] Excessive quantities rejected (> max allowed)

### Stock Race Conditions
- [ ] Stock decrement uses pessimistic locking
- [ ] Stock decrement is atomic (UPDATE with WHERE condition)
- [ ] Concurrent order handling tested
- [ ] Out-of-stock products cannot be ordered
- [ ] Stock quantity never goes negative

### Payment Status Tampering
- [ ] Only admins can verify payments
- [ ] Payment verification logged with admin ID
- [ ] Payment status changes audit logged
- [ ] No client-side payment verification
- [ ] UPI transaction ID stored for reference

### Order Status Manipulation
- [ ] Order status changes validated against state machine
- [ ] Invalid status transitions blocked
- [ ] Status changes require admin authorization
- [ ] Status changes audit logged
- [ ] Cancelled orders restore stock atomically

---

## Infrastructure Security

### Network Security
- [ ] Application bound to localhost only (127.0.0.1:10001)
- [ ] Database bound to localhost only (127.0.0.1:3306)
- [ ] No unnecessary ports exposed
- [ ] Firewall configured (ufw or iptables)
- [ ] SSH key authentication enabled
- [ ] Root SSH login disabled
- [ ] Fail2Ban configured for Apache/SSH

### HTTPS & TLS
- [ ] HTTPS enforced (HTTP redirects to HTTPS)
- [ ] Valid SSL certificate (Let's Encrypt)
- [ ] TLS 1.2+ required (no SSLv3, TLS 1.0, TLS 1.1)
- [ ] Strong cipher suites configured
- [ ] HSTS header enabled (Strict-Transport-Security)
- [ ] Certificate auto-renewal working (Certbot)

### Web Server Security
- [ ] Apache version up-to-date
- [ ] Unnecessary Apache modules disabled
- [ ] Server signature hidden (ServerTokens Prod)
- [ ] Directory listing disabled
- [ ] `.htaccess` files disabled (AllowOverride None)
- [ ] Request size limits configured
- [ ] Timeout limits configured

### Application Security
- [ ] Application runs as non-root user (`hcb`)
- [ ] Application directory read-only (except logs/uploads)
- [ ] Log directory writable only by `hcb` user
- [ ] Upload directory writable only by `hcb` user
- [ ] Configuration files have restrictive permissions (600)
- [ ] No secrets in Git repository

---

## Security Headers

**Verify all headers present:**
```bash
curl -I https://hcb.ideaai.in | grep -i "x-\|strict\|content-security"
```

### Required Headers
- [ ] `X-Frame-Options: DENY`
- [ ] `X-Content-Type-Options: nosniff`
- [ ] `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- [ ] `Content-Security-Policy: [policy]`
- [ ] `Referrer-Policy: strict-origin-when-cross-origin`
- [ ] `Permissions-Policy: geolocation=(), microphone=(), camera=()`

---

## Data Protection

### Secrets Management
- [ ] Database passwords in environment variables
- [ ] SMTP passwords in environment variables
- [ ] No secrets in application.properties (use placeholders)
- [ ] No secrets in Git repository
- [ ] Configuration files have restrictive permissions
- [ ] Secrets not logged
- [ ] Secrets not exposed in error messages

### Database Security
- [ ] Dedicated database user (`hcb_user`)
- [ ] Database user has minimum required privileges
- [ ] No root database access from application
- [ ] Database bound to localhost only
- [ ] Database backups encrypted (optional, consider for PII)
- [ ] Database credentials rotated regularly (quarterly)

### Backup Security
- [ ] Backups stored with restricted permissions (700)
- [ ] Backup scripts have credentials secured
- [ ] Backup integrity verified after creation
- [ ] Backup restoration tested quarterly
- [ ] Old backups cleaned up (30-day retention)
- [ ] Backups not accessible via web

### Audit Logging
- [ ] All admin actions logged
- [ ] Product price changes logged
- [ ] Product stock changes logged
- [ ] Order status changes logged
- [ ] Payment verifications logged
- [ ] Settings changes logged
- [ ] Failed authentication attempts logged
- [ ] Suspicious activity logged
- [ ] Audit logs include IP address and user-agent
- [ ] Audit logs tamper-proof (insert-only)

---

## Error Handling

### Error Messages
- [ ] No stack traces displayed to users in production
- [ ] Generic error messages for authentication failures
- [ ] No database errors displayed to users
- [ ] No file path disclosure in error messages
- [ ] No sensitive data in error messages
- [ ] Detailed errors only in server logs

### Exception Handling
- [ ] Global exception handler configured
- [ ] Unexpected exceptions caught and logged
- [ ] Database connection failures handled gracefully
- [ ] Email send failures don't block order creation
- [ ] File upload failures handled gracefully

---

## Testing & Validation

### Security Testing Performed
- [ ] CSRF attack tested (POST without token)
- [ ] IDOR attack tested (access other user's order)
- [ ] XSS tested (inject scripts in forms)
- [ ] SQL injection tested (inject SQL in search/filter)
- [ ] Path traversal tested (../ in file uploads)
- [ ] Price manipulation tested (send fake prices)
- [ ] Quantity manipulation tested (negative/huge quantities)
- [ ] Session hijacking tested (steal session cookie)
- [ ] Concurrent ordering tested (race condition)
- [ ] File upload attack tested (.exe, .php uploads)

### Penetration Testing
- [ ] OWASP Top 10 vulnerabilities checked
- [ ] Automated scanner run (ZAP, Burp Suite, or Acunetix)
- [ ] Manual penetration testing performed
- [ ] Findings documented and fixed
- [ ] Re-test after fixes

---

## Compliance & Documentation

### Privacy & Legal
- [ ] Privacy policy published
- [ ] Terms and conditions published
- [ ] Cookie consent implemented (if using analytics)
- [ ] GDPR considerations reviewed (even for Indian business)
- [ ] Customer data retention policy defined
- [ ] Data deletion procedure documented

### Documentation
- [ ] Security architecture documented
- [ ] Authentication flow documented
- [ ] Authorization model documented
- [ ] Incident response plan documented
- [ ] Password reset procedure documented
- [ ] Admin account recovery procedure documented

---

## Monitoring & Alerting

### Security Monitoring
- [ ] Failed login attempts monitored
- [ ] Unusual IP addresses flagged
- [ ] High error rates alerted
- [ ] Database connection failures alerted
- [ ] Disk space low alerted
- [ ] SSL certificate expiry monitored (Certbot)
- [ ] Application downtime alerted

### Log Monitoring
- [ ] Application logs reviewed daily
- [ ] Apache access logs reviewed weekly
- [ ] Apache error logs reviewed daily
- [ ] Audit logs reviewed weekly
- [ ] Suspicious patterns flagged
- [ ] Automated alerts for critical errors

---

## Maintenance & Updates

### Regular Updates
- [ ] System packages updated monthly
- [ ] Java/JDK updated when security patches released
- [ ] Spring Boot updated quarterly
- [ ] Maven dependencies updated quarterly
- [ ] Dependency vulnerability scan performed (OWASP Dependency-Check)
- [ ] Known vulnerabilities remediated

### Security Audits
- [ ] Quarterly security checklist review
- [ ] Quarterly penetration testing
- [ ] Annual third-party security audit (if budget allows)
- [ ] Code review for security issues
- [ ] Configuration review for security hardening

---

## Incident Response

### Preparation
- [ ] Incident response plan documented
- [ ] Contact list maintained (admin email, VPS provider)
- [ ] Backup restore procedure tested
- [ ] Rollback procedure tested
- [ ] Emergency communication plan ready

### Response Capabilities
- [ ] Can identify security incident from logs
- [ ] Can stop application immediately (systemctl stop)
- [ ] Can block malicious IP addresses (firewall)
- [ ] Can restore from backup within 1 hour
- [ ] Can rollback to previous version within 15 minutes
- [ ] Can rotate compromised credentials within 30 minutes

---

## Pre-Launch Security Sign-Off

**Before going live, verify:**
- [ ] All items in this checklist completed
- [ ] Security testing passed
- [ ] Penetration testing passed
- [ ] Backup/restore tested
- [ ] Incident response plan ready
- [ ] Admin credentials secured
- [ ] Database credentials secured
- [ ] SMTP credentials secured
- [ ] SSL certificate valid
- [ ] Firewall configured
- [ ] Monitoring configured
- [ ] Audit logging working

**Sign-off:**
- Reviewed by: ___________________________
- Date: ___________________________
- Next review date: ___________________________

---

## Quarterly Security Review

**Next Review Date:** ___________________________

**Items to Review:**
1. Run through this entire checklist
2. Review audit logs for suspicious activity
3. Update system packages
4. Update application dependencies
5. Rotate database credentials
6. Test backup restoration
7. Verify SSL certificate renewal
8. Review and update firewall rules
9. Review access logs for patterns
10. Update incident response plan if needed

---

## Security Contact

**Security Issues:** Report to ashwin@ideaai.in  
**PGP Key:** (Optional: Add PGP public key for encrypted communication)

---

## Resources

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- OWASP Cheat Sheets: https://cheatsheetseries.owasp.org/
- Spring Security: https://docs.spring.io/spring-security/reference/
- Let's Encrypt: https://letsencrypt.org/
- Certbot: https://certbot.eff.org/

---

**Last Updated:** September 28, 2026  
**Next Review:** December 28, 2026 (Quarterly)
