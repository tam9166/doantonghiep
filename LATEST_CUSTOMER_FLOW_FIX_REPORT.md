# Latest Customer Flow Fix Report

Date: 2026-08-25

## Delivered changes

- Admin customer invoice history now uses an opaque dark-text modal, stronger overlay/table headers, themed summary values, search input and `Xem & In`/`Đóng` controls.
- Customer registration has an accessible red-brand terms checkbox. The button and submit handler block unchecked registration; the backend requires `termsAccepted=true`.
- Customer and dine-in checkout send stable `X-Idempotency-Key` values. A retry with the same cart returns the original order/payment rather than duplicating order, inventory hold or QR intent.
- Public dine-in no longer requires QR. The backend still locks and validates table existence, active/available state, product state, quantities and inventory; the first order occupies the table and publishes a Kitchen refresh event.
- Smart Suggestion now collects party size, multiple favorite groups, palate and allergies. Results are limited to persisted, active, positive-availability menu products.
- Customer menu uses six desktop columns, responsive tablet/mobile columns, centered low/out-of-stock messaging and filter-first pagination of 24 products per page while preserving cart state.
- V073 seeds 40 alcoholic beverages across Vietnamese/international beer, red/white wine, whisky, vodka, cognac/brandy and sake. Every row has its own remote product image, price, volume, ABV, description, active state and recipe-backed inventory.

## Root cause of checkout 409

V006 created the active payment-intent uniqueness key on `(reservation_id, payment_option)`. Order payments have no reservation, and SQL Server's filtered unique index treated a second pending order payment as another `(NULL, FULL)` key. The database rejected an otherwise valid QR intent, which the global handler exposed only as a generic data conflict.

V072 replaces that key with `(aggregate_type, aggregate_id, payment_option)` and adds a unique checkout idempotency key/request hash. Conflict responses for changed/inactive/out-of-stock cart data now retain a meaningful message and affected inventory fields.

## Main source files

- Frontend: `Register.vue`, `ProductMenu.vue`, `DineInOrder.vue`, `AdminStaff.vue`, `menuPagination.js` and related tests.
- Backend: `OrderController`, `OrderCheckoutService`, `MenuRecommendationService`, signup/menu/product/order DTO/entities/repositories and exception mapping.
- Database: `V072__fix_order_payment_and_checkout_idempotency.sql`, `V073__seed_alcoholic_beverage_menu.sql`.
- Regression: checkout/controller/recommendation/auth/migration tests plus new frontend customer-flow and pagination contracts.

## Verification

- Frontend focused regression: 11/11 passed.
- Frontend lint: passed.
- Frontend full test suite: 81/81 passed across 24 files.
- Frontend production build: passed and packaged static assets were regenerated.
- Backend focused customer-flow tests: 50/50 passed before final additions; recommendation allergy-only regression: 9/9 passed.
- SQL Server checkout/payment persistence: 12/12 passed.
- Blank canonical database: 73/73 Flyway migrations applied through V073.
- Legacy integrity fixture: scoped through V072 and passed 4/4 checks.
- Backend full regression: 435/435 passed with 0 failures, 0 errors and 0 skipped.

## Remaining issues

- None known in the requested scope after focused and full-suite verification.
