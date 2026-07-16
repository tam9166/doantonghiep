<template>
  <AdminLayout>
  <div class="admin-wrapper luxury-theme">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Lý Tài Khoản & Nhân Sự</h1>
        <p class="page-subtitle">Quản lý tài khoản, khách hàng, chấm công và xếp lịch ca làm</p>
      </div>

      <div class="tabs" style="margin-bottom: 20px;">
        <button :class="{ active: currentTab === 'staff' }" @click="currentTab = 'staff'">Tài Khoản Nhân Viên</button>
        <button :class="{ active: currentTab === 'customer' }" @click="currentTab = 'customer'">Khách Hàng</button>
        <button :class="{ active: currentTab === 'schedule' }" @click="currentTab = 'schedule'">Xếp Lịch Làm Việc</button>
        <button :class="{ active: currentTab === 'zones' }" @click="currentTab = 'zones'; fetchZones()">📍 Phân Khu Vực Phục Vụ</button>
        <button :class="{ active: currentTab === 'timekeeping' }" @click="currentTab = 'timekeeping'">Báo Cáo Chấm Công</button>
        <button :class="{ active: currentTab === 'salary' }" @click="currentTab = 'salary'">Bảng Lương</button>
      </div>

      <div class="g-card">
      <!-- TAB: NHÂN VIÊN -->
      <div v-if="currentTab === 'staff'" class="tab-panel">
        <div class="panel-header">
          <h3>Danh Sách Nhân Viên</h3>
          <div style="display:flex; gap:10px; align-items:center;">
            <input type="text" class="g-form-control" v-model="searchStaffQuery" placeholder="Tìm tên, username, vị trí..." style="width: 250px;" />
            <button class="g-btn-primary" @click="showAddModal = true">➕ Thêm Nhân Viên</button>
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Họ Tên</th>
              <th>Email</th>
              <th>Ca Làm</th>
              <th>Khu Vực</th>
              <th>Vị Trí (Role)</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="staff in filteredStaffList" :key="staff.username">
              <td>{{ staff.username }}</td>
              <td>{{ staff.fullname }}</td>
              <td>{{ staff.email }}</td>
              <td>{{ staff.shift || 'N/A' }}</td>
              <td>{{ staff.assignedArea || 'N/A' }}</td>
              <td>
                <span class="role-badge" :class="staff.role">{{ translateRole(staff.role) }}</span>
              </td>
              <td>
                <button class="g-btn-primary" @click="openEditModal(staff)" style="margin-right: 5px;">Xem/Sửa</button>
                <button class="g-btn-danger" @click="deleteStaff(staff.username)" v-if="staff.username !== 'admin'">Xóa</button>
              </td>
            </tr>
            <tr v-if="filteredStaffList.length === 0">
              <td colspan="5" class="text-center">Không tìm thấy nhân viên nào.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: KHÁCH HÀNG -->
      <div v-if="currentTab === 'customer'" class="tab-panel">
        <div class="panel-header">
          <h3>Danh Sách Khách Hàng</h3>
          <div style="display:flex; gap:10px; align-items:center;">
            <input type="text" class="g-form-control" v-model="searchCustomerQuery" placeholder="Tìm tên, username..." style="width: 250px;" />
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Họ Tên</th>
              <th>Email</th>
              <th>Điểm Tích Lũy</th>
              <th>Hạng Thẻ</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cus in filteredCustomerList" :key="cus.username">
              <td>{{ cus.username }}</td>
              <td>{{ cus.fullname }}</td>
              <td>{{ cus.email }}</td>
              <td><span style="color: gold; font-weight: bold;">⭐ {{ cus.points }}</span></td>
              <td><span class="role-badge" :class="cus.membershipTier === 'Vàng' ? 'ROLE_ADMIN' : 'ROLE_USER'">{{ cus.membershipTier }}</span></td>
              <td>
                <button class="g-btn-primary" @click="viewCustomerOrders(cus)">Xem Lịch Sử</button>
              </td>
            </tr>
            <tr v-if="filteredCustomerList.length === 0">
              <td colspan="6" class="text-center">Không tìm thấy khách hàng nào.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: XẾP LỊCH -->
      <div v-if="currentTab === 'schedule'" class="tab-panel">
        <div class="panel-header">
          <h3>Xếp Lịch Ca Làm Việc</h3>
          <div style="display:flex; gap:15px; align-items:center;">
            <input type="text" class="g-form-control" v-model="searchScheduleQuery" placeholder="Tìm tên nhân viên..." style="width:200px;" />
            <div class="date-picker">
              <label>Từ ngày: </label>
              <input type="date" v-model="scheduleStartDate" @change="fetchSchedules" />
              <label style="margin-left:10px;">Đến ngày: </label>
              <input type="date" v-model="scheduleEndDate" @change="fetchSchedules" />
            </div>
          </div>
        </div>
        <div class="schedule-form" style="display:flex; gap:10px; align-items:center; flex-wrap:wrap; margin-bottom:15px;">
          <select class="g-form-control" v-model="newSchedule.username" style="width: 200px;">
            <option value="">-- Chọn nhân viên --</option>
            <option v-for="staff in staffList" :key="staff.username" :value="staff.username">{{ staff.fullname }} ({{ translateRole(staff.role) }})</option>
          </select>
          <input type="text" class="g-form-control" v-model="newSchedule.shift" list="shift-options" style="width: 200px;" placeholder="Vd: 08:00 - 16:00 hoặc Sáng" />
          <datalist id="shift-options">
            <option value="Sáng">Ca Sáng (06:00 - 14:00)</option>
            <option value="Chiều">Ca Chiều (14:00 - 22:00)</option>
            <option value="Tối">Ca Tối (22:00 - 06:00)</option>
          </datalist>
          <label style="display:flex; align-items:center; gap:5px; font-weight:bold; color:var(--primary);">
            <input type="checkbox" v-model="repeatForWeek" /> Áp dụng 1 tuần (7 ngày)
          </label>
          <button class="g-btn-primary" @click="addSchedule">Phân Ca</button>
        </div>
        <table class="data-table mt-20">
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Ca Làm</th>
              <th>Nhân Viên</th>
              <th>Vị Trí</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sched in filteredScheduleList" :key="sched.id">
              <td>{{ new Date(sched.workDate).toLocaleDateString('vi-VN') }}</td>
              <td>{{ sched.shift }}</td>
              <td>{{ sched.account.fullname }}</td>
              <td>{{ sched.account.username }}</td>
              <td><button class="g-btn-danger" @click="deleteSchedule(sched.id)">Hủy Ca</button></td>
            </tr>
            <tr v-if="filteredScheduleList.length === 0">
              <td colspan="5" class="text-center">Không có lịch làm việc nào phù hợp.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: PHÂN KHU VỰC PHỤC VỤ -->
      <div v-if="currentTab === 'zones'" class="tab-panel">
        <div class="panel-header">
          <h3>📍 Phân Công Khu Vực Phục Vụ Theo Tầng & Ca</h3>
          <div style="display:flex; gap:15px; align-items:center;">
            <div class="date-picker">
              <label>Ngày: </label>
              <input type="date" v-model="zoneDate" @change="fetchZones" />
            </div>
          </div>
        </div>

        <!-- Form phân công -->
        <div class="zone-form">
          <select class="g-form-control" v-model="newZone.username" style="width: 200px;">
            <option value="">-- Chọn nhân viên phục vụ --</option>
            <option v-for="staff in waiterList" :key="staff.username" :value="staff.username">{{ staff.fullname }} ({{ staff.username }})</option>
          </select>
          <select class="g-form-control" v-model="newZone.floor" style="width: 220px;">
            <option value="">-- Chọn tầng --</option>
            <option v-for="f in floorList" :key="f" :value="f">{{ f }}</option>
          </select>
          <select class="g-form-control" v-model="newZone.shift" style="width: 150px;">
            <option value="Sáng">Ca Sáng</option>
            <option value="Chiều">Ca Chiều</option>
            <option value="Tối">Ca Tối</option>
          </select>
          <label style="display:flex; align-items:center; gap:5px; font-weight:bold; color:var(--primary); white-space:nowrap;">
            <input type="checkbox" v-model="zoneRepeatWeek" /> 7 ngày
          </label>
          <button class="g-btn-primary" @click="addZoneAssignment">📌 Phân Công</button>
        </div>

        <!-- Bản đồ khu vực: Ai phục vụ tầng nào -->
        <div class="zone-map-section">
          <h4 style="color:var(--primary); margin-bottom: 15px;">🗺️ Bản Đồ Phân Công Hôm Nay
            <select v-model="zoneMapShift" @change="fetchZoneMap" style="margin-left:15px; padding:5px 10px; border-radius:6px; background:var(--bg-root); color:var(--text-heading); border:1px solid var(--border); font-weight:bold;">
              <option value="">Tất cả ca</option>
              <option value="Sáng">Ca Sáng</option>
              <option value="Chiều">Ca Chiều</option>
              <option value="Tối">Ca Tối</option>
            </select>
          </h4>
          <div class="zone-map-grid">
            <div v-for="(staffList2, floorName) in zoneMap" :key="floorName" class="zone-map-card">
              <div class="zone-map-floor">
                <span v-if="floorName.toLowerCase().includes('vip')">👑</span>
                <span v-else-if="floorName.toLowerCase().includes('thượng')">☁️</span>
                <span v-else>🏢</span>
                {{ floorName }}
              </div>
              <div class="zone-map-staff">
                <div v-for="s in staffList2" :key="s.username" class="zone-staff-chip">
                  <span class="zone-chip-avatar">👤</span>
                  <div>
                    <div class="zone-chip-name">{{ s.fullname }}</div>
                    <div class="zone-chip-shift">Ca {{ s.shift }}</div>
                  </div>
                </div>
                <div v-if="staffList2.length === 0" style="color:var(--text-muted); font-style:italic; padding:10px;">Chưa phân công</div>
              </div>
            </div>
            <div v-if="Object.keys(zoneMap).length === 0" style="grid-column: 1 / -1; text-align:center; color:var(--text-muted); padding:40px;">
              Chưa có phân công nào cho ngày này. Hãy sử dụng form bên trên để phân công.
            </div>
          </div>
        </div>

        <!-- Bảng chi tiết phân công -->
        <table class="data-table mt-20">
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Ca Làm</th>
              <th>Tầng / Khu Vực</th>
              <th>Nhân Viên</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="zone in zoneList" :key="zone.id">
              <td>{{ new Date(zone.workDate).toLocaleDateString('vi-VN') }}</td>
              <td><span class="shift-badge" :class="'shift-' + zone.shift">{{ zone.shift }}</span></td>
              <td><strong>{{ zone.floor }}</strong></td>
              <td>{{ zone.account.fullname }} ({{ zone.account.username }})</td>
              <td><button class="g-btn-danger" @click="deleteZoneAssignment(zone.id)">Xóa</button></td>
            </tr>
            <tr v-if="zoneList.length === 0">
              <td colspan="5" class="text-center">Chưa có phân công khu vực nào cho ngày này.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: CHẤM CÔNG -->
      <div v-if="currentTab === 'timekeeping'" class="tab-panel">
        <div class="panel-header">
          <h3>Báo Cáo Chấm Công</h3>
          <div style="display:flex; gap:15px; align-items:center;">
            <input type="text" class="g-form-control" v-model="searchTimekeepingQuery" placeholder="Tìm tên nhân viên..." style="width:200px;" />
            <div class="date-picker">
              <label>Từ ngày: </label>
              <input type="date" v-model="timekeepingStartDate" @change="fetchTimekeeping" />
              <label style="margin-left:10px;">Đến ngày: </label>
              <input type="date" v-model="timekeepingEndDate" @change="fetchTimekeeping" />
            </div>
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Nhân Viên</th>
              <th>Giờ Check-in</th>
              <th>Giờ Check-out</th>
              <th>Trạng Thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tk in filteredTimekeepingList" :key="tk.id">
              <td>{{ new Date(tk.workDate).toLocaleDateString('vi-VN') }}</td>
              <td>{{ tk.account.fullname }}</td>
              <td>{{ formatTime(tk.checkInTime) }}</td>
              <td>{{ formatTime(tk.checkOutTime) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(tk.status)">{{ tk.status }}</span>
              </td>
            </tr>
            <tr v-if="filteredTimekeepingList.length === 0">
              <td colspan="5" class="text-center">Không có dữ liệu chấm công nào phù hợp.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: BẢNG LƯƠNG -->
      <div v-if="currentTab === 'salary'" class="tab-panel">
        <div class="panel-header">
          <h3>Bảng Lương Nhân Sự</h3>
          <div class="date-picker">
            <label>Tháng: </label>
            <input type="month" v-model="salaryMonth" @change="fetchSalary" />
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Tài Khoản</th>
              <th>Họ Tên</th>
              <th>Chức Vụ</th>
              <th>Số Ca Được Xếp</th>
              <th>Số Ca Đi Làm</th>
              <th>Đơn Giá / Ca</th>
              <th>Tổng Lương Tạm Tính</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sal in salaryList" :key="sal.username">
              <td>{{ sal.username }}</td>
              <td>{{ sal.fullname }}</td>
              <td><span class="role-badge" :class="sal.role">{{ translateRole(sal.role) }}</span></td>
              <td style="text-align:center; font-weight:bold;">{{ sal.scheduledShifts }}</td>
              <td style="text-align:center; font-weight:bold; color:var(--primary);">{{ sal.workedShifts }}</td>
              <td style="text-align:right;">{{ sal.rate.toLocaleString() }}đ</td>
              <td style="text-align:right; font-weight:bold; color:var(--primary); font-size:1.1em;">{{ sal.totalSalary.toLocaleString() }}đ</td>
            </tr>
            <tr v-if="salaryList.length === 0">
              <td colspan="7" class="text-center">Chưa có dữ liệu lương tháng này.</td>
            </tr>
          </tbody>
        </table>
      </div>
      </div>
    </main>

    <!-- MODAL THÊM NHÂN VIÊN -->
    <div class="g-modal-overlay" v-if="showAddModal" @click.self="showAddModal = false">
      <div class="g-modal">
        <h3>Thêm Nhân Viên Mới</h3>
        <div class="form-group">
          <label>Tên Đăng Nhập</label>
          <input type="text" class="g-form-control" v-model="newStaff.username" />
        </div>
        <div class="form-group">
          <label>Mật Khẩu</label>
          <input type="password" class="g-form-control" v-model="newStaff.password" />
        </div>
        <div class="form-group">
          <label>Họ Tên</label>
          <input type="text" class="g-form-control" v-model="newStaff.fullname" />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input type="email" class="g-form-control" v-model="newStaff.email" />
        </div>
        <div class="form-group">
          <label>Vị Trí</label>
          <select class="g-form-control" v-model="newStaff.role">
            <option value="ROLE_KITCHEN">Bếp (Kitchen)</option>
            <option value="ROLE_WAITER">Phục Vụ (Waiter)</option>
            <option value="ROLE_CASHIER">Thu Ngân (Cashier)</option>
            <option value="ROLE_MANAGER">Quản Lý (Manager)</option>
          </select>
        </div>
        <div class="form-group" v-if="newStaff.role === 'ROLE_WAITER'">
          <label>Ca Làm Mặc Định</label>
          <select class="g-form-control" v-model="newStaff.shift">
            <option value="">-- Chọn Ca --</option>
            <option value="Sáng">Sáng</option>
            <option value="Chiều">Chiều</option>
            <option value="Tối">Tối</option>
          </select>
        </div>
        <div class="form-group" v-if="newStaff.role === 'ROLE_WAITER'">
          <label>Khu Vực Phục Vụ</label>
          <input type="text" class="g-form-control" v-model="newStaff.assignedArea" placeholder="Vd: Tầng 1, Tầng 2..." />
        </div>
        <div class="modal-actions" style="margin-top: 20px; text-align: right;">
          <button class="g-btn-danger" style="margin-right: 10px;" @click="showAddModal = false">Hủy</button>
          <button class="g-btn-primary" @click="createStaff">Xác Nhận</button>
        </div>
      </div>
    </div>

    <!-- MODAL XEM/SỬA NHÂN VIÊN -->
    <div class="g-modal-overlay" v-if="showEditModal" @click.self="showEditModal = false">
      <div class="g-modal">
        <h3>Xem & Cập Nhật Nhân Viên</h3>
        <div class="form-group">
          <label>Tên Đăng Nhập</label>
          <input type="text" class="g-form-control" v-model="editStaff.username" disabled style="background:#222; color:#aaa; cursor:not-allowed;" />
        </div>
        <div class="form-group">
          <label>Mật Khẩu (Để trống nếu không đổi)</label>
          <input type="password" class="g-form-control" v-model="editStaff.password" placeholder="***" />
        </div>
        <div class="form-group">
          <label>Họ Tên</label>
          <input type="text" class="g-form-control" v-model="editStaff.fullname" />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input type="email" class="g-form-control" v-model="editStaff.email" />
        </div>
        <div class="form-group">
          <label>Vị Trí</label>
          <select class="g-form-control" v-model="editStaff.role">
            <option value="ROLE_KITCHEN">Bếp (Kitchen)</option>
            <option value="ROLE_WAITER">Phục Vụ (Waiter)</option>
            <option value="ROLE_CASHIER">Thu Ngân (Cashier)</option>
            <option value="ROLE_MANAGER">Quản Lý (Manager)</option>
            <option value="ROLE_ADMIN" v-if="editStaff.role === 'ROLE_ADMIN'">Admin</option>
          </select>
        </div>
        <div class="form-group" v-if="editStaff.role === 'ROLE_WAITER'">
          <label>Ca Làm Mặc Định</label>
          <select class="g-form-control" v-model="editStaff.shift">
            <option value="">-- Chọn Ca --</option>
            <option value="Sáng">Sáng</option>
            <option value="Chiều">Chiều</option>
            <option value="Tối">Tối</option>
          </select>
        </div>
        <div class="form-group" v-if="editStaff.role === 'ROLE_WAITER'">
          <label>Khu Vực Phục Vụ</label>
          <input type="text" class="g-form-control" v-model="editStaff.assignedArea" placeholder="Vd: Tầng 1, Tầng 2..." />
        </div>
        <div class="modal-actions" style="margin-top: 20px; text-align: right;">
          <button class="g-btn-danger" style="margin-right: 10px;" @click="showEditModal = false">Đóng</button>
          <button class="g-btn-primary" @click="updateStaff">Lưu Thay Đổi</button>
        </div>
      </div>
    </div>
    <!-- MODAL LỊCH SỬ KHÁCH HÀNG -->
    <div class="g-modal-overlay" v-if="showCustomerOrdersModal" @click.self="showCustomerOrdersModal = false">
      <div class="g-modal" style="max-width: 800px; width: 90%; max-height: 90vh; overflow-y: auto; position: relative;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <h3 style="margin: 0;">Lịch Sử Hóa Đơn Khách Hàng</h3>
          <button class="g-btn-danger" style="padding: 5px 10px; border-radius: 4px;" @click="showCustomerOrdersModal = false">✖ Đóng</button>
        </div>
        <div style="display:flex; justify-content:space-between; align-items:center; margin-top: 15px;">
          <input type="text" class="g-form-control" v-model="searchCustomerOrderQuery" placeholder="Tìm kiếm theo mã đơn..." style="width: 250px;" />
          <button class="g-btn-warning" @click="analyzeCustomer" :disabled="isAnalyzing">
            🤖 {{ isAnalyzing ? 'Đang phân tích...' : 'AI Phân Tích Khách Hàng' }}
          </button>
        </div>
        <div v-if="aiCustomerAnalysis" style="margin-top:15px; padding:15px; background:rgba(241, 196, 15, 0.1); border:1px solid rgba(241, 196, 15, 0.4); border-radius:8px;">
          <h4 style="margin:0 0 10px 0; color:#f1c40f;">🤖 Đánh Giá Từ AI:</h4>
          <p style="margin:0; font-size:0.95rem; line-height:1.6; white-space:pre-wrap;">{{ aiCustomerAnalysis }}</p>
        </div>
        <div style="max-height: 400px; overflow-y: auto;">
          <table class="data-table mt-10">
            <thead>
              <tr>
                <th>Mã Đơn</th>
                <th>Ngày Lập</th>
                <th>Tổng Tiền</th>
                <th>Tình Trạng</th>
                <th>Hành Động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in filteredCustomerOrders" :key="order.id">
                <td>#{{ order.id }}</td>
                <td>{{ new Date(order.createDate).toLocaleString('vi-VN') }}</td>
                <td style="color: gold; font-weight: bold;">{{ calculateOrderTotal(order).toLocaleString() }}đ</td>
                <td>
                  <span class="status-badge" :class="'status-' + order.status">
                    {{ order.status === 0 ? 'Chờ Duyệt' : order.status === 1 ? 'Đang Nấu' : order.status === 2 ? 'Đã Lên Món' : order.status === 3 ? 'Đã Hủy' : order.status === 4 ? 'Hoàn Thành' : order.status === 5 ? 'Chờ Hẹn Giờ' : 'Đang Phục Vụ' }}
                  </span>
                </td>
                <td>
                  <button class="g-btn-primary" @click="viewCustomerInvoice(order)">👁 Xem & In</button>
                </td>
              </tr>
              <tr v-if="filteredCustomerOrders.length === 0">
                <td colspan="5" class="text-center">Không tìm thấy hóa đơn nào.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-actions hide-on-print" style="margin-top: 20px; text-align: right;">
          <button class="g-btn-danger" @click="showCustomerOrdersModal = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- MODAL CHI TIẾT HÓA ĐƠN ĐỂ IN -->
    <div v-if="selectedCustomerOrder" class="g-modal-overlay" @click.self="selectedCustomerOrder = null">
      <div class="g-modal printable-area invoice-modal">
        <div class="modal-header hide-on-print" style="display:flex; justify-content:space-between;">
          <h2 style="color:var(--primary); margin:0;">Chi Tiết Hóa Đơn</h2>
          <button @click="selectedCustomerOrder = null" style="background:transparent; border:none; font-size:1.5rem; cursor:pointer; color:#aaa;">✖</button>
        </div>
        
        <div class="invoice-content" style="padding:20px; color:#111; background:#fff;">
          <div style="text-align:center; border-bottom:2px solid #111; padding-bottom:15px; margin-bottom:20px;">
            <h1 style="margin:0; font-size:2rem; letter-spacing:2px;">Mộc Vị <span style="color:#00b894;">RESTAURANT</span></h1>
            <p style="margin:5px 0 0; color:#555; text-transform:uppercase; font-size:0.8rem;">Luxury Dining Experience</p>
            <p style="margin:5px 0 0; font-size:0.85rem; color:#777;">137 Nguyễn Thị Thập, Đà Nẵng | Hotline: 0905.XXX.XXX</p>
          </div>
          
          <div style="display:flex; justify-content:space-between; margin-bottom:20px;">
            <div>
              <p style="margin:5px 0;"><strong>Khách hàng:</strong> {{ selectedCustomerOrder.account?.fullname || selectedCustomerOrder.username || 'Khách Vãng Lai' }}</p>
              <p style="margin:5px 0;"><strong>Ngày lập:</strong> {{ new Date(selectedCustomerOrder.createDate).toLocaleString('vi-VN') }}</p>
            </div>
            <div style="text-align:right;">
              <h3 style="margin:0; color:#c0392b;">HÓA ĐƠN THANH TOÁN</h3>
              <p style="margin:5px 0; font-size:1.2rem; font-weight:bold; font-family:monospace;">#{{ selectedCustomerOrder.id }}</p>
            </div>
          </div>
          
          <table style="width:100%; border-collapse:collapse; margin-bottom:20px;">
            <thead>
              <tr style="background:#111; color:#fff;">
                <th style="padding:10px; text-align:left;">Tên Món Ăn</th>
                <th style="padding:10px; text-align:right;">Đơn Giá</th>
                <th style="padding:10px; text-align:center;">SL</th>
                <th style="padding:10px; text-align:right;">Thành Tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in selectedCustomerOrder.orderDetails" :key="item.id" style="border-bottom:1px solid #ddd;">
                <td style="padding:10px;">{{ item.product?.name }}</td>
                <td style="padding:10px; text-align:right;">{{ (item.price / item.quantity).toLocaleString() }}đ</td>
                <td style="padding:10px; text-align:center;">{{ item.quantity }}</td>
                <td style="padding:10px; text-align:right; font-weight:bold;">{{ item.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>
          
          <div style="text-align:right; margin-bottom:20px;">
            <div style="font-size:1.2rem; font-weight:bold; color:#c0392b; border-top:2px solid #111; display:inline-block; padding-top:10px;">
              TỔNG CỘNG: {{ calculateOrderTotal(selectedCustomerOrder).toLocaleString() }} đ
            </div>
          </div>
          
          <div style="text-align:center; font-size:0.9rem; color:#777; border-top:1px solid #eee; padding-top:15px;">
            <p style="margin:0 0 5px; color:#00b894; font-size:1.1rem; font-style:italic;">Cảm ơn quý khách!</p>
            <p style="margin:0;">In từ hệ thống Mộc Vị Restaurant</p>
          </div>
        </div>
        
        <div class="modal-actions hide-on-print" style="padding:15px; background:#f8f9fa; text-align:center;">
          <button class="g-btn-primary" @click="printInvoice" style="background:#00b894; color:#fff; padding:10px 25px; border:none; font-weight:bold; cursor:pointer;">🖨️ Xuất Hóa Đơn PDF</button>
        </div>
      </div>
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const currentTab = ref('staff');
const staffList = ref([]);
const customerList = ref([]);
const scheduleList = ref([]);
const timekeepingList = ref([]);

const searchStaffQuery = ref('');
const filteredStaffList = computed(() => {
  const q = searchStaffQuery.value.toLowerCase();
  if (!q) return staffList.value;
  return staffList.value.filter(s => 
    s.username.toLowerCase().includes(q) || 
    s.fullname.toLowerCase().includes(q) ||
    translateRole(s.role).toLowerCase().includes(q)
  );
});

const searchCustomerQuery = ref('');
const filteredCustomerList = computed(() => {
  const q = searchCustomerQuery.value.toLowerCase();
  if (!q) return customerList.value;
  return customerList.value.filter(c => 
    c.username.toLowerCase().includes(q) || 
    c.fullname.toLowerCase().includes(q)
  );
});

const showCustomerOrdersModal = ref(false);
const customerOrders = ref([]);
const searchCustomerOrderQuery = ref('');
const currentCustomerView = ref(null);
const aiCustomerAnalysis = ref('');
const isAnalyzing = ref(false);

const filteredCustomerOrders = computed(() => {
  const q = searchCustomerOrderQuery.value.trim();
  if (!q) return customerOrders.value;
  return customerOrders.value.filter(o => String(o.id).includes(q));
});

const calculateOrderTotal = (order) => {
  if (!order?.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + item.price, 0);
};

const selectedCustomerOrder = ref(null);
const viewCustomerInvoice = (order) => {
  selectedCustomerOrder.value = order;
};
const printInvoice = () => {
  window.print();
};

const searchScheduleQuery = ref('');
const filteredScheduleList = computed(() => {
  const q = searchScheduleQuery.value.toLowerCase();
  if (!q) return scheduleList.value;
  return scheduleList.value.filter(s => 
    s.account.fullname.toLowerCase().includes(q) || 
    s.account.username.toLowerCase().includes(q)
  );
});

const searchTimekeepingQuery = ref('');
const filteredTimekeepingList = computed(() => {
  const q = searchTimekeepingQuery.value.toLowerCase();
  if (!q) return timekeepingList.value;
  return timekeepingList.value.filter(t => 
    t.account.fullname.toLowerCase().includes(q) || 
    t.account.username.toLowerCase().includes(q)
  );
});

const showAddModal = ref(false);
const newStaff = ref({ username: '', password: '', fullname: '', email: '', role: 'ROLE_WAITER', shift: '', assignedArea: '' });

const showEditModal = ref(false);
const editStaff = ref({ username: '', password: '', fullname: '', email: '', role: '', shift: '', assignedArea: '' });

// Lấy ngày hôm nay định dạng yyyy-MM-dd
const todayStr = new Date().toISOString().split('T')[0];
const scheduleStartDate = ref(todayStr);
const scheduleEndDate = ref(todayStr);
const timekeepingStartDate = ref(todayStr);
const timekeepingEndDate = ref(todayStr);

const repeatForWeek = ref(false);
const newSchedule = ref({ username: '', shift: 'Sáng', workDate: '' });

const salaryMonth = ref(todayStr.slice(0, 7)); // YYYY-MM
const salaryList = ref([]);

const configHeader = () => {
  const token = localStorage.getItem('token');
  return { headers: { Authorization: 'Bearer ' + token } };
};

const translateRole = (role) => {
  const map = {
    'ROLE_ADMIN': 'Admin',
    'ROLE_MANAGER': 'Quản Lý',
    'ROLE_KITCHEN': 'Bếp',
    'ROLE_WAITER': 'Phục Vụ',
    'ROLE_CASHIER': 'Thu Ngân'
  };
  return map[role] || role;
};

const formatTime = (dateStr) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleTimeString('vi-VN');
};

const getStatusClass = (status) => {
  if (status === 'Đã Check-in') return 'status-warning';
  if (status === 'Hoàn thành' || status === 'Đúng giờ') return 'status-success';
  if (status === 'Đi trễ') return 'status-danger';
  return 'status-default';
};

const fetchStaff = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/admin/staff', configHeader());
    staffList.value = response.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách nhân viên:', error);
  }
};

const fetchCustomers = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/admin/staff/customers', configHeader());
    customerList.value = response.data;
  } catch (error) {
    console.error('Lỗi tải khách hàng:', error);
  }
};

const viewCustomerOrders = async (cus) => {
  try {
    currentCustomerView.value = cus;
    searchCustomerOrderQuery.value = '';
    aiCustomerAnalysis.value = '';
    const res = await axios.get(`http://localhost:8080/api/admin/staff/customers/${cus.username}/orders`, configHeader());
    customerOrders.value = res.data;
    showCustomerOrdersModal.value = true;
  } catch (err) {
    alert('Lỗi lấy lịch sử hóa đơn');
  }
};

const analyzeCustomer = async () => {
  if (!currentCustomerView.value) return;
  isAnalyzing.value = true;
  aiCustomerAnalysis.value = '';
  
  // Format data
  const data = {
    username: currentCustomerView.value.username,
    fullname: currentCustomerView.value.fullname,
    points: currentCustomerView.value.points,
    membershipTier: currentCustomerView.value.membershipTier,
    orders: customerOrders.value.map(o => ({
      id: o.id,
      date: o.createDate,
      status: o.status === 4 ? 'Hoàn Thành' : 'Khác',
      items: o.orderDetails?.map(d => `${d.product?.name} x${d.quantity}`) || [],
      totalAmount: calculateOrderTotal(o)
    }))
  };

  try {
    const res = await axios.post('/api/admin/ai/customer', {
      message: JSON.stringify(data),
    }, configHeader());
    
    // Tự đánh máy cho giống AI thật
    const reply = res.data.reply || 'Không có phản hồi từ AI.';
    let i = 0;
    const interval = setInterval(() => {
      aiCustomerAnalysis.value += reply.charAt(i);
      i++;
      if (i >= reply.length) clearInterval(interval);
    }, 15);
  } catch (err) {
    aiCustomerAnalysis.value = 'Lỗi kết nối đến AI. Vui lòng thử lại sau.';
  } finally {
    isAnalyzing.value = false;
  }
};

const createStaff = async () => {
  try {
    const payload = { ...newStaff.value };
    const roleId = payload.role;
    delete payload.role; // Remove role from body since it expects Account only
    await axios.post(`http://localhost:8080/api/admin/staff?roleId=${roleId}`, payload, configHeader());
    alert('Thêm nhân viên thành công!');
    showAddModal.value = false;
    newStaff.value = { username: '', password: '', fullname: '', email: '', role: 'ROLE_WAITER', shift: '', assignedArea: '' };
    fetchStaff();
  } catch (err) {
    alert('Lỗi tạo nhân viên: ' + (err.response?.data || err.message));
  }
};

const openEditModal = (staff) => {
  editStaff.value = {
    username: staff.username,
    password: '',
    fullname: staff.fullname,
    email: staff.email,
    role: staff.role,
    shift: staff.shift || '',
    assignedArea: staff.assignedArea || ''
  };
  showEditModal.value = true;
};

const updateStaff = async () => {
  try {
    const payload = { 
      fullname: editStaff.value.fullname, 
      email: editStaff.value.email,
      shift: editStaff.value.shift,
      assignedArea: editStaff.value.assignedArea
    };
    if (editStaff.value.password) {
      payload.password = editStaff.value.password;
    }
    
    await axios.put(`http://localhost:8080/api/admin/staff/${editStaff.value.username}?roleId=${editStaff.value.role}`, payload, configHeader());
    alert('Cập nhật nhân viên thành công!');
    showEditModal.value = false;
    fetchStaff();
  } catch (err) {
    alert('Lỗi cập nhật nhân viên: ' + (err.response?.data || err.message));
  }
};

const deleteStaff = async (username) => {
  if (!confirm('Bạn có chắc muốn xóa nhân viên ' + username + '?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/admin/staff/${username}`, configHeader());
    alert('Xóa thành công!');
    fetchStaff();
  } catch (err) {
    alert('Lỗi xóa nhân viên');
  }
};

const fetchSchedules = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/schedules?startDate=${scheduleStartDate.value}&endDate=${scheduleEndDate.value}`, configHeader());
    scheduleList.value = res.data;
  } catch (err) {
    console.error('Lỗi lấy lịch', err);
  }
};

const addSchedule = async () => {
  if (!newSchedule.value.username) return alert('Vui lòng chọn nhân viên!');
  try {
    const daysToAdd = repeatForWeek.value ? 7 : 1;
    const baseDate = new Date(scheduleStartDate.value);
    
    // Tạo mảng các request API
    const requests = [];
    for (let i = 0; i < daysToAdd; i++) {
      const d = new Date(baseDate);
      d.setDate(d.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];
      const payload = { ...newSchedule.value, workDate: dateStr };
      requests.push(axios.post('http://localhost:8080/api/schedules', payload, configHeader()));
    }
    
    await Promise.all(requests);
    fetchSchedules();
    if (repeatForWeek.value) {
      alert('Đã xếp lịch cho 7 ngày liên tiếp thành công!');
    }
  } catch (err) {
    alert(err.response?.data || 'Lỗi xếp lịch, có thể nhân viên đã bị trùng ca trong một số ngày.');
    fetchSchedules(); // Vẫn fetch lại vì có thể lưu thành công vài ngày
  }
};

const deleteSchedule = async (id) => {
  if (!confirm('Hủy ca làm này?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/schedules/${id}`, configHeader());
    fetchSchedules();
  } catch (err) {
    alert('Lỗi hủy lịch');
  }
};

const fetchTimekeeping = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/timekeeping?startDate=${timekeepingStartDate.value}&endDate=${timekeepingEndDate.value}`, configHeader());
    timekeepingList.value = res.data;
  } catch (err) {
    console.error('Lỗi lấy báo cáo chấm công', err);
  }
};

const generateDemoSalary = async () => {
  if (!confirm('Bạn có chắc muốn tạo dữ liệu lương mẫu cho tháng 05/2026? Dữ liệu cũ của tháng này sẽ bị xóa!')) return;
  try {
    const res = await axios.post('http://localhost:8080/api/admin/staff/demo-salary', {}, configHeader());
    alert(res.data.message || 'Thành công!');
    salaryMonth.value = '2026-05';
    fetchSalary();
    fetchTimekeeping();
    fetchSchedules();
  } catch (err) {
    alert('Lỗi tạo dữ liệu: ' + (err.response?.data || err.message));
  }
};

const fetchSalary = async () => {
  try {
    const year = salaryMonth.value.split('-')[0];
    const month = salaryMonth.value.split('-')[1];
    const startDate = `${year}-${month}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const endDate = `${year}-${month}-${lastDay}`;
    
    const [resSched, resTk] = await Promise.all([
      axios.get(`http://localhost:8080/api/schedules?startDate=${startDate}&endDate=${endDate}`, configHeader()),
      axios.get(`http://localhost:8080/api/timekeeping?startDate=${startDate}&endDate=${endDate}`, configHeader())
    ]);
    
    const schedules = resSched.data;
    const timekeepings = resTk.data;
    
    const map = {};
    staffList.value.forEach(st => {
      if (st.role === 'ROLE_ADMIN' || st.role === 'ADMIN') return; // Không tính lương theo tháng cho admin
      map[st.username] = {
        username: st.username,
        fullname: st.fullname,
        role: st.role,
        scheduledShifts: 0,
        workedShifts: 0,
        rate: st.role === 'ROLE_KITCHEN' ? 250000 : (st.role === 'ROLE_MANAGER' ? 357143 : 214286),
        totalSalary: 0
      };
    });
    
    schedules.forEach(s => {
      if (map[s.account.username]) map[s.account.username].scheduledShifts++;
    });
    
    timekeepings.forEach(tk => {
      if (tk.checkInTime && map[tk.account.username]) {
        map[tk.account.username].workedShifts++;
      }
    });
    
    Object.values(map).forEach(m => {
      m.totalSalary = m.workedShifts * m.rate;
    });
    
    salaryList.value = Object.values(map).filter(m => m.scheduledShifts > 0 || m.workedShifts > 0);
  } catch (err) {
    console.error('Lỗi tính lương', err);
  }
};

// === PHÂN KHU VỰC PHỤC VỤ ===
const zoneDate = ref(todayStr);
const zoneList = ref([]);
const floorList = ref([]);
const zoneMap = ref({});
const zoneMapShift = ref('');
const zoneRepeatWeek = ref(false);
const newZone = ref({ username: '', floor: '', shift: 'Sáng' });

const waiterList = computed(() => {
  return staffList.value.filter(s => s.role === 'ROLE_WAITER');
});

const fetchFloors = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/service-zones/floors', configHeader());
    floorList.value = res.data;
  } catch (err) {
    console.error('Lỗi lấy danh sách tầng', err);
  }
};

const fetchZones = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/service-zones?date=${zoneDate.value}`, configHeader());
    zoneList.value = res.data;
    fetchZoneMap();
  } catch (err) {
    console.error('Lỗi lấy phân công khu vực', err);
  }
};

const fetchZoneMap = async () => {
  try {
    let url = `http://localhost:8080/api/service-zones/map?date=${zoneDate.value}`;
    if (zoneMapShift.value) url += `&shift=${encodeURIComponent(zoneMapShift.value)}`;
    const res = await axios.get(url, configHeader());
    zoneMap.value = res.data;
  } catch (err) {
    console.error('Lỗi lấy bản đồ khu vực', err);
  }
};

const addZoneAssignment = async () => {
  if (!newZone.value.username || !newZone.value.floor) {
    return alert('Vui lòng chọn nhân viên và tầng!');
  }
  try {
    const daysToAdd = zoneRepeatWeek.value ? 7 : 1;
    const baseDate = new Date(zoneDate.value);
    const requests = [];
    for (let i = 0; i < daysToAdd; i++) {
      const d = new Date(baseDate);
      d.setDate(d.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];
      requests.push(
        axios.post('http://localhost:8080/api/service-zones', {
          username: newZone.value.username,
          floor: newZone.value.floor,
          shift: newZone.value.shift,
          workDate: dateStr
        }, configHeader())
      );
    }
    await Promise.all(requests);
    if (zoneRepeatWeek.value) {
      alert('Đã phân công khu vực cho 7 ngày liên tiếp!');
    } else {
      alert('Phân công khu vực thành công!');
    }
    fetchZones();
  } catch (err) {
    alert(err.response?.data || 'Lỗi phân công, có thể đã bị trùng lặp.');
    fetchZones();
  }
};

const deleteZoneAssignment = async (id) => {
  if (!confirm('Xóa phân công khu vực này?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/service-zones/${id}`, configHeader());
    fetchZones();
  } catch (err) {
    alert('Lỗi xóa phân công');
  }
};

onMounted(() => {
  fetchStaff();
  fetchCustomers();
  fetchSchedules();
  fetchTimekeeping();
  fetchSalary();
  fetchFloors();
});
</script>

<style scoped>
@media print {
  body * { visibility: hidden !important; }
  .printable-area, .printable-area * { visibility: visible !important; }
  .printable-area {
    position: fixed !important;
    inset: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    height: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    background: white !important;
    color: #111 !important;
    padding: 0 !important;
    z-index: 99999 !important;
  }
  .hide-on-print { display: none !important; }
}

.tabs {
  display: flex;
  gap: 10px;
}
.tabs button {
  padding: 12px 24px;
  border: none;
  background: var(--bg-card);
  border-radius: 8px 8px 0 0;
  font-weight: bold;
  cursor: pointer;
  transition: 0.3s;
  color: var(--text-muted);
  font-size: 1rem;
}
.tabs button.active {
  background: var(--primary);
  color: var(--bg-dark);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.panel-header h3 {
  margin: 0;
  color: var(--text-heading);
}

.date-picker { display: flex; align-items: center; gap: 10px; font-weight: bold; color: var(--text-heading); }
.date-picker input { padding: 8px; border-radius: 6px; border: 1px solid var(--border); background: var(--bg-root); color: var(--text-heading); }

.data-table { width: 100%; border-collapse: collapse; color: var(--text-heading); }
.data-table th, .data-table td { padding: 12px 15px; text-align: left; border-bottom: 1px solid var(--border); }
.data-table th { background: rgba(0, 212, 170, 0.1); font-weight: bold; color: var(--primary); }
.data-table tr:hover { background: rgba(255, 255, 255, 0.02); }

.role-badge { padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold; color: white; }
.role-badge.ROLE_ADMIN { background: #e74c3c; }
.role-badge.ROLE_MANAGER { background: #8e44ad; }
.role-badge.ROLE_KITCHEN { background: #e67e22; }
.role-badge.ROLE_WAITER { background: #3498db; }
.role-badge.ROLE_CASHIER { background: #2ecc71; }

.status-badge { padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold; color: white; }
.status-warning { background: #f39c12; }
.status-success { background: #27ae60; }
.status-danger { background: #e74c3c; }
.status-default { background: #95a5a6; }

.schedule-form { display: flex; gap: 15px; background: rgba(0, 0, 0, 0.2); padding: 15px; border-radius: 8px; align-items: center; border: 1px solid var(--border); }
.mt-20 { margin-top: 20px; }

.form-group { margin-bottom: 15px; }
.form-group label { display: block; margin-bottom: 5px; font-weight: bold; font-size: 0.9rem; color: var(--text-secondary); }

/* Zone Assignment Styles */
.zone-form {
  display: flex; gap: 10px; align-items: center; flex-wrap: wrap;
  margin-bottom: 20px; background: rgba(0, 0, 0, 0.2);
  padding: 15px; border-radius: 8px; border: 1px solid var(--border);
}

.zone-map-section {
  margin: 20px 0;
  padding: 20px;
  background: rgba(0, 212, 170, 0.03);
  border: 1px solid rgba(0, 212, 170, 0.15);
  border-radius: 12px;
}

.zone-map-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.zone-map-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  overflow: hidden;
  transition: 0.3s;
}
.zone-map-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 212, 170, 0.15);
  border-color: rgba(0, 212, 170, 0.3);
}

.zone-map-floor {
  padding: 12px 16px;
  background: rgba(0, 212, 170, 0.1);
  font-weight: 800;
  font-size: 0.95rem;
  color: var(--primary);
  border-bottom: 1px solid var(--border-light);
}

.zone-map-staff {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.zone-staff-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(52, 152, 219, 0.08);
  border: 1px solid rgba(52, 152, 219, 0.15);
  border-radius: 8px;
  transition: 0.2s;
}
.zone-staff-chip:hover {
  background: rgba(52, 152, 219, 0.15);
}

.zone-chip-avatar {
  font-size: 1.2rem;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(52, 152, 219, 0.15);
  border-radius: 50%;
}

.zone-chip-name {
  font-weight: 700;
  font-size: 0.88rem;
  color: var(--text-heading);
}

.zone-chip-shift {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-weight: 600;
}

.shift-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.82rem;
  font-weight: 700;
  color: white;
}
.shift-badge.shift-Sáng { background: #f39c12; }
.shift-badge.shift-Chiều { background: #e67e22; }
.shift-badge.shift-Tối { background: #8e44ad; }
</style>
