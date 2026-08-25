# Restaurant Table, Language and UI Update Report

Ngày xác minh: 2026-08-26

## 1. Lỗi và nguyên nhân gốc

| Hạng mục | Lỗi tìm thấy | Nguyên nhân gốc |
|---|---|---|
| Mã bàn Waiter/Cashier | Card và chi tiết không bảo đảm cùng mã nghiệp vụ nổi bật. | Hai màn hình tự chọn trường hiển thị; có nguy cơ dùng tên/ID khác nhau. |
| Customer English | Chuyển EN vẫn còn nhiều chuỗi tiếng Việt ở Home, Menu, Booking, Checkout và Account. | Locale bị chia giữa JSON/JavaScript và nhiều component còn hardcode; dữ liệu song ngữ từ API chưa được chọn theo locale. |
| Nút sơ đồ bàn Admin | Trạng thái active có thể tạo chữ/icon đỏ trên nền đỏ. | Inline style ghi đè màu chữ trong khi class active đổi nền. |
| Sức chứa và layout | Thiếu đơn vị `người`; danh sách bàn chưa thống nhất tầng → khu vực → bàn. | Mỗi màn hình format và gom nhóm độc lập. |
| Năng lực khu vực | 20 bàn gốc không đủ các mức 100/50/70 người. | Chưa có migration mở rộng/idempotent và chuẩn hóa dữ liệu bàn. |

## 2. Thay đổi đã thực hiện

- Dùng chung `tableIdentifier`: ưu tiên `code`, sau đó `name`, không dùng database ID làm mã hiển thị.
- Dùng chung `groupTablesByFloorAndArea` cho Admin, Waiter và Cashier; customer/AI/báo cáo tiếp tục đọc cùng API/database bàn.
- Chuẩn hóa nhãn sức chứa thành `người` và nhãn form thành `(người)`.
- Loại bỏ CSS inline gây xung đột; active/hover/pressed dùng nền theme chính và chữ/icon `--color-on-primary`.
- Chuyển toàn bộ dictionary Menu/Reservation vào `locales/vi.json` và `locales/en.json`; xóa hai dictionary JavaScript cũ.
- Locale EN được lưu trong `localStorage`, giữ nguyên khi refresh/chuyển trang.
- Các màn hình customer chọn tên/mô tả song ngữ từ API; khi EN thiếu nội dung động thì hiển thị thông báo tiếng Anh trung lập thay vì rơi về tiếng Việt.
- Thêm V079 để chuẩn hóa 20 bàn cũ, bổ sung 20 bàn mới và giữ nguyên khóa chính/liên kết đơn hàng của bàn cũ.

## 3. File nguồn chính đã sửa

- Frontend locale: `src/i18n.js`, `src/locales/vi.json`, `src/locales/en.json`, `src/locales/locales.test.js`.
- Customer: `Home.vue`, `ProductMenu.vue`, `DineInOrder.vue`, `Reservation.vue`, `Login.vue`, `Register.vue`, `CustomerProfile.vue`, `OrderHistory.vue`.
- Table UI: `AdminTable.vue`, `AdminTableArea.vue`, `Waiter.vue`, `CashierView.vue`, `utils/tableOperations.js`.
- Database: `V079__expand_restaurant_table_capacity.sql`.
- Regression: `restaurantTableLanguageUiContracts.test.js`, `tableIdentifierContracts.test.js`, các customer flow contract tests và `BlankDatabaseMigrationIntegrationTest.java`.

## 4. Translation đã bổ sung

- Home: hero, giới thiệu, dịch vụ, CTA, giờ/trạng thái mở cửa, tin tức, tuyển dụng, chat và vòng quay.
- Menu/Checkout: danh mục, món, mô tả, tìm kiếm, tồn món, giỏ hàng, gợi ý, thanh toán và thông báo lỗi/thành công.
- Booking: toàn bộ 9 bước, placeholder, validation, khu vực/bàn, món đặt trước, yêu cầu, thanh toán, QR, xác nhận và đánh giá.
- Account: đăng nhập, đăng ký, hồ sơ, lịch sử, hóa đơn, voucher, điểm/tier khách hàng.

`locales.test.js` xác nhận hai file JSON parse được và có key đệ quy tương ứng.

## 5. Dữ liệu bàn và capacity

| Khu vực | Bàn | Sức chứa |
|---|---:|---:|
| Khu trong nhà / Indoor Dining | 20 | 100 người |
| Phòng riêng / VIP | 7 | 50 người |
| Sân vườn / Ngoài trời | 13 | 70 người |
| **Tổng** | **40** | **220 người** |

- Bổ sung: 20 bàn.
- Mã nghiệp vụ sau đồng bộ: B01–B37, VIP01–VIP03.
- Có bàn 2, 4, 6 và 8 người; chức năng ghép bàn hiện hữu được giữ nguyên.
- Đối chiếu trực tiếp `RestaurantDB`: Flyway V079 thành công, 40 bàn, không có mã ngoài quy ước trên, tổng từng khu vực khớp cấu hình.

## 6. Kiểm thử và kết quả

| Kiểm thử | Kết quả xác minh |
|---|---|
| `npm test -- --run` | PASS — 32 file, 105/105 test |
| `npm run lint` | PASS |
| `npm run build` | PASS — production bundle đã ghi vào backend static |
| `mvnw.cmd test` | PASS — 456/456 test, BUILD SUCCESS |
| Migration database trắng | PASS — áp dụng đủ đến V079, 40 bàn và capacity 100/50/70 |
| Public table/area contracts | PASS — 2/2 test |
| SQL Server live aggregate | PASS — 40 bàn, 220 người, đúng 20/7/13 bàn và 100/50/70 người |
| JAR runtime route smoke | PASS — 11/11 route Home/Menu/Booking/Account/Waiter/Cashier/Admin trả SPA HTTP 200 |
| JAR runtime public APIs | PASS — `/api/tables` trả 40 bàn/220 chỗ, `/api/areas` trả capacity 100/70/50 |

Không còn lỗi kiểm thử hoặc lỗi build tại thời điểm lập báo cáo. Việc kiểm tra trực quan thủ công trên nhiều trình duyệt/thiết bị không được ghi nhận như một bài test tự động trong báo cáo này.
