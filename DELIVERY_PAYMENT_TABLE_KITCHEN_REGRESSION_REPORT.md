# Delivery Payment + Order Code + Table Name + Kitchen State Regression Report

Date: 2026-09-04
Branch: appmod/java-upgrade-20260818181401

## Scope

This batch covered:

- delivery checkout payment option selection
- order code / order status success screen
- manual confirmation for prepaid transfer orders
- table name visibility on staff-facing order flows
- kitchen "start work" state visibility
- regression tests and frontend bundle regeneration

## Files changed in this batch

- Frontend/nha-hang-frontend/src/locales/en.json
- Frontend/nha-hang-frontend/src/locales/vi.json
- Frontend/nha-hang-frontend/src/services/orderCheckout.js
- Frontend/nha-hang-frontend/src/services/orderCheckout.test.js
- Frontend/nha-hang-frontend/src/views/AdminOrder.vue
- Frontend/nha-hang-frontend/src/views/Kitchen.vue
- Frontend/nha-hang-frontend/src/views/ProductMenu.vue
- Frontend/nha-hang-frontend/src/views/finalFixContracts.test.js
- Frontend/nha-hang-frontend/src/views/kitchenRender.test.js
- Frontend/nha-hang-frontend/src/views/latestCustomerFlowContracts.test.js
- Frontend/nha-hang-frontend/src/views/secondUiBusinessRefinementContracts.test.js
- quanlynhahang/src/main/java/poly/edu/quanlynhahang/controller/AdminOrderController.java
- quanlynhahang/src/main/java/poly/edu/quanlynhahang/service/OrderPaymentService.java
- quanlynhahang/src/main/java/poly/edu/quanlynhahang/service/OrderStateMachineService.java
- quanlynhahang/src/test/java/poly/edu/quanlynhahang/controller/OrderWorkflowGuardTest.java
- quanlynhahang/src/test/java/poly/edu/quanlynhahang/service/OrderPaymentServiceTest.java

## Root cause

1. Delivery checkout did not preserve an explicit payment option for shipment orders, so the UI and backend could not distinguish prepaid transfer from cash-on-delivery flow.
2. The success state after order creation did not expose order code / payment metadata clearly enough for the customer.
3. Staff order management did not show payment state or a safe manual confirmation path for prepaid transfer orders.
4. Kitchen state transitions required a ready dish before entering the "partially ready / start work" flow, which blocked the intended parent-order workflow.

## Fix summary

- Added a delivery payment selector in `ProductMenu.vue`.
- Passed `paymentOption` through `orderCheckout.js`.
- Added success-state order details in the delivery checkout flow.
- Added payment method/status columns and a manual transfer-payment confirmation action in `AdminOrder.vue`.
- Added backend confirmation endpoint and service logic for transfer payments.
- Relaxed the kitchen state-machine guard so starting work does not require a ready dish.
- Updated kitchen UI to show a visible "Đang làm" state for in-progress dishes.
- Added/updated regression tests for frontend and backend workflows.

## Validation

Frontend:

- `npm run lint` — PASS
- `npm test` — PASS, 34 test files, 138 tests
- `npm run build` — PASS

Backend:

- `mvn test` — PASS, 497 tests, 0 failures, 0 errors
- `mvn package` — PASS

## Notes

- The build regenerated Spring Boot static frontend assets under `quanlynhahang/src/main/resources/static/assets`.
- The user-owned deleted Word file remains untouched in the working tree:
  `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx`
- No commit or push was performed in this step.
