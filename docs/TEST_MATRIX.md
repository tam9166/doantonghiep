# Test matrix

For every flow in `BUSINESS_FLOWS.md`, test happy path, validation, unauthenticated, forbidden, not found, conflict, concurrent request, retry, timeout/network failure, DB failure and UI feedback. Priority execution order: auth/password, reservation/capacity, order/inventory, payment/voucher, staff/timekeeping, then realtime and deployment.

| Flow | Happy/valid | Boundary/failure | Regression evidence |
|---|---|---|---|
| Order classification | Explicit DINE_IN/TAKEAWAY/DELIVERY persists | Missing type returns 422; historical table-linked rows repaired | `OrderCheckoutServiceTest`, V049 |
| Rate-limit configuration | Production uses shared database quota | Base cannot silently disable; dev/test opt-out is explicit | `RateLimitProfileConfigurationTest` |
| Reservation expiry | Explicit expiry, legacy deposit timeout and no-show candidates transition | Query is capped at 200 and excludes unrelated rows | `ReservationServiceTest`, V050 |
| Dual browser sessions | Customer and staff can coexist | Staff request has no customer-token fallback; logout is namespace-local | `api.test.js`, `session.test.js` |
| Legacy migration | Blank and clean legacy databases reach V050 | Duplicate keys fail in preflight with remediation text | Migration integration tests |
| Kitchen feedback | Safe backend message is shown | Missing/unsafe payload uses a stable Vietnamese fallback | `errorMessage.test.js` and Kitchen integration in production build |
| Visual theme | Burgundy/navy tokens render across customer and staff screens | Legacy palette source values fail the suite | `themeTokens.test.js` |
