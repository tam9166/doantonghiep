# Staff, Kitchen, Customer và tên bàn — báo cáo audit/fix

## Đã xử lý

- Endpoint nhân sự tiếp tục được bảo vệ bởi `ROLE_ADMIN`/`ROLE_MANAGER`; service hiện có giới hạn Manager chỉ quản lý Waiter, Kitchen và Cashier.
- Bổ sung API quản lý khách hàng: cập nhật hồ sơ cơ bản, đặt lại mật khẩu dạng hash, khóa/mở tài khoản và audit log. Không trả password hoặc password hash về frontend.
- Màn hình Admin Staff hiển thị thêm số điện thoại/trạng thái và action đặt lại mật khẩu, khóa/mở tài khoản.
- Bếp được điều hướng tới `/kitchen/inventory` thay vì route Admin; route này tự mở tab Tồn kho. Các nút thiết lập công thức/chỉnh giá chỉ còn cho Admin/Manager.
- Bổ sung workflow đề xuất Bếp: gửi JSON cho nguyên liệu/món/công thức ở trạng thái `PENDING`; Admin/Manager xem, duyệt hoặc từ chối có lý do. Duyệt nguyên liệu tạo bản ghi chính thức; duyệt món/công thức tạo bản nháp không giá bán/không active để Admin hoàn thiện trước khi bán.
- Bổ sung migration V095 cho `Accounts.phone` trên database nâng cấp cũ và V096 cho `kitchen_proposals`; cả hai đều idempotent trên database sạch.
- Tên bàn Cashier và Waiter dùng màu thương hiệu, font đậm và text-shadow để giữ tương phản ở các trạng thái.

## Permission matrix

| Chức năng | Admin | Manager | Kitchen |
|---|---:|---:|---:|
| Quản lý nhân sự | Có | Có, trừ Admin/Manager | Không |
| Xem khách hàng | Có | Có | Không |
| Reset mật khẩu khách | Có | Có | Không |
| Khóa/mở khách | Có | Có | Không |
| CRUD nguyên liệu chính thức | Có | Có | Không |
| Xem tồn kho qua workspace Bếp | Có | Có | Có |
| Thiết lập giá/công thức chính thức | Có | Có | Không |

## Các phần còn tồn tại

- Workflow đề xuất nguyên liệu/món/công thức của Bếp (PENDING → APPROVED/REJECTED) chưa có entity, migration và API riêng; chưa tự tạo dữ liệu chính thức.
- Quy trình duyệt món/công thức hiện lưu payload và tạo Product nháp; phần cấu hình giá, danh mục và chỉnh sửa recipe sau duyệt vẫn do Admin hoàn thiện trong màn quản trị hiện có.
- Cần kiểm tra trực quan bằng browser ở Admin, Waiter, Cashier và Kitchen sau khi chạy lại backend/frontend; môi trường hiện tại chưa có phiên browser tự động để xác nhận thao tác bằng mắt.

## Kiểm thử

- Backend compile: `mvnw.cmd -q -DskipTests compile` PASS.
- Focused backend (staff, kitchen, migration, reservation concurrency): PASS; clean database chạy V001–V096.
- Full backend: `mvnw.cmd test` PASS — 464 tests, 0 failures, 0 errors.
- Maven package: `mvnw.cmd package` PASS.
- Frontend: `npm run lint` PASS; `npm test -- --run` PASS (32 files, 121 tests); `npm run build` PASS.
- Chưa commit/push.
