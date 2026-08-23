# Source audit – money, cancellation, inventory and operational flows

Date: 2026-08-21

This matrix was produced from the current frontend/backend/schema and tests, not from previous `FIXED` labels. `PARTIALLY_FIXED` means a useful implementation exists but at least one requirement in the new prompt is still absent or unsafe.

| Part | Status | Current-source evidence / remaining gap |
|---|---|---|
| 1. Reservation cancellation refund | FIXED | Policy uses the exact Vietnam booking datetime and `>= 12h => 50%` of the actually paid deposit from the successful payment ledger. |
| 2–4. Customer cancellation request + 2/4 verification + admin queue | FIXED | V054, same-reservation 2/4 verification, generic failures, duplicate guard, public receipt, admin queue and approve/reject actions are implemented. |
| 5. No fake refund completion | FIXED | Approval creates a `PENDING` refund; completion requires a unique real provider reference and writes a successful REFUND ledger transaction first. |
| 6. Cashier cancellation | FIXED | UI no longer promises a hard-coded/manual refund and displays backend `refundAmount`/`refundStatus`. |
| 7. Reservation contact confirmation | FIXED | Each contact update appends an immutable `ReservationContactLog`; current summary fields remain for compatibility. |
| 8–9. Available servings and frontend cap | FIXED | Public products expose batch-derived `availableQuantity = MIN(floor(batch stock / recipe amount))`; both customer carts cap quantities and show sold-out state. |
| 10. Ingredient aggregate versus FIFO batch | FIXED | Batch totals are the availability source of truth and direct aggregate quantity mutation now returns a conflict directing users to batch adjustments. |
| 11. Reserve stock until payment | FIXED | V055 stores per-order ingredient holds. Checkout leaves FIFO batches unchanged; active holds reduce availability; full payment/manual dispatch consumes once; cancellation/expiry releases. |
| 12. Anonymous checkout anti-spam | FIXED | Checkout has a dedicated 10/minute/IP limit and the stock-hold lifecycle prevents unpaid requests from permanently consuming batches. |
| 13. Kitchen item cancellation | FIXED | V056 and `KitchenOrderDetailService` now audit actor/reason/time, adjust held/consumed recipe stock based on whether preparation started, recalculate the parent order and create a pending prepaid excess refund. |
| 14. Central order state machine | FIXED | `OrderStatus` defines the persisted values and `OrderStateMachineService` is now the only operational writer of order status. It rejects unknown/skipped/terminal transitions and derives ready states from active dish state. |
| 15. Waiter serve permission | FIXED | Security allows WAITER only for detail `status=2`; the endpoint delegates to `KitchenOrderDetailService.serve`, which requires a completed dish and advances the parent only after every active dish is served. |
| 16. Merge table money state | FIXED | Merge locks both orders and refuses any paid amount, nontrivial payment status, intent/ledger, refund, voucher or active stock hold because safe allocation is unsupported; the unpaid source is zeroed after totals move. |
| 17. Split table validation | FIXED | Split requires an active target without an open order, rejects duplicate/missing/foreign detail IDs atomically, creates a canonical order and recalculates both unpaid balances; financial/stock allocations are safely blocked. |
| 18. Transfer table | FIXED | Transfer locks the target table, rejects disabled/occupied/conflicting targets and closed/non-dine-in orders, releases the source only if no other active order remains, occupies the target and revokes QR sessions on both sides. |
| 19. Unlink/release table | FIXED | `TableLifecycleService` is the single table-availability writer for direct release, unlink, cancellation, refund completion and order workflows. It locks tables, blocks unpaid/pending-payment/pending-refund/unserved/active-hold states, and revokes table capabilities in the same transaction. |
| 20. Event booking | FIXED | Event creation now records the authenticated owner, deposit expiry, payment capability, history, manager notification and realtime event; the UI stores the capability and creates a deposit QR. Public creation has dedicated CAPTCHA and 10/minute/IP limiting. |
| 21. Registration contract | FIXED | Email is explicitly required and uses the same structural rule on both sides; username/full-name constraints, 10–72 character password policy and common-password rejection run before confirmation and immediately before submit, with focused backend/frontend contract tests. |
| 22. Router role separation | ALREADY_FIXED | `session.js`, API interceptors and route guards use window-scoped customer/staff identities; unit and real-browser isolation checks pass. |
| 23. Staff profile | FIXED | `/staff/profile` now provides the existing staff schedule/timekeeping/salary profile under staff-token context for every staff role; Kitchen no longer opens customer `/profile`, `/staff` remains a compatibility redirect, and staff logout uses the shared session cleanup. |
| 24. Auth endpoint exposure | FIXED | Only exact login/staff-login/signup auth endpoints are public; profile/password endpoints require authentication. |
| 25. Lookup PII in URL | FIXED | Reservation lookup and cancellation verification use request bodies; phone is no longer written to or restored from the route query. |
| 26. Waitlist UI | FIXED | The auto-assignment step now shows single/combined availability and exposes a real waitlist action when neither option exists. The legacy manual table picker was removed because quote/creation deliberately assign the table on the backend; large unsupported groups still transfer to event booking. |
| 27. Table availability | FIXED | `/api/tables/available` and legacy `/api/tables/check-availability` now share date/time/duration/guest/area/late-dining validation and capacity/status filtering. Conflict detection uses full datetimes and checks the previous date, so reservations spanning midnight are not missed. |
| 28. Delivery/takeaway DTO | FIXED | DELIVERY now requires backend-validated `recipientName`, Vietnamese `recipientPhone` and `deliveryAddress`, accepts a separate `deliveryNote`, and persists/returns each field without concatenating PII into `address`; TAKEAWAY/DINE_IN remain conditionally independent. |
| 29. Voucher accounting | FIXED | Checkout calculates and persists `originalSubtotal`, membership discount, voucher discount, tax and final total separately; voucher usage records only the actual voucher reduction on its true pre-voucher base. |
| 30. Work schedule schema | FIXED | The entity maps all required shift snapshot columns, schedule creation derives canonical name/start/end values, and V057 repairs/backfills blank and legacy schemas without changing old migrations. |
| 31. Attendance integrity | FIXED | V058 enforces one row per employee/business date, check-in locks the employee identity before testing/inserting the row, lateness uses the persisted schedule start plus configured grace, and check-out persists one overnight-safe `totalHours` calculation. |
| 32. Kitchen performance | FIXED | Kitchen uses WebSocket as the primary update path, a status/date-scoped board endpoint instead of the admin order list, and a 60-second fallback that refreshes inventory/menu data only for the active tab. |
| 33. Avoid `findAll()` | FIXED | Named operational hot paths are query-scoped: admin orders are capped, dashboard/revenue/waiter/kitchen use status/date queries, reservations query operational tables, popular/purchase reports query completed orders in-period and only relevant recipes, and assistant invoice summaries use filtered repository queries. Remaining full lists are intentional small reference/catalog CRUD or startup validation/seed paths. |
| 34. Canonical theme | ALREADY_FIXED | Source/generated-bundle palette regressions pass; Edge rendered eight public routes without legacy palette matches. |
| 35. Native dialogs | FIXED | One global accessible confirm/prompt dialog and the existing global toast now cover refund/payment, cancel dish/order, merge/split/delete/release table and order dispatch. Noncritical legacy notices may migrate incrementally without blocking dangerous flows. |
| 36. Invoice printing | FIXED | All three cashier print paths clone the rendered invoice/report into an isolated print iframe; Vue's body, listeners and application state are never replaced or reloaded. |
| 37. Recruitment upload | FIXED | JSON and multipart metadata validate name/phone/email/message/post ID before persistence, the recruitment post must exist, and `CvFileStorageService` enforces size, extension, MIME, magic/structure, private download and retention. |
| 38. Controlled dead-code cleanup | FIXED | Disconnected reservation code was removed only after flow verification; remaining fallbacks are connected and observable, and ESLint completes cleanly after a fresh install. |
| 39. Clean package/build | FIXED | `npm ci` succeeds from the lockfile, transitive audit findings were upgraded to zero known vulnerabilities, lint/test/build pass, backend wrapper test/package pass and `mvnw` is tracked executable for Linux. |
| 40. Migrations | FIXED | Changes through V059 are additive Flyway migrations with blank/legacy database integration tests. V059 enforces exclusive waitlist-to-reservation linkage; new schema work must start at V060. |
| 41. Transactions/concurrency | FIXED | Last-stock checkout uses pessimistic FIFO locks and holds; refund/cancellation approval locks request/reservation rows; and SQL Server integration regressions exercise last-table, reservation retry, waitlist claim, voucher, attendance, payment callback, contended transfer and contended merge races. Provider credits serialize on a transaction-owned application lock, while table workflows lock rows deterministically and revoke obsolete capabilities. |
| 42. Role matrix | FIXED | Backend method and route rules enforce public customer actions, waiter serving/table scope, kitchen dish transitions, cashier finance scope and manager/admin cancellation/refund administration; frontend visibility is not the security boundary. |
| 43. Cancellation 2/4 tests | FIXED | `ReservationCancellationServiceTest` covers valid pairs, 0/1 matches, cross-booking fields, normalization, closed and duplicate requests. |
| 44. Twelve-hour refund tests | FIXED | `ReservationCancellationPolicyTest` covers 13h, 12h01, exactly 12h, 11h59 and a past booking. |
| 45. Inventory lifecycle tests | FIXED | Hold-without-consume, release, consume-once, availability subtraction, payment expiry and legacy/blank migration regressions pass alongside concurrent last-stock and atomic inventory tests. |
| 46. Contact-log tests | FIXED | `ReservationServiceTest` verifies contact updates append history instead of replacing it. |
| 47. Whole-system regression | FIXED | Focused security, money, inventory, reservation, event, table, kitchen, waiter, attendance and migration regressions exist; final full backend/frontend runs verify their combined contract. |
| 48. Code quality/settings | FIXED | Refund hours/rate and attendance grace are configuration-backed, order states use the canonical enum/state machine, shared dialog/toast/print helpers avoid duplicate view logic, and no duplicate DTO/status/service was introduced. |
| 49. Error honesty | FIXED | Operational failures use explicit HTTP errors or visible UI feedback; intentionally nonfatal realtime/AI audit/config/date fallbacks are logged instead of silently ignored, and malformed persisted JSON now fails visibly. |
| 50. Final verification | FIXED | Current source passes 408 backend tests (0 failures/errors/skips), 56 frontend tests, lint, clean dependency install/audit, production frontend build, backend package and blank/legacy SQL Server migrations through V059. Regenerated static assets pass the theme regression. |
| 51. Final report | FIXED | Source-state, defects, migrations, APIs, business flows, verification evidence and remaining nonblocking debt are recorded in this audit, `KNOWN_FIXED_BUGS.md`, `BUSINESS_FLOWS.md` and `REGRESSION_TESTS.md`. |

## Confirmed highest-priority defects

1. `P0-MONEY-01`: reservation refunds are falsely completed without provider confirmation.
2. `P0-MONEY-02`: reservation cancellation refund calculation does not implement the requested `>= 12 hours => 50% of actually paid deposit` rule.
3. `P0-CANCEL-03`: there is no customer cancellation-request workflow or same-booking 2/4 verification.
4. `P0-CANCEL-04`: there is no admin/manager approval queue, request history or double-approval guard.
5. `P0-INVENTORY-05`: FIXED by V055 and the order inventory reservation lifecycle.
6. `P0-ORDER-06`: FIXED by the transactional kitchen cancellation flow and V056 audit field.

Implementation order is the order above, keeping existing verified session, migration, theme, QR capability and ledger behavior intact.
