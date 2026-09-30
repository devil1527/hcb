# HCB Deployment Guide

**Target Environment:** Ubuntu 24.04 LTS VPS (2GB RAM, 30GB Disk)  
**Application:** Spring Boot 3.3.4 JAR  
**Database:** MariaDB 10.11  
**Web Server:** Apache 2.4  
**Domain:** hcb.ideaai.in  

---

## Pre-Deployment Checklist

- [ ] Application builds successfully: `mvn clean package`
- [ ] All tests pass: `mvn test`
- [ ] Database schema reviewed
- [ ] Environment variables documented
- [ ] Apache configuration prepared
- [ ] systemd service file prepared
- [ ] Backup scripts prepared
- [ ] Rollback procedure documented
- [ ] SSL certificate plan ready (Certbot)

---

## Step 1: VPS Preparation

### 1.1 Create Application User

```bash
# Create dedicated non-root user for HCB application
sudo useradd -r -m -s /bin/bash hcb

# Verify user created
id hcb
```

**Output should show:**
```
uid=999(hcb) gid=999(hcb) groups=999(hcb)
```

### 1.2 Create Directory Structure

```bash
# Application directory
sudo mkdir -p /opt/hcb
sudo mkdir -p /opt/hcb/backups

# File upload directory
sudo mkdir -p /var/lib/hcb/uploads/products

# Log directory
sudo mkdir -p /var/log/hcb

# Backup directory
sudo mkdir -p /var/backups/hcb/database
sudo mkdir -p /var/backups/hcb/files

# Set ownership
sudo chown -R hcb:hcb /opt/hcb
sudo chown -R hcb:hcb /var/lib/hcb
sudo chown -R hcb:hcb /var/log/hcb
sudo chown -R hcb:hcb /var/backups/hcb

# Set permissions
sudo chmod 755 /opt/hcb
sudo chmod 755 /var/lib/hcb
sudo chmod 755 /var/log/hcb
sudo chmod 700 /opt/hcb/backups  # Backups accessible only to hcb user
```

### 1.3 Verify Port Availability

```bash
# Check if port 10001 is available
sudo netstat -tuln | grep 10001

# If no output, port is available
# If output shows port in use, choose different port
```

---

## Step 2: Database Setup

### 2.1 Create Database and User

```bash
# Connect to MariaDB as root
sudo mysql -u root -p
```

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS hcb 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Create dedicated user
CREATE USER IF NOT EXISTS 'hcb_user'@'localhost' 
IDENTIFIED BY 'REPLACE_WITH_SECURE_PASSWORD';

-- Grant necessary privileges (least privilege principle)
GRANT SELECT, INSERT, UPDATE, DELETE ON hcb.* TO 'hcb_user'@'localhost';
GRANT CREATE, DROP, INDEX, ALTER ON hcb.* TO 'hcb_user'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify user created
SELECT User, Host FROM mysql.user WHERE User = 'hcb_user';

-- Exit
EXIT;
```

### 2.2 Deploy Database Schema

```bash
# Copy schema.sql to VPS (from your local machine)
scp docs/database/schema.sql user@vps-ip:/tmp/hcb-schema.sql

# On VPS, import schema
mysql -u hcb_user -p hcb < /tmp/hcb-schema.sql

# Verify tables created
mysql -u hcb_user -p hcb -e "SHOW TABLES;"
```

**Expected output:**
```
+------------------+
| Tables_in_hcb    |
+------------------+
| audit_logs       |
| order_items      |
| orders           |
| password_reset_tokens |
| products         |
| settings         |
| user_roles       |
| users            |
+------------------+
```

### 2.3 Update Admin Password

**CRITICAL:** Change default admin password immediately!

```bash
# Generate bcrypt hash for your secure password
# Use online tool: https://bcrypt-generator.com/ (strength 12)
# Or use Java code to generate

# Connect to database
mysql -u hcb_user -p hcb
```

```sql
-- Update admin password
UPDATE users 
SET password_hash = '$2a$12$YOUR_BCRYPT_HASH_HERE'
WHERE email = 'ashwin@ideaai.in';

-- Verify update
SELECT email, full_name FROM users WHERE email = 'ashwin@ideaai.in';

EXIT;
```

---

## Step 3: Application Configuration

### 3.1 Create Application Properties File

```bash
# Create production configuration
sudo nano /opt/hcb/application.properties
```

**File content:**
```properties
# ============================================
# HCB PRODUCTION CONFIGURATION
# ============================================

# Server Configuration
server.port=10001
server.address=127.0.0.1

# Database Configuration
spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/hcb
spring.datasource.username=hcb_user
spring.datasource.password=YOUR_DATABASE_PASSWORD_HERE

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect

# Flyway Database Migration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Email Configuration (Provider-Agnostic SMTP)
# Example providers: Gmail (smtp.gmail.com:587), SendGrid (smtp.sendgrid.net:587), AWS SES
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# File Upload Configuration
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
hcb.upload.directory=/var/lib/hcb/uploads

# Session Configuration
spring.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict

# Security
spring.security.filter.order=5

# Logging
logging.level.root=WARN
logging.level.com.hcb=INFO
logging.level.org.springframework.security=INFO
logging.level.org.springframework.web=INFO
logging.file.name=/var/log/hcb/application.log
logging.file.max-size=100MB
logging.file.max-history=30
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.info.env.enabled=true
management.info.java.enabled=true

# Application Info
info.app.name=HCB - Healthy Choco Bytes
info.app.description=Chocolate Ordering Application
info.app.version=@project.version@
```

### 3.2 Secure Configuration File

```bash
# Set restrictive permissions (only hcb user can read)
sudo chown hcb:hcb /opt/hcb/application.properties
sudo chmod 600 /opt/hcb/application.properties

# Verify permissions
ls -la /opt/hcb/application.properties
```

**Expected output:**
```
-rw------- 1 hcb hcb 2048 Sep 28 12:00 /opt/hcb/application.properties
```

---

## Step 4: Deploy Application JAR

### 4.1 Build Production JAR

**On your local development machine:**

```bash
# Navigate to project directory
cd /path/to/hcb-project

# Clean and build (skip tests for faster build, or run tests first)
mvn clean package -DskipTests

# Verify JAR created
ls -lh target/hcb-*.jar
```

**Expected output:**
```
-rw-r--r-- 1 user user 45M Sep 28 12:00 target/hcb-1.0.0.jar
```

### 4.2 Copy JAR to VPS

```bash
# Copy JAR to VPS
scp target/hcb-1.0.0.jar user@vps-ip:/tmp/

# SSH into VPS
ssh user@vps-ip

# Move JAR to application directory
sudo mv /tmp/hcb-1.0.0.jar /opt/hcb/
sudo chown hcb:hcb /opt/hcb/hcb-1.0.0.jar
sudo chmod 755 /opt/hcb/hcb-1.0.0.jar

# Create symbolic link for "current" version
sudo ln -sf /opt/hcb/hcb-1.0.0.jar /opt/hcb/hcb-current.jar
```

### 4.3 Test Application Manually (Optional)

```bash
# Test run as hcb user
sudo -u hcb java -Xms256m -Xmx512m \
  -jar /opt/hcb/hcb-current.jar \
  --spring.config.location=/opt/hcb/application.properties

# Watch logs
# Should see "Started HcbApplication in X seconds"

# Test health check (in another terminal)
curl http://localhost:10001/actuator/health

# Expected response:
# {"status":"UP"}

# Stop test run: Ctrl+C
```

---

## Step 5: systemd Service Configuration

### 5.1 Create Service File

```bash
sudo nano /etc/systemd/system/hcb.service
```

**File content:**
```ini
[Unit]
Description=HCB - Healthy Choco Bytes Ordering Application
Documentation=https://hcb.ideaai.in
After=syslog.target network.target mariadb.service
Wants=mariadb.service

[Service]
Type=simple
User=hcb
Group=hcb
WorkingDirectory=/opt/hcb

# Java command with memory constraints
ExecStart=/usr/bin/java \
  -Xms256m \
  -Xmx512m \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /opt/hcb/hcb-current.jar \
  --spring.config.location=/opt/hcb/application.properties

# Graceful shutdown
ExecStop=/bin/kill -SIGTERM $MAINPID
SuccessExitStatus=143
TimeoutStopSec=30

# Restart policy
Restart=on-failure
RestartSec=10
StartLimitInterval=300
StartLimitBurst=3

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=hcb

# Security Hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/hcb /var/log/hcb
ReadOnlyPaths=/opt/hcb

# Resource Limits
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

### 5.2 Enable and Start Service

```bash
# Reload systemd daemon
sudo systemctl daemon-reload

# Enable service (auto-start on boot)
sudo systemctl enable hcb.service

# Start service
sudo systemctl start hcb.service

# Check service status
sudo systemctl status hcb.service
```

**Expected output:**
```
● hcb.service - HCB - Healthy Choco Bytes Ordering Application
     Loaded: loaded (/etc/systemd/system/hcb.service; enabled; vendor preset: enabled)
     Active: active (running) since Mon 2026-09-28 12:00:00 UTC; 5s ago
   Main PID: 12345 (java)
      Tasks: 42 (limit: 4096)
     Memory: 345.2M
        CPU: 5.234s
     CGroup: /system.slice/hcb.service
             └─12345 /usr/bin/java -Xms256m -Xmx512m -jar /opt/hcb/hcb-current.jar
```

### 5.3 Verify Application Started

```bash
# Check application logs
sudo tail -f /var/log/hcb/application.log

# Check health endpoint
curl http://localhost:10001/actuator/health

# Check application info
curl http://localhost:10001/actuator/info

# Test home page (should return HTML)
curl http://localhost:10001/
```

### 5.4 Test Auto-Restart

```bash
# Kill process to test restart policy
sudo systemctl kill hcb.service

# Wait 10 seconds (RestartSec=10)
sleep 10

# Check status (should be running again)
sudo systemctl status hcb.service
```

---

## Step 6: Apache Reverse Proxy Configuration

### 6.1 Create Virtual Host Configuration

```bash
sudo nano /etc/apache2/sites-available/hcb.conf
```

**File content:**
```apache
<VirtualHost *:80>
    ServerName hcb.ideaai.in
    ServerAdmin ashwin@ideaai.in

    # Redirect all HTTP to HTTPS
    RewriteEngine On
    RewriteCond %{HTTPS} off
    RewriteRule ^(.*)$ https://%{HTTP_HOST}$1 [R=301,L]

    ErrorLog ${APACHE_LOG_DIR}/hcb-error.log
    CustomLog ${APACHE_LOG_DIR}/hcb-access.log combined
</VirtualHost>

<VirtualHost *:443>
    ServerName hcb.ideaai.in
    ServerAdmin ashwin@ideaai.in

    # SSL Configuration (Certbot will add certificates here)
    # SSLEngine on
    # SSLCertificateFile /etc/letsencrypt/live/hcb.ideaai.in/fullchain.pem
    # SSLCertificateKeyFile /etc/letsencrypt/live/hcb.ideaai.in/privkey.pem

    # Reverse Proxy to Spring Boot
    ProxyPreserveHost On
    ProxyPass / http://127.0.0.1:10001/
    ProxyPassReverse / http://127.0.0.1:10001/

    # WebSocket support (if needed in future)
    # ProxyPass /ws ws://127.0.0.1:10001/ws
    # ProxyPassReverse /ws ws://127.0.0.1:10001/ws

    # Security Headers
    Header always set X-Frame-Options "DENY"
    Header always set X-Content-Type-Options "nosniff"
    Header always set Referrer-Policy "strict-origin-when-cross-origin"
    Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"

    # HSTS (enable after SSL is working)
    # Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"

    # Content Security Policy (adjust as needed)
    Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' fonts.googleapis.com; font-src 'self' fonts.gstatic.com; img-src 'self' data:; frame-ancestors 'none';"

    # Timeout Configuration
    ProxyTimeout 300
    TimeOut 300

    # Logging
    ErrorLog ${APACHE_LOG_DIR}/hcb-ssl-error.log
    CustomLog ${APACHE_LOG_DIR}/hcb-ssl-access.log combined

    # Error Documents
    ErrorDocument 502 /error/502.html
    ErrorDocument 503 /error/503.html
</VirtualHost>
```

### 6.2 Enable Site and Required Modules

```bash
# Enable required Apache modules (if not already enabled)
sudo a2enmod proxy
sudo a2enmod proxy_http
sudo a2enmod rewrite
sudo a2enmod ssl
sudo a2enmod headers

# Enable HCB site
sudo a2ensite hcb.conf

# Test Apache configuration
sudo apache2ctl configtest

# Expected output: "Syntax OK"

# Reload Apache
sudo systemctl reload apache2
```

### 6.3 Obtain SSL Certificate (Let's Encrypt)

```bash
# Install Certbot (if not already installed)
sudo apt install certbot python3-certbot-apache

# Obtain certificate for hcb.ideaai.in
sudo certbot --apache -d hcb.ideaai.in

# Follow prompts:
# - Enter email: ashwin@ideaai.in
# - Agree to terms: Y
# - Share email with EFF: Your choice (optional)
# - Redirect HTTP to HTTPS: Select 2 (Redirect)

# Certbot will automatically:
# - Obtain SSL certificate
# - Update Apache configuration
# - Set up auto-renewal

# Test certificate auto-renewal
sudo certbot renew --dry-run

# Expected output: "Congratulations, all simulated renewals succeeded"
```

### 6.4 Verify HTTPS Working

```bash
# Test from VPS
curl -I https://hcb.ideaai.in

# Expected: HTTP/2 200 OK

# Test health check via HTTPS
curl https://hcb.ideaai.in/actuator/health

# Test from external network (your laptop)
curl -I https://hcb.ideaai.in
```

---

## Step 7: DNS Configuration (Cloudflare)

### 7.1 Update DNS Records

**In Cloudflare Dashboard:**

1. Navigate to DNS settings for `ideaai.in`
2. Add/Update A record:
   - **Type:** A
   - **Name:** hcb
   - **IPv4 address:** [Your VPS IP]
   - **Proxy status:** Proxied (orange cloud) — for DDoS protection
   - **TTL:** Auto

3. Save changes

### 7.2 Wait for DNS Propagation

```bash
# Check DNS resolution
dig hcb.ideaai.in

# Or use online tool: https://dnschecker.org/

# Wait 5-30 minutes for global propagation
```

### 7.3 Test from Multiple Locations

- Test from your laptop
- Test from mobile phone (WiFi + mobile data)
- Test from different city/country (use VPN or ask friend)

---

## Step 8: Backup Configuration

### 8.1 Create Database Backup Script

```bash
sudo nano /opt/hcb/backup-database.sh
```

**File content:**
```bash
#!/bin/bash
#
# HCB Database Backup Script
# Runs daily at 2 AM via cron
#

set -e

# Configuration
DB_NAME="hcb"
DB_USER="hcb_user"
DB_PASS="YOUR_DATABASE_PASSWORD_HERE"
BACKUP_DIR="/var/backups/hcb/database"
RETENTION_DAYS=30
ADMIN_EMAIL="ashwin@ideaai.in"

# Generate backup filename
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="$BACKUP_DIR/hcb-db-$TIMESTAMP.sql.gz"

# Create backup
echo "[$(date)] Starting database backup..."
mysqldump -u $DB_USER -p$DB_PASS \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --quick \
    --lock-tables=false \
    $DB_NAME | gzip > "$BACKUP_FILE"

# Verify backup integrity
echo "[$(date)] Verifying backup..."
gunzip -t "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "[$(date)] Backup successful: $BACKUP_FILE"
    
    # Get backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "[$(date)] Backup size: $BACKUP_SIZE"
else
    echo "[$(date)] ERROR: Backup verification failed!" >&2
    echo "HCB Database Backup Failed" | mail -s "ALERT: HCB Backup Failure" $ADMIN_EMAIL
    exit 1
fi

# Clean old backups (keep last 30 days)
echo "[$(date)] Cleaning old backups..."
find "$BACKUP_DIR" -name "hcb-db-*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Count remaining backups
BACKUP_COUNT=$(find "$BACKUP_DIR" -name "hcb-db-*.sql.gz" | wc -l)
echo "[$(date)] Total backups: $BACKUP_COUNT"

echo "[$(date)] Backup completed successfully."
```

**Make executable:**
```bash
sudo chmod +x /opt/hcb/backup-database.sh
sudo chown hcb:hcb /opt/hcb/backup-database.sh
```

### 8.2 Create File Backup Script

```bash
sudo nano /opt/hcb/backup-files.sh
```

**File content:**
```bash
#!/bin/bash
#
# HCB File Backup Script
# Runs weekly on Sunday at 3 AM
#

set -e

# Configuration
UPLOAD_DIR="/var/lib/hcb/uploads"
BACKUP_DIR="/var/backups/hcb/files"
RETENTION_DAYS=90

# Generate backup filename
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="$BACKUP_DIR/hcb-files-$TIMESTAMP.tar.gz"

# Create backup
echo "[$(date)] Starting file backup..."
tar -czf "$BACKUP_FILE" -C /var/lib/hcb uploads/

if [ $? -eq 0 ]; then
    echo "[$(date)] File backup successful: $BACKUP_FILE"
    
    # Get backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "[$(date)] Backup size: $BACKUP_SIZE"
else
    echo "[$(date)] ERROR: File backup failed!" >&2
    exit 1
fi

# Clean old backups
echo "[$(date)] Cleaning old file backups..."
find "$BACKUP_DIR" -name "hcb-files-*.tar.gz" -mtime +$RETENTION_DAYS -delete

echo "[$(date)] File backup completed."
```

**Make executable:**
```bash
sudo chmod +x /opt/hcb/backup-files.sh
sudo chown hcb:hcb /opt/hcb/backup-files.sh
```

### 8.3 Schedule Backup Cron Jobs

```bash
# Edit crontab for hcb user
sudo crontab -u hcb -e
```

**Add these lines:**
```cron
# HCB Backup Jobs

# Database backup - Daily at 2:00 AM
0 2 * * * /opt/hcb/backup-database.sh >> /var/log/hcb/backup.log 2>&1

# File backup - Weekly on Sunday at 3:00 AM
0 3 * * 0 /opt/hcb/backup-files.sh >> /var/log/hcb/backup.log 2>&1
```

### 8.4 Test Backup Scripts

```bash
# Test database backup
sudo -u hcb /opt/hcb/backup-database.sh

# Verify backup created
ls -lh /var/backups/hcb/database/

# Test file backup
sudo -u hcb /opt/hcb/backup-files.sh

# Verify backup created
ls -lh /var/backups/hcb/files/
```

---

## Step 9: Monitoring & Health Checks

### 9.1 Create Health Check Script

```bash
sudo nano /opt/hcb/healthcheck.sh
```

**File content:**
```bash
#!/bin/bash
#
# HCB Health Check Script
# Runs every 5 minutes via cron
#

HEALTH_URL="http://localhost:10001/actuator/health"
ADMIN_EMAIL="ashwin@ideaai.in"
STATUS_FILE="/tmp/hcb-health-status"

# Check application health
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ "$RESPONSE" = "200" ]; then
    # Application is healthy
    if [ -f "$STATUS_FILE" ]; then
        # Was down, now recovered
        echo "HCB Application Recovered" | mail -s "HCB Status: UP" $ADMIN_EMAIL
        rm "$STATUS_FILE"
    fi
else
    # Application is down or unhealthy
    if [ ! -f "$STATUS_FILE" ]; then
        # First failure detection
        echo "down" > "$STATUS_FILE"
        echo "HCB Application is not responding. HTTP Status: $RESPONSE" | \
            mail -s "ALERT: HCB Application DOWN" $ADMIN_EMAIL
    fi
fi
```

**Make executable:**
```bash
sudo chmod +x /opt/hcb/healthcheck.sh
```

### 9.2 Schedule Health Check

```bash
# Edit root crontab
sudo crontab -e
```

**Add line:**
```cron
# HCB Health Check - Every 5 minutes
*/5 * * * * /opt/hcb/healthcheck.sh
```

---

## Step 10: Post-Deployment Verification

### 10.1 Production Smoke Test

**Customer Flow:**
```bash
# 1. Home page loads
curl -I https://hcb.ideaai.in
# Expected: HTTP/2 200

# 2. Products page loads
curl -I https://hcb.ideaai.in/products
# Expected: HTTP/2 200

# 3. Login page loads
curl -I https://hcb.ideaai.in/login
# Expected: HTTP/2 200

# 4. Register page loads
curl -I https://hcb.ideaai.in/register
# Expected: HTTP/2 200
```

**Admin Flow:**
```bash
# Admin login page loads
curl -I https://hcb.ideaai.in/admin/login
# Expected: HTTP/2 200
```

**Manual Testing (Browser):**
- [ ] Home page displays correctly
- [ ] Product images load
- [ ] Product listing shows database products
- [ ] Customer registration works
- [ ] Customer login works
- [ ] Add to cart works
- [ ] Checkout flow works
- [ ] Order creation succeeds
- [ ] Order confirmation email received
- [ ] Admin login works
- [ ] Admin can view orders
- [ ] Admin can verify payment
- [ ] Admin can update order status
- [ ] Admin receives order notification email

### 10.2 Resource Monitoring

```bash
# Check application memory usage
ps aux | grep java | grep hcb

# Check total system memory
free -h

# Check disk usage
df -h

# Check database size
sudo du -sh /var/lib/mysql/hcb

# Check upload directory size
sudo du -sh /var/lib/hcb/uploads

# Check log file sizes
sudo du -sh /var/log/hcb/*
```

### 10.3 Log Monitoring

```bash
# Watch application logs
sudo tail -f /var/log/hcb/application.log

# Watch Apache access logs
sudo tail -f /var/log/apache2/hcb-ssl-access.log

# Watch Apache error logs
sudo tail -f /var/log/apache2/hcb-ssl-error.log

# Check for errors in last 24 hours
sudo grep -i error /var/log/hcb/application.log | tail -n 50
```

---

## Step 11: Rollback Procedure

### 11.1 Prepare Rollback

**Before deploying new version, always:**
```bash
# Backup current JAR
sudo cp /opt/hcb/hcb-current.jar /opt/hcb/backups/hcb-$(date +%Y%m%d-%H%M%S).jar

# Backup database
sudo -u hcb /opt/hcb/backup-database.sh
```

### 11.2 Rollback Steps

**If new deployment fails:**
```bash
# 1. Stop current application
sudo systemctl stop hcb.service

# 2. Revert to previous JAR
sudo ln -sf /opt/hcb/hcb-0.9.0.jar /opt/hcb/hcb-current.jar

# 3. Start application
sudo systemctl start hcb.service

# 4. Verify application started
sudo systemctl status hcb.service
curl http://localhost:10001/actuator/health

# 5. Check logs
sudo tail -f /var/log/hcb/application.log
```

**If database migration fails:**
```bash
# Restore from most recent backup
gunzip < /var/backups/hcb/database/hcb-db-TIMESTAMP.sql.gz | \
    mysql -u hcb_user -p hcb
```

---

## Deployment Checklist

**Pre-Deployment:**
- [ ] Code tested locally
- [ ] All tests pass
- [ ] Database migration reviewed
- [ ] Backup current JAR
- [ ] Backup current database
- [ ] Review changelog
- [ ] Schedule deployment (low-traffic window)

**During Deployment:**
- [ ] Stop application
- [ ] Deploy new JAR
- [ ] Update symbolic link
- [ ] Start application
- [ ] Wait for startup
- [ ] Check health endpoint
- [ ] Review startup logs

**Post-Deployment:**
- [ ] Smoke test all critical flows
- [ ] Monitor logs for 30 minutes
- [ ] Monitor server resources
- [ ] Test customer registration
- [ ] Test order placement
- [ ] Test admin functions
- [ ] Verify email notifications
- [ ] Check error logs

**If Issues:**
- [ ] Execute rollback procedure
- [ ] Investigate root cause
- [ ] Fix issue
- [ ] Re-test locally
- [ ] Schedule re-deployment

---

## Useful Commands

```bash
# Application Management
sudo systemctl start hcb        # Start application
sudo systemctl stop hcb         # Stop application
sudo systemctl restart hcb      # Restart application
sudo systemctl status hcb       # Check status
sudo systemctl enable hcb       # Enable auto-start
sudo systemctl disable hcb      # Disable auto-start

# Logs
sudo tail -f /var/log/hcb/application.log              # Application logs
sudo journalctl -u hcb -f                              # systemd logs (live)
sudo journalctl -u hcb --since "1 hour ago"            # Last hour logs
sudo journalctl -u hcb --since today                   # Today's logs

# Apache Management
sudo systemctl restart apache2                          # Restart Apache
sudo apache2ctl configtest                             # Test configuration
sudo a2ensite hcb.conf                                 # Enable site
sudo a2dissite hcb.conf                                # Disable site

# Database
mysql -u hcb_user -p hcb                               # Connect to database
sudo systemctl status mariadb                          # Database status

# Resource Monitoring
htop                                                    # Interactive process viewer
free -h                                                 # Memory usage
df -h                                                   # Disk usage
du -sh /var/lib/hcb/uploads/*                          # Upload directory size

# Network
sudo netstat -tuln | grep 10001                        # Check if port listening
curl http://localhost:10001/actuator/health            # Health check
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
sudo journalctl -u hcb -n 100

# Common issues:
# 1. Database connection refused
#    - Verify MariaDB running: sudo systemctl status mariadb
#    - Verify credentials in application.properties
#
# 2. Port already in use
#    - Check what's using port: sudo netstat -tuln | grep 10001
#    - Kill process or change port
#
# 3. Permission denied
#    - Verify file permissions: ls -la /opt/hcb
#    - Verify user: id hcb
```

### Application Slow or High Memory

```bash
# Check memory usage
ps aux | grep java | grep hcb

# If memory usage > 512MB:
# 1. Check for memory leaks in logs
# 2. Consider increasing max heap (-Xmx)
# 3. Consider VPS upgrade
```

### Database Connection Issues

```bash
# Test database connection
mysql -u hcb_user -p -h 127.0.0.1 hcb

# Check MariaDB status
sudo systemctl status mariadb

# Check connection pool exhaustion
grep "HikariPool" /var/log/hcb/application.log
```

### SSL Certificate Issues

```bash
# Check certificate expiry
sudo certbot certificates

# Renew certificate manually
sudo certbot renew

# Test auto-renewal
sudo certbot renew --dry-run
```

---

## Security Hardening

### Firewall Configuration

```bash
# Allow only necessary ports
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP
sudo ufw allow 443/tcp    # HTTPS
sudo ufw enable

# Deny direct access to application port (already bound to localhost)
# Port 10001 not exposed to internet
```

### Fail2Ban Configuration

```bash
# Install Fail2Ban
sudo apt install fail2ban

# Configure for Apache
sudo nano /etc/fail2ban/jail.local
```

**Add:**
```ini
[apache-auth]
enabled = true
port = http,https
logpath = /var/log/apache2/hcb-ssl-error.log
```

```bash
sudo systemctl restart fail2ban
```

### Regular Updates

```bash
# System updates (monthly)
sudo apt update
sudo apt upgrade

# Java security updates (as available)
sudo apt install openjdk-21-jdk

# Dependency updates (check monthly)
mvn versions:display-dependency-updates
```

---

**Deployment complete!** Application should now be live at https://hcb.ideaai.in
