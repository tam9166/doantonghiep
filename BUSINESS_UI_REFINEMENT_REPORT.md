# BUSINESS UI REFINEMENT REPORT

## 1. Executive summary

Đợt xử lý này tập trung vào các nghiệp vụ kho, đề xuất mua hàng, nhân sự, tri thức AI và giá bán món. Các thay đổi đã được triển khai ở cả backend và frontend, kèm test regression cho các rule chính.

## 2. P0 audit scope

Các khu vực đã được rà soát:

- AI dự báo nhập kho trong trang nguyên liệu.
- Đề xuất mua hàng và luồng duyệt nhập hàng hàng loạt.
- Quản lý nhân viên, reset mật khẩu và quyền Admin.
- Sidebar Admin.
- Tri thức AI.
- Quản trị sản phẩm, giá vốn và giá bán.
- Nguồn giá nhập gần nhất từ lịch sử lô nhập kho.

## 3. Inventory forecast changes

Trang Admin Ingredient đã bổ sung hành động duyệt tất cả gợi ý nhập kho từ AI Forecast. Hệ thống chỉ nhận các gợi ý hợp lệ có `suggestedAmount > 0`.

AI Forecast không tự bịa nhà cung cấp, đơn giá nhập hoặc hạn sử dụng. Khi admin duyệt gợi ý, hệ thống mở luồng nhập kho để admin nhập dữ liệu thực tế.

## 4. Purchase suggestion changes

Modal duyệt đề xuất mua hàng hàng loạt đã được cập nhật để hỗ trợ nhập dữ liệu thực tế rõ hơn:

- Bỏ nút footer “Đóng”.
- Giữ nút xác nhận chính.
- Thêm cột đơn giá nhập trước đó.
- Hiển thị chênh lệch giá ngay dưới ô nhập đơn giá thực tế.

## 5. Previous unit price source

Đơn giá nhập trước đó được lấy từ lô nhập mới nhất của nguyên liệu qua `IngredientBatchRepository`.

Nguồn dữ liệu dùng chung:

- `InventoryAlertService`
- `IngredientBatch.unitPrice`
- `IngredientBatch.importDate`

## 6. Bulk approval validation

Luồng duyệt hàng loạt từ AI Forecast yêu cầu admin nhập đủ dữ liệu thực:

- Nhà cung cấp.
- Đơn giá nhập.
- Hạn sử dụng.
- Số lượng nhập hợp lệ.

Nếu thiếu dữ liệu bắt buộc, frontend chặn submit trước khi gọi API.

## 7. Staff reset UI

Nút reset mật khẩu nhân viên/khách hàng đã được chỉnh theo cùng phong cách action button trong hệ thống:

- Icon thống nhất.
- Màu sắc theo theme.
- Không dùng text dài gây lệch layout.

## 8. Admin role invariant

Backend đã bổ sung rule bảo vệ quyền Admin:

- Không cho tạo Admin thứ hai nếu đã có Admin.
- Không cho hạ quyền tài khoản Admin gốc.
- Không cho promote user khác lên Admin khi hệ thống đã có Admin.
- Role không thay đổi vẫn được cập nhật thông tin bình thường.

Frontend cũng khóa role của tài khoản Admin trong modal sửa nhân viên.

## 9. Sidebar navigation

Menu “Tri thức AI” đã được chuyển khỏi nhóm “TỔNG QUAN” sang nhóm “HỆ THỐNG”, đặt dưới “Nhật ký thao tác”. Không còn duplicate menu item.

## 10. AI Knowledge default newest 3

Trang Tri thức AI đã sắp xếp nguồn tri thức và FAQ theo mới nhất trước. Mặc định chỉ hiển thị 3 item mới nhất.

Người dùng có thể:

- “Xem toàn bộ”.
- “Thu gọn”.

## 11. Product cost price source

`Product.costPrice` tiếp tục là source-of-truth cho giá vốn. Không tạo field mới, không tạo migration mới.

Frontend Admin Product đã có field nhập “Giá vốn (VNĐ)”.

## 12. Minimum sale price policy

Rule giá bán tối thiểu đã được áp dụng:

- Giá vốn dưới 100.000đ: giá bán tối thiểu = giá vốn x 1.15.
- Giá vốn từ 100.000đ đến dưới 1.000.000đ: giá bán tối thiểu = giá vốn x 1.10.
- Giá vốn từ 1.000.000đ trở lên: giá bán tối thiểu = giá vốn x 1.05.

Frontend hiển thị “Giá bán tối thiểu: xxxđ” và chặn lưu nếu giá bán không đạt rule.

## 13. Backend validation

Backend `AdminProductController` đã validate giá bán tối thiểu khi tạo và cập nhật sản phẩm. Nếu không hợp lệ, API trả `400 BAD_REQUEST`, không trả lỗi 500 chung.

DTO `ProductUpsertRequest` đã nhận thêm `costPrice`.

## 14. Frontend validation

Frontend Admin Product tính giá bán tối thiểu theo cùng policy với backend. Khi giá bán dưới mức tối thiểu, form hiển thị lỗi nghiệp vụ và không gọi API lưu.

## 15. Tests added or updated

Backend tests đã cập nhật/bổ sung:

- `InventoryAlertServiceTest`
- `PurchaseSuggestionControllerTest`
- `StaffAccountServiceTest`
- `AdminProductControllerTest`
- `EntityRequestIsolationTest`

Frontend tests đã cập nhật/bổ sung:

- `invoiceInventoryAdminFixes.test.js`
- `finalFixContracts.test.js`

## 16. Verification results

Đã chạy và pass:

- Backend focused tests: 27 tests, 0 failures, 0 errors.
- Full backend test: 480 tests, 0 failures, 0 errors.
- Maven package: PASS.
- Frontend lint: PASS.
- Frontend test: 33 test files, 125 tests, PASS.
- Frontend build: PASS.

## 17. Manual browser verification status

Chưa thực hiện browser manual verification trong lượt này. Các kiểm tra hiện tại dựa trên source audit, unit/integration tests, lint và build.

## 18. Generated artifacts note

Sau khi chạy `npm run build`, backend static bundle trong `quanlynhahang/src/main/resources/static` được tạo lại với tên file hash mới. Đây là generated build output.

Cần quyết định trước khi commit:

- Commit cả generated static assets nếu backend jar cần phục vụ frontend build mới nhất.
- Hoặc loại khỏi commit nếu CI/deploy tự build frontend riêng.

Hiện tại chưa stage bất kỳ file nào.

## 19. Risk and rollback notes

Rủi ro chính:

- Nếu commit generated static assets, diff sẽ lớn vì các hashed asset cũ bị xóa và hashed asset mới được thêm.
- Nếu không commit generated static assets, backend static bundle có thể chưa phản ánh UI mới khi chạy trực tiếp Spring Boot jar.
- Rule một Admin cần đảm bảo dữ liệu seed/production đang có đúng một Admin hợp lệ.

Rollback an toàn:

- Revert commit nghiệp vụ nếu cần.
- Không cần rollback migration vì đợt này không tạo migration mới.

## 20. Commit candidate list

Nhóm source/test nên commit:

- `Frontend/nha-hang-frontend/src/components/AdminLayout.vue`
- `Frontend/nha-hang-frontend/src/views/AdminAiKnowledge.vue`
- `Frontend/nha-hang-frontend/src/views/AdminIngredient.vue`
- `Frontend/nha-hang-frontend/src/views/AdminProduct.vue`
- `Frontend/nha-hang-frontend/src/views/AdminPurchaseSuggestion.vue`
- `Frontend/nha-hang-frontend/src/views/AdminStaff.vue`
- `Frontend/nha-hang-frontend/src/views/finalFixContracts.test.js`
- `Frontend/nha-hang-frontend/src/views/invoiceInventoryAdminFixes.test.js`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/controller/AdminProductController.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/dto/ProductUpsertRequest.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/repository/IngredientBatchRepository.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/service/InventoryAlertService.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/service/StaffAccountService.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/controller/AdminProductControllerTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/controller/PurchaseSuggestionControllerTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/dto/EntityRequestIsolationTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/service/InventoryAlertServiceTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/service/StaffAccountServiceTest.java`
- `BUSINESS_UI_REFINEMENT_REPORT.md`

Nhóm cần hỏi trước khi commit:

- `quanlynhahang/src/main/resources/static/index.html`
- `quanlynhahang/src/main/resources/static/assets/*`

Không được commit:

- `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx`
