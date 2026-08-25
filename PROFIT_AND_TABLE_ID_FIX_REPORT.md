# PROFIT AND TABLE ID FIX REPORT

Ngày xác minh: 2026-08-26
Phạm vi: `/kitchen`, `/cashier`, `/waiter` và API/migration trực tiếp phục vụ ba luồng này.

## 1. Root cause món âm lợi nhuận

Đã quét toàn bộ 134 món trong `RestaurantDB`. Có đúng 2 món có `sellingPrice <= recipeCost`.

Giá vốn hiển thị 81.850đ và 55.440đ là đúng theo công thức hiện tại. Root cause không phải lỗi nhân/chia 1.000: `recipes.amount_required` được lưu theo chính đơn vị cơ sở của nguyên liệu (`kg`, `lít`, ...), còn `ingredients.unit_price` là giá trên cùng đơn vị đó. Không tìm thấy cặp nguyên liệu bị lặp trong công thức của hai món.

Lỗi dữ liệu là giá bán seed thấp hơn giá vốn thực tế, đồng thời `products.cost_price` là giá legacy không còn đồng bộ (32.000đ và 22.000đ). Trước bản sửa, API đã tính giá vốn động từ recipe nhưng chưa có pricing policy dùng chung để đưa ra giá đề xuất và dữ liệu legacy vẫn gây sai lệch giữa các nơi đọc dữ liệu.

## 2. Cost calculation đã kiểm tra

Công thức canonical:

`dishCost = SUM(recipe.amount_required * ingredient.unit_price)`

- Phở bò Kobe: `0,18 kg * 420.000 + 0,25 kg * 25.000 = 81.850đ`.
- Cơm rang dưa bò: `0,12 kg * 420.000 + 0,18 kg * 28.000 = 55.440đ`.
- `MenuEconomicsService` luôn đọc lại recipe và giá nguyên liệu hiện tại, nên thay đổi `ingredient.unit_price` sẽ tính lại giá món ở lần đọc tiếp theo.
- Migration V078 đồng bộ `products.cost_price` cho toàn bộ món có recipe, không hardcode riêng hai món.
- Setting dùng chung `min_profit_margin_percent = 30.00` được lưu trong `restaurant_settings`.

## 3. Danh sách món âm và xử lý

| Món | Giá bán | Giá vốn đúng | Lợi nhuận | Margin | Giá đề xuất (30%) | Xử lý |
|---|---:|---:|---:|---:|---:|---|
| Phở bò Kobe | 65.000đ | 81.850đ | -16.850đ | -25,92% | 120.000đ | Tạm dừng bán, giữ nguyên giá |
| Cơm rang dưa bò | 45.000đ | 55.440đ | -10.440đ | -23,20% | 80.000đ | Tạm dừng bán, giữ nguyên giá |

Kết quả quét:

- Tổng món kiểm tra: 134.
- Món có cost legacy sai trong tập món âm: 2.
- Món thực sự bán dưới giá vốn: 2.
- Món tự động tăng giá: 0.
- Món được đồng bộ cost và tạm dừng: 2.
- Món âm vẫn đang bán sau migration: 0.
- Blocker còn lại: 0.

V078 ghi `AUTO_PAUSE_NEGATIVE_MARGIN` vào `activity_logs` cho từng món, gồm ID, tên, giá, cost legacy/cost mới, trạng thái margin, actor `SYSTEM` và timestamp. Cập nhật giá qua Admin ghi old/new price, cost và old/new margin; username/timestamp do `ActivityLogService` gắn tự động.

## 4. Kitchen UI và business rule

- API Admin/Kitchen trả thêm `recommendedPrice`, `targetMarginPercent`, `marginStatus`.
- Card món lỗ hiển thị viền/cảnh báo đỏ, số lỗ và margin, giá đề xuất, trạng thái tạm dừng.
- Admin/Manager có nút `Điều chỉnh giá` dẫn tới trang quản lý món; Kitchen không được cấp quyền chỉnh giá.
- Không thể chuyển món sang đang bán nếu thiếu recipe, không có suất khả dụng, hoặc `sellingPrice <= costPrice`.
- Giá đề xuất dùng `cost / (1 - targetMargin)` rồi làm tròn lên bội số 5.000đ.

## 5. Cashier table-name fix

- Card dùng `tableIdentifier(table)` và ưu tiên `table.code`, sau đó `table.name`.
- Tiêu đề chi tiết là `Thanh toán — <mã bàn>`.
- Identifier có font đậm/lớn, ellipsis an toàn và giữ nguyên trên breakpoint nhỏ.

## 6. Waiter table-name fix

- Card và modal cùng dùng helper chung với Cashier.
- Modal giữ format `Chi Tiết — <mã bàn>`.
- Filter, group và trạng thái bàn không tham gia vào việc chọn identifier; do đó mã không mất khi trạng thái/filter thay đổi.
- Internal database ID không được dùng làm tên bàn; dữ liệu thiếu code/name hiển thị `Chưa đặt mã bàn`.

## 7. File nguồn thay đổi

- Backend: `MenuEconomicsService.java`, `RestaurantSettingsService.java`, `AdminProductController.java`, `AdminProductResponse.java`.
- Database: `V078__enforce_menu_profit_margin_policy.sql`.
- Frontend: `Kitchen.vue`, `CashierView.vue`, `Waiter.vue`, `tableOperations.js`.
- Regression: `MenuEconomicsServiceTest.java`, `AdminProductControllerTest.java`, `BlankDatabaseMigrationIntegrationTest.java`, `tableOperations.test.js`, `tableIdentifierContracts.test.js`, `kitchenRender.test.js`.
- Production bundle trong `src/main/resources/static` đã được build lại từ frontend.

## 8. Verification

- Focused backend: 6 tests, 0 failure; migration V001-V078 chạy thành công trên SQL Server trắng.
- Focused frontend: 9 tests, 0 failure.
- `npm run lint`: PASS.
- `npm test`: 31 test files / 101 tests, 0 failure.
- `npm run build`: PASS.
- `./mvnw clean test`: 456 tests, 0 failure.
- `./mvnw clean package`: PASS; 456 tests, 0 failure; tạo thành công `quanlynhahang-0.0.1-SNAPSHOT.jar`.
- `RestaurantDB`: Flyway V078 thành công; 134 món; 2 món âm đã tạm dừng; 0 món âm còn active.

## 9. Remaining issues

Không còn lỗi chức năng đã biết trong phạm vi prompt. Giá bán của hai món được giữ nguyên có chủ đích; Admin/Manager cần quyết định giá kinh doanh trước khi mở bán lại.
