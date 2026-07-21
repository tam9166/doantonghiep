<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    

    <main class="admin-content">
      <div class="page-header">
        <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 5px;">
          <button v-if="isKitchenOnly" @click="$router.push('/kitchen')" class="g-btn-outline" style="border-radius: 100px; padding: 6px 15px; border-color: rgba(255,255,255,0.2);">
            ← Quay Lại Bếp
          </button>
          <h1 class="page-title" style="margin: 0;">Quản Lý Nguyên Liệu & Công Thức</h1>
        </div>
        <p class="page-subtitle">Kiểm soát tồn kho, thiết lập định lượng và tự động trừ nguyên liệu</p>
      </div>

      <!-- Thống kê nhanh -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">Tổng Nguyên Liệu</span>
          </div>
        </div>
        <div class="stat-card stat-warn">
          <div class="stat-icon">⚠️</div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.lowStock }}</span>
            <span class="stat-label">Sắp Hết</span>
          </div>
        </div>
        <div class="stat-card stat-danger">
          <div class="stat-icon">🚫</div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.outOfStock }}</span>
            <span class="stat-label">Hết Hàng</span>
          </div>
        </div>
        <div class="stat-card stat-warn">
          <div class="stat-icon">📅</div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.expiringBatchesCount || 0 }}</span>
            <span class="stat-label">Lô Sắp Hết Hạn (3 Ngày)</span>
          </div>
        </div>
      </div>

      <!-- Tabs Control -->
      <div class="tabs-header">
        <button @click="activeTab = 'inventory'" :class="['tab-btn', { active: activeTab === 'inventory' }]">
          📦 Kho Nguyên Liệu
        </button>
        <button @click="activeTab = 'recipes'" :class="['tab-btn', { active: activeTab === 'recipes' }]">
          🍳 Công Thức Nấu (Định lượng)
        </button>
        <button @click="activeTab = 'invoices'" :class="['tab-btn', { active: activeTab === 'invoices' }]">
          📄 Lịch Sử Nhập Hàng
        </button>
        <div style="margin-left: auto; display: flex; gap: 10px;">
          <button @click="openCreateInvoiceModal" class="g-btn-primary">
             📦 Nhập Hàng Vào Kho
          </button>
          <button @click="analyzeInventory" class="btn-ai-forecast">
             🤖 AI Dự Báo Nhập Kho
          </button>
        </div>
      </div>

      <!-- ================== TAB 1: KHO NGUYÊN LIỆU ================== -->
      <div v-if="activeTab === 'inventory'" class="tab-content">
        <div class="content-grid">
          <!-- Form Thêm/Sửa Nguyên Liệu -->
          <div class="form-card" :class="{ 'edit-mode': isEditingIng }">
            <h3>{{ isEditingIng ? '✏️ Cập Nhật Nguyên Liệu' : '➕ Thêm Nguyên Liệu Mới' }}</h3>
            
            <div class="form-group">
              <label>Tên nguyên liệu (*)</label>
              <input v-model="ingForm.name" placeholder="VD: Thịt Bò Kobe" class="g-form-control" />
            </div>

            <div class="form-group">
              <label>Đơn vị tính (*)</label>
              <input v-model="ingForm.unit" placeholder="VD: kg, gam, lít..." class="g-form-control" />
            </div>

            <div class="form-group">
              <label>Mức cảnh báo sắp hết (Tồn tối thiểu)</label>
              <input v-model="ingForm.minStock" type="number" step="0.1" class="g-form-control" />
            </div>

            <div class="form-group">
              <label>Đơn giá nhập (VNĐ)</label>
              <input v-model="ingForm.unitPrice" type="number" step="500" placeholder="VD: 50000" class="g-form-control" />
            </div>

            <div class="form-group">
              <label>Thời gian bảo quản (Ngày)</label>
              <input v-model="ingForm.shelfLifeDays" type="number" placeholder="VD: 30" class="g-form-control" />
            </div>

            <div class="form-group">
              <label>Link hình ảnh (Tùy chọn)</label>
              <input v-model="ingForm.image" placeholder="URL..." class="g-form-control" />
            </div>

            <div class="form-actions">
              <button @click="saveIngredient" class="g-btn-primary">
                {{ isEditingIng ? '💾 Lưu Cập Nhật' : '➕ Thêm Nguyên Liệu' }}
              </button>
              <button v-if="isEditingIng" @click="cancelEditIng" class="btn-cancel">Hủy</button>
            </div>
          </div>

          <!-- Bảng Kho Nguyên Liệu -->
          <div class="table-card">
            <h3>📜 Danh Sách Nguyên Liệu</h3>
            <table class="g-table">
              <thead>
                <tr>
                  <th>Ảnh</th>
                  <th>Tên</th>
                  <th>Đơn Giá</th>
                  <th>Tồn Kho</th>
                  <th>Trạng Thái</th>
                  <th>Thao Tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="ing in ingredients" :key="ing.id">
                  <td>
                    <img :src="ing.image || 'https://placehold.co/40x40/0a1929/00d4aa?text=📦'" class="img-thumb-sm" />
                  </td>
                  <td><strong>{{ ing.name }}</strong></td>
                  <td style="color: #00d4aa; font-weight: bold;">{{ ing.unitPrice?.toLocaleString() || 0 }}đ / {{ ing.unit }}</td>
                  <td class="qty-col">
                    <span class="qty-val">{{ ing.quantity?.toFixed(2) }}</span> {{ ing.unit }}
                  </td>
                  <td>
                    <span v-if="ing.quantity <= 0" class="g-badge g-badge-danger">Hết</span>
                    <span v-else-if="ing.quantity <= ing.minStock" class="g-badge g-badge-warning">Sắp hết</span>
                    <span v-else class="g-badge g-badge-success">Đủ</span>
                  </td>
                  <td>
                    <div class="action-btns">
                      <button @click="viewBatches(ing.id)" class="btn-sm btn-secondary">👀 Lịch Sử Lô</button>
                      <button @click="startEditIng(ing)" class="btn-sm btn-edit">✏️ Sửa</button>
                      <button @click="deleteIngredient(ing.id)" class="btn-sm btn-delete">🗑️ Xóa</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ================== TAB 2: CÔNG THỨC NẤU ================== -->
      <div v-if="activeTab === 'recipes'" class="tab-content">
        <div class="recipe-layout">
          <!-- Chọn món ăn bên trái -->
          <div class="recipe-sidebar">
            <div class="search-box">
              <input v-model="searchProduct" placeholder="🔍 Tìm món ăn..." class="g-form-control" />
            </div>
            <div class="product-list">
              <div 
                v-for="p in filteredProducts" :key="p.id" 
                :class="['product-item', { active: selectedProduct?.id === p.id }]"
                @click="selectProduct(p)"
              >
                <img :src="p.image" class="prod-thumb" />
                <div>
                  <h4>{{ p.name }}</h4>
                  <span>{{ p.category?.name }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Quản lý công thức bên phải -->
          <div class="recipe-main" v-if="selectedProduct">
            <div class="recipe-header">
              <h2>🍳 Công thức: <span>{{ selectedProduct.name }}</span></h2>
              <p>Thêm nguyên liệu cần thiết để nấu 1 phần món này.</p>
            </div>

            <!-- Form thêm nguyên liệu vào món -->
            <div class="add-recipe-box">
              <select v-model="newRecipe.ingredientId" class="g-form-control">
                <option value="" disabled>-- Chọn nguyên liệu --</option>
                <option v-for="ing in ingredients" :key="ing.id" :value="ing.id">
                  {{ ing.name }} (tính bằng {{ ing.unit }})
                </option>
              </select>
              <input v-model="newRecipe.amount" type="number" step="0.01" placeholder="Số lượng..." class="g-form-control" style="width: 150px;"/>
              <button @click="addRecipe" class="g-btn-primary">Thêm Vào Món</button>
            </div>

            <!-- Danh sách nguyên liệu của món -->
            <div class="recipe-table-wrap">
              <table class="g-table">
                <thead>
                  <tr>
                    <th>Nguyên Liệu</th>
                    <th>Định lượng 1 phần</th>
                    <th>Tồn kho hiện tại</th>
                    <th>Nấu được (dự kiến)</th>
                    <th>Xóa</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="rec in currentRecipes" :key="rec.id">
                    <td><strong>{{ rec.ingredient?.name }}</strong></td>
                    <td class="amount-cell">{{ rec.amountRequired }} {{ rec.ingredient?.unit }}</td>
                    <td>{{ rec.ingredient?.quantity }} {{ rec.ingredient?.unit }}</td>
                    <td class="est-cell">
                      {{ Math.floor((rec.ingredient?.quantity || 0) / rec.amountRequired) }} phần
                    </td>
                    <td><button @click="deleteRecipe(rec.id)" class="g-btn-danger">🗑</button></td>
                  </tr>
                  <tr v-if="currentRecipes.length === 0">
                    <td colspan="5" style="text-align: center; color: var(--text-muted)">Chưa có công thức. Món này sẽ không trừ kho.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-else class="empty-selection">
            <div class="icon">👈</div>
            <h3>Chọn một món ăn bên trái để thiết lập công thức</h3>
          </div>
        </div>
      </div>

      <!-- ================== TAB 3: HÓA ĐƠN NHẬP HÀNG ================== -->
      <div v-if="activeTab === 'invoices'" class="tab-content">
        <div class="content-grid">
          <div class="form-card" style="grid-column: span 12;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
              <h3>📄 Lịch Sử Các Đợt Nhập Hàng (Hóa Đơn)</h3>
              <button @click="openCreateInvoiceModal" class="g-btn-primary hide-on-print">📦 Nhập Hàng Mới</button>
            </div>
            
            <!-- Filter & Search for Invoices -->
            <div class="filter-bar hide-on-print" style="display: flex; gap: 16px; margin-bottom: 20px; align-items: flex-end;">
              <div class="filter-item">
                <span style="font-size: 0.8rem; font-weight: 600; color: var(--text-muted); display: block; margin-bottom: 6px;">⏱️ Lọc thời gian</span>
                <select v-model="invoiceTimeFilter" class="g-form-control" style="width: 200px;">
                  <option value="all">Tất cả thời gian</option>
                  <option value="today">Hôm nay</option>
                  <option value="week">7 Ngày qua</option>
                  <option value="month">Tháng này</option>
                  <option value="year">Năm nay</option>
                </select>
              </div>
              <div class="filter-item" style="flex: 1;">
                <span style="font-size: 0.8rem; font-weight: 600; color: var(--text-muted); display: block; margin-bottom: 6px;">🔍 Tìm hóa đơn</span>
                <input
                  v-model="invoiceSearchQuery"
                  type="text"
                  placeholder="Nhập mã phiếu hoặc nhà cung cấp..."
                  class="g-form-control"
                />
              </div>
              <button v-if="invoiceSearchQuery || invoiceTimeFilter !== 'all'" @click="resetInvoiceFilters" class="g-btn-secondary" style="padding: 12px 20px; white-space: nowrap;">
                ✕ Xóa lọc
              </button>
            </div>

            <table class="g-table">
              <thead>
                <tr>
                  <th>Mã HĐ</th>
                  <th>Ngày Nhập</th>
                  <th>Nhà Cung Cấp</th>
                  <th>Tổng Tiền</th>
                  <th>Ghi Chú</th>
                  <th class="hide-on-print">Thao Tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="inv in filteredInvoices" :key="inv.id">
                  <td style="font-weight: bold; color: var(--primary);">#{{ inv.id }}</td>
                  <td>{{ new Date(inv.importDate).toLocaleString('vi-VN') }}</td>
                  <td>{{ inv.supplier || '---' }}</td>
                  <td style="color: #e74c3c; font-weight: bold;">{{ inv.totalAmount?.toLocaleString() || 0 }}đ</td>
                  <td>{{ inv.note || '---' }}</td>
                  <td class="hide-on-print">
                    <button @click="viewInvoiceDetails(inv.id)" class="btn-sm btn-secondary">👀 Chi Tiết</button>
                  </td>
                </tr>
                <tr v-if="filteredInvoices.length === 0">
                  <td colspan="6" style="text-align: center; color: var(--text-muted)">Không tìm thấy hóa đơn nào phù hợp!</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </main>
    <div v-if="toastMsg" class="toast-notification">{{ toastMsg }}</div>

    <!-- AI Forecast Modal -->
    <div v-if="showForecastModal" class="modal-overlay" @click.self="showForecastModal = false">
      <div class="forecast-box">
        <div class="forecast-header">
          <h3>🤖 AI Phân Tích & Dự Báo</h3>
          <button @click="showForecastModal = false" class="btn-close-modal">✖</button>
        </div>
        <div class="forecast-body">
          <div v-if="isForecasting" class="forecasting-loader">
            <div class="pulse">🤖</div>
            <p>AI đang đọc dữ liệu tồn kho và tính toán dự báo tuần tới...</p>
          </div>
          <div v-else-if="forecastError" class="error-msg" style="color:#e74c3c; text-align:center;">
            <p>{{ forecastError }}</p>
          </div>
          <div v-else-if="forecastResults.length > 0">
            <p class="forecast-desc">Dựa trên dữ liệu tồn kho sắp hết, AI đề xuất nhập thêm:</p>
            <div v-for="(res, idx) in forecastResults" :key="idx" class="forecast-item">
              <div class="forecast-info">
                <h4>{{ res.name }}</h4>
                <span class="forecast-reason">💡 Lý do: {{ res.reason }}</span>
              </div>
              <div class="forecast-action">
                <span class="forecast-qty">Đề xuất: <strong style="color:var(--primary)">{{ res.suggestedAmount }} {{ res.unit }}</strong></span>
                <button @click="applyForecast(res.name, res.suggestedAmount)" class="g-btn-primary" style="width:100%; font-size:0.8rem; padding:8px;">Duyệt & Nhập</button>
              </div>
            </div>
          </div>
          <div v-else class="empty-selection" style="padding: 40px 0; border: none;">
            <p>Tất cả nguyên liệu đang ở mức an toàn. Không có nguyên liệu nào cần AI dự báo nhập kho.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Restock Modal (Nhập Lô Mới) -->
    <div v-if="showRestockModal" class="modal-overlay" @click.self="showRestockModal = false">
      <div class="form-card" style="max-width: 500px; width: 100%; z-index: 1000; position: relative;">
        <h3>📦 Nhập Lô Mới - {{ selectedIngForRestock?.name }}</h3>
        
        <div class="form-group">
          <label>Số lượng nhập ({{ selectedIngForRestock?.unit }}) *</label>
          <input v-model="batchForm.quantity" type="number" step="0.1" class="g-form-control" />
        </div>
        
        <div class="form-group">
          <label>Đơn giá nhập (VNĐ / 1 đơn vị)</label>
          <input v-model="batchForm.unitPrice" type="number" step="500" class="g-form-control" />
        </div>
        
        <div class="form-group">
          <label>Hạn sử dụng (Tùy chọn - Hệ thống sẽ tự tính theo TG bảo quản nếu để trống)</label>
          <input v-model="batchForm.expirationDate" type="date" class="g-form-control" />
        </div>

        <div class="form-actions" style="flex-direction: row; gap: 10px;">
          <button @click="submitBatch" class="g-btn-primary" style="flex:1;">✅ Xác Nhận Nhập Kho</button>
          <button @click="showRestockModal = false" class="btn-cancel" style="flex:1;">Hủy</button>
        </div>
      </div>
    </div>
    
    <!-- View Batches Modal -->
    <div v-if="showBatchesModal" class="modal-overlay" @click.self="showBatchesModal = false">
      <div class="table-card" style="max-width: 800px; width: 100%; z-index: 1000; position: relative; max-height: 80vh; overflow-y: auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-light); padding-bottom: 10px; margin-bottom: 20px;">
           <h3 style="margin: 0; border: none; padding: 0;">📦 Danh Sách Lô Hàng</h3>
           <button @click="showBatchesModal = false" style="background: none; border: none; font-size: 1.5rem; color: #e74c3c; cursor: pointer;">✖</button>
        </div>
        <table class="g-table">
          <thead>
            <tr>
              <th>Ngày Nhập</th>
              <th>Hạn Sử Dụng</th>
              <th>Số Lượng Còn</th>
              <th>Đơn Giá</th>
              <th>Tổng Tiền</th>
              <th>Thao Tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="b in selectedBatches" :key="b.id">
              <td>{{ new Date(b.importDate).toLocaleDateString('vi-VN') }}</td>
              <td :style="{ color: isExpiring(b.expirationDate) ? '#e74c3c' : 'inherit', fontWeight: isExpiring(b.expirationDate) ? 'bold' : 'normal' }">
                {{ b.expirationDate ? new Date(b.expirationDate).toLocaleDateString('vi-VN') : '---' }}
                <span v-if="isExpiring(b.expirationDate)">⚠️</span>
              </td>
              <td>{{ b.quantity }}</td>
              <td>{{ b.unitPrice?.toLocaleString() }}đ</td>
              <td style="color: #e74c3c; font-weight: bold;">{{ (b.quantity * (b.unitPrice || 0)).toLocaleString() }}đ</td>
              <td>
                <button @click="deleteBatch(b.id)" class="btn-sm btn-delete">🗑️ Xóa</button>
              </td>
            </tr>
            <tr v-if="selectedBatches.length === 0">
              <td colspan="6" style="text-align: center; color: var(--text-muted)">Chưa có lô hàng nào!</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create Invoice Modal -->
    <div v-if="showCreateInvoiceModal" class="modal-overlay" @click.self="showCreateInvoiceModal = false">
      <div class="modal-content" style="max-width: 800px; width: 90%;">
        <div class="modal-header">
          <h3>📦 Phiếu Nhập Hàng Vào Kho</h3>
          <button @click="showCreateInvoiceModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <div v-if="ingredients.length === 0" style="padding: 20px; background: rgba(231,76,60,0.1); border: 1px solid #e74c3c; border-radius: 8px; color: #e74c3c; margin-bottom: 20px; text-align: center;">
            <strong>⚠️ Kho chưa có nguyên liệu nào!</strong><br>
            Vui lòng thêm "Nguyên Liệu Mới" ở tab <strong>Kho Nguyên Liệu</strong> trước khi lập phiếu nhập kho.
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px;">
            <div class="form-group">
              <label>Nhà Cung Cấp</label>
              <input v-model="invoiceForm.supplier" placeholder="VD: Công ty TNHH Thực Phẩm A" class="g-form-control" />
            </div>
            <div class="form-group">
              <label>Ghi Chú</label>
              <input v-model="invoiceForm.note" placeholder="VD: Nhập hàng tuần 1 tháng 11" class="g-form-control" />
            </div>
          </div>
          
          <h4>Danh Sách Nguyên Liệu Nhập</h4>
          <table class="g-table" style="margin-top: 10px; margin-bottom: 20px;">
            <thead>
              <tr>
                <th>Nguyên liệu</th>
                <th>Số lượng</th>
                <th>Đơn giá (VNĐ)</th>
                <th>Thành tiền</th>
                <th>Hạn SD (Tùy chọn)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in invoiceForm.items" :key="index">
                <td>
                  <select v-model="item.ingredientId" class="g-form-control" @change="onInvoiceItemIngChange(item)">
                    <option value="" disabled>-- Chọn --</option>
                    <option v-for="ing in ingredients" :key="ing.id" :value="ing.id">{{ ing.name }}</option>
                  </select>
                </td>
                <td>
                  <input v-model="item.quantity" type="number" step="0.1" class="g-form-control" style="width: 80px;" />
                </td>
                <td>
                  <input v-model="item.unitPrice" type="number" step="500" class="g-form-control" style="width: 120px;" />
                </td>
                <td style="color: #e74c3c; font-weight: bold;">{{ ((item.quantity || 0) * (item.unitPrice || 0)).toLocaleString() }}đ</td>
                <td>
                  <input v-model="item.expirationDate" type="date" class="g-form-control" style="width: 140px;" />
                </td>
                <td>
                  <button @click="invoiceForm.items.splice(index, 1)" class="btn-sm btn-delete">🗑</button>
                </td>
              </tr>
            </tbody>
          </table>
          <button @click="invoiceForm.items.push({ ingredientId: '', quantity: 1, unitPrice: 0, expirationDate: '' })" class="btn-sm btn-secondary" style="margin-bottom: 20px;">➕ Thêm dòng</button>
          
          <div style="text-align: right; font-size: 1.2rem; font-weight: bold; margin-bottom: 20px;">
            Tổng Tiền: <span style="color: #e74c3c;">{{ calculateInvoiceTotal().toLocaleString() }}đ</span>
          </div>

          <div class="form-actions">
            <button @click="submitInvoice" class="g-btn-primary">✅ Xác Nhận Nhập Kho</button>
            <button @click="showCreateInvoiceModal = false" class="g-btn-secondary">Hủy</button>
          </div>
        </div>
      </div>
    </div>

    <!-- View Invoice Details Modal -->
    <div v-if="showInvoiceDetailsModal" class="modal-overlay" @click.self="showInvoiceDetailsModal = false">
      <div class="modal-content printable-area" style="max-width: 800px; width: 90%; background: #ffffff; color: #111;">
        <div class="modal-header hide-on-print">
          <h3>Chi Tiết Phiếu Nhập Kho #{{ selectedInvoiceId }}</h3>
          <button @click="showInvoiceDetailsModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body invoice-content">
          <div class="invoice-brand" style="text-align: center; border-bottom: 2px solid #111; padding-bottom: 20px; margin-bottom: 28px;">
             <h1 style="margin: 0; font-size: 2rem; color: #111;">PHIẾU NHẬP KHO</h1>
             <p style="margin: 4px 0 0 0; color: #777; font-size: 0.9rem;">Hệ Thống Quản Lý Kho Mộc Vị</p>
          </div>
          <div class="invoice-meta" style="display: flex; justify-content: space-between; margin-bottom: 28px;">
             <div class="meta-left">
                <p style="margin: 6px 0; color: #444;"><strong>Mã phiếu:</strong> <span style="background: #111; color: #00b894; padding: 4px 10px; border-radius: 4px; font-family: monospace; font-weight: 800;">#{{ selectedInvoiceId }}</span></p>
                <p style="margin: 6px 0; color: #444;"><strong>Ngày nhập:</strong> {{ selectedInvoice ? new Date(selectedInvoice.importDate).toLocaleString('vi-VN') : '---' }}</p>
             </div>
             <div class="meta-right" style="text-align: right;">
                <p style="margin: 6px 0; color: #444;"><strong>Nhà cung cấp:</strong> {{ selectedInvoice?.supplier || '---' }}</p>
                <p style="margin: 6px 0; color: #444;"><strong>Ghi chú:</strong> {{ selectedInvoice?.note || '---' }}</p>
             </div>
          </div>
          <table class="print-table g-table" style="width: 100%; margin-bottom: 28px; border-collapse: collapse;">
            <thead>
              <tr>
                <th style="background: #111; color: white; padding: 12px; font-size: 0.88rem; text-transform: uppercase;">Nguyên Liệu</th>
                <th style="background: #111; color: white; padding: 12px; font-size: 0.88rem; text-transform: uppercase;">Số Lượng</th>
                <th style="background: #111; color: white; padding: 12px; font-size: 0.88rem; text-transform: uppercase;">Đơn Giá</th>
                <th style="background: #111; color: white; padding: 12px; font-size: 0.88rem; text-transform: uppercase;">Thành Tiền</th>
                <th style="background: #111; color: white; padding: 12px; font-size: 0.88rem; text-transform: uppercase;">Hạn SD</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="b in invoiceDetails" :key="b.id">
                <td style="border-bottom: 1px solid #ddd; padding: 14px 12px; color: #333;">{{ b.ingredient?.name }}</td>
                <td style="border-bottom: 1px solid #ddd; padding: 14px 12px; color: #333;">{{ b.quantity }} {{ b.ingredient?.unit }}</td>
                <td style="border-bottom: 1px solid #ddd; padding: 14px 12px; color: #333;">{{ b.unitPrice?.toLocaleString() }}đ</td>
                <td style="border-bottom: 1px solid #ddd; padding: 14px 12px; color: #e74c3c; font-weight: bold;">{{ ((b.quantity || 0) * (b.unitPrice || 0)).toLocaleString() }}đ</td>
                <td style="border-bottom: 1px solid #ddd; padding: 14px 12px; color: #333;">{{ b.expirationDate ? new Date(b.expirationDate).toLocaleDateString('vi-VN') : '---' }}</td>
              </tr>
            </tbody>
          </table>
          <div class="invoice-total" style="display: flex; justify-content: flex-end; margin-top: 20px;">
             <table class="total-table" style="min-width: 300px; border-collapse: collapse;">
                <tr class="total-row" style="border-top: 2px solid #111; font-size: 1.15rem; color: #c0392b; font-weight: 900;">
                   <td style="padding: 12px 10px;">TỔNG TIỀN:</td>
                   <td style="text-align: right; padding: 12px 10px;">{{ selectedInvoice?.totalAmount?.toLocaleString() || 0 }} đ</td>
                </tr>
             </table>
          </div>
          
          <div class="invoice-footer" style="text-align: center; margin-top: 40px; border-top: 1px solid #eee; padding-top: 20px;">
             <p class="system-msg" style="font-size: 0.8rem; color: #999; margin: 0;">Phiếu nhập kho được tạo tự động bởi hệ thống Quản Lý Nhà Hàng MỘC VỊ</p>
          </div>
        </div>
        <div class="modal-actions hide-on-print" style="padding: 20px; background: #f8f9fa; text-align: center; border-top: 1px solid #eee;">
           <button @click="exportInvoiceToPDF" class="g-btn-primary" style="padding: 12px 24px; font-size: 1rem; font-weight: 800; cursor: pointer; border: none; border-radius: 8px;">📥 In Phiếu Nhập Kho</button>
        </div>
      </div>
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted } from 'vue';
import api from '@/services/api';

// Kiểm tra quyền để hiển thị Navbar phù hợp
const userRoles = computed(() => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    try {
      return JSON.parse(storedUser).roles || [];
    } catch (e) {
      return [];
    }
  }
  return [];
});
const isKitchenOnly = computed(() => {
  return userRoles.value.includes('ROLE_KITCHEN') && !userRoles.value.includes('ROLE_ADMIN') && !userRoles.value.includes('ROLE_MANAGER');
});

const activeTab = ref('inventory');
const ingredients = ref([]);
const products = ref([]);
const categories = ref([]);
const stats = ref({ total: 0, lowStock: 0, outOfStock: 0, expiringBatchesCount: 0 });
const toastMsg = ref('');

// Tab 1 State
const isEditingIng = ref(false);
const editingIngId = ref(null);
const ingForm = ref({ name: '', unit: '', minStock: 5.0, unitPrice: 0, shelfLifeDays: 30, image: '' });

// Batch State
const showRestockModal = ref(false);
const selectedIngForRestock = ref(null);
const batchForm = ref({ quantity: 0, unitPrice: 0, expirationDate: '' });

const showBatchesModal = ref(false);
const selectedBatches = ref([]);

const isExpiring = (dateStr) => {
  if (!dateStr) return false;
  const d = new Date(dateStr);
  const now = new Date();
  const diffTime = d - now;
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays <= 3; // <= 3 days is considered expiring
};

// Tab 2 State
const searchProduct = ref('');
const selectedProduct = ref(null);
const currentRecipes = ref([]);
const newRecipe = ref({ ingredientId: '', amount: '' });

// AI Forecast State
const showForecastModal = ref(false);
const isForecasting = ref(false);
const forecastResults = ref([]);
const forecastError = ref('');

// Invoices State
const invoices = ref([]);
const invoiceSearchQuery = ref('');
const invoiceTimeFilter = ref('all');
const showCreateInvoiceModal = ref(false);
const invoiceForm = ref({ supplier: '', note: '', items: [] });
const showInvoiceDetailsModal = ref(false);
const selectedInvoiceId = ref(null);
const invoiceDetails = ref([]);

const selectedInvoice = computed(() => invoices.value.find(i => i.id === selectedInvoiceId.value));

const filteredInvoices = computed(() => {
  let result = invoices.value;
  if (invoiceTimeFilter.value !== 'all') {
    const now = new Date();
    result = result.filter(inv => {
      if (!inv.importDate) return true;
      const invDate = new Date(inv.importDate);
      if (invoiceTimeFilter.value === 'today') return invDate.toDateString() === now.toDateString();
      if (invoiceTimeFilter.value === 'week') {
        const diffDays = Math.ceil(Math.abs(now - invDate) / (1000 * 60 * 60 * 24));
        return diffDays <= 7;
      }
      if (invoiceTimeFilter.value === 'month') return invDate.getMonth() === now.getMonth() && invDate.getFullYear() === now.getFullYear();
      if (invoiceTimeFilter.value === 'year') return invDate.getFullYear() === now.getFullYear();
      return true;
    });
  }
  if (invoiceSearchQuery.value.trim()) {
    const q = invoiceSearchQuery.value.toLowerCase().trim();
    result = result.filter(inv => 
      String(inv.id).includes(q) || 
      (inv.supplier && inv.supplier.toLowerCase().includes(q))
    );
  }
  return result;
});

const resetInvoiceFilters = () => {
  invoiceSearchQuery.value = '';
  invoiceTimeFilter.value = 'all';
};

const exportInvoiceToPDF = () => {
  window.print();
};

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

// === CHUNG ===
const showToast = (msg) => { toastMsg.value = msg; setTimeout(() => toastMsg.value = '', 3000); };

const loadData = async () => {
  try {
    const resIng = await api.get('http://localhost:8080/api/admin/ingredients', configHeader());
    ingredients.value = resIng.data;
    
    const resStats = await api.get('http://localhost:8080/api/admin/ingredients/stats', configHeader());
    stats.value = resStats.data;

    const resProd = await api.get('http://localhost:8080/api/products');
    products.value = resProd.data;

    const resCat = await api.get('http://localhost:8080/api/categories');
    categories.value = resCat.data;
    
    await fetchInvoices();
  } catch (err) { console.error(err); }
};

const fetchInvoices = async () => {
  try {
    const res = await api.get('http://localhost:8080/api/admin/import-invoices', configHeader());
    invoices.value = res.data;
  } catch (err) { console.error('Lỗi lấy danh sách hóa đơn:', err); }
};

// === TAB 1: INVENTORY ===
const startEditIng = (ing) => {
  isEditingIng.value = true;
  editingIngId.value = ing.id;
  ingForm.value = { name: ing.name, unit: ing.unit, minStock: ing.minStock, unitPrice: ing.unitPrice || 0, shelfLifeDays: ing.shelfLifeDays || 30, image: ing.image || '' };
};

const cancelEditIng = () => {
  isEditingIng.value = false;
  editingIngId.value = null;
  ingForm.value = { name: '', unit: '', minStock: 5.0, unitPrice: 0, shelfLifeDays: 30, image: '' };
};

const saveIngredient = async () => {
  if (!ingForm.value.name || !ingForm.value.unit) return alert('Nhập đủ Tên và Đơn vị!');
  try {
    if (isEditingIng.value) {
      await api.put(`http://localhost:8080/api/admin/ingredients/${editingIngId.value}`, ingForm.value, configHeader());
      showToast('✅ Đã cập nhật nguyên liệu!');
    } else {
      await api.post('http://localhost:8080/api/admin/ingredients', ingForm.value, configHeader());
      showToast('✅ Đã thêm nguyên liệu mới!');
    }
    cancelEditIng();
    loadData();
  } catch (err) { alert('Lỗi lưu nguyên liệu'); }
};

const deleteIngredient = async (id) => {
  if (!confirm('Xóa nguyên liệu này?')) return;
  try {
    await api.delete(`http://localhost:8080/api/admin/ingredients/${id}`, configHeader());
    showToast('✅ Đã xóa!');
    loadData();
  } catch (err) { alert('Không thể xóa vì nguyên liệu này đang có trong công thức!'); }
};

const openRestockModal = (ing) => {
  selectedIngForRestock.value = ing;
  batchForm.value = { quantity: 0, unitPrice: ing.unitPrice || 0, expirationDate: '' };
  showRestockModal.value = true;
};

const submitBatch = async () => {
  if (!batchForm.value.quantity || batchForm.value.quantity <= 0) return alert('Số lượng phải > 0');
  
  try {
    await api.post(`http://localhost:8080/api/admin/ingredients/${selectedIngForRestock.value.id}/batches`, batchForm.value, configHeader());
    showToast(`📦 Đã nhập lô mới thành công!`);
    showRestockModal.value = false;
    loadData();
  } catch (err) { alert('Lỗi nhập kho'); }
};

const viewBatches = async (id) => {
  try {
    const res = await api.get(`http://localhost:8080/api/admin/ingredients/${id}/batches`, configHeader());
    selectedBatches.value = res.data;
    showBatchesModal.value = true;
  } catch (err) { alert('Lỗi tải danh sách lô hàng'); }
};

const deleteBatch = async (batchId) => {
  if (!confirm('Bạn có chắc muốn xóa lô hàng này? (Dùng để loại bỏ các lô đã hết hạn hoặc sai lệch)')) return;
  try {
    await api.delete(`http://localhost:8080/api/admin/ingredients/batches/${batchId}`, configHeader());
    showToast('🗑️ Đã xóa lô hàng!');
    showBatchesModal.value = false;
    loadData();
  } catch (err) {
    alert('Không thể xóa lô hàng này!');
  }
};

// === TAB 2: RECIPES ===
const filteredProducts = computed(() => {
  if (!searchProduct.value) return products.value;
  return products.value.filter(p => p.name.toLowerCase().includes(searchProduct.value.toLowerCase()));
});

const selectProduct = async (prod) => {
  selectedProduct.value = prod;
  try {
    const res = await api.get(`http://localhost:8080/api/admin/recipes/product/${prod.id}`, configHeader());
    currentRecipes.value = res.data;
  } catch (err) { console.error('Lỗi lấy công thức', err); }
};

const addRecipe = async () => {
  if (!newRecipe.value.ingredientId || !newRecipe.value.amount) return alert('Nhập đủ nguyên liệu và số lượng!');
  const payload = {
    productId: selectedProduct.value.id,
    ingredientId: newRecipe.value.ingredientId,
    amountRequired: parseFloat(newRecipe.value.amount)
  };
  try {
    await api.post('http://localhost:8080/api/admin/recipes', payload, configHeader());
    showToast('🍳 Đã thêm nguyên liệu vào món!');
    newRecipe.value = { ingredientId: '', amount: '' };
    selectProduct(selectedProduct.value); // reload recipes for this product
  } catch (err) { alert('Lỗi thêm công thức'); }
};

const deleteRecipe = async (recipeId) => {
  if (!confirm('Xóa nguyên liệu này khỏi món?')) return;
  try {
    await api.delete(`http://localhost:8080/api/admin/recipes/${recipeId}`, configHeader());
    showToast('✅ Đã xóa!');
    selectProduct(selectedProduct.value);
  } catch (err) { alert('Lỗi xóa'); }
};

// === AI FORECAST ===
const analyzeInventory = async () => {
  showForecastModal.value = true;
  isForecasting.value = true;
  forecastError.value = '';
  forecastResults.value = [];
  
  try {
    const lowStockItems = ingredients.value.filter(i => (i.quantity || 0) <= (i.minStock || 0));
    if (lowStockItems.length === 0) {
      isForecasting.value = false;
      return;
    }
    
    const dataStr = lowStockItems.map(i => `- ${i.name}: Tồn kho hiện tại ${i.quantity || 0}${i.unit}, Mức tối thiểu yêu cầu: ${i.minStock}${i.unit}`).join('\n');
    
    const res = await api.post('/api/admin/ai/inventory', {
      message: dataStr,
    }, configHeader());
    
    let reply = res.data.reply || '';
    reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
    
    forecastResults.value = JSON.parse(reply);
  } catch (err) {
    forecastError.value = "Hệ thống AI không phản hồi hoặc trả về sai định dạng. Vui lòng thử lại!";
  } finally {
    isForecasting.value = false;
  }
};

const applyForecast = async (ingName, amount) => {
  const ing = ingredients.value.find(i => i.name.toLowerCase() === ingName.toLowerCase());
  if (!ing) return alert(`Không tìm thấy nguyên liệu "${ingName}" trong hệ thống!`);
  
  // Open restock modal and pre-fill amount
  selectedIngForRestock.value = ing;
  batchForm.value = { quantity: amount, unitPrice: ing.unitPrice || 0, expirationDate: '' };
  showForecastModal.value = false;
  showRestockModal.value = true;
};

// ================== HÓA ĐƠN NHẬP HÀNG ==================
const openCreateInvoiceModal = () => {
  invoiceForm.value = { supplier: '', note: '', items: [{ ingredientId: '', quantity: 1, unitPrice: 0, expirationDate: '' }] };
  showCreateInvoiceModal.value = true;
};

const onInvoiceItemIngChange = (item) => {
  const ing = ingredients.value.find(i => i.id === item.ingredientId);
  if (ing) {
    item.unitPrice = ing.unitPrice || 0;
  }
};

const calculateInvoiceTotal = () => {
  return invoiceForm.value.items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
};

const submitInvoice = async () => {
  const validItems = invoiceForm.value.items.filter(i => i.ingredientId && i.quantity > 0);
  if (validItems.length === 0) return alert('Vui lòng thêm ít nhất 1 nguyên liệu hợp lệ!');
  
  try {
    const payload = {
      supplier: invoiceForm.value.supplier,
      note: invoiceForm.value.note,
      items: validItems.map(i => ({
        ingredientId: i.ingredientId,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        expirationDate: i.expirationDate ? new Date(i.expirationDate).toISOString() : null
      }))
    };
    
    await api.post('http://localhost:8080/api/admin/import-invoices', payload, configHeader());
    showToast('📦 Đã nhập hàng thành công! Đã tạo phiếu lưu kho.');
    showCreateInvoiceModal.value = false;
    fetchInvoices();
    // Cập nhật lại kho
    const res = await api.get('http://localhost:8080/api/admin/ingredients', configHeader());
    ingredients.value = res.data;
  } catch (err) {
    alert('Lỗi tạo phiếu nhập kho!');
    console.error(err);
  }
};

const viewInvoiceDetails = async (id) => {
  selectedInvoiceId.value = id;
  try {
    const res = await api.get(`http://localhost:8080/api/admin/import-invoices/${id}`, configHeader());
    invoiceDetails.value = res.data;
    showInvoiceDetailsModal.value = true;
  } catch (err) { console.error('Lỗi lấy chi tiết HD:', err); }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

/* Stats */
.stats-row { display: flex; gap: 20px; margin-bottom: 30px; }
.stat-card {
  flex: 1; background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); padding: 20px; display: flex; align-items: center; gap: 16px;
}
.stat-warn { border-color: rgba(243,156,18,0.3); }
.stat-warn .stat-icon { color: #f39c12; background: rgba(243,156,18,0.1); }
.stat-warn .stat-value { color: #f39c12; }
.stat-danger { border-color: rgba(231,76,60,0.3); }
.stat-danger .stat-icon { color: #e74c3c; background: rgba(231,76,60,0.1); }
.stat-danger .stat-value { color: #e74c3c; }
.stat-icon { font-size: 2rem; width: 60px; height: 60px; border-radius: 12px; background: var(--primary-glow); color: var(--primary); display: flex; align-items: center; justify-content: center; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 1.8rem; font-weight: 900; line-height: 1.2; }
.stat-label { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; }

/* Tabs */
.tabs-header { display: flex; gap: 10px; border-bottom: 1px solid var(--border); margin-bottom: 24px; padding-bottom: 12px; }
.tab-btn { background: transparent; border: 1px solid transparent; color: var(--text-muted); padding: 10px 20px; border-radius: 8px; font-weight: 700; font-size: 1rem; cursor: pointer; transition: 0.3s; }
.tab-btn:hover { color: var(--primary); }
.tab-btn.active { background: var(--primary-glow); color: var(--primary); border: 1px solid var(--border); }

.content-grid { display: grid; grid-template-columns: 350px 1fr; gap: 24px; }
.form-card, .table-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 24px; }
.form-card.edit-mode { border-color: var(--primary); box-shadow: 0 0 20px var(--primary-glow); }
.form-card h3, .table-card h3 { margin: 0 0 20px 0; border-bottom: 1px solid var(--border-light); padding-bottom: 10px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 6px; font-weight: 600; text-transform: uppercase; }
.form-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-cancel { background: transparent; border: 1px solid var(--border-light); color: var(--text-muted); padding: 10px; border-radius: var(--radius-md); font-weight: 600; cursor: pointer; }

/* Table Elements */
.img-thumb-sm { width: 36px; height: 36px; border-radius: 6px; object-fit: cover; border: 1px solid var(--border); }
.qty-col { font-weight: 600; color: var(--text-muted); }
.qty-val { font-size: 1.1rem; color: var(--primary); font-weight: 800; }
.action-buttons { display: flex; gap: 6px; }
.btn-edit { background: rgba(52,152,219,0.15); border: 1px solid rgba(52,152,219,0.3); color: #3498db; padding: 6px 10px; border-radius: 4px; cursor: pointer; }
.restock-group { display: flex; gap: 6px; }
.restock-input { width: 70px; background: var(--bg-input); border: 1px solid var(--border); color: white; padding: 6px; border-radius: 4px; text-align: center; }
.btn-restock { background: var(--primary); color: #000; border: none; padding: 6px 12px; border-radius: 4px; font-weight: bold; cursor: pointer; }

/* Recipes Layout */
.recipe-layout { display: grid; grid-template-columns: 350px 1fr; gap: 24px; height: 600px; }
.recipe-sidebar { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); display: flex; flex-direction: column; overflow: hidden; }
.search-box { padding: 16px; border-bottom: 1px solid var(--border-light); }
.product-list { flex: 1; overflow-y: auto; }
.product-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-bottom: 1px solid var(--border-light); cursor: pointer; transition: 0.2s; }
.product-item:hover { background: var(--bg-hover); }
.product-item.active { background: var(--primary-glow); border-left: 4px solid var(--primary); }
.prod-thumb { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; }
.product-item h4 { margin: 0; font-size: 0.95rem; color: var(--text-heading); }
.product-item span { font-size: 0.75rem; color: var(--text-muted); }

.recipe-main { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 24px; display: flex; flex-direction: column; }
.recipe-header { border-bottom: 1px dashed var(--border); padding-bottom: 16px; margin-bottom: 20px; }
.recipe-header h2 { margin: 0; font-size: 1.5rem; color: var(--text-heading); }
.recipe-header h2 span { color: var(--primary); }
.add-recipe-box { display: flex; gap: 10px; margin-bottom: 24px; background: rgba(0,0,0,0.2); padding: 16px; border-radius: 10px; border: 1px solid var(--border-light); }
.amount-cell { color: #f39c12; font-weight: bold; font-size: 1.1rem; }
.est-cell { color: var(--primary); font-weight: bold; font-size: 1.1rem; }
.empty-selection { display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--bg-card); border: 1px dashed var(--border); border-radius: var(--radius-lg); color: var(--text-muted); }
.empty-selection .icon { font-size: 4rem; margin-bottom: 16px; }

/* Toast */
.toast-notification { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--bg-card); color: var(--primary); padding: 14px 28px; border-radius: 30px; border: 1px solid var(--primary); box-shadow: 0 0 30px rgba(0,212,170,0.3); font-weight: 700; z-index: 1000; }

/* AI Forecast Modal */
.btn-ai-forecast { background: linear-gradient(135deg, #9b59b6, #8e44ad); color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 15px rgba(155, 89, 182, 0.4); transition: 0.3s; }
.btn-ai-forecast:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(155, 89, 182, 0.6); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); z-index: 999; display: flex; align-items: center; justify-content: center; }
.forecast-box { background: var(--bg-card); padding: 0; border-radius: 12px; width: 100%; max-width: 700px; max-height: 85vh; display: flex; flex-direction: column; overflow: hidden; border: 1px solid #9b59b6; box-shadow: 0 10px 30px rgba(0,0,0,0.8); }
.forecast-header { background: rgba(155,89,182,0.1); padding: 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(155,89,182,0.3); }
.forecast-header h3 { margin: 0; color: #9b59b6; font-size: 1.3rem; }
.btn-close-modal { background: transparent; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; }
.btn-close-modal:hover { color: #e74c3c; }

.forecast-body { padding: 24px; overflow-y: auto; }
.forecasting-loader { text-align: center; padding: 40px; color: var(--text-muted); }
.forecasting-loader .pulse { font-size: 3.5rem; animation: pulse-ai 1s infinite alternate; margin-bottom: 15px; }
.forecast-desc { font-style: italic; color: var(--text-muted); margin-bottom: 20px; font-size: 0.95rem; }
.forecast-item { background: var(--bg-input); padding: 18px; border-radius: 10px; margin-bottom: 15px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; gap: 15px; }
.forecast-info { flex: 1; }
.forecast-info h4 { margin: 0 0 8px 0; color: var(--text-heading); font-size: 1.1rem; }
.forecast-reason { font-size: 0.85rem; color: #f39c12; line-height: 1.4; display: block; }
.forecast-action { text-align: right; min-width: 140px; }
.forecast-qty { display: block; margin-bottom: 10px; font-size: 0.95rem; color: var(--text-muted); }
@keyframes pulse-ai { from { transform: scale(1); opacity: 0.7; } to { transform: scale(1.2); opacity: 1; } }

.modal-content {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h3 { margin: 0; color: var(--primary); font-size: 1.2rem; }
.btn-close {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 1.5rem;
  cursor: pointer;
  transition: 0.3s;
}
.btn-close:hover { color: #e74c3c; transform: scale(1.1); }
.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

@media print {
  @page {
    size: A4 portrait;
    margin: 10mm;
  }
  * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  body * { visibility: hidden !important; }
  .printable-area,
  .printable-area * { visibility: visible !important; }
  .printable-area {
    position: fixed !important;
    inset: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    background: white !important;
    color: #111 !important;
    padding: 0 !important;
    z-index: 99999 !important;
  }
  .invoice-content { padding: 16px !important; }
  .invoice-brand { padding-bottom: 10px !important; margin-bottom: 12px !important; }
  .invoice-brand h1 { font-size: 1.3rem !important; }
  .invoice-meta { margin-bottom: 12px !important; }
  .print-table th { padding: 7px 8px !important; font-size: 0.78rem !important; }
  .print-table td { padding: 7px 8px !important; font-size: 0.82rem !important; }
  .invoice-total { margin-top: 10px !important; }
  .total-table td { padding: 4px 8px !important; font-size: 0.88rem !important; }
  .invoice-footer { margin-top: 16px !important; padding-top: 10px !important; }
  .hide-on-print { display: none !important; }
}
</style>

