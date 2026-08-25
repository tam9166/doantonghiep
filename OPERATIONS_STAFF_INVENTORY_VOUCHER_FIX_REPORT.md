# Báo cáo sửa lỗi vận hành, nhân viên, kho và voucher

Ngày xác minh: 25/08/2026
Phạm vi: các mục 1–21 của prompt release check mới nhất; không audit lại các chức năng ngoài phạm vi.

## 1. Root cause từng nhóm lỗi

### Kitchen và chuyển đơn xuống bếp

- Kitchen có thể trắng trang vì các API phụ được tải chung và dữ liệu `null`/request lỗi đi vào computed/render mà không có trạng thái lỗi cục bộ.
- Nút chuyển bếp của Admin gọi sai hợp đồng quyền/trạng thái ở một số loại đơn; đường legacy chưa được chuẩn hóa và thao tác lặp chưa được coi là idempotent.
- Đã tách lỗi tải dữ liệu phụ khỏi render chính, chuẩn hóa dữ liệu mặc định, giữ đúng role guard và cho phép đúng `ADMIN`, `MANAGER`, `WAITER`; `CUSTOMER` vẫn bị từ chối.

### Reservation bước yêu cầu đặc biệt

- Navigation của bước 7 gắn với nhánh thanh toán nhưng chưa chuẩn hóa các trường tùy chọn, làm payload `undefined` hoặc tiền cọc bằng 0 không đi tới bước xác nhận.
- Đã chuẩn hóa checkbox/note, giữ draft khi quay lại, giới hạn note và điều hướng theo `depositAmount`: bằng 0 tới Confirm, lớn hơn 0 tới Payment.

### Kho và vòng đời lô

- Logic cũ dựa chủ yếu vào tổng tồn nguyên liệu nên coi lượng sắp hết hạn là tồn dài hạn và vẫn có thể tính lô hết hạn vào khả dụng.
- Batch chưa có vòng đời và bản ghi tiêu hủy riêng nên thao tác xử lý có nguy cơ làm mất audit.
- Đã thống nhất tính toán theo từng batch, hạn dùng và FEFO; lô hết hạn bị khóa, lô hợp lệ của cùng nguyên liệu vẫn dùng bình thường.

### Giá vốn, công thức và số suất

- Món có thể bật bán mà không kiểm tra đầy đủ công thức hoặc quan hệ giá bán/giá vốn; số suất dùng số tồn chưa loại hết hạn/hold.
- Đã gom tính toán vào service kinh tế và khả dụng của món, validate công thức và chặn kích hoạt món thiếu công thức hoặc bán không cao hơn giá vốn.

### Waiter và Cashier

- Hai màn hình tự suy diễn trạng thái từ các trường/ID khác nhau, thiếu tên khu vực trong DTO, dẫn đến lệch bàn chờ thanh toán và không thể nhóm ổn định.
- Frontend từng gọi thêm mutation trạng thái bàn sau khi thanh toán, trong khi backend chưa hoàn tất toàn bộ lifecycle trong một transaction.
- Đã dùng order/payment/table lifecycle làm nguồn sự thật, hoàn tất payment và chuyển bàn sang chờ dọn trong backend transaction; UI chỉ làm mới dữ liệu.

### Nhân viên

- Staff UI đọc `account` trong khi API trả `employee`; `LocalTime` bị parse như `Date`, gây runtime error và trắng tab.
- Bảng lương tính ở browser bằng đơn giá hardcode, không liên kết đúng lịch/chấm công/tháng và không biểu diễn trường hợp thiếu đơn giá.
- Đã sửa hợp đồng DTO, thời gian/ca qua đêm, overlap validation và chuyển tổng hợp lương sang backend dựa trên dữ liệu thật.

### Voucher

- Mô hình cũ chỉ có cờ `isUsed`, không biểu diễn bật/tắt, thời hạn, nhiều lượt hoặc cạnh tranh lượt cuối.
- Các schema lịch sử không đồng nhất cột giá trị voucher nên migration mới không thể giả định database đã có đầy đủ cột.
- Đã thêm lifecycle chung, khóa bi quan khi redeem, migration tương thích cả database cũ và database trắng, đồng thời áp dụng cùng rule cho reservation, checkout và lucky wheel.

## 2. File thay đổi chính

- Frontend: `Kitchen.vue`, `Reservation.vue`, `AdminOrder.vue`, `AdminIngredient.vue`, `Waiter.vue`, `CashierView.vue`, `AdminStaff.vue`, `AdminVoucher.vue`, `tableOperations.js` và các test contract liên quan.
- Controller/DTO: `AdminOrderController`, `AdminProductController`, `IngredientController`, `RecipeController`, `RestaurantTableController`, `ScheduleController`, `TimekeepingController`, `PayrollController`, `VoucherController` cùng các request/response mới.
- Service: `IngredientBatchLifecycleService`, `InventoryAlertService`, `MenuAvailabilityService`, `MenuEconomicsService`, `OrderPaymentService`, `ReservationService`, `PayrollService`, `WorkScheduleConflictService`, `VoucherLifecycleService`.
- Entity/repository: `IngredientBatch`, `IngredientBatchDisposal`, `Voucher`, `Account` và các repository batch, recipe, table, schedule, voucher.
- Tài liệu regression: `docs/KNOWN_FIXED_BUGS.md`, `docs/REGRESSION_TESTS.md`.
- Static assets trong `quanlynhahang/src/main/resources/static` được tạo lại từ frontend build mới nhất.

## 3. Migration/schema changes

- `V074`: trạng thái batch và bảng audit tiêu hủy lô.
- `V075`: `accounts.shift_rate` phục vụ tính lương theo đơn giá ca.
- `V076`: `active`, `usage_limit`, `used_count`, `start_date`, `end_date` và các constraint vòng đời voucher; backfill voucher một lượt cũ.
- `V077`: bổ sung/sửa tương thích các cột giá trị voucher cho database legacy thực tế.
- Đã kiểm tra cả nâng cấp database đang dùng và migration database trắng đủ 77 versioned migrations (78 mục khi tính callback/preflight trong validation log).

## 4. Inventory expiry logic

- `NEAR_EXPIRY` vẫn được dùng trước theo FEFO nhưng không được coi là tồn an toàn dài hạn.
- Đề xuất nhập tính phần nhu cầu sau ngày lô gần hết hạn, tồn hạn dài, mức an toàn và tốc độ tiêu thụ.
- `EXPIRED` không được xuất kho, không dùng cho recipe, không tính available stock/servings và không được AI coi là khả dụng.
- Tiêu hủy không xóa batch; lưu nguyên liệu, mã lô, lượng, hạn dùng, ngày xử lý, lý do và người xác nhận.

## 5. Cost, recipe và servings

- Giá vốn được tổng hợp từ định lượng recipe và giá nguyên liệu.
- API/UI cung cấp giá vốn, giá bán, lợi nhuận, biên lợi nhuận và số suất có thể làm.
- Không bật bán món nếu chưa có recipe hoặc `sellingPrice <= costPrice`.
- Recipe bắt buộc quantity dương, unit hợp lệ và không trùng ingredient.
- Số suất là giá trị nhỏ nhất của `available ingredient / quantity per serving`, đã loại batch hết hạn và lượng hold; near-expiry còn hạn vẫn được tính và consume FEFO.

## 6. Đồng bộ Waiter–Cashier

- DTO bàn có `floor`, `areaName`, mã/tên bàn chuẩn hóa; cả hai UI dùng chung helper nhóm Tầng → Khu vực → Bàn.
- Bàn chờ thanh toán xuất hiện nhất quán theo order chưa thanh toán.
- Backend từ chối double payment; payment thành công và chuyển bàn sang `CLEANING`/chờ dọn trong cùng transaction.
- Không release bàn trước khi có bằng chứng tài chính hợp lệ.

## 7. Staff schedule, attendance và payroll

- Schedule hỗ trợ xem/tạo/sửa, kiểm tra ca trùng và ca qua đêm.
- Attendance trả nhân viên, ngày, ca, giờ vào/ra, phút muộn/về sớm, tổng giờ và trạng thái; UI có loading/error/empty state.
- Payroll tổng hợp theo khoảng tháng ở backend: số ca xếp, số ca đi làm, đơn giá/ca và tổng lương tạm tính.
- Nhân viên thiếu `shiftRate` vẫn hiện dòng dữ liệu kèm cảnh báo cấu hình, không bị loại khỏi bảng.

## 8. Voucher lifecycle

- Trạng thái: `ACTIVE`, `PAUSED`, `NOT_STARTED`, `EXPIRED`, `EXHAUSTED`.
- Hỗ trợ bật/tắt, thời gian có/không giới hạn, giới hạn lượt, tăng giới hạn và reset lượt theo action quản trị.
- Validation và redeem dùng chung service trong các luồng áp dụng voucher.
- Redeem khóa row bằng `PESSIMISTIC_WRITE`; test hai request cạnh tranh lượt cuối xác nhận chỉ một request thành công và `used_count` không vượt giới hạn.

## 9. Tests thêm/cập nhật

- Backend: lifecycle/disposal batch, inventory alert near-expiry, menu economics/availability, payment/table lifecycle, schedule conflict, payroll, voucher lifecycle và voucher concurrency.
- Migration: database trắng và schema assertions cho disposal, `shift_rate`, các cột voucher.
- Authorization/contract: quyền chuyển bếp, dữ liệu bàn, staff/voucher frontend contracts và helper nhóm bàn.
- Regression voucher reservation được cập nhật để fixture “lượt cuối” khai báo rõ `usageLimit = 1` và kỳ vọng HTTP `409` theo lifecycle mới.

## 10. Commands executed

```text
Backend focused Maven tests (theo từng module)
.\mvnw.cmd -Dtest=ReservationConcurrencyIntegrationTest#twoReservationsCompetingForTheLastVoucherUseAllowExactlyOneSuccess test
.\mvnw.cmd clean test
.\mvnw.cmd clean package

Frontend
npm run lint
npm test
npm run build

Review
git status --short
git diff --stat
git diff --check
```

## 11. Test results

- Backend focused tests: PASS, gồm inventory/menu/staff/voucher/payment và cạnh tranh voucher.
- Backend `clean test`: **PASS — 454 tests, 0 failures, 0 errors, 0 skipped**.
- Backend `clean package`: **PASS — 454 tests**, tạo `quanlynhahang/target/quanlynhahang-0.0.1-SNAPSHOT.jar`.
- Frontend lint: **PASS**.
- Frontend tests: **PASS — 30 test files, 98 tests**.
- Frontend production build: **PASS**; assets đã được đồng bộ vào backend static resources.
- Flyway: database hiện có lên version 077 và database trắng migration thành công.

## 12. Remaining blockers

- Không còn blocker chức năng hoặc test tự động trong phạm vi prompt.
- Trước khi production cần cấu hình `JWT_SECRET` thật; hiện profile dev chủ động cảnh báo đang dùng development signing key.
- Nên thực hiện smoke/UAT trên trình duyệt bằng tài khoản thật cho từng role và dữ liệu vận hành thực tế trước khi phát hành production; đây là bước triển khai/UAT, không phải test đang fail.
- File Word do người dùng xóa `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx` được giữ nguyên trạng thái, không phục hồi hoặc ghi đè.
