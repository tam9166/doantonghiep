# Checklist demo FPoly Restaurant

## Chuẩn bị trước buổi bảo vệ

- Bật SQL Server tại cổng `1433`, kiểm tra database `RestaurantDB`.
- Mở project trong IDE và chạy `QuanlynhahangApplication.java`.
- Mở `http://localhost:8080`, nhấn `Ctrl+F5` một lần để bỏ cache cũ.
- Kiểm tra nhanh Trang chủ, Thực đơn, Đặt bàn và Thống kê trước khi trình chiếu.
- Khi cần nạp lại dữ liệu thống kê: `./scripts/run-db-upgrade.ps1 -Mode AnalyticsSeed -Username Tam -Password 123456`.

## Tài khoản demo nội bộ

Các tài khoản dưới đây được tạo bởi `sql/02_seed_data.sql` và chỉ dùng cho môi trường demo local. Mật khẩu seed chung là `123`.

| Vai trò | Tài khoản | Màn hình chính |
|---|---|---|
| Khách hàng | `customer` | Trang chủ, đặt bàn, lịch sử |
| Quản trị | `admin` | Toàn bộ khu vực quản trị |
| Quản lý | `manager` | Quản lý vận hành và thống kê |
| Phục vụ | `waiter` | `/waiter` |
| Bếp | `kitchen` | `/kitchen` |
| Thu ngân | `cashier` | `/cashier` |

Không dùng mật khẩu này khi triển khai môi trường thật.

## Thứ tự trình diễn đề xuất

1. **Trang chủ:** giới thiệu nhận diện Mộc Vị, responsive và các CTA Thực đơn/Đặt bàn.
2. **Thực đơn:** lọc danh mục, ảnh dự phòng khi URL lỗi, thêm món vào giỏ và mở thanh toán QR.
3. **Đặt bàn:** đăng nhập `customer`, chọn thời gian, số khách, khu vực, bàn, món đặt trước, yêu cầu và hình thức thanh toán.
4. **Tra cứu/lịch sử:** kiểm tra mã đặt bàn và trạng thái xử lý.
5. **Quản trị đặt bàn:** đăng nhập `admin`, duyệt hoặc cập nhật trạng thái đặt bàn.
6. **Thống kê:** mở `/admin/analytics`, đổi phạm vi 7 ngày/tháng/năm và chạy Phân tích bằng AI.
7. **Vận hành:** mở nhanh màn Bếp, Phục vụ hoặc Thu ngân để minh họa phân quyền theo vai trò.

## Điểm đóng góp kỹ thuật

- **Đặt bàn:** wizard nhiều bước có kiểm tra dữ liệu, gợi ý bàn, đặt món trước, cọc và theo dõi trạng thái.
- **Thanh toán:** tạo VietQR theo số tiền/nội dung giao dịch; webhook được thiết kế có kiểm soát trùng giao dịch.
- **Phân quyền:** RBAC tách khách hàng, quản trị, quản lý, phục vụ, bếp và thu ngân.
- **Thống kê:** tổng hợp doanh thu, giá vốn, vận hành, lợi nhuận và top món; biểu đồ Chart.js responsive.
- **AI:** gửi dữ liệu tổng hợp phía server tới Gemini, không đưa API key vào mã frontend.
- **Trải nghiệm:** lazy loading ảnh, ảnh dự phòng, skeleton khi chuyển trang admin và trạng thái loading/empty/error.

## Kiểm tra kết thúc

- Không có lỗi đỏ trong Console hoặc request 4xx/5xx bất thường trong Network.
- Không có thanh cuộn ngang ở kích thước 390px.
- Ảnh món lỗi được thay bằng ảnh dự phòng.
- Chữ tiếng Việt hiển thị đúng UTF-8; nếu thấy bản cũ, dùng `Ctrl+F5` và chạy lại backend.
- Sau demo, không đưa `.env`, API key hoặc mật khẩu thật lên Git.
