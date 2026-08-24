# FINAL FIX REPORT — Mộc Vị Restaurant Management System

Ngày xác minh: 2026-08-24
Phạm vi: FINAL FIX, 17 phần trước release

## 1. Trạng thái release

`READY FOR RELEASE`

Toàn bộ lint, unit/integration test, migration validation, frontend production build và backend clean package đều đạt. Không còn lỗi P0/P1 đã biết trong phạm vi FINAL FIX.

## 2. Global theme và design system

- Chuẩn hóa theme đỏ hồng/trắng/hồng nhạt, focus và primary action dùng màu thương hiệu.
- Loại bỏ màu xanh mặc định khỏi các token/component đã rà soát.
- Đồng bộ button, border, focus ring và trạng thái tương tác.
- Regression chính: `themeTokens.test.js`, `finalFixContracts.test.js`.

## 3. Icon system

- Dùng `UiIcon.vue`/`AdminNavIcon.vue` với SVG `currentColor` cho action, thống kê và điều hướng.
- Thay emoji và các ô icon trống trên Admin, Customer, Kitchen, Waiter, Cashier và màn hình đăng nhập.
- Chuẩn hóa căn giữa, khoảng cách và màu icon theo component cha.

## 4. Toast và notification

- Error: nền `#FFF1F2`, chữ `#991B1B`, viền `#E11D48`.
- Success và warning dùng nền pastel với chữ đậm dễ đọc.
- `window.alert` cũ được chuyển sang toast không chặn giao diện; không còn bật hộp thoại browser alert khi chạy ứng dụng.

## 5. Reservation và pre-order

- Quote có `depositAmount <= 0` bỏ qua Payment, không tạo payment intent/QR/giao dịch 0 đồng.
- Khi có tiền phải trả, luồng Payment giữ nguyên.
- Pre-order dùng một `orderNote` cấp reservation; không lưu ghi chú riêng từng món.
- Menu responsive: desktop 5, tablet 3, mobile 1–2 món mỗi hàng.
- V070 bổ sung dữ liệu order note và Brand Brain tương thích dữ liệu cũ.

## 6. Payment QR

- Thông tin nhận tiền lấy từ config/env, không hardcode ở frontend.
- Mặc định phát triển: MB Bank, `919112006789`, Hoàng Nguyễn Minh Tâm.
- Nội dung chuyển khoản: `DATBAN {ReservationCode}`.
- Response QR bổ sung metadata ngân hàng để hiển thị/copy an toàn.

## 7. Khu vực bàn và pricing

- Khu thường (`DINING`) không thu phí; `basePrice` cũ bị vô hiệu hóa và trả về 0.
- Phòng riêng (`PRIVATE_ROOM`) dùng entity/bảng `AreaPricing` riêng với `roomFee`, `minimumSpend`, `active`.
- Reservation chỉ cộng `roomFee` của phòng riêng đang hoạt động. `minimumSpend` là ngưỡng cam kết, được lưu/hiển thị riêng và không bị cộng như một khoản phí giả.
- V071 sao chép phí phòng riêng cũ sang `area_pricing`, sau đó đưa `table_areas.base_price` về 0.

## 8. Table map

- Heatmap lấy lượt sử dụng và doanh thu theo `tableId` từ order data.
- Có trạng thái `Chưa đủ dữ liệu phân tích` khi không có số liệu.
- Nút sơ đồ/heatmap/gộp bàn/layout không rớt chữ; status chip tự wrap và không chồng nhau.

## 9. Order management

- ADMIN, MANAGER và WAITER được phép thực hiện transition chuyển bếp đã được guard; không nới quyền sang transition khác.
- Scheduler chạy hằng ngày lúc 02:15, múi giờ Asia/Ho_Chi_Minh.
- Chỉ tự hoàn tất đơn quá hạn đã thanh toán; đơn chưa thanh toán vẫn được cảnh báo để tránh sai lệch tài chính.
- Audit ghi action `SYSTEM_AUTO_UPDATE`; dashboard có chỉ số/cảnh báo đơn quá hạn.
- Hệ thống hiện dùng numeric state machine; bước chuyển bếp tương ứng `IN_PREPARATION`, không tạo enum giả `SENT_TO_KITCHEN`.

## 10. Admin dashboard

- Khôi phục icon Doanh thu, Giá vốn, Chi phí, Lợi nhuận.
- Bổ sung card cảnh báo đơn quá hạn và endpoint dashboard trả thêm số lượng quá hạn.

## 11. Menu và category management

- Product desktop: form 30%, bảng 70%; bảng rộng tối thiểu 1200px và scroll ngang trong container.
- Category: 20 mục/trang, desktop 2 cột × 10 mục, pagination chỉ giữ vùng trang gần trang hiện tại, responsive trên màn hình nhỏ.

## 12. AI Knowledge và Brand Brain

- Card tri thức compact, preview 140 ký tự, action xem/sửa/xóa và modal xem đầy đủ.
- Brand Brain dùng giọng thân thiện, lịch sự, tự nhiên; xưng hô anh/chị/quý khách và cấm bịa dữ liệu.
- FAQ bao phủ đặt bàn, sinh nhật, đoàn đông, dị ứng, gợi ý món và hủy bàn.

## 13. Inventory management

- Khôi phục icon lịch sử/sửa/xóa và icon thống kê.
- Tách cảnh báo hết hạn, sắp hết hạn, tồn thấp; admin thấy cảnh báo mức cao.
- AI nhập kho nhận ngữ cảnh tồn kho, lịch sử bán, công thức, tốc độ tiêu thụ và hạn sử dụng; output gồm phân tích/lý do/đề xuất và không tự bịa dữ liệu thiếu.

## 14. Customer CRM

- Lịch sử hóa đơn tải bằng query chi tiết trong transaction, gồm bàn/khu vực/order details/product.
- Frontend null-safe với legacy data thiếu product/detail/price.
- Modal hiển thị lịch sử đơn, tổng chi tiêu, số lần sử dụng và điểm tích lũy.

## 15. Post management

- Action Edit/Delete nằm ngang với flex và gap thống nhất; empty icon không còn là ô trắng.

## 16. API, migration và nhóm file thay đổi

API thay đổi đều mang tính cộng thêm, không xóa contract cũ:

- Table-area DTO thêm `roomFee`, `minimumSpend`; `basePrice` tương thích nhưng luôn 0.
- Dashboard order stats thêm số đơn quá hạn.
- Ingredient stats thêm `expiredBatchesCount` và phân loại sắp hết hạn.
- Payment QR response thêm tên ngân hàng, chủ tài khoản và số tài khoản từ config.
- CRM history giữ endpoint cũ, sửa cách fetch/mapping chi tiết.

Migration:

- `V070__reservation_order_note_and_ai_brand_brain.sql`.
- `V071__separate_area_pricing.sql`.
- Flyway validate thành công 72 migration; blank/preflight SQL Server test đi qua V071.

Nhóm file chính:

- Frontend theme/icon/toast: `src/assets/*`, `src/components/UiIcon.vue`, `AdminNavIcon.vue`, `Toast*.vue`, `main.js`.
- Frontend nghiệp vụ/admin: `Reservation.vue`, `AdminTable.vue`, `AdminTableArea.vue`, `AdminOrder.vue`, `AdminAnalytics.vue`, `AdminProduct.vue`, `AdminCategory.vue`, `AdminAiKnowledge.vue`, `AdminIngredient.vue`, `AdminStaff.vue`, `AdminPost.vue` và các màn hình role liên quan.
- Backend: `TableArea*`, `AreaPricing*`, `ReservationService`, `PaymentService`, `OrderRepository`, `AdminOrderController`, `OverdueOrderService`, `IngredientController`, `ChatbotController`, `AdminAccountController`.
- Regression/docs: `finalFixContracts.test.js`, `TableAreaPricingContractTest`, `OverdueOrderServiceTest`, migration/auth/concurrency tests, `KNOWN_FIXED_BUGS.md`, `REGRESSION_TESTS.md`.
- Static production assets được Vite rebuild vào `quanlynhahang/src/main/resources/static`.

## 17. Kết quả test và rủi ro còn lại

Kết quả cuối:

- Frontend `npm run lint`: PASS.
- Frontend `npm test`: 22 files, 74/74 tests PASS.
- Frontend `npm run build`: PASS.
- Backend `mvnw clean test`: 427/427 tests PASS, 0 failure, 0 error, 0 skipped.
- Backend package sau full test: PASS; JAR tạo tại `quanlynhahang/target/quanlynhahang-0.0.1-SNAPSHOT.jar`.
- Focused reservation concurrency: 4/4 PASS.
- Flyway: 72 migrations validated; không dùng `repair`, không sửa schema history.

Rủi ro còn lại không chặn code release:

- Cần cấu hình và acceptance test credential thật cho payment provider/webhook, CAPTCHA, SMTP, AI, storage, TLS/reverse proxy và production database.
- `minimumSpend` hiện là ngưỡng cam kết/metadata, chưa tự động biến thành phí; nếu nhà hàng muốn cưỡng chế mức chi tối thiểu khi checkout cần chốt thêm policy cụ thể.
- Đơn quá hạn chưa thanh toán cố ý không auto-complete; admin phải xử lý cảnh báo để bảo toàn trạng thái tài chính.
- Một số API Spring MVC cũ phát cảnh báo deprecation nhưng không gây lỗi compile/test/package.
- File DOCX do người dùng xóa từ trước vẫn được giữ nguyên trạng thái, không khôi phục hay ghi đè.

`READY FOR RELEASE`

## Bổ sung sau release check — Inventory consistency

- Action nguyên liệu đã chuyển thành ba icon button đồng kích thước, dùng `UiIcon`, đúng theme và có tooltip/ARIA cho lịch sử, chỉnh sửa, xóa.
- Thêm `InventoryAlertService` làm nguồn duy nhất cho Dashboard kho, AI Forecast và Đề xuất mua hàng.
- Phân tích dùng lô nguyên liệu, ngày nhập/hết hạn, tồn còn dùng được, đơn hoàn tất 7 ngày và công thức định lượng.
- Lô hết hạn không được tính vào tồn dùng được và không được đề xuất sử dụng; hành động là cách ly/loại bỏ rồi ưu tiên các lô còn hạn.
- Lô sắp hết hạn tạm không đề xuất nhập thêm; nguyên liệu thiếu an toàn mới tính lượng mua và chi phí dự kiến.
- Regression mô phỏng đúng 24 lô hết hạn: Dashboard nhận 24, AI prompt bắt buộc nhận 24, danh sách đề xuất có mục cần xử lý và không thể trả “kho an toàn”.
