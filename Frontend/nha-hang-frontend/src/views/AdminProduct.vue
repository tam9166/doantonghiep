<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Trị Thực Đơn</h1>
        <p class="page-subtitle">Thêm mới, cập nhật thông tin và báo hết món</p>
      </div>

      <div class="content-grid">
        <!-- FORM THÊM / SỬA MÓN -->
        <div class="form-card" :class="{ 'edit-mode': isEditing }">
          <h3>{{ isEditing ? ' Cập Nhật Món Ăn' : ' Thêm Món Ăn Mới' }}</h3>

          <div class="form-group">
            <label>Tên món ăn (*)</label>
            <input v-model="formData.name" placeholder="VD: Phở Bò Kobe..." class="g-form-control" />
          </div>

          <div class="form-group">
            <label>Danh mục (*)</label>
            <select v-model="formData.categoryId" class="g-form-control">
              <option value="" disabled>-- Chọn danh mục --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>Giá vốn (VNĐ) (*)</label>
            <input v-model.number="formData.costPrice" type="number" min="0" step="1000" placeholder="VD: 40000" class="g-form-control" />
          </div>

          <div class="form-group">
            <label>Giá bán (VNĐ) (*)</label>
            <input v-model="formData.price" type="number" placeholder="VD: 55000" class="g-form-control" />
            <small class="price-policy">Giá bán tối thiểu: {{ formatCurrency(minimumSalePrice) }}</small>
            <small v-if="priceValidationMessage" class="price-policy-error">{{ priceValidationMessage }}</small>
          </div>

          <div class="form-group">
            <label>Thuế suất (%)</label>
            <input v-model="formData.taxRate" type="number" placeholder="VD: 8" class="g-form-control" />
          </div>

          <div class="form-group">
            <label>Chế độ món</label>
            <select v-model="formData.dietType" class="g-form-control">
              <option value="MAN">Món mặn</option>
              <option value="CHAY">Món chay</option>
            </select>
          </div>

          <div class="form-group">
            <label>Cách chế biến</label>
            <select v-model="formData.cookingMethod" class="g-form-control">
              <option value="KHAC">Khác</option>
              <option value="NUONG">Nướng</option>
              <option value="HAP">Hấp</option>
              <option value="CHIEN">Chiên</option>
              <option value="XAO">Xào</option>
              <option value="LUOC">Luộc</option>
            </select>
          </div>

          <div class="form-group">
            <label>Độ cay (0-3)</label>
            <input v-model.number="formData.spicyLevel" min="0" max="3" type="number" class="g-form-control" />
          </div>

          <label class="signature-field">
            <input v-model="formData.isSignatureDish" type="checkbox" />
            Món đặc trưng của nhà hàng
          </label>

          <div class="form-group">
            <label>Trạng thái</label>
            <select v-model="formData.status" class="g-form-control">
              <option :value="true"> Đang bán (Còn hàng)</option>
              <option :value="false"> Hết món (Ngừng bán)</option>
            </select>
          </div>

          <div class="form-group">
            <label>Mô tả chi tiết</label>
            <textarea v-model="formData.description" placeholder="Nhập mô tả..." class="g-form-control" rows="3"></textarea>
          </div>

          <div class="form-group">
            <label>Link hình ảnh</label>
            <input v-model="formData.image" placeholder="URL ảnh món ăn..." class="g-form-control" />
          </div>

          <div class="form-actions">
            <button @click="saveProduct" class="g-btn-primary" style="width:100%">
              {{ isEditing ? ' Lưu Thay Đổi' : ' Thêm Vào Menu' }}
            </button>
            <button v-if="isEditing" @click="cancelEdit" class="btn-cancel">Hủy Cập Nhật</button>
          </div>
        </div>

        <!-- BẢNG DANH SÁCH MÓN ĂN -->
        <div class="table-card">
          <h3> Danh Sách Món Ăn <span class="count-chip">{{ products.length }}</span></h3>
          <div class="table-responsive">
            <table class="g-table">
              <thead>
                <tr>
                  <th>STT</th>
                  <th>Ảnh</th>
                  <th>Tên món</th>
                  <th>Danh mục</th>
                  <th>Giá Bán (VNĐ)</th>
                  <th>Thuế (%)</th>
                  <th>Giá Vốn (VNĐ)</th>
                  <th>Đánh giá</th>
                  <th>Trạng thái</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, index) in pagedProducts" :key="p.id" :class="{ 'row-disabled': p.status === false }">
                  <td class="index-cell">{{ pageStart + index + 1 }}</td>
                  <td>
                    <img :src="foodImage(p.image)" class="img-thumb" @error="replaceFoodImage" />
                  </td>
                  <td><strong class="product-name">{{ p.name }}</strong><div class="tag-summary"><span v-if="p.isSignatureDish"> Đặc trưng</span><span v-if="p.dietType === 'CHAY'"> Chay</span><span v-if="p.spicyLevel > 0"> {{ p.spicyLevel }}</span></div></td>
                  <td><span class="category-chip">{{ p.category ? p.category.name : 'Chưa phân loại' }}</span></td>
                  <td class="price-text">{{ p.price.toLocaleString() }}đ</td>
                  <td>{{ p.taxRate !== null && p.taxRate !== undefined ? p.taxRate + '%' : '8%' }}</td>
                  <td style="color: var(--primary); font-weight: bold;">{{ p.costPrice > 0 ? p.costPrice.toLocaleString() + 'đ' : 'N/A' }}</td>
                  <td><strong style="color: var(--color-tertiary);">{{ p.averageRating > 0 ? ' ' + p.averageRating : 'N/A' }}</strong></td>
                  <td>
                    <span :class="p.status === false ? 'g-badge g-badge-danger' : 'g-badge g-badge-success'">
                      {{ p.status === false ? 'Hết món' : 'Đang bán' }}
                    </span>
                  </td>
                  <td class="action-cell">
                    <div class="action-buttons">
                      <button @click="startEdit(p)" class="btn-edit" title="Sửa món" aria-label="Sửa món"><UiIcon name="edit" /></button>
                      <button @click="toggleStatus(p)" class="btn-toggle" :title="p.status === false ? 'Mở bán lại' : 'Báo hết món'" :aria-label="p.status === false ? 'Mở bán lại' : 'Báo hết món'">
                        <UiIcon :name="p.status === false ? 'play' : 'pause'" />
                      </button>
                      <button @click="handleDelete(p.id)" class="g-btn-danger" title="Xóa món" aria-label="Xóa món"><UiIcon name="trash" /></button>
                    </div>
                  </td>
                </tr>
                <tr v-if="pagedProducts.length === 0">
                  <td colspan="10" class="empty-row">Chưa có món ăn nào.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="products.length" class="pagination-bar">
            <span class="pagination-summary">Hiển thị {{ pageStart + 1 }}–{{ pageEnd }} / {{ products.length }} món</span>
            <nav class="pagination" aria-label="Phân trang danh sách món">
              <button class="page-button" :disabled="currentPage === 1" @click="currentPage--">‹</button>
              <button v-for="page in visiblePages" :key="page" class="page-button" :class="{ active: page === currentPage }" @click="currentPage = page">{{ page }}</button>
              <button class="page-button" :disabled="currentPage === totalPages" @click="currentPage++">›</button>
            </nav>
          </div>
        </div>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted } from 'vue';
import api from '@/services/api';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';
import { getApiErrorMessage } from '@/services/errorMessage';

const products = ref([]);
const currentPage = ref(1);
const pageSize = 10;
const totalPages = computed(() => Math.max(1, Math.ceil(products.value.length / pageSize)));
const pageStart = computed(() => (currentPage.value - 1) * pageSize);
const pageEnd = computed(() => Math.min(pageStart.value + pageSize, products.value.length));
const pagedProducts = computed(() => products.value.slice(pageStart.value, pageEnd.value));
const visiblePages = computed(() => {
  const total = totalPages.value;
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  let start = Math.max(1, currentPage.value - 2);
  let end = Math.min(total, start + 4);
  start = Math.max(1, end - 4);
  return Array.from({ length: end - start + 1 }, (_, i) => start + i);
});
const categories = ref([]);
const isEditing = ref(false);
const editingId = ref(null);
const toast = useToast();
const { confirmDialog } = useDialog();

const formData = ref({
  name: '', price: '', costPrice: 0, description: '', image: '', categoryId: '', status: true, taxRate: 8,
  dietType: 'MAN', cookingMethod: 'KHAC', spicyLevel: 0, isSignatureDish: false
});

const formatCurrency = value => `${Math.ceil(Number(value || 0)).toLocaleString('vi-VN')}đ`;
const minimumSalePriceForCost = cost => {
  const normalized = Number(cost || 0);
  if (normalized < 100_000) return normalized * 1.15;
  if (normalized < 1_000_000) return normalized * 1.10;
  return normalized * 1.05;
};
const minimumSalePrice = computed(() => minimumSalePriceForCost(formData.value.costPrice));
const priceValidationMessage = computed(() => {
  const price = Number(formData.value.price || 0);
  const cost = Number(formData.value.costPrice || 0);
  const minimum = minimumSalePriceForCost(cost);
  if (price <= 0 || price >= minimum) return '';
  const tier = cost < 100_000 ? '15%' : cost < 1_000_000 ? '10%' : '5%';
  return `Giá bán phải đạt tối thiểu ${formatCurrency(minimum)} theo giá vốn ${formatCurrency(cost)} và biên lợi nhuận ${tier}.`;
});

const getAuthConfig = () => {
  const token = sessionStorage.getItem('staff_token');
  return token ? { headers: { 'Authorization': `Bearer ${token}` } } : {};
};

const fetchProducts = async () => {
  try {
    const res = await api.get('/api/admin/products', getAuthConfig());
    products.value = res.data;
    currentPage.value = Math.min(currentPage.value, totalPages.value);
  } catch (error) { console.error('Lỗi lấy sản phẩm', error); }
};

const fetchCategories = async () => {
  try {
    const res = await api.get('/api/categories');
    categories.value = res.data;
  } catch (error) { console.error('Lỗi lấy danh mục', error); }
};

const startEdit = (product) => {
  isEditing.value = true;
  editingId.value = product.id;
  formData.value = {
    name: product.name,
    price: product.price,
    costPrice: product.costPrice || 0,
    description: product.description || '',
    image: product.image || '',
    categoryId: product.category ? product.category.id : '',
    status: product.status !== false,
    taxRate: product.taxRate !== null && product.taxRate !== undefined ? product.taxRate : 8,
    dietType: product.dietType || 'MAN',
    cookingMethod: product.cookingMethod || 'KHAC',
    spicyLevel: product.spicyLevel ?? 0,
    isSignatureDish: product.isSignatureDish === true
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const cancelEdit = () => {
  isEditing.value = false;
  editingId.value = null;
  formData.value = { name: '', price: '', costPrice: 0, description: '', image: '', categoryId: '', status: true, taxRate: 8, dietType: 'MAN', cookingMethod: 'KHAC', spicyLevel: 0, isSignatureDish: false };
};

const saveProduct = async () => {
  if (!formData.value.name || !formData.value.price || !formData.value.categoryId) {
    toast.warning('Vui lòng nhập đủ tên, giá và danh mục.'); return;
  }
  if (priceValidationMessage.value) {
    toast.warning(priceValidationMessage.value); return;
  }

  const payload = {
    name: formData.value.name,
    price: formData.value.price,
    costPrice: formData.value.costPrice || 0,
    description: formData.value.description,
    image: formData.value.image,
    status: formData.value.status,
    taxRate: formData.value.taxRate,
    dietType: formData.value.dietType,
    cookingMethod: formData.value.cookingMethod,
    spicyLevel: formData.value.spicyLevel,
    isSignatureDish: formData.value.isSignatureDish,
    category: { id: formData.value.categoryId }
  };

  try {
    if (isEditing.value) {
      await api.put(`/api/admin/products/${editingId.value}`, payload, getAuthConfig());
      toast.success('Cập nhật món thành công.');
    } else {
      await api.post('/api/admin/products', payload, getAuthConfig());
      toast.success('Thêm món thành công.');
    }
    cancelEdit();
    await fetchProducts();
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Không thể lưu món. Vui lòng kiểm tra quyền quản trị.'));
  }
};

const toggleStatus = async (product) => {
  const newStatus = product.status === false ? true : false;
  const payload = { ...product, status: newStatus, category: product.category ? { id: product.category.id } : null };
  try {
    await api.put(`/api/admin/products/${product.id}`, payload, getAuthConfig());
    await fetchProducts();
  } catch (error) { toast.error(getApiErrorMessage(error, 'Không thể cập nhật trạng thái món.')); }
};

const handleDelete = async (id) => {
  if (!await confirmDialog({ title: 'Xóa món', message: 'Bạn có chắc muốn xóa món này?', confirmLabel: 'Xóa', danger: true })) return;
  try {
    await api.delete(`/api/admin/products/${id}`, getAuthConfig());
    toast.success('Đã xóa món.');
    await fetchProducts();
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Món này đã có trong hóa đơn, không thể xóa. Hãy dùng "Báo Hết" thay vì xóa.'));
  }
};

onMounted(() => {
  fetchProducts();
  fetchCategories();
});
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { width: 100%; max-width: 1600px; margin: 0 auto; padding: 36px 24px; box-sizing: border-box; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

.content-grid { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 24px; }

/* Form Card */
.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
  height: fit-content;
  transition: var(--transition);
}
.form-card.edit-mode {
  border-color: var(--primary);
  box-shadow: var(--shadow-md), 0 0 30px var(--primary-glow);
}
.form-card h3 {
  margin: 0 0 24px 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-heading);
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block;
  font-size: 0.83rem;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 7px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.price-policy { display: block; margin-top: 6px; color: var(--text-secondary); font-weight: 750; }
.price-policy-error { display: block; margin-top: 5px; color: var(--danger); font-weight: 850; line-height: 1.35; }
.signature-field { display: flex; align-items: center; gap: 8px; margin: 0 0 16px; color: var(--text-secondary); font-weight: 600; cursor: pointer; }
.tag-summary { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 5px; font-size: 0.72rem; color: var(--text-muted); }
.tag-summary span { background: var(--bg-input); border-radius: 999px; padding: 2px 6px; }
.form-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-cancel {
  width: 100%;
  background: rgba(92,107,101,0.15);
  border: 1px solid var(--border-light);
  color: var(--text-muted);
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
  font-family: inherit;
  transition: var(--transition);
}
.btn-cancel:hover { background: rgba(92,107,101,0.3); color: var(--text-secondary); }

/* Table Card */
.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
  overflow: visible;
}
.table-card h3 {
  margin: 0 0 24px 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-heading);
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  gap: 10px;
}
.count-chip {
  background: var(--primary-glow);
  color: var(--primary);
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 800;
}
.table-responsive { display: block; width: 100%; max-width: 100%; min-width: 0; overflow-x: auto !important; overflow-y: visible; }
.table-responsive .g-table { display: table; width: max(100%, 1320px); min-width: 1320px; table-layout: auto; }
.table-responsive .g-table th:nth-child(1) { width: 64px; }
.table-responsive .g-table th:nth-child(2) { width: 78px; }
.table-responsive .g-table th:nth-child(3) { width: 230px; }
.table-responsive .g-table th:nth-child(4) { width: 140px; }
.table-responsive .g-table th:nth-child(5), .table-responsive .g-table th:nth-child(7) { width: 120px; }
.table-responsive .g-table th:nth-child(6) { width: 80px; }
.table-responsive .g-table th:nth-child(8) { width: 95px; }
.table-responsive .g-table th:nth-child(9) { width: 120px; }
.table-responsive .g-table th:nth-child(10) { width: 220px; }
.index-cell { color: var(--text-muted); font-weight: 850; text-align: center; }

.img-thumb {
  width: 52px; height: 52px;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.product-name { color: var(--text-primary); font-weight: 600; }

.category-chip {
  background: var(--bg-input);
  color: var(--text-secondary);
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 500;
}

.price-text { color: var(--primary); font-weight: 700; }

.action-cell { min-width: 220px; opacity: 1 !important; }
.action-buttons { display: flex; min-width: 210px; align-items: center; justify-content: flex-end; gap: 6px; white-space: nowrap; }
.action-buttons button { min-width: 42px; min-height: 36px; display: inline-flex; align-items: center; justify-content: center; }
.action-buttons button .ui-icon { width: 17px; height: 17px; flex-basis: 17px; }
.btn-edit {
  background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--secondary) 30%, transparent);
  color: var(--secondary);
  padding: 7px 12px; border-radius: var(--radius-sm);
  cursor: pointer; font-size: 0.88rem; transition: var(--transition);
}
.btn-edit:hover { background: color-mix(in srgb, var(--secondary) 30%, transparent); }
.btn-toggle {
  background: color-mix(in srgb, var(--color-tertiary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  color: var(--color-tertiary);
  padding: 7px 12px; border-radius: var(--radius-sm);
  cursor: pointer; font-size: 0.88rem; transition: var(--transition);
}
.btn-toggle:hover { background: color-mix(in srgb, var(--color-tertiary) 30%, transparent); }

.row-disabled td:not(.action-cell) { opacity: 0.5; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }
.pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 20px; flex-wrap: wrap; }
.pagination-summary { color: var(--text-muted); font-size: 0.88rem; }
.pagination { display: flex; align-items: center; gap: 5px; }
.page-button { min-width: 34px; height: 34px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-secondary); cursor: pointer; font-weight: 700; }
.page-button:hover:not(:disabled), .page-button.active { background: var(--primary); border-color: var(--primary); color: #fff; }
.page-button:disabled { opacity: 0.4; cursor: not-allowed; }
@media (max-width: 1100px) { .content-grid { grid-template-columns: 1fr; } .form-card { max-width: 560px; } }
@media (max-width: 600px) { .admin-content { padding: 24px 12px; } .table-card { padding: 16px; } .pagination-bar { align-items: flex-start; flex-direction: column; } }
</style>
