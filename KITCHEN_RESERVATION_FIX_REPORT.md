# Kitchen and Reservation Fix Report

## Kitchen blank page

- Root cause: the expiry-batch API returns `ingredientId`, but `Kitchen.vue` dereferenced `batch.ingredient.name` and `batch.ingredient.unit`. With real expiry data this caused a Vue render exception. Inventory/menu/realtime failures also lacked isolated fallback state.
- Files changed: `Kitchen.vue`, `kitchenData.js`, Kitchen runtime and route tests.
- API/permission: existing queue, ingredient, expiry-batch and product read APIs remain available to KITCHEN. Ingredient and batch mutation methods are now limited to ADMIN/MANAGER; no endpoint was changed to `permitAll`.
- Behavior: queue, inventory and menu failures render within their own section. Invalid collection/decimal payloads cannot crash the page, and failed WebSocket setup falls back to the existing 60-second polling.
- Tests: real component mount covers queue data, empty queue, queue failure, inventory data with expiry DTO, and inventory failure; route tests cover direct KITCHEN access and unrelated-role denial.

## Admin send to kitchen

- Root cause: dispatch reused `confirmManualDispatch`, whose first guard rejected every paid order, including a valid paid PENDING transfer. `AdminOrder.vue` then hid the backend reason behind a generic permission message.
- Authorization: `PUT /api/admin/orders/{id}/dispatch-to-kitchen` allows only ADMIN, MANAGER and WAITER through method security. CUSTOMER, CASHIER and KITCHEN are not granted this action.
- Status transition: only PENDING/SCHEDULED orders continue through the existing locked state machine and inventory-hold consumption. Cancelled, already-dispatched, unpaid-transfer and malformed legacy orders return specific conflicts.
- Idempotency: the pessimistic order lock serializes double clicks; the second request is rejected as already dispatched before inventory consumption or realtime publication.
- Realtime: a successful dispatch publishes one `NEW_ORDER` event to `/topic/kitchen`, which triggers the Kitchen board refresh.
- Tests: role matrix, valid COD and paid-transfer dispatch, unpaid transfer, repeat dispatch, legacy null payment option and Kitchen notification assertions.

## Reservation special requirements

- Root cause: Step 7 awaited `/api/reservations/quote` outside `try/catch`; any backend error became an unhandled promise and the UI appeared to do nothing. Checkbox labels were joined into an unbounded translated string.
- Step navigation: quote loading is guarded, backend reasons are shown in the page and toast, and duplicate clicks are disabled while navigating.
- Deposit = 0: `nextReservationStep(7, quote)` goes directly to Step 9 Confirm.
- Deposit > 0: the same function goes to Step 8 Payment.
- Data: all seven checkboxes normalize to explicit true/false values and serialize to a bounded stable flag contract; the optional free note remains separate and is capped at 500 characters. Back navigation retains the reactive selections.
- Tests: empty, single/multiple, allergy and note normalization; zero/positive payable navigation; source contract for API error handling.

## Commands executed

- Focused frontend Vitest command for Kitchen/router/reservation tests.
- `npm run lint`.
- Focused Maven tests for order dispatch, authorization and workflow.
- `npm run test -- --run`.
- `.\mvnw.cmd test`.
- `.\mvnw.cmd clean test`.
- `npm run build`.

## Test results

- Focused frontend: 21/21 passed.
- Frontend lint: passed with zero warnings.
- Focused backend: 40/40 passed.
- Full frontend: 94/94 passed across 28 test files.
- Backend `test`: 440/440 passed with zero failures, errors or skips.
- Backend `clean test`: 440/440 passed with zero failures, errors or skips.
- Frontend production build: passed (Vite, 282 modules transformed).

## Remaining issues

- No known remaining defect in the three requested Kitchen, dispatch and reservation flows. Release check: PASS.
