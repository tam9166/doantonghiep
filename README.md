# Quan Ly Nha Hang

Du an gom Spring Boot backend va Vue frontend. Frontend duoc build vao `quanlynhahang/src/main/resources/static`, vi vay khi da build xong ban chi can chay `QuanlynhahangApplication.java` la mo duoc ca website.

## Yeu cau

- Java 21
- Node.js 20.19+ hoac 22.12+
- SQL Server chay cong 1433
- Database `RestaurantDB`

## Tao database tren may moi

Chay file SQL da gop truoc khi mo ung dung. Script nay tao database, schema va du lieu mau:

```powershell
cd E:\DoAnTotNghiep
sqlcmd -S localhost -E -i .\sql\SETUP_RESTAURANTDB.sql
```

Sau do khoi dong `QuanlynhahangApplication.java`; Flyway se tu dong cap nhat schema den ban migration moi nhat.

## Cau hinh local

Tao bien moi truong theo `.env.example`. Toi thieu can:

```powershell
setx DB_USERNAME "your_db_user"
setx DB_PASSWORD "change_me"
setx JWT_SECRET "change_me_to_a_random_value_at_least_32_chars"
setx PAYMENT_WEBHOOK_SECRET "change_me_to_a_random_webhook_secret"
setx GEMINI_API_KEY "your_google_ai_studio_key"
setx BANK_ACCOUNT_NUMBER "0000000000"
setx BANK_ACCOUNT_NAME "Demo Restaurant"
setx BANK_CODE "MB"
setx VITE_BANK_ACCOUNT_NUMBER "0000000000"
setx VITE_BANK_ACCOUNT_NAME "Demo Restaurant"
setx VITE_BANK_CODE "MB"
setx VITE_BANK_LABEL "MB Bank"
```

Mo PowerShell/IDE moi sau khi dung `setx`.

Neu muon gui email xac nhan yeu cau xuat hoa don, cau hinh SMTP va bat tinh nang nay. Khong dua mat khau email vao source code hoac Git:

```powershell
setx INVOICE_EMAIL_ENABLED "true"
setx INVOICE_EMAIL_FROM "restaurant@example.com"
setx MAIL_HOST "smtp.example.com"
setx MAIL_PORT "587"
setx MAIL_USERNAME "restaurant@example.com"
setx MAIL_PASSWORD "your_smtp_app_password"
```

Neu bat CAPTCHA that trong production, them:

```powershell
setx CAPTCHA_ENABLED "true"
setx CAPTCHA_PROVIDER "turnstile"
setx CAPTCHA_SECRET "your_turnstile_or_recaptcha_secret"
setx VITE_CAPTCHA_ENABLED "true"
setx VITE_CAPTCHA_PROVIDER "turnstile"
setx VITE_CAPTCHA_SITE_KEY "your_turnstile_or_recaptcha_site_key"
```

## Chay full du an

Lenh goi gon cho may local:

```powershell
cd E:\DoAnTotNghiep
.\scripts\run-local.ps1 -DbUsername your_db_user -DbPassword your_db_password
```

Neu cong 8080 bi chiem, dung cong trong tiep theo:

```powershell
.\scripts\run-local.ps1 -DbUsername your_db_user -DbPassword your_db_password -UseNextFreePort
```

Neu muon vua chay migration, vua build frontend truoc khi start:

```powershell
.\scripts\run-local.ps1 -DbUsername your_db_user -DbPassword your_db_password -RunMigrations -BuildFrontend
```

Neu frontend da build:

```powershell
cd E:\DoAnTotNghiep\quanlynhahang
.\mvnw.cmd spring-boot:run
```

Hoac mo `QuanlynhahangApplication.java` trong IDE va Run. Sau do truy cap:

```text
http://localhost:8080
```

Kiem tra nhanh sau khi server da chay:

```powershell
cd E:\DoAnTotNghiep
.\scripts\smoke-test-local.ps1
```

Neu muon test ca login nhan su:

```powershell
.\scripts\smoke-test-local.ps1 -StaffUsername admin -StaffPassword your_password
```

## Build production

```powershell
cd E:\DoAnTotNghiep
.\scripts\build-all.ps1
```

## Migration moi can chay

- `database/advanced-reservation-upgrade/19_reservation_idempotency.sql`
- `database/advanced-reservation-upgrade/20_reservation_waitlist.sql`
- `database/advanced-reservation-upgrade/21_seed_admin_analytics_demo_ascii.sql`
- `database/advanced-reservation-upgrade/22_repair_demo_product_category_vietnamese.sql`

Chay nhanh cac migration moi va seed thong ke:

```powershell
cd E:\DoAnTotNghiep
.\scripts\run-db-upgrade.ps1 -Mode Latest -Username your_db_user -Password your_db_password
```

Chi nap lai du lieu mau trang thong ke:

```powershell
cd E:\DoAnTotNghiep
.\scripts\run-db-upgrade.ps1 -Mode AnalyticsSeed -Username your_db_user -Password your_db_password
```
