# HCB Documentation

**Healthy Choco Bytes - Chocolate Ordering Application**

This documentation covers the complete architecture, deployment, security, and implementation of the HCB application.

---

## Quick Start

**For rapid onboarding:**
1. Read [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) — Essential commands and common tasks
2. Read [00-EXECUTIVE-SUMMARY.md](./architecture/00-EXECUTIVE-SUMMARY.md) — Project overview

**For implementation:**
1. Review [IMPLEMENTATION-ROADMAP.md](./tasks/IMPLEMENTATION-ROADMAP.md) — 15-20 day timeline
2. Follow [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md) — Step-by-step VPS setup

**For security:**
1. Complete [SECURITY-CHECKLIST.md](./security/SECURITY-CHECKLIST.md) before launch
2. Review quarterly thereafter

---

## Documentation Structure

```
docs/
├── README.md                    # This file
├── QUICK-REFERENCE.md           # Quick command reference
│
├── architecture/                # System architecture
│   ├── 00-EXECUTIVE-SUMMARY.md  # Project overview
│   └── 01-SYSTEM-ARCHITECTURE.md # Detailed technical architecture
│
├── database/                    # Database design
│   └── schema.sql               # Complete database schema
│
├── deployment/                  # Deployment procedures
│   └── DEPLOYMENT-GUIDE.md      # VPS deployment guide
│
├── security/                    # Security documentation
│   └── SECURITY-CHECKLIST.md    # Pre-launch security checklist
│
└── tasks/                       # Implementation tasks
    └── IMPLEMENTATION-ROADMAP.md # Phase-by-phase implementation plan
```

---

## Document Index

### Architecture Documentation

**[00-EXECUTIVE-SUMMARY.md](./architecture/00-EXECUTIVE-SUMMARY.md)**
- Project overview
- Current vs target state
- Technology stack
- Timeline and phases
- Key architectural decisions
- Business configuration

**[01-SYSTEM-ARCHITECTURE.md](./architecture/01-SYSTEM-ARCHITECTURE.md)**
- Modular monolith pattern
- Technology stack details
- Component architecture (controllers, services, repositories)
- Request flow examples
- Security configuration
- State management
- Error handling
- Logging strategy
- Performance considerations
- Monitoring approach

### Database Documentation

**[schema.sql](./database/schema.sql)**
- Complete database schema
- All tables with indexes and constraints
- Initial settings data
- Views for convenience
- Stored procedures
- Triggers for auditing
- Setup verification queries

### Deployment Documentation

**[DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md)**
- VPS preparation (users, directories)
- Database setup (create database, user, schema)
- Application configuration (application.properties)
- Application deployment (JAR copy, systemd)
- Apache reverse proxy configuration
- SSL certificate setup (Let's Encrypt)
- DNS configuration (Cloudflare)
- Backup configuration (database, files, cron)
- Health check setup
- Post-deployment verification
- Rollback procedure
- Troubleshooting guide

### Security Documentation

**[SECURITY-CHECKLIST.md](./security/SECURITY-CHECKLIST.md)**
- Authentication & session security
- Authorization & access control
- Input validation & injection protection
- File upload security
- Business logic security
- Infrastructure security
- Security headers verification
- Data protection
- Error handling
- Testing & validation
- Monitoring & alerting
- Maintenance & updates
- Incident response

### Task Documentation

**[IMPLEMENTATION-ROADMAP.md](./tasks/IMPLEMENTATION-ROADMAP.md)**
- 15-20 day implementation timeline
- Daily task breakdown
- Phase 1: Foundation (Days 1-5)
- Phase 2: Customer Frontend (Days 6-10)
- Phase 3: Admin Panel (Days 11-14)
- Phase 4: Email & Notifications (Days 15-16)
- Phase 5: Testing & Hardening (Days 17-18)
- Phase 6: Deployment (Days 19-20)
- Phase 7: Post-Launch (Days 21+)
- Critical path items
- Risk mitigation
- Pre-launch checklist

### Quick Reference

**[QUICK-REFERENCE.md](./QUICK-REFERENCE.md)**
- Essential commands (systemctl, logs, database)
- Directory structure
- Key URLs
- Configuration file locations
- Database quick queries
- Order state machine
- Common deployment tasks
- Monitoring checklist
- Troubleshooting guide
- Security quick checks
- Performance baselines
- Backup & restore procedures

---

## Reading Order by Role

### For Developer (You - Ashwin)

**Day 0 (Planning):**
1. [00-EXECUTIVE-SUMMARY.md](./architecture/00-EXECUTIVE-SUMMARY.md) — Understand the big picture
2. [01-SYSTEM-ARCHITECTURE.md](./architecture/01-SYSTEM-ARCHITECTURE.md) — Understand technical design
3. [schema.sql](./database/schema.sql) — Review database design
4. [IMPLEMENTATION-ROADMAP.md](./tasks/IMPLEMENTATION-ROADMAP.md) — Plan your 15-20 days

**During Development (Days 1-18):**
- Follow [IMPLEMENTATION-ROADMAP.md](./tasks/IMPLEMENTATION-ROADMAP.md) day-by-day
- Refer to [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) for commands
- Refer to [01-SYSTEM-ARCHITECTURE.md](./architecture/01-SYSTEM-ARCHITECTURE.md) for design decisions

**Before Deployment (Day 17-18):**
- Complete [SECURITY-CHECKLIST.md](./security/SECURITY-CHECKLIST.md)
- Review [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md)

**During Deployment (Days 19-20):**
- Follow [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md) step-by-step
- Use [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) for quick commands

**Post-Launch:**
- Use [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) for daily operations
- Review [SECURITY-CHECKLIST.md](./security/SECURITY-CHECKLIST.md) quarterly

### For Business Owner (Future)

**Understanding the System:**
1. [00-EXECUTIVE-SUMMARY.md](./architecture/00-EXECUTIVE-SUMMARY.md) — Non-technical overview
2. [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) → "Common Tasks" section

**Daily Operations:**
- No need to read docs — use Admin UI for all operations
- Product management: Add/edit products via `/admin/products`
- Order management: Verify payments, update status via `/admin/orders`
- Settings: Update shipping, UPI, WhatsApp via `/admin/settings`

**If Application Issues:**
- Contact developer (you)
- Share logs: `/var/log/hcb/application.log`

### For Future Developers/Maintainers

**Onboarding:**
1. [00-EXECUTIVE-SUMMARY.md](./architecture/00-EXECUTIVE-SUMMARY.md)
2. [01-SYSTEM-ARCHITECTURE.md](./architecture/01-SYSTEM-ARCHITECTURE.md)
3. [schema.sql](./database/schema.sql)
4. [QUICK-REFERENCE.md](./QUICK-REFERENCE.md)

**Before Making Changes:**
- Review architecture docs to understand design decisions
- Check [SECURITY-CHECKLIST.md](./security/SECURITY-CHECKLIST.md) if touching auth/security
- Review [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md) if changing deployment

---

## Key Design Decisions

### Why Monolith?
- 2GB VPS too small for microservices
- Simple business (2 products, straightforward workflows)
- Easy to maintain for small team
- Can scale vertically (4GB/8GB VPS) without architecture changes

### Why Thymeleaf?
- Preserves existing HTML/CSS
- Server-side rendering (secure by default)
- No separate frontend build step
- SEO-friendly

### Why MariaDB?
- Already installed on VPS
- Excellent for transactional workloads
- ACID compliance critical for orders

### Why Manual Payment Verification?
- Matches current business process
- No payment gateway fees (2-3% savings)
- No PCI compliance burden
- Can add gateway later if needed

### Why Not Redis/Kafka/etc?
- Premature optimization
- 2GB RAM too small
- No genuine need yet
- Can add later if scale demands

---

## Critical Success Factors

**Before Launch:**
1. ✅ All security checklist items completed
2. ✅ Backup/restore tested
3. ✅ Concurrent order handling tested
4. ✅ Email notifications working
5. ✅ Admin can manage products without developer
6. ✅ Server-side price calculation validated
7. ✅ Stock deduction safe under concurrency
8. ✅ HTTPS working with valid certificate

**Post-Launch:**
1. Monitor logs daily for first week
2. Respond to production issues within 2 hours
3. Backup tested quarterly
4. Security checklist reviewed quarterly
5. Plan VPS upgrade when hitting resource limits

---

## Common Questions

### Q: Can I change the database password after deployment?
**A:** Yes, but requires:
1. Update password in MariaDB: `ALTER USER 'hcb_user'@'localhost' IDENTIFIED BY 'new_password';`
2. Update `/opt/hcb/application.properties`
3. Restart application: `sudo systemctl restart hcb`

### Q: Can I add more products without code changes?
**A:** Yes! Use Admin UI at `/admin/products` → "Add New"

### Q: Can I change shipping fee without code changes?
**A:** Yes! Use Admin UI at `/admin/settings` → Update "Shipping fee"

### Q: How do I upgrade to 4GB RAM?
**A:** 
1. Contact VPS provider to upgrade
2. Update JVM heap in `/etc/systemd/system/hcb.service` (change `-Xmx512m` to `-Xmx1024m`)
3. Reload systemd: `sudo systemctl daemon-reload`
4. Restart application: `sudo systemctl restart hcb`

### Q: How do I add a new admin user?
**A:** Run SQL:
```sql
-- Generate bcrypt hash first (use online tool or Java code)
INSERT INTO users (email, mobile, password_hash, full_name, email_verified, active)
VALUES ('newadmin@example.com', '9876543210', '$2a$12$HASH_HERE', 'New Admin', TRUE, TRUE);

INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE email = 'newadmin@example.com'), 'ADMIN');
```

### Q: What if Gmail's 100 email/day limit is exceeded?
**A:** Options:
1. Upgrade to G Suite/Google Workspace (higher limits)
2. Switch to SendGrid (free tier: 100 emails/day, paid: unlimited)
3. Switch to AWS SES (very cheap, high limits)

Configuration change only — no code changes needed.

### Q: How do I restore from backup?
**A:** See [DEPLOYMENT-GUIDE.md](./deployment/DEPLOYMENT-GUIDE.md) → "Rollback Procedure"

### Q: Can customers place orders without creating an account?
**A:** Currently: No (guest checkout disabled by default)
Future: Can be enabled via settings: `guest_checkout_enabled = true`
(Requires code implementation for guest orders)

### Q: How do I migrate to PostgreSQL later?
**A:** Complex process:
1. Dump MariaDB data
2. Convert schema (minor dialect differences)
3. Update `application.properties` (change JDBC URL)
4. Update `pom.xml` (change driver dependency)
5. Test thoroughly

Not recommended unless there's a compelling reason.

---

## Version History

**v1.0.0** (Target: October 2026)
- Initial production release
- Customer registration/login
- Product browsing
- Order placement
- Payment verification (manual)
- Admin panel
- Email notifications
- Database backups
- Security hardening

**Planned Future Enhancements:**
- Product variants (size, flavor) — v1.1.0
- Discount codes — v1.2.0
- Customer reviews — v1.3.0
- Bulk product import (CSV) — v1.4.0
- Invoice generation (PDF) — v1.5.0
- SMS notifications (optional) — v2.0.0
- Payment gateway integration (optional) — v2.0.0

---

## Support & Contact

**Project Lead:** Ashwin Golani  
**Email:** ashwin@ideaai.in  
**Domain:** hcb.ideaai.in  
**Repository:** (Add GitHub repository URL)

**For Issues:**
1. Check [QUICK-REFERENCE.md](./QUICK-REFERENCE.md) → Troubleshooting section
2. Review application logs: `/var/log/hcb/application.log`
3. Contact: ashwin@ideaai.in

**For Security Issues:**
- Email: ashwin@ideaai.in with subject "HCB Security Issue"
- Encrypt with PGP if sensitive (optional)

---

## License & Attribution

**Application License:** (Add your license — MIT, Apache 2.0, Proprietary, etc.)

**Third-Party Dependencies:**
- Spring Boot — Apache License 2.0
- Thymeleaf — Apache License 2.0
- MariaDB — GPL v2
- Apache HTTP Server — Apache License 2.0
- Let's Encrypt — Free SSL certificates

---

## Acknowledgments

**Architecture designed by:** Kiro AI Assistant  
**Developed by:** Ashwin Golani  
**Business Owner:** Healthy Choco Bytes  

---

**Documentation Last Updated:** September 28, 2026  
**Next Review:** December 28, 2026 (Quarterly)

---

**Ready to build? Start with [IMPLEMENTATION-ROADMAP.md](./tasks/IMPLEMENTATION-ROADMAP.md) Day 1!**
