# Business flows

Auth, forced password change, reservation/event booking, order/inventory, kitchen/waiter, table state, cashier/payment, voucher, review, staff/timekeeping, recruitment, AI, WebSocket, errors, browser navigation, DB migration, and deployment are tracked in `TEST_MATRIX.md`. Each change must identify affected flows before patching.

The 2026-08-20 continuation specifically reverified order classification/history, rate-limit profile safety, scheduled reservation expiry/no-show processing, customer/staff dual-session isolation, kitchen error feedback, Flyway legacy-data preflight, and the shared visual theme. `contact_task_status` remains documented technical/product debt and is not an active business flow.
