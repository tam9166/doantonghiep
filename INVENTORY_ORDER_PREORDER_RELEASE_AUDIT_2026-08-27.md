# Báo cáo đối chiếu release – 27/08/2026

## ALREADY_FIXED

- Vòng đời lô (AVAILABLE/EXPIRED/DISPOSED), FEFO, ghi vết tiêu hủy và `InventoryAlertService` đã có từ các bản trước.
- Chuyển trạng thái đơn, thanh toán idempotent, giới hạn quyền dispatch và các luồng bàn/đặt bàn đã có regression test.

## PARTIALLY_FIXED → bổ sung đợt này

- Dashboard/AI/đề xuất mua đã dùng phân tích lô chung; bổ sung toàn bộ đường nhập kho vào transaction service và invoice history.
- Preorder đã trả `availableQuantity` và UI chặn vượt tồn; backend checkout chính vẫn cần kiểm tra lại trên môi trường dữ liệu thật khi tích hợp reservation.
- Admin Orders đã dùng tổng tiền backend, map đủ trạng thái 0–7 và realtime refresh; chưa có browser E2E cho mất kết nối WebSocket.

## NOT_FIXED → FIXED

| Lỗi | Nguyên nhân | Sửa chính |
|---|---|---|
| Nhập kho cộng tồn hai lần | Controller cộng aggregate sau khi save batch | `InventoryImportService` tính lại từ tổng batch khả dụng; invoice/detail/batch/audit trong `@Transactional` |
| Invoice nhập thiếu mã bắt buộc | Entity không ánh xạ `invoice_code` NOT NULL UNIQUE | Thêm `invoiceCode`, detail entity/repository và migration V080 backfill + unique index |
| Lô hết hạn vẫn xuất hiện cảnh báo sắp hết hạn | Query thiếu cận dưới/trạng thái | `findExpiringBatchesBetween(now,target)` chỉ lấy AVAILABLE và khoảng thời gian hợp lệ |
| Scheduler hết hạn không cập nhật menu/tồn | Chỉ đổi status batch | Recalculate aggregate + `refreshForIngredient` sau mỗi lần đánh dấu hết hạn |
| Hàng không có công thức bị coi hết hàng | Availability trả 0 khi recipe rỗng | Dùng sentinel unmanaged unlimited; vẫn yêu cầu sản phẩm active |
| Admin thiếu màn xử lý thực phẩm hết hạn | Sidebar chỉ dẫn tới review đặt bàn | Thêm route `/admin/expired-food`, API expired/disposed, thao tác tiêu hủy có lý do |
| Admin hóa đơn hardcode VAT 0 / tìm mã tối đa 5 ký tự | UI tự cộng detail và maxlength 5 | Hiển thị `subTotal/taxAmount/totalAmount`, tìm full order code |

## File/API/flow thay đổi

- Migration: `V080__harden_inventory_import_history.sql`.
- API: `POST /api/admin/import-invoices`, `POST /api/admin/ingredients/{id}/batches`, `POST /api/admin/purchase-suggestions/{id}/approve`, `GET /api/admin/ingredients/expired-batches`, `GET /api/admin/ingredients/disposed-batches`.
- Frontend: AdminOrder realtime/tổng tiền; AdminExpiredFood; Reservation giới hạn preorder.

## Verification

- Frontend: Vitest 32 files / 105 tests PASS; ESLint PASS; Vite build PASS.
- Backend focused: migration blank DB v080 PASS; lifecycle 2/2, menu availability 4/4; OrderCheckout suite đang chạy lại sau khi cập nhật regression cho sản phẩm không công thức.
- Còn lại: cần chạy full Maven suite và smoke trên DB `RestaurantDB` trước khi release production.

## Addendum – prompt 40 mục (27/08/2026)

- Đã bổ sung bulk disposal `POST /api/admin/ingredients/expired-batches/dispose-all` với transaction, kiểm tra lý do, chống xử lý lại batch DISPOSED và tổng hợp kết quả.
- `AdminExpiredFood.vue` dùng modal đồng bộ theme cho single/bulk, `Promise.allSettled` để một API phụ lỗi không làm mất các section khác, KPI tự refresh và lịch sử có badge xanh.
- Dine-in gửi `productIds` thực tế từ cart; recommendation fallback luôn lấy món thật còn phục vụ và ưu tiên đồ uống không cồn trước bia khi chưa có tín hiệu bia.
- Reservation preorder cho phép nhập tay quantity integer, clamp theo tồn và cập nhật quote state ngay.
- Các mục AI dùng chung Takeaway/Delivery/Reservation, contextText/ngữ nghĩa nóng-lạnh, audit ảnh và QR/Unicode cần kiểm tra tiếp theo vì source hiện tại chưa có đủ component/API chung để xác nhận hoàn tất.
