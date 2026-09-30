# HCB Quick Reference Guide

**For quick lookups during development and deployment**

---

## Essential Commands

### Application Management
```bash
# Start
sudo systemctl start hcb

# Stop
sudo systemctl stop hcb

# Restart
sudo systemctl restart hcb

# Status
sudo systemctl status hcb

# Logs (live)
sudo tail -f /var/log/hcb/application.log

# Logs (last 100 lines)
sudo journalctl -u hcb -n 100

# Logs (last hour)
sudo journalctl -u hcb --since "1 hour ago"
```

### Database Quick Access
```bash
# Connect to database
mysql -u hcb_user -p hcb

# Backup database manually
sudo -u hcb /opt/hcb/backup-database.sh

# Restore from backup
gunzip < /var/backups/hcb/database/hcb-db-TIMESTAMP.sql.gz | mysql -u hcb_user -p hcb

# Check database size
sudo du -sh /var/lib/mysql/hcb
```

### Apache Management
```bash
# Restart Apache
sudo systemctl restart apache2

# Test configuration
sudo apache2ctl configtest

# View access logs
sudo tail -f /var/log/apache2/hcb-ssl-access.log

# View error logs
sudo tail -f /var/log/apache2/hcb-ssl-error.log
```

### Health Checks
```bash
# Application health
curl http://localhost:10001/actuator/health

# Application info
curl http://localhost:10001/actuator/info

# HTTPS check
curl -I https://hcb.ideaai.in

# Check listening ports
sudo netstat -tuln | grep -E "10001|3306|80|443"
```

---

## Directory Structure

```
/opt/hcb/
├── hcb-current.jar           # Symlink to current version
├── hcb-1.0.0.jar             # Current release
├── hcb-0.9.0.jar             # Previous release (rollback)
├── application.properties    # Production config
├── backup-database.sh        # Database backup script
├── backup-files.sh           # File backup script
├── healthcheck.sh            # Health check script
└── backups/                  # Old JAR backups

/var/lib/hcb/
└── uploads/
    └── products/             # Product images

/var/log/hcb/
├── application.log           # Application logs
├── audit.log                 # Audit logs
├── error.log                 # Error-only logs
└── backup.log                # Backup logs

/var/backups/hcb/
├── database/                 # Database backups (30 days)
└── files/                    # File backups (90 days)

/etc/systemd/system/
└── hcb.service               # systemd service file

/etc/apache2/sites-available/
└── hcb.conf                  # Apache virtual host
```

---

## Key URLs

### Production
- **Website:** https://hcb.ideaai.in
- **Admin Login:** https://hcb.ideaai.in/admin/login
- **Health Check:** https://hcb.ideaai.in/actuator/health

### Local (VPS)
- **Application:** http://127.0.0.1:10001
- **Database:** 127.0.0.1:3306
- **Health Check:** http://127.0.0.1:10001/actuator/health

---

## Configuration Files

### Application Properties
**Location:** `/opt/hcb/application.properties`

**Key settings:**
```properties
server.port=10001
spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/hcb
spring.datasource.username=hcb_user
spring.mail.host=smtp.gmail.com
hcb.upload.directory=/var/lib/hcb/uploads
```

### systemd Service
**Location:** `/etc/systemd/system/hcb.service`

**Key settings:**
```ini
User=hcb
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/hcb/hcb-current.jar
Restart=on-failure
```

### Apache Virtual Host
**Location:** `/etc/apache2/sites-available/hcb.conf`

**Key settings:**
```apache
ServerName hcb.ideaai.in
ProxyPass / http://127.0.0.1:10001/
```

---

## Database Schema Quick Ref

### Main Tables
- `users` — Customer accounts
- `user_roles` — User roles (CUSTOMER, ADMIN)
- `products` — Product catalog
- `orders` — Customer orders
- `order_items` — Order line items (snapshot)
- `settings` — Configurable settings
- `audit_logs` — Audit trail

### Key Queries

**Count orders:**
```sql
SELECT COUNT(*) FROM orders;
```

**Recent orders:**
```sql
SELECT order_number, customer_name, total_amount, status, created_at 
FROM orders 
ORDER BY created_at DESC 
LIMIT 10;
```

**Pending payments:**
```sql
SELECT order_number, customer_name, total_amount, created_at
FROM orders
WHERE payment_status = 'SUBMITTED'
ORDER BY created_at ASC;
```

**Low stock products:**
```sql
SELECT name, stock_quantity, price
FROM products
WHERE stock_quantity <= low_stock_threshold
AND active = TRUE;
```

**Today's order total:**
```sql
SELECT COUNT(*) as order_count, SUM(total_amount) as total_revenue
FROM orders
WHERE DATE(created_at) = CURDATE();
```

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

## Common Tasks

### Deploy New Version

```bash
# 1. Backup current version
sudo cp /opt/hcb/hcb-current.jar /opt/hcb/backups/hcb-$(date +%Y%m%d-%H%M%S).jar

# 2. Backup database
sudo -u hcb /opt/hcb/backup-database.sh

# 3. Copy new JAR to VPS
scp target/hcb-1.1.0.jar user@vps:/tmp/

# 4. Stop application
sudo systemctl stop hcb

# 5. Deploy new JAR
sudo mv /tmp/hcb-1.1.0.jar /opt/hcb/
sudo chown hcb:hcb /opt/hcb/hcb-1.1.0.jar
sudo ln -sf /opt/hcb/hcb-1.1.0.jar /opt/hcb/hcb-current.jar

# 6. Start application
sudo systemctl start hcb

# 7. Verify
curl http://localhost:10001/actuator/health
sudo tail -f /var/log/hcb/application.log
```

### Rollback to Previous Version

```bash
# 1. Stop application
sudo systemctl stop hcb

# 2. Revert symlink
sudo ln -sf /opt/hcb/hcb-1.0.0.jar /opt/hcb/hcb-current.jar

# 3. Start application
sudo systemctl start hcb

# 4. Verify
curl http://localhost:10001/actuator/health
```

### Add New Product (Admin UI)

1. Login at `/admin/login`
2. Navigate to "Products" → "Add New"
3. Fill form:
   - Name
   - Slug (URL-friendly, e.g., `protein-bar`)
   - Description
   - Price (₹)
   - Stock quantity
   - Upload image
   - Sort order (optional)
4. Click "Save"

### Verify Payment (Admin UI)

1. Login at `/admin/login`
2. Navigate to "Payments" (pending payments list)
3. Click order to view details
4. Click "Verify Payment" or "Reject Payment"
5. Customer receives email notification

### Update Settings (Admin UI)

1. Login at `/admin/login`
2. Navigate to "Settings"
3. Update values:
   - Shipping fee
   - Free shipping threshold
   - WhatsApp number
   - UPI ID
   - UPI name
4. Click "Save"
5. Changes take effect immediately (no restart needed)

---

## Monitoring Checklist

**Daily:**
- [ ] Check application status: `sudo systemctl status hcb`
- [ ] Review error logs: `sudo grep -i error /var/log/hcb/application.log | tail -n 20`
- [ ] Check disk space: `df -h`
- [ ] Check memory usage: `free -h`

**Weekly:**
- [ ] Review audit logs
- [ ] Check backup success
- [ ] Review Apache logs for unusual patterns
- [ ] Verify SSL certificate validity: `sudo certbot certificates`

**Monthly:**
- [ ] Update system packages: `sudo apt update && sudo apt upgrade`
- [ ] Update dependencies: `mvn versions:display-dependency-updates`
- [ ] Review security checklist
- [ ] Test backup restoration

**Quarterly:**
- [ ] Full security audit
- [ ] Penetration testing
- [ ] Performance review
- [ ] Capacity planning (consider VPS upgrade)

---

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
sudo journalctl -u hcb -n 100
```

**Common issues:**
1. **Database connection refused**
   - Check: `sudo systemctl status mariadb`
   - Verify credentials in `/opt/hcb/application.properties`

2. **Port already in use**
   - Check: `sudo netstat -tuln | grep 10001`
   - Kill process or change port

3. **Permission denied**
   - Check: `ls -la /opt/hcb`
   - Fix: `sudo chown -R hcb:hcb /opt/hcb`

### High Memory Usage

```bash
# Check Java memory
ps aux | grep java | grep hcb

# If > 512MB, consider:
# 1. Check for memory leaks in logs
# 2. Increase -Xmx in systemd service
# 3. Upgrade VPS to 4GB RAM
```

### Slow Response Times

```bash
# Check database queries
# Look for slow query log in MariaDB

# Check connection pool
grep "HikariPool" /var/log/hcb/application.log

# Check system resources
htop
```

### Email Not Sending

**Check logs:**
```bash
grep -i "email\|smtp" /var/log/hcb/application.log | tail -n 50
```

**Common issues:**
1. **SMTP authentication failed**
   - Verify SMTP credentials (username/password)
   - Check credentials in `application.properties`
   - For Gmail: Use app-specific password (not account password)

2. **Daily limit exceeded (100 emails/day)**
   - Monitor send count
   - Consider upgrading to SendGrid

3. **Network connectivity**
   - Test: `telnet smtp.gmail.com 587`

### SSL Certificate Expired

```bash
# Check expiry
sudo certbot certificates

# Renew manually
sudo certbot renew

# Restart Apache
sudo systemctl restart apache2
```

---

## Security Quick Checks

```bash
# 1. Check if port 10001 exposed (should NOT be)
curl -I http://<VPS-IP>:10001
# Should timeout or connection refused

# 2. Check HTTPS working
curl -I https://hcb.ideaai.in
# Should return HTTP/2 200

# 3. Check security headers
curl -I https://hcb.ideaai.in | grep -i "x-\|strict\|content-security"

# 4. Check application user (should NOT be root)
ps aux | grep java | grep hcb

# 5. Check file permissions
ls -la /opt/hcb/application.properties
# Should be -rw------- (600)

# 6. Check database user privileges
mysql -u hcb_user -p -e "SHOW GRANTS;"
# Should NOT have global privileges
```

---

## Performance Baselines

**Expected values (2GB VPS):**
- Application memory: 300-500MB
- Database memory: 150-250MB
- Response time (home page): < 500ms
- Response time (checkout): < 2s
- Concurrent users: 10-50
- Orders per day: 100-500

**If exceeding baselines:**
1. Review slow queries in database
2. Check for memory leaks
3. Optimize images (compress, resize)
4. Consider 4GB VPS upgrade

---

## Backup & Restore

### Manual Backup
```bash
# Database
sudo -u hcb /opt/hcb/backup-database.sh

# Files
sudo -u hcb /opt/hcb/backup-files.sh
```

### Manual Restore
```bash
# Database
gunzip < /var/backups/hcb/database/hcb-db-TIMESTAMP.sql.gz | mysql -u hcb_user -p hcb

# Files
sudo tar -xzf /var/backups/hcb/files/hcb-files-TIMESTAMP.tar.gz -C /var/lib/hcb/
```

---

## Emergency Contacts

**Admin:** Ashwin Golani  
**Email:** ashwin@ideaai.in  
**Domain:** hcb.ideaai.in  
**VPS Provider:** [Your VPS provider support]

---

## Useful Links

- [Architecture Docs](./architecture/)
- [Database Schema](./database/schema.sql)
- [Deployment Guide](./deployment/DEPLOYMENT-GUIDE.md)
- [Security Checklist](./security/SECURITY-CHECKLIST.md)
- [Implementation Roadmap](./tasks/IMPLEMENTATION-ROADMAP.md)

---

**Last Updated:** September 28, 2026
