# Master Prompt V2 flow audit — 2026-08-20

## 1. Source state

| Item | Result |
|---|---|
| Current commit | `1efaaf1` (`origin/main` was the same at audit start) |
| Working tree | Modified by this audit; user ZIP, backup directories and `.bak` files were preserved and not edited/deleted. No commit was created. |
| Baseline | Backend 293 tests; frontend 26 tests; lint 0 errors/8 warnings; frontend build; blank DB 50 migrations — all passed before this continuation. |
| Final automated verification | Backend 311 tests; frontend 42 tests; lint 0 errors/8 existing warnings; frontend production build; blank DB 53 migrations — all passed. Packaged runtime served 34/34 Vue routes and repeated expiry scans without errors. Edge rendered 8 public routes without failed HTTP responses and verified five page-scoped sessions in one browser context. |

## 2. Existing fixes inspected

| Item | Status | Verified how | Modified? | Reason |
|---|---|---|---|---|
| FIXED-01 order-type backfill | DONE_AND_VERIFIED | V049 and explicit checkout validation/tests | No | Existing fix remains correct. |
| FIXED-02 rate-limit defaults | DONE_AND_VERIFIED | profile configuration and regression | No | Existing fix remains fail-closed. |
| FIXED-03 bounded reservation expiry | DONE_AND_VERIFIED | capped candidate query and V050 indexes | No | No full-table expiration scan. |
| FIXED-04 contact task status | NOT_APPLICABLE | source/workflow comparison | No | The live workflow uses `contact_status`; a second undefined state machine was not invented. |
| FIXED-05 unique preflight | DONE_AND_VERIFIED | legacy/blank migration integration tests | No | Existing preflight remains correct. |
| FIXED-06 deposit consistency | DONE_AND_VERIFIED | payment/deposit fields and existing state tests | No | Kept current paid/remaining/payment behavior. |
| FIXED-07 kitchen safe errors | DONE_AND_VERIFIED | safe extractor and frontend tests/build | No | Existing toast behavior remains correct. |
| FIXED-08 AI recommendation | DONE_AND_VERIFIED | DB-backed availability and fallback tests | No | Existing architecture preserved. |
| FIXED-09 deposit expiry | DONE_AND_VERIFIED | create/expiry service and regression | No | Existing timestamp contract preserved. |

## 3. Bug audit

| ID | Severity | Flow / before | After / principal files | Regression | Status |
|---|---|---|---|---|---|
| BUG-A01 | P0 | Cancel used deposit-derived refund semantics | Ledger-derived refundable balance, one active refund, pending state and table/session release; `OrderRefundService`, `AdminOrderController` | `OrderRefundServiceTest` | DONE_AND_VERIFIED |
| BUG-A02 | P0 | Browser-wide auth keys could cross windows | Active identity/context moved to `sessionStorage`; `session.js` and all callers | `session.test.js`, `api.test.js` | DONE_AND_VERIFIED |
| BUG-A03 | P0 | Customer route could observe another window's staff token | Router/API read the active window context only | auth regression matrix | DONE_AND_VERIFIED |
| BUG-A04 | P0 | Password change inferred context from token existence | Login records explicit `auth_context`; password screen consumes it | auth regression matrix | DONE_AND_VERIFIED |
| BUG-A05 | P1 | Delivery caller omitted required type | Delivery request always sends `DELIVERY`; `orderCheckout.js`, `ProductMenu.vue` | `orderCheckout.test.js` | DONE_AND_VERIFIED |
| BUG-A06 | P1 | Quote required a selected table while create auto-assigned | Quote request accepts area/party/time and invokes the create assignment path | `ReservationServiceTest` | DONE_AND_VERIFIED |
| BUG-A07 | P1 | No preorder skipped payment | Payment/deposit choice is independent of preorder | frontend reservation test/build | DONE_AND_VERIFIED |
| BUG-A08 | P1 | Frontend duplicated opening-hour literals | Public settings expose service hours/timezone; reservation/event use them | business-hours tests | DONE_AND_VERIFIED |
| BUG-A09 | P1 | UTC ISO slicing could shift the business date | Shared Asia/Ho_Chi_Minh date helper used by business screens | `businessDate.test.js` | DONE_AND_VERIFIED |
| BUG-A10 | P1 | Event double submit/retry could duplicate | UI guard + UUID key; persisted fingerprint/unique key and conflict semantics | `ReservationServiceTest` | DONE_AND_VERIFIED |
| BUG-A11 | P0 | QR table ID/name was authorization | 256-bit capability, SHA-256 at rest, expiry/rotation/revocation; V052 | `TableSessionServiceTest` | DONE_AND_VERIFIED |
| BUG-A12 | P0 | Guest could not securely add to current order | Capability resolves current open order and authorizes idempotent add-items without staff token | `TableSessionServiceTest` | DONE_AND_VERIFIED |
| BUG-A13 | P1 | Kitchen inferred delivery from address | Uses `orderType`, `tableName`, `areaName` | DTO/privacy workflow tests | DONE_AND_VERIFIED |
| BUG-A14 | P1 | Waiter zone filter parsed address | Uses exact table/area/service-zone IDs | workflow tests + build | DONE_AND_VERIFIED |
| BUG-A15 | P1 | Cashier rendered table from address | Uses structured table/delivery fields | workflow tests + build | DONE_AND_VERIFIED |
| BUG-A16 | P1 | Order DTO lacked operational structure | Added type/table/area/delivery/code/schedule fields; `OrderResponse` | `OrderResponsePrivacyTest` | DONE_AND_VERIFIED |
| BUG-A17 | P1 | Order code was transient | Persisted unique `order_code`, collision checked; V051 | migration/workflow tests | DONE_AND_VERIFIED |
| BUG-A18 | P1 | Scheduled activation loaded all orders/address metadata | Indexed query by status and `scheduled_at`; V051 | repository/workflow + migration tests | DONE_AND_VERIFIED |
| BUG-A19 | P1 | Admin scheduled polling survived route exit | Interval retained and cleared on unmount; other persistent resources audited | `resourceCleanup.test.js` | DONE_AND_VERIFIED |
| BUG-A20 | P1 | Numeric status comparison included cancellation | Kitchen/waiter KPIs use explicit completion states | frontend tests/build | DONE_AND_VERIFIED |
| BUG-A21 | P1 | Waitlist phone appeared in URL | Validated POST body lookup; controller/security/filter updated | waitlist/rate-limit tests | DONE_AND_VERIFIED |
| BUG-A22 | P1 | Waitlist duplicated business hours | Uses `RestaurantBusinessHoursService` | service tests | DONE_AND_VERIFIED |
| BUG-A23 | P1 | Count-based code could race | UUID-based code with collision retry | service tests | DONE_AND_VERIFIED |
| BUG-A24 | P1 | Schedule/zone parsing was lenient legacy date | Strict `LocalDate` with Vietnam zone | controller tests/full suite | DONE_AND_VERIFIED |
| BUG-A25 | P1 | Work schedule DTO allowed invalid input | Bean constraints plus controller `@Valid` | `WorkScheduleRequestValidationTest` | DONE_AND_VERIFIED |
| UI-BUG-01 | P1 | Route skeleton used old palette | `App.vue` uses canonical tokens | theme scan/build | DONE_AND_VERIFIED |
| UI-BUG-02 | P1 | RGB/RGBA variants escaped HEX scan | normalized case-insensitive RGB/RGBA blacklist | `themeTokens.test.js` | DONE_AND_VERIFIED |
| UI-BUG-03 | P1 | Reservation contained olive/gold variants | classified into brand/success/warning/neutral semantic tokens | theme scan/build | DONE_AND_VERIFIED |
| UI-BUG-04 | P2 | Unnecessary legacy hardcodes remained | source-wide Vue/CSS/JS audit; semantic white/status values retained when intentional | theme scan | DONE_AND_VERIFIED |
| UI-BUG-05 | P1 | Backend served prior hashes | Vite `emptyOutDir` rebuilt backend static output and rewrote index references | build + output scan | DONE_AND_VERIFIED |
| UI-BUG-06 | P1 | Regression test missed formats/output | scan covers source and generated backend output | `themeTokens.test.js` | DONE_AND_VERIFIED |

## 4. Multi-window session report

| Window | Role | Storage/token/router/API identity/logout isolation | Status |
|---|---|---|---|
| A | Customer | Window `sessionStorage`; customer context/token; customer routes; customer bearer; customer-only logout | DONE_AND_VERIFIED by unit simulation |
| B | Waiter | Independent staff context/token; waiter route/role/bearer; local logout | DONE_AND_VERIFIED by unit simulation |
| C | Kitchen | Independent staff context/token; kitchen route/role/bearer; local logout | DONE_AND_VERIFIED by unit simulation |
| D | Cashier | Independent staff context/token; cashier route/role/bearer; local logout | DONE_AND_VERIFIED by unit simulation |
| E | Admin/Manager | Independent staff context/token; protected admin route/bearer; local logout | DONE_AND_VERIFIED by unit simulation |

Refresh, 401 clearing, display identity, direct-route authorization and password context use the same window-scoped contract. Five simultaneous Edge pages in one browser context retained distinct customer/waiter/kitchen/cashier/admin session values through reload and customer-only logout. Live authenticated API behavior for all five roles still requires valid role credentials.

## 5. Order flow report

| Flow | Result |
|---|---|
| Delivery / takeaway / staff dine-in | Explicit order type; no backend default. |
| QR first order / add more | Current capability required; open order is reused; wrong/old capability is rejected. |
| Kitchen / waiter / cashier | Structured type, table and area data; no address inference. |
| Cancel/refund | Actual successful ledger payments minus refunds; duplicate-safe pending refund; table and capability released/revoked. |
| Scheduled order | Persisted indexed timestamp and order code; database activation query. |

## 6. Reservation flow report

Availability, quote and create share auto-table assignment. Preorder is optional and does not suppress the payment/deposit choice. Public configured hours and Vietnam dates are shared by reservation, event and waitlist. Event creation has persisted idempotency/fingerprint conflict handling. Waitlist lookup keeps phone out of URLs and its code generation is race-safe.

## 7. Theme and generated static report

| Area | Legacy before | Converted / token | Remaining | Visual transition verified? |
|---|---:|---|---:|---|
| `App.vue` route skeleton | old cream/olive literals | brand/surface tokens | 0 blacklisted | Automated only |
| `AppNavbar.vue` | old RGB/olive variants | brand/text tokens | 0 blacklisted | Automated only |
| `Reservation.vue` | olive/gold variants | brand + semantic success/warning/neutral | 0 blacklisted | Automated only |
| Login/Register | old RGB/cream variants | brand/surface/text tokens | 0 blacklisted | Automated only |
| Admin layout/views | old palette variants | canonical tokens | 0 blacklisted | Automated only |
| Backend static assets | previous hashed build | fresh Vite bundle from current source | old index references 0 | Build/output scan only |

Old output was cleaned by the configured Vite `emptyOutDir`; frontend rebuilt; backend static assets and `index.html` reference the new hashes. Generated hashed assets were not edited by hand.

## 8. Files modified (grouped impact matrix)

| Files | Bug IDs | Why / risk |
|---|---|---|
| `Frontend/.../services/session.js`, `api.js`, router/auth views and auth callers | A02-A04 | Window-scoped identity; high auth risk, covered by auth tests. |
| `ProductMenu.vue`, `DineInOrder.vue`, `orderCheckout.*` | A05, A11-A12 | Explicit checkout/table capability; high order security risk. |
| Reservation/event/waitlist frontend, date/hour helpers | A06-A10, A21-A23 | Align quote/payment/time/idempotency contracts. |
| Kitchen/Waiter/Cashier/AdminOrder/AdminTable | A13-A20 | Structured operational fields, QR issuance and resource cleanup. |
| Backend controllers/services/entities/repositories/DTOs | A01, A06-A18, A21-A25 | Root-cause domain/API persistence changes; high, full suite passed. |
| V051-V053 | A10-A12, A17-A18 | Additive schema migrations with guarded legacy/blank behavior. |
| Frontend theme files/tests and backend static output | UI-01-06 | Token migration and regenerated deployable bundle. |
| Backend/frontend regression tests and `docs/*` | all | Prevent recurrence and record evidence. |

## 9. Files checked but not modified

| File/area | Inspected for | Why skipped |
|---|---|---|
| V049/V050 and rate-limit profile configuration | Existing fixes 01-03 | Already correct and covered. |
| AI recommendation services/tests | FIXED-08 | DB/fallback contract already correct. |
| Deposit-expiry creation/job | FIXED-06/09 | Existing state/timestamp behavior correct. |
| Kitchen safe error extractor | FIXED-07 | Existing safe toast path correct. |
| `contact_task_status` mapping | FIXED-04 | No defined second product workflow to implement safely. |
| User ZIP, backup directories and `.bak` files | Working-tree ownership | Preserved as user-owned artifacts. |

## 10. Test result

| Check | Result |
|---|---|
| Frontend lint | PASS — 0 errors, 8 pre-existing warnings |
| Frontend unit | PASS — 14 files, 42 tests |
| Frontend build | PASS — production Vite bundle copied to backend static |
| Frontend E2E | NOT_VERIFIED — no runnable E2E suite was present |
| Backend unit/integration | PASS — 311 tests, 0 failures/errors/skips |
| Backend final compile | PASS after public QR response minimization |
| Migration | PASS — blank/legacy preflight through V053 (53 migrations) |
| Concurrency/idempotency | PASS — inventory plus refund/event/table-session regressions |
| Multi-session | PASS by unit simulation and five simultaneous Edge pages using browser-native `sessionStorage` |
| Theme scan | PASS — source and generated output |
| Theme navigation | PARTIALLY VERIFIED — 8 public routes rendered in Edge, no legacy palette nodes or failed HTTP responses; reservation screenshot inspected |

## 11. Not verified

- **Live five-role authenticated browser exercise:** five real Edge pages verified browser-native window isolation with synthetic role sessions, refresh and one-window logout. End-to-end authenticated APIs, menus and authorization still require valid credentials for all five roles.
- **Complete manual visual interaction:** Edge rendered and inspected all 8 public routes with no legacy palette nodes or HTTP failures, including a full-page reservation screenshot. Staff/admin routes, hover/focus states, responsive breakpoints and cache behavior were not exhaustively inspected.
- **External payment-provider refund settlement:** the application correctly creates a pending ledger refund; final provider settlement requires configured provider credentials/webhook behavior and was not claimed as refunded cash.

## 12. Runtime continuation

Two defects that source/unit inspection had not exposed were found by launching the packaged application against SQL Server:

| ID | Observed failure | Root cause | Fix / evidence | Status |
|---|---|---|---|---|
| BUG-063 | `/dat-su-kien` and `/change-password` returned 401 on refresh; `/admin/ai-knowledge` returned 500 | MVC and Security maintained different SPA route lists | Shared `SpaRouteRegistry`; `SpaRouteRegistryTest`; 34/34 exact Vue routes returned 200 with the current asset hash | DONE_AND_VERIFIED |
| BUG-064 | Reservation expiry scheduler failed with SQL Server `TIME` versus `DATETIME` comparison | Hibernate/driver bound the `LocalTime` query parameter incompatibly | Explicit HQL `LocalTime` cast; live-database integration test; scheduler accelerated to five seconds and observed through multiple clean scans | DONE_AND_VERIFIED |
| BUG-065 | `/api/menu/hot` returned 500 in the packaged application | Entity/native SQL used `OrderDetails`, but the Flyway schema is `order_details` | Canonical table mapping/query; live SQL Server integration test; browser smoke with no failed responses | DONE_AND_VERIFIED |
| BUG-066 | Public `/dine-in` received 401 while loading popular dishes | Guest screen called an admin-only endpoint | Switched to public `/api/menu/hot`; frontend endpoint contract test; browser smoke | DONE_AND_VERIFIED |

The final packaged runtime was shut down cleanly after smoke testing and port 8080 was confirmed free. Edge driven through Playwright rendered all 8 public routes with non-empty DOM, zero legacy palette matches and no failed HTTP responses. The inspected full-page reservation screenshot is `C:\Users\dophu\AppData\Local\Temp\codex-v2-reservation-final.png`; this evidence is intentionally described as partial visual verification rather than exhaustive manual UX coverage.
