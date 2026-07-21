<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Trị Danh Mục</h1>
        <p class="page-subtitle">Phân loại món ăn (VD: Món nướng, Lẩu, Đồ uống...)</p>
      </div>

      <div class="content-grid">
        <!-- Form thêm/sửa danh mục -->
        <div class="form-card">
          <h3 v-if="!isEditMode">➕ Thêm Danh Mục Mới</h3>
          <h3 v-else>✏️ Sửa Danh Mục</h3>
          <div class="form-group">
            <label>Tên danh mục (*)</label>
            <input v-model="newCategory.name" placeholder="VD: Món Tráng Miệng..." class="g-form-control" />
          </div>
          <button v-if="!isEditMode" @click="handleAdd" class="g-btn-primary" style="width: 100%; margin-top: 8px;">
            ➕ Thêm Danh Mục
          </button>
          <div v-else style="display: flex; gap: 10px; margin-top: 8px;">
            <button @click="handleUpdate" class="g-btn-primary" style="flex: 1;">💾 Cập Nhật</button>
            <button @click="cancelEdit" class="g-btn-secondary" style="flex: 1;">❌ Hủy</button>
          </div>
        </div>

        <!-- Bảng danh sách -->
        <div class="table-card">
          <h3>📂 Danh Sách Danh Mục <span class="count-chip">{{ categories.length }}</span></h3>
          <table class="g-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Tên Danh Mục</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in categories" :key="c.id">
                <td><span class="id-badge">#{{ c.id }}</span></td>
                <td><strong class="category-name">{{ c.name }}</strong></td>
                <td>
                  <div style="display: flex; gap: 8px;">
                    <button @click="startEdit(c)" class="g-btn-primary" style="padding: 4px 10px; font-size: 0.85rem;">✏️ Sửa</button>
                    <button @click="handleDelete(c.id)" class="g-btn-danger" style="padding: 4px 10px; font-size: 0.85rem;">🗑 Xóa</button>
                  </div>
                </td>
              </tr>
              <tr v-if="categories.length === 0">
                <td colspan="3" class="empty-row">Chưa có danh mục nào.</td>
              </tr>
            </tbody>
          </table>
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

const categories = ref([]);
const newCategory = ref({ name: '' });
const isEditMode = ref(false);
const editingId = ref(null);

const fetchCategories = async () => {
  try {
    const res = await api.get('http://localhost:8080/api/categories');
    categories.value = res.data;
  } catch (error) { console.error('Lỗi:', error); }
};

const handleAdd = async () => {
  if (!newCategory.value.name) {
    alert('Vui lòng nhập tên danh mục!'); return;
  }
  const token = localStorage.getItem('token');
  try {
    await api.post('http://localhost:8080/api/categories', newCategory.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert('Thêm thành công!');
    newCategory.value.name = '';
    fetchCategories();
  } catch (error) { alert('Lỗi! Kiểm tra quyền Admin.'); }
};

const startEdit = (c) => {
  isEditMode.value = true;
  editingId.value = c.id;
  newCategory.value.name = c.name;
};

const cancelEdit = () => {
  isEditMode.value = false;
  editingId.value = null;
  newCategory.value.name = '';
};

const handleUpdate = async () => {
  if (!newCategory.value.name) return alert('Vui lòng nhập tên danh mục!');
  const token = localStorage.getItem('token');
  try {
    await api.put(`http://localhost:8080/api/categories/${editingId.value}`, newCategory.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert('Cập nhật thành công!');
    cancelEdit();
    fetchCategories();
  } catch (error) { alert('Lỗi! Kiểm tra quyền Admin.'); }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa danh mục này? Lưu ý: Cần đảm bảo không có món ăn nào đang dùng danh mục này!')) return;
  const token = localStorage.getItem('token');
  try {
    await api.delete(`http://localhost:8080/api/categories/${id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert('Đã xóa!');
    fetchCategories();
  } catch (error) { alert('Không thể xóa! Có thể đang có món ăn thuộc danh mục này.'); }
};

onMounted(fetchCategories);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1100px; margin: 0 auto; padding: 36px 24px; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

.content-grid { display: grid; grid-template-columns: 320px 1fr; gap: 28px; }

.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
  height: fit-content;
}
.form-card h3 {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading);
  padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block; font-size: 0.83rem; font-weight: 600; color: var(--text-muted);
  margin-bottom: 7px; text-transform: uppercase; letter-spacing: 0.5px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
}
.table-card h3 {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading);
  padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
  display: flex; align-items: center; gap: 10px;
}
.count-chip {
  background: var(--primary-glow); color: var(--primary);
  padding: 3px 10px; border-radius: 20px; font-size: 0.85rem; font-weight: 800;
}

.id-badge {
  background: var(--bg-input); color: var(--text-muted);
  padding: 3px 10px; border-radius: 6px;
  font-family: monospace; font-size: 0.9rem; font-weight: 600;
}
.category-name { color: var(--text-primary); font-weight: 600; font-size: 1rem; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }
</style>
