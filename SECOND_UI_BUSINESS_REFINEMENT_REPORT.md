# SECOND UI BUSINESS REFINEMENT REPORT

## 1. Scope
Batch này xử lý refinement UI/nghiệp vụ sau commit `0a46344`, tập trung vào kho, nhân sự, sản phẩm, khu vực đặt bàn, đặt món trước, thực đơn tại bàn và thực đơn public.

## 2. Baseline
Branch kiểm tra: `appmod/java-upgrade-20260818181401`. Baseline đầu vào: `0a46344 feat: refine inventory staff knowledge and pricing workflows`.

## 3. Safety Constraints
Không reset/checkout. Không commit/push. File Word `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx` là thay đổi user-owned và không thuộc batch.

## 4. Source Audit Summary
Đã rà các luồng chính: nguyên liệu/lô nhập/công thức, nhân viên/khách hàng/password reset, quản trị sản phẩm, đặt bàn/khu vực/bàn, dine-in menu, public product menu, API table areas.

## 5. Latest Ingredient Import Unit Price
Modal nhập lô mới hiển thị `Đơn giá nhập gần nhất` từ lịch sử batch đúng nguyên liệu qua `/api/admin/ingredients/{ingredientId}/batches`. Không prefill đơn giá nhập mới.

## 6. Ingredient Empty State
Nếu nguyên liệu chưa từng có lô nhập có `unitPrice`, UI hiển thị `Chưa có dữ liệu`.

## 7. Ingredient Pagination
Danh sách nguyên liệu admin dùng 7 dòng/trang, phân trang sau dữ liệu hiện hành và clamp page khi dữ liệu thay đổi.

## 8. Recipe Ingredient Search
Recipe editor có ô tìm nguyên liệu theo substring, trim, case-insensitive và loại nguyên liệu đã nằm trong công thức để tránh duplicate.

## 9. Import History Detail Button
Nút `Chi Tiết` trong lịch sử nhập kho được đổi sang button có khung, padding, border-radius và hover state rõ ràng.

## 10. Import Invoice Print
CSS print được chỉnh để chỉ in vùng biên nhận, giảm khả năng tràn trang thứ hai và dùng header bảng màu nâu/chữ trắng.

## 11. Staff Edit Username Contrast
Input username trong modal sửa nhân viên bị disabled nên nhận màu global input. Đã override đúng scope modal staff bằng background nâu và chữ trắng, gồm cả `-webkit-text-fill-color`.

## 12. Customer Edit Modal
Modal sửa khách hàng có container/card trắng, header/body/footer rõ ràng, border-radius, border, shadow, padding và responsive footer.

## 13. Password Reset Security
Luồng reset password giữ mô hình an toàn hiện có: trả temporary password một lần khi admin generate, lưu hash, bắt buộc đổi mật khẩu, revoke token, không log plaintext/current password.

## 14. Admin Product STT
Bảng quản trị sản phẩm có cột STT, tính theo trang bằng `pageStart + index + 1`.

## 15. Admin Product Width and Actions
Wrapper/table sản phẩm được nới rộng, action column giữ nowrap/min-width để nút sửa/xóa không bị cắt trên desktop; narrow viewport vẫn dùng scroll ngang.

## 16. Booking Summary Duplication
Đã bỏ summary trùng trong step chọn khách, giữ một summary canonical ở cột phải.

## 17. Booking Layout Semantics
Reservation layout đổi sang `booking-layout` chứa `form.reservation-card` và `aside.booking-summary` ngang hàng; summary không còn nằm trong form submit.

## 18. Booking Area Pagination
Màn chọn khu vực đặt bàn hiển thị 3 area/trang trên desktop, có pagination, giữ selection/state khi đổi trang.

## 19. Area Booking Readiness Rule
Backend thêm `TableAreaReadinessService`: khu vực dining/private chỉ booking-ready khi `ACTIVE`, có ít nhất 2 bàn hoạt động và tổng sức chứa bàn không vượt capacity khu vực. Event hall giữ rule booking sự kiện riêng.

## 20. Area API Contract
Public `/api/areas` chỉ trả khu booking-ready. Admin `/api/areas/admin` trả tất cả khu kèm `bookingReady`, `bookingReadyReason`, `usableTableCount`, `totalTableCapacity`.

## 21. Reservation Backend Enforcement
Backend chặn booking/quote/gợi ý bàn/bảng khả dụng/gán bàn nếu khu vực không booking-ready; không phụ thuộc frontend.

## 22. Menu Pagination
Reservation preorder dùng 10 món/trang. Dine-in full menu dùng 10 món/trang và sort món còn hàng trước món tạm hết. Public ProductMenu dùng 12 món/trang qua shared `MENU_PAGE_SIZE`.

## 23. Regression Result
Focused backend tests after the expiry-job refinement PASS: 29 tests, 0 failures, 0 errors. Full backend `mvnw.cmd test` PASS: 489 tests, 0 failures, 0 errors. Backend `mvnw.cmd package` PASS and produced the Spring Boot jar. Frontend `npm run lint` PASS. Frontend `npm test` PASS: 34 files, 131 tests. Frontend `npm run build` PASS and regenerated the Spring Boot static bundle under `quanlynhahang/src/main/resources/static`.

Runtime smoke on `localhost:8080` PASS: Spring Boot started, Flyway reported schema up to date, `/`, `/reservation`, `/menu`, `/staff-login`, `/admin/products`, `/admin/ingredients`, `/admin/tables`, `/admin/table-areas` all returned SPA `index.html`, public APIs/images returned 200, and important Vite chunks for Reservation/AdminProduct/AdminIngredient/AdminStaff/AdminTableArea/DineInOrder/ProductMenu/AdminDepositPolicy/AdminTable loaded with HTTP 200.

Scheduled reservation expiry regression PASS: the scan now isolates each candidate in its own transaction. A reservation blocked by the existing business rule `Bàn còn hóa đơn chưa thanh toán` is logged as an item-level failure, the scan reports `0/1 succeeded, 1 failed`, and no longer throws `UnexpectedRollbackException` or breaks the running application.

Visual browser automation was not available in this environment because Chrome/Edge/Firefox and Playwright/Puppeteer/Selenium were not installed.
