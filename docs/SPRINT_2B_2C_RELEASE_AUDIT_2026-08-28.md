# Sprint 2B/2C release audit (2026-08-28)

## Sprint 2B status

- **QR payment integration:** existing payment and order-payment suites cover reservation deposits, order QR reuse/regeneration, paid/expired states, webhook idempotency and concurrency. Unicode account-holder responses are asserted by the payment service regressions; no real provider is called.
- **Encoding health:** `GET /api/admin/system/encoding-health` is restricted to ADMIN/MANAGER and reports only scoped counts for `PaymentIntent.accountHolder`. It never returns account-holder values or writes PII to logs.
- **Realtime:** the WebSocket configuration and client reconnect/fallback paths are covered by `WebSocketConfigTest`, kitchen flow contracts and the existing order workflow tests. Reservation lookup deactivates/reconnects its client and Kitchen polling is bounded and cleaned up on unmount; REST remains the fallback.
- **RBAC/IDOR:** the endpoint authorization matrix, staff-operation security tests and customer data-isolation integration tests cover the sensitive inventory, operations, staff, attendance, reservation, cancellation, payment and analytics surfaces. The new encoding-health route is explicitly protected in `SecurityConfig`.

## Sprint 2C status

### Static strategy

**STATIC STRATEGY = A: bundled Spring Boot SPA.** Vite writes directly to `quanlynhahang/src/main/resources/static`, and the backend serves that directory. The release unit is therefore the frontend source plus one deterministic Vite output and the backend jar.

Build/sync sequence:

1. `npm ci`
2. `npm run lint`
3. `npm run test`
4. `npm run build` (the configured `outDir` is the backend static directory)
5. `mvn test`
6. `mvn package`

`node_modules`, `target`, logs, temporary files, local `.env` files and standalone `dist` output are not release inputs. Generated hashed files under `quanlynhahang/src/main/resources/static` are deployment artifacts and must be reviewed as one atomic build change; do not hand-edit or selectively delete hashes.

### Responsive verification

CSS/source contracts were inspected for Home, Menu, Reservation, ReservationLookup, OrderHistory, QR/payment and checkout dialogs. Order history now becomes labelled cards at <=700px; checkout becomes a full-height mobile sheet with a sticky header, scrolling body and sticky actions. Existing Reservation/Menu responsive rules retain compact quantity controls and viewport-safe dialogs.

**REAL BROWSER VERIFIED = PARTIAL.** Chrome headless rendered the public `/`, `/menu`, `/reservation` and `/reservation-lookup` routes at 375x812, 390x844, 430x932, 768x1024, 1024x768, 1366x768 and 1920x1080. The protected `/order-history` route correctly redirected to the public home without an authenticated session; authenticated history/checkout interaction and overflow inspection still require a logged-in browser session. Source contracts cover the protected layouts, but are not a substitute for that authenticated verification.

## Deferred safety item

Reschedule/reassign-table UX remains deferred until the backend exposes an availability lock, transactional resource swap and audit history. No dead “Đổi lịch/Đổi bàn” buttons were added.
