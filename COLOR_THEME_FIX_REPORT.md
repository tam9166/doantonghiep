# Báo cáo đồng bộ màu Admin Panel

Ngày kiểm tra: 24/08/2026

## 1. Nguyên nhân

Nguồn gây lệch màu chính là token dùng chung `--color-secondary` đang mang màu navy `#485f84`. Token này được tham chiếu 356 lần trong frontend nên tab, card, button, badge, toast, hiệu ứng hover và icon có thể đồng loạt hiện màu xanh dù từng component không chứa class Tailwind `blue`.

Ngoài token chung còn có một số literal xanh/xám xanh trong biểu đồ, cảnh báo đặt bàn, nút hủy liên kết, dialog, trang đăng nhập nhân viên và bóng của hạng thành viên. Sidebar dùng emoji nhiều màu nên không thể nhận màu từ CSS theo trạng thái active/inactive.

## 2. Hệ thống màu sau khi chuẩn hóa

| Vai trò | Giá trị |
| --- | --- |
| Primary | `#b7102a` |
| Primary hover | `#92001c` |
| Secondary rose | `#8f3044` |
| Primary/secondary light | `#ffdad8`, `#ffe0e5` |
| Background | `#fff8f7`, `#ffffff` |
| Border | `#e4bebc`, `#f4d4d2` |
| Text | `#271717` |
| Muted text | `#765b5a` |
| Success | `#197a45` |
| Warning | `#9a6500` |
| Error | `#ba1a1a` |

`--color-secondary` và toàn bộ secondary container/fixed/on-color đã được chuyển từ navy sang rose. Info dùng secondary rose; success vẫn giữ xanh lá, warning giữ vàng và error giữ đỏ để không làm mất ý nghĩa trạng thái.

## 3. File nguồn đã sửa

- Color system: `src/assets/theme-tokens.css`, `src/assets/global.css`.
- Chống hồi quy: `src/assets/themeTokens.test.js`.
- Sidebar/icon/focus: `src/components/AdminLayout.vue`, `src/components/AdminNavIcon.vue`.
- Dialog/toast/badge: `src/components/AppDialog.vue`, `src/components/ToastGlobal.vue`, `src/components/ToastNotification.vue`.
- Màn hình: `AdminAiKnowledge.vue`, `AdminAnalytics.vue`, `AdminOrder.vue`, `AdminReservation.vue`, `AdminTable.vue`, `CustomerProfile.vue`, `Reservation.vue`, `StaffLogin.vue`.
- Browser smoke: `scripts/cdp-role-responsive-smoke.mjs`.
- Bundle production trong `quanlynhahang/src/main/resources/static` đã được build lại bằng Vite.

## 4. Kết quả quét màu trước và sau

Phạm vi đếm là source giao diện chạy thực tế trong `Frontend/nha-hang-frontend/src`; file regression test được loại khỏi số liệu để từ khóa dùng mô tả test không làm sai kết quả.

| Chỉ số | Trước | Sau |
| --- | ---: | ---: |
| Từ khóa/class liên quan `blue` | 2 | 0 |
| Literal có hue xanh, gồm hex và rgb/rgba | 26 | 0 |
| Các mã yêu cầu `#2563eb`, `#3b82f6`, `#1d4ed8`, `#60a5fa` | 0 | 0 |
| `fill`/`stroke` SVG chứa literal xanh | 0 | 0 |

Regression mới tự động chặn class/từ khóa blue, literal có hue xanh và xác nhận Admin sidebar dùng SVG `stroke="currentColor"`.

## 5. Sidebar, active, hover và focus

- Emoji ở menu được thay bằng một component SVG chung, dùng `currentColor`.
- Icon inactive dùng `--text-muted`; hover và active dùng `--primary`.
- Menu active dùng nền hồng nhạt `--color-primary-fixed` và chữ/icon đỏ chủ đạo.
- Button/link/input/select/textarea trong Admin dùng focus outline đỏ và focus shadow đỏ hồng.
- Button chính dùng primary/primary-dark; secondary dùng nền trắng hoặc hồng rất nhạt, border hồng và chữ đỏ.
- Dialog, toast info, badge và notification không còn kế thừa navy. Success vẫn xanh lá, warning vàng, error đỏ.

## 6. Màn hình đã kiểm tra

Đã chạy ứng dụng đóng gói và đăng nhập thật, sau đó kiểm tra desktop 1366×768, tablet 768×1024 và mobile 390×844 trên các màn hình:

- Dashboard/Thống kê, Tri thức AI, Sản phẩm, Đơn hàng.
- Đặt bàn, Chính sách cọc, Sơ đồ bàn, Khu vực bàn.
- Kho nguyên liệu, Món hay dùng, Đề xuất mua hàng.
- Nhân viên, Voucher, Tin tức.
- Các màn hình bổ sung: Manager dashboard, Bếp, Phục vụ, Thu ngân.

Tổng cộng 54 lượt màn hình/viewport: 0 lỗi JavaScript, 0 overflow ngang và đúng route ở mọi lượt. Ba request ảnh sản phẩm bên ngoài bị Edge chặn ORB; fallback ảnh vẫn hoạt động và lỗi này không liên quan đến theme.

Ảnh smoke được lưu tại `release/color-theme-smoke`. Kiểm tra trực quan các trang Thống kê, Tri thức AI và Sơ đồ bàn xác nhận sidebar, router active, button, form, card và icon đều dùng hệ đỏ hồng trắng. Rule hover/focus và popup/modal được kiểm tra bằng source regression và CSS token chung.

## 7. Xác minh kỹ thuật

- `npm run lint`: PASS.
- `npm test`: 63/63 PASS trên 20 file.
- `npm run build`: PASS.
- Packaged runtime/browser smoke: 54/54 lượt route/viewport hoàn tất.

**Admin Panel đã đồng bộ toàn bộ màu theo nhận diện đỏ hồng trắng.**
