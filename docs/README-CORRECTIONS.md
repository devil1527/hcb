# Architecture Corrections Applied

**Date:** September 28, 2026  
**Status:** ✅ All corrections applied and approved

---

## What Changed

The original architecture was 90% correct. The following corrections have been applied based on architectural review:

### 1. ✅ Timeline Reorganized
- **Was:** 6-8 weeks with artificial day numbers
- **Now:** Milestone-based implementation (15-20 days realistic)

### 2. ✅ Order Number Generation Fixed
- **Was:** `MAX(order_number) + 1` stored procedure (race condition)
- **Now:** Database auto-increment ID → Display format `HCB-1057`

### 3. ✅ Stock Transaction Design Explicit
- **Was:** "Use pessimistic locking" (vague)
- **Now:** Explicit transaction flow with `@Lock(PESSIMISTIC_WRITE)` and atomic UPDATE

### 4. ✅ Payment Screenshot Handling Clarified
- **Was:** Inconsistent (sometimes stored, sometimes WhatsApp)
- **Now:** WhatsApp-only initially, no file upload system

### 5. ✅ CSRF Implementation Corrected
- **Was:** Over-specified (`CookieCsrfTokenRepository`)
- **Now:** Spring Security default (Thymeleaf auto-token)

### 6. ✅ Security Headers Updated
- **Was:** Required obsolete `X-XSS-Protection` header
- **Now:** Removed, using `Content-Security-Policy` instead

### 7. ✅ Memory Allocation Made Configurable
- **Was:** Fixed at 512MB
- **Now:** Configuration-driven, measured based on actual VPS capacity

### 8. ✅ SMTP Made Provider-Agnostic
- **Was:** Gmail-specific configuration
- **Now:** Generic SMTP (works with Gmail, SendGrid, AWS SES)

### 9. ✅ Backup Designed for Offsite
- **Was:** VPS-only backups
- **Now:** Local + designed for future offsite extension

### 10. ✅ JAR Deployment Versioned
- **Was:** Overwrite single JAR file
- **Now:** Versioned JARs + symlink (easy rollback)

### 11. ✅ Order Idempotency Added
- **Was:** Missing duplicate order prevention
- **Now:** Idempotency token prevents double-submit

---

## Documents Updated

1. **ARCHITECTURE-CORRECTIONS.md** — Complete list of corrections (NEW)
2. **schema.sql** — Added `idempotency_token` column, removed MAX() stored procedure
3. **README-CORRECTIONS.md** — This summary document (NEW)

---

## What Did NOT Change

The core architecture remains the same:

✅ Modular monolith (Spring Boot + Thymeleaf + MariaDB)  
✅ Apache reverse proxy in front  
✅ Server-side price calculation  
✅ Historical order snapshots  
✅ Separate admin/customer authentication  
✅ Audit logging  
✅ systemd deployment  
✅ Let's Encrypt SSL  
✅ Cloudflare DNS  
✅ Daily backups  

---

## Implementation Status

**✅ Architecture approved for implementation**

**Next step:** Begin Milestone 1 (Foundation + Database)

Read [ARCHITECTURE-CORRECTIONS.md](./architecture/ARCHITECTURE-CORRECTIONS.md) for complete details of all corrections.

---

**Reviewed by:** Ashwin Golani  
**Approved:** September 28, 2026
