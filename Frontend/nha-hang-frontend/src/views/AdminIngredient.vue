<template>
  <div class="admin-wrapper">
    <header class="g-navbar" v-if="!isKitchenOnly">
      <div class="g-logo">
        <h2>Mộc Vị <span>RESTAURANT</span></h2>
        <p>Admin Dashboard</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/ingredients" class="active">Nguyên Liệu</router-link>
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/vouchers">Khuyến Mãi</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
        <router-link to="/admin/analytics">Thống Kê</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

    <header class="g-navbar" v-else>
      <div class="g-logo">
        <h2>Mộc Vị <span>RESTAURANT</span></h2>
        <p>Kitchen Inventory</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/kitchen" class="active">🔙 Quay Lại Bếp</router-link>
      </nav>
      <button @click="$router.push('/kitchen')" class="g-btn-nav">🏠 Về Bếp</button>
    </header>

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Lý Nguyên Liệu & Công Thức</h1>
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
        <button @click="analyzeInventory" class="btn-ai-forecast" style="margin-left:auto;">
           🤖 AI Dự Báo Nhập Kho
        </button>
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
                  <th>Nhập Kho</th>
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
                    <button @click="openRestockModal(ing)" class="btn-restock">📦 Nhập Lô Mới</button>
                    <button @click="viewBatches(ing.id)" class="btn-restock" style="background: #3498db; margin-left: 5px;">👀 Xem Các Lô</button>
                  </td>
                  <td>
                    <div class="action-buttons">
                      <button @click="startEditIng(ing)" class="btn-edit">✏️</button>
                      <button @click="deleteIngredient(ing.id)" class="g-btn-danger">🗑</button>
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
          <label>Đơn giá nhập (VNĐ)</label>
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
            </tr>
            <tr v-if="selectedBatches.length === 0">
              <td colspan="4" style="text-align: center; color: var(--text-muted)">Chưa có lô hàng nào!</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

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

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

// === CHUNG ===
const showToast = (msg) => { toastMsg.value = msg; setTimeout(() => toastMsg.value = '', 3000); };

const loadData = async () => {
  try {
    const resIng = await axios.get('http://localhost:8080/api/admin/ingredients', configHeader());
    ingredients.value = resIng.data;
    
    const resStats = await axios.get('http://localhost:8080/api/admin/ingredients/stats', configHeader());
    stats.value = resStats.data;

    const resProd = await axios.get('http://localhost:8080/api/products');
    products.value = resProd.data;
  } catch (err) { console.error(err); }
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
      await axios.put(`http://localhost:8080/api/admin/ingredients/${editingIngId.value}`, ingForm.value, configHeader());
      showToast('✅ Đã cập nhật nguyên liệu!');
    } else {
      await axios.post('http://localhost:8080/api/admin/ingredients', ingForm.value, configHeader());
      showToast('✅ Đã thêm nguyên liệu mới!');
    }
    cancelEditIng();
    loadData();
  } catch (err) { alert('Lỗi lưu nguyên liệu'); }
};

const deleteIngredient = async (id) => {
  if (!confirm('Xóa nguyên liệu này?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/admin/ingredients/${id}`, configHeader());
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
    await axios.post(`http://localhost:8080/api/admin/ingredients/${selectedIngForRestock.value.id}/batches`, batchForm.value, configHeader());
    showToast(`📦 Đã nhập lô mới thành công!`);
    showRestockModal.value = false;
    loadData();
  } catch (err) { alert('Lỗi nhập kho'); }
};

const viewBatches = async (id) => {
  try {
    const res = await axios.get(`http://localhost:8080/api/admin/ingredients/${id}/batches`, configHeader());
    selectedBatches.value = res.data;
    showBatchesModal.value = true;
  } catch (err) { alert('Lỗi tải danh sách lô hàng'); }
};

// === TAB 2: RECIPES ===
const filteredProducts = computed(() => {
  if (!searchProduct.value) return products.value;
  return products.value.filter(p => p.name.toLowerCase().includes(searchProduct.value.toLowerCase()));
});

const selectProduct = async (prod) => {
  selectedProduct.value = prod;
  try {
    const res = await axios.get(`http://localhost:8080/api/admin/recipes/product/${prod.id}`, configHeader());
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
    await axios.post('http://localhost:8080/api/admin/recipes', payload, configHeader());
    showToast('🍳 Đã thêm nguyên liệu vào món!');
    newRecipe.value = { ingredientId: '', amount: '' };
    selectProduct(selectedProduct.value); // reload recipes for this product
  } catch (err) { alert('Lỗi thêm công thức'); }
};

const deleteRecipe = async (recipeId) => {
  if (!confirm('Xóa nguyên liệu này khỏi món?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/admin/recipes/${recipeId}`, configHeader());
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
    
    const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
      message: dataStr,
      type: 'INVENTORY_FORECAST'
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
</style>

