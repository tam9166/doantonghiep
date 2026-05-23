<template>
  <div class="admin-wrapper">
    <header class="g-navbar">
      <div class="g-logo">
        <h2>FPOLY <span>RESTAURANT</span></h2>
        <p>Admin Dashboard</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Trị Thực Đơn</h1>
        <p class="page-subtitle">Thêm mới, cập nhật thông tin và báo hết món</p>
      </div>

      <div class="content-grid">
        <!-- FORM THÊM / SỬA MÓN -->
        <div class="form-card" :class="{ 'edit-mode': isEditing }">
          <h3>{{ isEditing ? '✏️ Cập Nhật Món Ăn' : '➕ Thêm Món Ăn Mới' }}</h3>

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
            <label>Trạng thái</label>
            <select v-model="formData.status" class="g-form-control">
              <option :value="true">✅ Đang bán (Còn hàng)</option>
              <option :value="false">❌ Hết món (Ngừng bán)</option>
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
              {{ isEditing ? '💾 Lưu Thay Đổi' : '➕ Thêm Vào Menu' }}
            </button>
            <button v-if="isEditing" @click="cancelEdit" class="btn-cancel">Hủy Cập Nhật</button>
          </div>
        </div>

        <!-- BẢNG DANH SÁCH MÓN ĂN -->
        <div class="table-card">
          <h3>📜 Danh Sách Món Ăn <span class="count-chip">{{ products.length }}</span></h3>
          <div class="table-responsive">
            <table class="g-table">
              <thead>
                <tr>
                  <th>Ảnh</th>
                  <th>Tên món</th>
                  <th>Danh mục</th>
                  <th>Giá (VNĐ)</th>
                  <th>Trạng thái</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in products" :key="p.id" :class="{ 'row-disabled': p.status === false }">
                  <td>
                    <img :src="p.image || 'https://placehold.co/60x60/0d1b2a/00d4aa?text=🍽'" class="img-thumb" />
                  </td>
                  <td><strong class="product-name">{{ p.name }}</strong></td>
                  <td><span class="category-chip">{{ p.category ? p.category.name : 'Chưa phân loại' }}</span></td>
                  <td class="price-text">{{ p.price.toLocaleString() }}đ</td>
                  <td>
                    <span :class="p.status === false ? 'g-badge g-badge-danger' : 'g-badge g-badge-success'">
                      {{ p.status === false ? 'Hết món' : 'Đang bán' }}
                    </span>
                  </td>
                  <td>
                    <div class="action-buttons">
                      <button @click="startEdit(p)" class="btn-edit">✏️</button>
                      <button @click="toggleStatus(p)" class="btn-toggle">
                        {{ p.status === false ? '▶' : '⏸' }}
                      </button>
                      <button @click="handleDelete(p.id)" class="g-btn-danger">🗑</button>
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
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const products = ref([]);
const categories = ref([]);
const isEditing = ref(false);
const editingId = ref(null);

const formData = ref({
  name: '', price: '', description: '', image: '', categoryId: '', status: true
});

const getAuthConfig = () => {
  const token = localStorage.getItem('token');
  return token ? { headers: { 'Authorization': `Bearer ${token}` } } : {};
};

const fetchProducts = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/products');
    products.value = res.data;
  } catch (error) { console.error('Lỗi lấy sản phẩm', error); }
};

const fetchCategories = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/categories');
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
    status: product.status !== false
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const cancelEdit = () => {
  isEditing.value = false;
  editingId.value = null;
  formData.value = { name: '', price: '', description: '', image: '', categoryId: '', status: true };
};

const saveProduct = async () => {
  if (!formData.value.name || !formData.value.price || !formData.value.categoryId) {
    alert('Vui lòng nhập đủ Tên, Giá và Chọn Danh Mục!'); return;
  }

  const payload = {
    name: formData.value.name,
    price: formData.value.price,
    description: formData.value.description,
    image: formData.value.image,
    status: formData.value.status,
    category: { id: formData.value.categoryId }
  };

  try {
    if (isEditing.value) {
      await axios.put(`http://localhost:8080/api/admin/products/${editingId.value}`, payload, getAuthConfig());
      alert('Cập nhật thành công!');
    } else {
      await axios.post('http://localhost:8080/api/admin/products', payload, getAuthConfig());
      alert('Thêm món thành công!');
    }
    cancelEdit();
    fetchProducts();
  } catch (error) {
    alert('Lỗi: ' + (error.response?.data?.message || 'Kiểm tra lại quyền Admin!'));
  }
};

const toggleStatus = async (product) => {
  const newStatus = product.status === false ? true : false;
  const payload = { ...product, status: newStatus, category: product.category ? { id: product.category.id } : null };
  try {
    await axios.put(`http://localhost:8080/api/admin/products/${product.id}`, payload, getAuthConfig());
    fetchProducts();
  } catch (error) { alert('Lỗi cập nhật trạng thái!'); }
};

const handleDelete = async (id) => {
  if (!confirm('Chắc chắn muốn xóa?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/admin/products/${id}`, getAuthConfig());
    alert('Đã xóa!');
    fetchProducts();
  } catch (error) {
    alert('Món này đã có trong hóa đơn, không thể xóa! Hãy dùng "Báo Hết" thay vì xóa.');
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

.content-grid { display: grid; grid-template-columns: 380px 1fr; gap: 28px; }

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
.form-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-cancel {
  width: 100%;
  background: rgba(90,122,138,0.15);
  border: 1px solid var(--border-light);
  color: var(--text-muted);
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
  font-family: inherit;
  transition: var(--transition);
}
.btn-cancel:hover { background: rgba(90,122,138,0.3); color: var(--text-secondary); }

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
.table-responsive { overflow-x: auto; }

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
  background: rgba(52,152,219,0.15);
  border: 1px solid rgba(52,152,219,0.3);
  color: #3498db;
  padding: 7px 12px; border-radius: var(--radius-sm);
  cursor: pointer; font-size: 0.88rem; transition: var(--transition);
}
.btn-edit:hover { background: rgba(52,152,219,0.3); }
.btn-toggle {
  background: rgba(241,196,15,0.15);
  border: 1px solid rgba(241,196,15,0.3);
  color: #f1c40f;
  padding: 7px 12px; border-radius: var(--radius-sm);
  cursor: pointer; font-size: 0.88rem; transition: var(--transition);
}
.btn-toggle:hover { background: rgba(241,196,15,0.3); }

.row-disabled td { opacity: 0.5; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }
</style>