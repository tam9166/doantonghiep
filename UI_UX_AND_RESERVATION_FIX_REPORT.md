# UI/UX AND RESERVATION FIX REPORT

Ngày kiểm chứng: 2026-08-24

## 1. Các lỗi đã sửa

- Chuẩn hóa theme đỏ burgundy/rose/trắng, loại bỏ token và màu xanh còn sót khỏi nguồn giao diện.
- Header, footer, Admin sidebar và luồng đặt bàn dùng SVG `currentColor`; loại bỏ toàn bộ emoji trực tiếp khỏi mã Vue/JavaScript để tránh lệch baseline và màu ngoài theme.
- Thêm ảnh ngang, bo góc và responsive cho ba khu vực đặt bàn: trong nhà, phòng riêng và sân vườn.
- Luồng đặt bàn bỏ qua bước thanh toán khi `payableNow <= 0`; báo giá có món hoặc chính sách cọc vẫn đi qua thanh toán.
- Backend từ chối tạo QR có giá trị bằng 0 trước khi tạo `PaymentIntent`, vì vậy không sinh QR/giao dịch rỗng.
- Bỏ ghi chú theo từng món; dùng một `orderNote` tối đa 500 ký tự ở cấp reservation/preorder, giữ qua chuyển bước và `sessionStorage`.
- Lưới món đặt trước dùng 5 cột desktop, 3 cột tablet, 2 cột mobile và 1 cột ở màn hình rất hẹp; sidebar món đã chọn được giữ nguyên.
- QR lấy tên ngân hàng, số tài khoản và chủ tài khoản từ cấu hình backend; nội dung chuyển khoản chuẩn hóa `DATBAN {ReservationCode}`.
- Thêm sao chép số tài khoản/nội dung, trạng thái chờ/đã nhận tiền và nút kiểm tra trạng thái.
- Nâng Brand Brain theo giọng thân thiện, tự nhiên, chủ động; xưng hô anh/chị/quý khách; không bịa và chuyển nhân viên khi thiếu dữ liệu.
- Bổ sung bốn nguồn tri thức và bảy tình huống FAQ yêu cầu hỏi thêm, tư vấn theo dữ liệu thật và không tự tạo thông tin.

## 2. Các file thay đổi

Nhóm file chính:

- Giao diện và icon: `Frontend/nha-hang-frontend/src/components/UiIcon.vue`, `AdminNavIcon.vue`, `AppNavbar.vue`, `AppFooter.vue`, `AdminLayout.vue`, cùng các view cũ được loại bỏ emoji trực tiếp.
- Đặt bàn: `Frontend/nha-hang-frontend/src/views/Reservation.vue`, `src/utils/reservationPaymentFlow.js`, ba ảnh trong `src/assets/reservation-areas/`.
- Test frontend: `reservationPaymentFlow.test.js`, `publicEndpointContracts.test.js`, `themeTokens.test.js`.
- Backend reservation/payment: `PaymentProperties.java`, `PaymentQrResponse.java`, `ReservationRequest.java`, `ReservationResponse.java`, `Reservation.java`, `ReservationService.java`, `OrderCheckoutService.java`, `PaymentService.java` và các file `application-*.properties`.
- Dữ liệu: `quanlynhahang/src/main/resources/db/migration/V070__reservation_order_note_and_ai_brand_brain.sql`.
- Test backend: `PaymentPropertiesTest.java`, `PaymentServiceTest.java` và các migration integration test.
- Bundle production trong `quanlynhahang/src/main/resources/static/` được dựng lại từ frontend mới nhất.

## 3. Logic reservation thay đổi

Sau bước yêu cầu, frontend tải quote và quyết định bước kế tiếp bằng `payableNow`. Giá trị không dương đi thẳng đến xác nhận; giá trị dương đi đến thanh toán. Nút quay lại cũng dùng cùng quy tắc để không đưa khách vào màn hình thanh toán không cần thiết.

Payload preorder chỉ còn `menuId`, `quantity` cho từng item và một `orderNote` chung. Migration V070 thêm cột `reservations.order_note`; khi chuyển preorder sang order, ghi chú này trở thành ghi chú bếp cấp đơn và ghi chú item được để trống.

## 4. Payment flow thay đổi

- Backend cấu hình `payment.bank.name`, `payment.bank.account`, `payment.bank.owner`; frontend chỉ hiển thị dữ liệu API, không chứa số tài khoản cứng.
- Nội dung chuyển khoản là `DATBAN {ReservationCode}` và giữ dấu gạch nối hợp lệ trong mã.
- `PaymentService.createQr` chặn số tiền `<= 0` trước thao tác tạo intent.
- UI hỗ trợ sao chép, làm mới trạng thái và phân biệt đang chờ/đã nhận thanh toán.

## 5. AI Knowledge Base bổ sung

V070 cập nhật Brand Brain và thêm tri thức về giới thiệu nhà hàng, thực đơn/an toàn dị ứng, chính sách đặt bàn-thanh toán-hủy-hoàn tiền, khẩu vị/sức khỏe. Bảy FAQ bao phủ nhóm 5 người, sinh nhật, bàn yên tĩnh, dị ứng hải sản, hủy bàn, đoàn 10 người và món cho trẻ em.

## 6. Test đã chạy

- Frontend `npm run lint`: đạt.
- Frontend `npm test -- --run`: 21 file, 68 test đạt.
- Frontend `npm run build`: đạt; bundle production được cập nhật.
- Backend `mvnw clean test`: 421 test, 0 failure, 0 error, 0 skipped; đạt.
- Backend `mvnw clean package`: 421 test, 0 failure, 0 error, 0 skipped; đạt và tạo JAR Spring Boot.
- Migration tập trung: blank database và legacy/preflight đi qua V070; đạt.
- Runtime smoke trên JAR đóng gói: đăng nhập đủ 6 vai trò; 54 lượt của 18 màn hình nghiệp vụ trên desktop/tablet/mobile không có lỗi JavaScript, thông báo lỗi hoặc tràn ngang. Trang `/reservation` được kiểm tra riêng ở 1366x768, 768x1024 và 390x844, không có lỗi JavaScript, request thất bại hoặc tràn ngang.

## 7. Rủi ro còn lại

- Xác nhận thanh toán thật vẫn phụ thuộc webhook/đối soát của nhà cung cấp ngân hàng; test hiện xác minh contract và trạng thái nội bộ, không thực hiện giao dịch tiền thật.
- Ba ảnh khu vực có kích thước khoảng 2.0-2.6 MB mỗi ảnh; nên tạo WebP/AVIF ở vòng tối ưu hiệu năng tiếp theo nếu môi trường mạng chậm.
- Màn hình sản phẩm ghi nhận `net::ERR_BLOCKED_BY_ORB` cho một ảnh nguồn ngoài ở cả ba viewport; API nghiệp vụ và các màn hình còn lại không có request thất bại. Nên chuyển ảnh ngoài này về media do backend phục vụ để loại bỏ phụ thuộc CORS/ORB.
- Các thay đổi đang ở working tree và chưa được commit/push trong nhiệm vụ này.
