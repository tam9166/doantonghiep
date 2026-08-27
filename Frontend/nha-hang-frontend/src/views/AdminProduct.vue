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
            <label>Giá tiền (VNĐ) (*)</label>
            <input v-model="formData.price" type="number" placeholder="VD: 55000" class="g-form-control" />
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
                <tr v-for="p in products" :key="p.id" :class="{ 'row-disabled': p.status === false }">
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
                  <td>
                    <div class="action-buttons">
                      <button @click="startEdit(p)" class="btn-edit"><UiIcon name="edit" /></button>
                      <button @click="toggleStatus(p)" class="btn-toggle">
                        <UiIcon :name="p.status === false ? 'play' : 'pause'" />
                      </button>
                      <button @click="handleDelete(p.id)" class="g-btn-danger"><UiIcon name="trash" /></button>
                    </div>
                  </td>
                </tr>
                <tr v-if="products.length === 0">
                  <td colspan="6" class="empty-row">Chưa có món ăn nào.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, onMounted } from 'vue';
import api from '@/services/api';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';
import { getApiErrorMessage } from '@/services/errorMessage';

const products = ref([]);
const categories = ref([]);
const isEditing = ref(false);
const editingId = ref(null);
const toast = useToast();
const { confirmDialog } = useDialog();

const formData = ref({
  name: '', price: '', description: '', image: '', categoryId: '', status: true, taxRate: 8,
  dietType: 'MAN', cookingMethod: 'KHAC', spicyLevel: 0, isSignatureDish: false
});

const getAuthConfig = () => {
  const token = sessionStorage.getItem('staff_token');
  return token ? { headers: { 'Authorization': `Bearer ${token}` } } : {};
};

const fetchProducts = async () => {
  try {
    const res = await api.get('/api/admin/products', getAuthConfig());
    products.value = res.data;
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
  formData.value = { name: '', price: '', description: '', image: '', categoryId: '', status: true, taxRate: 8, dietType: 'MAN', cookingMethod: 'KHAC', spicyLevel: 0, isSignatureDish: false };
};

const saveProduct = async () => {
  if (!formData.value.name || !formData.value.price || !formData.value.categoryId) {
    toast.warning('Vui lòng nhập đủ tên, giá và danh mục.'); return;
  }

  const payload = {
    name: formData.value.name,
    price: formData.value.price,
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
    fetchProducts();
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Không thể lưu món. Vui lòng kiểm tra quyền quản trị.'));
  }
};

const toggleStatus = async (product) => {
  const newStatus = product.status === false ? true : false;
  const payload = { ...product, status: newStatus, category: product.category ? { id: product.category.id } : null };
  try {
    await api.put(`/api/admin/products/${product.id}`, payload, getAuthConfig());
    fetchProducts();
  } catch (error) { toast.error(getApiErrorMessage(error, 'Không thể cập nhật trạng thái món.')); }
};

const handleDelete = async (id) => {
  if (!await confirmDialog({ title: 'Xóa món', message: 'Bạn có chắc muốn xóa món này?', confirmLabel: 'Xóa', danger: true })) return;
  try {
    await api.delete(`/api/admin/products/${id}`, getAuthConfig());
    toast.success('Đã xóa món.');
    fetchProducts();
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
.admin-content { max-width: 1300px; margin: 0 auto; padding: 36px 24px; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

.content-grid { display: grid; grid-template-columns: minmax(280px, 30%) minmax(0, 70%); gap: 28px; }

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
  overflow: hidden;
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
.table-responsive { width: 100%; overflow-x: auto; }
.table-responsive .g-table { width: 100%; min-width: 1200px; }

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

.action-buttons { display: flex; gap: 6px; }
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

.row-disabled td { opacity: 0.5; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }
</style>
