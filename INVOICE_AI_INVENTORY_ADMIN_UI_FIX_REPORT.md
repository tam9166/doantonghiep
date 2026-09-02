# Báo cáo fix Invoice, AI Inventory và Admin UI

## Phạm vi và trạng thái

Đợt sửa này chỉ xử lý các yêu cầu trong prompt Invoice/AI/Inventory/Admin UI. Không reset database, không nới quyền bằng `permitAll`, không hardcode tồn kho/kết quả AI và không triển khai khả năng xem mật khẩu hiện tại. File Word `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx` là thay đổi do người dùng sở hữu và không được chỉnh sửa.

## 1. Root cause hóa đơn bị in thành khoảng 28 trang

Luồng cũ gọi `window.print()` trên chính document của Admin. CSS print sử dụng cơ chế ẩn/hiện trong document vẫn giữ lại cây DOM, layout, phần tử có `min-height`, `overflow`, fixed-position và vùng modal/list phía sau. Vì vậy print engine phân trang cả Admin layout và các vùng ẩn thay vì chỉ lấy hóa đơn đang xem.

Đây là lỗi print scope, không phải do riêng số lượng sáu món.

## 2. Cách giới hạn print scope

`printElement.js` nay tạo một iframe tách biệt, chỉ clone node hóa đơn được chọn vào `#print-root`, chèn stylesheet A4 riêng rồi gọi print trên document của iframe. Iframe được dọn sau khi in và có `aria-hidden="true"`.

Kết quả kiểm tra DOM print:

- đúng một `#print-root`;
- sáu dòng món;
- không có Admin sidebar, overlay, danh sách đơn hoặc toast;
- nút xuất/đóng không xuất hiện trong nội dung render;
- PDF lưu từ browser là một trang A4.

## 3. Invoice CSS đã sửa

- Thiết lập `@page` A4 và margin gọn.
- Giảm spacing/font phù hợp hóa đơn sáu món.
- Chống ngắt dòng không cần thiết ở hàng và phần tổng tiền.
- Bật `print-color-adjust: exact` và `-webkit-print-color-adjust: exact`.
- Header bảng nền nâu dùng chữ trắng với độ ưu tiên đủ cao để không bị `AdminLayout :deep(th)` ghi đè.
- Preview modal và bản print dùng cùng node dữ liệu, nên thông tin nhà hàng, khách hàng, mã, bàn/vị trí, ngày lập, món, đơn giá, số lượng, thành tiền, VAT và tổng cộng không bị tạo từ hai nguồn khác nhau.

PDF QA được render lại thành PNG và kiểm tra trực quan: một trang, đủ sáu món và tổng tiền; không có sidebar/nút/overlay.

## 4. Root cause duyệt gợi ý AI nhập kho bị lỗi

Payload UI cũ không bảo đảm đầy đủ dữ liệu nhập kho bắt buộc. Sau khi bổ sung form đúng nghiệp vụ, kiểm thử API thật phát hiện root cause backend/database tiếp theo:

- Hibernate ghi vào bảng canonical `import_invoice_details`.
- Schema lịch sử V001 từng dùng tên legacy `ImportInvoiceDetails`.
- Database local đã nâng cấp thực tế không có bảng detail tương thích.
- Vì vậy việc lưu `ImportInvoiceDetail` trả lỗi SQL Server `Invalid object name 'import_invoice_details'` và frontend chỉ nhận 500 chung.

Fix gồm:

- map entity về tên canonical `import_invoice_details`;
- migration V098 đổi tên bảng legacy nếu tồn tại, giữ nguyên dữ liệu; nếu database nâng cấp bị thiếu bảng thì tạo bảng canonical với PK, FK, check constraint và index cần thiết;
- test migration từ database sạch và integration test ghi invoice/detail/batch thật;
- request có `sourceRequestId` duy nhất để retry không tạo trùng invoice/lot.

API thật đã được kiểm tra với Bánh phở, 10 kg, 25.000đ/đơn vị, HSD 20/09/2026 và nhà cung cấp. Hai lần gửi cùng `sourceRequestId` trả cùng invoice; database có đúng một invoice, một detail và một batch cho request đó. Stock và purchase suggestion được refresh từ dữ liệu thật.

## 5. Root cause “Duyệt tất cả”

Luồng cũ lặp từng suggestion ở frontend, gửi nhiều request độc lập, không có batch contract và phát toast cho từng lỗi. Đồng thời suggestion AI không chứa sẵn supplier, unit price và expiry date hợp lệ để tạo lot chính thức; tự điền giả các trường này là sai nghiệp vụ.

UI nay mở batch review để Admin nhập đủ dữ liệu cho từng item. Không tự sinh giá, HSD hoặc nhà cung cấp.

## 6. Batch và partial failure

Backend có endpoint batch nhận danh sách DTO được validation. Mỗi item dùng service nhập kho hiện hữu với transaction riêng. Kết quả trả về gồm item thành công và item thất bại cùng lý do; một item lỗi không rollback các item hợp lệ khác.

Frontend:

- disable action trong lúc request chạy;
- chỉ hiển thị một thông báo tổng hợp;
- hiển thị chi tiết item lỗi trong batch review;
- refresh dữ liệu sau xử lý;
- không phát 24 toast;
- không tạo lot nếu dữ liệu bắt buộc chưa đủ.

## 7. Root cause AI Phân Tích Sâu khó đọc

Luồng cũ render một chuỗi dài trong vùng màu tối/đỏ, không có cấu trúc và làm chiều cao trang tăng theo toàn bộ kết quả. Ngoài ra frontend từng gửi JSON 24 nguyên liệu vào `AiRequest.message`, có thể vượt validation 4.000 ký tự và nhận 400.

Fix:

- modal nền sáng, overlay tách lớp, width/max-height theo viewport và body scroll riêng;
- dữ liệu canonical từ Inventory Alert được chia thành card nguyên liệu;
- badge cảnh báo (hết hàng, sắp hết, sắp hết hạn, bình thường);
- tồn hiện tại, tiêu thụ/ngày, số ngày dùng, lý do và hành động được tách trường;
- frontend chỉ gửi chỉ dẫn ngắn; backend tự gắn inventory context từ nguồn dữ liệu chuẩn.

Browser ở 1366x768 xác nhận modal 920x675, nằm trong viewport, scroll được, chữ tối trên nền trắng và hiển thị 24 card.

## 8. Root cause Customer AI

Frontend trước đây gọi AI ngay cả khi lịch sử khách không đủ, đồng thời lỗi/timeout provider bị dồn về toast 500 chung. Mapping lỗi khiến modal không có nội dung thay thế hữu ích.

Fix:

- backend kiểm tra dữ liệu khách trước khi gọi provider;
- không đủ dữ liệu trả thông báo “Chưa đủ dữ liệu để phân tích khách hàng này.”;
- provider lỗi/timeout trả fallback “Không thể tạo phân tích AI lúc này. Dữ liệu khách hàng vẫn có thể xem bình thường.”;
- frontend hiển thị kết quả/fallback trong panel riêng, không làm crash modal;
- action có loading/disabled guard.

Không tạo lịch sử giả hoặc kết luận ngoài dữ liệu thật.

## 9. Routes làm mất AdminLayout

- `AdminKitchenProposals.vue` trước đây là page standalone, không được bọc trong `AdminLayout`.
- Route SPA trực tiếp `/admin/kitchen-proposals` cũng thiếu trong `SpaRouteRegistry`, nên refresh URL bị backend trả JSON 500 thay vì `index.html`.
- Profile dùng chung render standalone, không chọn layout theo role.

Fix dùng wrapper/layout có sẵn, không copy sidebar thủ công. Admin/Manager profile dùng Admin layout; các role khác giữ flow hiện tại. Route kitchen proposal được thêm vào SPA forward registry và có regression test.

Browser xác nhận `/admin/kitchen-proposals` và `/staff/profile` có sidebar/topbar khi mở trực tiếp ở cả ba độ phân giải, không có horizontal overflow.

## 10. Modal/UI đã sửa

- Reservation: nhóm xác nhận cuộc gọi có border, padding, spacing, semantic state, hover/focus/disabled và request guard.
- Khu vực bàn: bỏ Gallery khỏi form Admin nhưng giữ nguyên giá trị gallery hiện có trong payload để không phá API/customer dependency; không có migration destructive.
- Phiếu nhập kho: style lại Thêm dòng, Hủy, X; X có hit-area rõ; validation và loading guard.
- Lịch sử khách hàng: chỉ còn một cách đóng; modal có card, radius, shadow, spacing và body scroll.
- Nút AI khách hàng: button rõ ràng và có loading state.
- Modal nhân viên: overlay/modal tách lớp, nền trắng, header/body/footer, responsive và scroll.
- Nhật ký thao tác: tăng khoảng cách header/cards và giảm độ đậm số Tổng thao tác để đồng bộ bốn card.
- AI Inventory: modal/card/badge và batch review như mô tả trên.

## 11. Password reset implementation

Không có chức năng xem/giải mã mật khẩu hiện tại. Admin có endpoint reset riêng cho staff/customer. Backend:

- sinh mật khẩu tạm bằng `SecureRandom`;
- hash bằng BCrypt trước khi lưu;
- đặt `mustChangePassword = true`;
- tăng token version/thu hồi phiên cũ;
- trả mật khẩu tạm đúng một lần trong response reset;
- frontend hiển thị modal một lần và có nút sao chép;
- loading guard chống double click.

## 12. Security checks

- Endpoint reset yêu cầu quyền Admin; không thêm `permitAll`.
- DTO staff/customer không trả password hash.
- Audit event `ADMIN_RESET_PASSWORD` ghi target, role, performedBy và timestamp, không ghi plaintext/hash.
- Request inventory/batch vẫn qua authorization backend.
- Error UX phân biệt validation/403/404/409/500 ở các flow sửa trong đợt này.
- Không reset database. Việc kiểm tra API thật đã tạo một lô Bánh phở có request ID `codex-ai-approval-20260902-banh-pho` trong database local; dữ liệu này được giữ lại để phản ánh đúng manual verification.

## 13. File đã sửa

### Frontend source

- `Frontend/nha-hang-frontend/src/utils/printElement.js`
- `Frontend/nha-hang-frontend/src/views/AdminOrder.vue`
- `Frontend/nha-hang-frontend/src/views/AdminReservation.vue`
- `Frontend/nha-hang-frontend/src/views/AdminTableArea.vue`
- `Frontend/nha-hang-frontend/src/views/AdminIngredient.vue`
- `Frontend/nha-hang-frontend/src/views/AdminPurchaseSuggestion.vue`
- `Frontend/nha-hang-frontend/src/views/AdminStaff.vue`
- `Frontend/nha-hang-frontend/src/views/AdminKitchenProposals.vue`
- `Frontend/nha-hang-frontend/src/views/AdminActivityLog.vue`
- `Frontend/nha-hang-frontend/src/views/Staff.vue`
- `Frontend/nha-hang-frontend/src/views/invoiceInventoryAdminFixes.test.js`

### Backend source và migration

- `SpaRouteRegistry.java`
- `AdminAccountController.java`
- `ChatbotController.java`
- `PurchaseSuggestionController.java`
- `ImportInvoiceRequest.java`
- `AdminPasswordResetRequest.java`
- `PurchaseSuggestionApprovalRequest.java`
- `PurchaseSuggestionBatchRequest.java`
- `ImportInvoice.java`
- `ImportInvoiceDetail.java`
- `ImportInvoiceRepository.java`
- `InventoryImportService.java`
- `StaffAccountService.java`
- `V097__add_inventory_import_idempotency_key.sql`
- `V098__align_import_invoice_detail_table.sql`

### Tests

- `SpaRouteRegistryTest.java`
- `OperationalOrderQueryScopeTest.java`
- `PurchaseSuggestionControllerTest.java`
- `BlankDatabaseMigrationIntegrationTest.java`
- `InventoryImportIntegrationTest.java`
- `StaffAccountServiceTest.java`

### Production frontend bundle

`npm run build` cập nhật `quanlynhahang/src/main/resources/static/index.html`, xóa các chunk hash cũ và tạo các chunk hash mới. Đây là output deployment của project hiện tại, không phải `node_modules`, log hoặc file trong `target`.

## 14. Tests đã chạy

- Focused backend: 17 test, 0 failure, 0 error.
- Database sạch: Flyway V001 đến V098 PASS; bảng `import_invoice_details` canonical tồn tại.
- Full backend `mvn test`: 468 test, 0 failure, 0 error, 0 skipped.
- Backend `mvn package`: PASS; JAR được tạo lại sau full test.
- Frontend lint: PASS.
- Frontend Vitest: 33 file, 125 test PASS.
- Frontend production build: PASS.
- API thật single approval/idempotency: PASS.
- PDF A4 sáu món: một trang, kiểm tra qua `pdfinfo` và render ảnh PASS.

## 15. Browser resolutions đã verify

- 1920x1080
- 1440x900
- 1366x768

Đã kiểm tra thực tế các route Admin Kitchen Proposals, Admin/Manager profile, AI Deep Analysis và invoice modal/print. Sidebar/topbar, refresh trực tiếp, viewport overflow, modal scroll và print scope đều đạt ở phạm vi đã nêu.

## 16. Remaining issues / giới hạn xác minh

- Không thực hiện reset mật khẩu thật trên tài khoản dùng chung để tránh thay đổi credential ngoài ý muốn; backend authorization/service tests đã bao phủ flow này.
- Không gửi batch thật cho toàn bộ 24 nguyên liệu vì thao tác đó sẽ tạo hàng loạt lot và thay đổi dữ liệu nghiệp vụ local. Batch controller/UI và partial-result behavior đã được automated test; single approval được kiểm tra API/database thật.
- AI provider phụ thuộc cấu hình/khả dụng dịch vụ bên ngoài. Khi provider không hoạt động, canonical inventory cards và customer fallback vẫn dùng được; không giả lập nội dung AI thành công.
- Log local còn cảnh báo scheduler reservation expiry `UnexpectedRollbackException` trên dữ liệu reservation cũ. Đây là lỗi tồn tại ngoài phạm vi prompt hiện tại và không liên quan các file vừa sửa.
- Một số ảnh món trong invoice preview/PDF có thể không tải nếu asset nguồn của record không tồn tại. Print hiện phản ánh đúng preview; đợt này không thay đổi dữ liệu ảnh sản phẩm.
- Maven còn warning API deprecated trong `WebConfig` và unchecked operation trong `KitchenProposalService`; build/test vẫn PASS và các warning này có trước phạm vi sửa.

Không commit hoặc push trong đợt báo cáo này. Cần người dùng xác nhận danh sách thay đổi trước khi tạo commit.
