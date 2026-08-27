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
          <h3 v-if="!isEditMode"> Thêm Danh Mục Mới</h3>
          <h3 v-else> Sửa Danh Mục</h3>
          <div class="form-group">
            <label>Tên danh mục (*)</label>
            <input v-model="newCategory.name" placeholder="VD: Món Tráng Miệng..." class="g-form-control" />
          </div>
          <button v-if="!isEditMode" @click="handleAdd" class="g-btn-primary" style="width: 100%; margin-top: 8px;">
             Thêm Danh Mục
          </button>
          <div v-else style="display: flex; gap: 10px; margin-top: 8px;">
            <button @click="handleUpdate" class="g-btn-primary" style="flex: 1;"> Cập Nhật</button>
            <button @click="cancelEdit" class="g-btn-secondary" style="flex: 1;"> Hủy</button>
          </div>
        </div>

        <!-- Bảng danh sách -->
        <div class="table-card">
          <h3> Danh Sách Danh Mục <span class="count-chip">{{ categories.length }}</span></h3>
          <div v-if="categories.length" class="category-columns">
            <table v-for="(column, columnIndex) in categoryColumns" :key="columnIndex" class="g-table">
              <thead><tr><th>ID</th><th>Tên Danh Mục</th><th>Hành động</th></tr></thead>
              <tbody>
                <tr v-for="c in column" :key="c.id">
                  <td><span class="id-badge">#{{ c.id }}</span></td>
                  <td><strong class="category-name">{{ c.name }}</strong></td>
                  <td><div class="row-actions">
                    <button @click="startEdit(c)" class="g-btn-primary icon-action" title="Sửa"><UiIcon name="edit" /></button>
                    <button @click="handleDelete(c.id)" class="g-btn-danger icon-action" title="Xóa"><UiIcon name="trash" /></button>
                  </div></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-row">Chưa có danh mục nào.</div>
          <nav v-if="totalPages > 1" class="pagination" aria-label="Phân trang danh mục">
            <button :disabled="currentPage === 1" @click="currentPage--">Trước</button>
            <button v-for="page in visiblePages" :key="page" :class="{ active: page === currentPage }" @click="currentPage = page">{{ page }}</button>
            <button :disabled="currentPage === totalPages" @click="currentPage++">Sau</button>
          </nav>
        </div>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted, watch } from 'vue';
import api from '@/services/api';
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';

const categories = ref([]);
const newCategory = ref({ name: '' });
const isEditMode = ref(false);
const editingId = ref(null);
const currentPage = ref(1);
const pageSize = 20;
const toast = useToast();
const { confirmDialog } = useDialog();
const totalPages = computed(() => Math.max(1, Math.ceil(categories.value.length / pageSize)));
const pagedCategories = computed(() => categories.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize));
const categoryColumns = computed(() => [pagedCategories.value.slice(0, 10), pagedCategories.value.slice(10, 20)].filter(column => column.length));
const visiblePages = computed(() => {
  const from = Math.max(1, currentPage.value - 2);
  const to = Math.min(totalPages.value, currentPage.value + 2);
  return Array.from({ length: to - from + 1 }, (_, index) => from + index);
});
watch(totalPages, pages => { if (currentPage.value > pages) currentPage.value = pages; });

const fetchCategories = async () => {
  try {
    const res = await api.get('/api/categories');
    categories.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) { console.error('Lỗi:', error); toast.error('Không thể tải danh mục.'); }
};

const handleAdd = async () => {
  if (!newCategory.value.name) {
    toast.warning('Vui lòng nhập tên danh mục!'); return;
  }
  const token = sessionStorage.getItem('staff_token');
  try {
    await api.post('/api/categories', newCategory.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    toast.success('Thêm danh mục thành công!');
    newCategory.value.name = '';
    fetchCategories();
  } catch (error) { toast.error('Không thể thêm danh mục. Vui lòng kiểm tra quyền quản trị.'); }
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
  if (!newCategory.value.name) return toast.warning('Vui lòng nhập tên danh mục!');
  const token = sessionStorage.getItem('staff_token');
  try {
    await api.put(`/api/categories/${editingId.value}`, newCategory.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    toast.success('Cập nhật danh mục thành công!');
    cancelEdit();
    fetchCategories();
  } catch (error) { toast.error('Không thể cập nhật danh mục.'); }
};

const handleDelete = async (id) => {
  if (!await confirmDialog({ title: 'Xóa danh mục', message: 'Cần đảm bảo không có món ăn nào đang dùng danh mục này.', confirmLabel: 'Xóa', danger: true })) return;
  const token = sessionStorage.getItem('staff_token');
  try {
    await api.delete(`/api/categories/${id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    toast.success('Đã xóa danh mục.');
    fetchCategories();
  } catch (error) { toast.error('Không thể xóa vì có thể danh mục đang được sử dụng.'); }
};

onMounted(fetchCategories);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1500px; margin: 0 auto; padding: 36px 24px; }

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
  font-family: var(--font-primary); font-size: 0.9rem; font-weight: 600;
}
.category-name { color: var(--text-primary); font-weight: 600; font-size: 1rem; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }
.category-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; align-items: start; }
.category-columns .g-table { width: 100%; }
.row-actions { display: flex; gap: 8px; }
.icon-action { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; padding: 0; }
.pagination { display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.pagination button { min-width: 38px; min-height: 38px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); color: var(--text-primary); cursor: pointer; }
.pagination button.active { background: var(--primary); border-color: var(--primary); color: var(--color-on-primary); }
.pagination button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 1100px) { .content-grid { grid-template-columns: 1fr; } .category-columns { grid-template-columns: 1fr; } }
</style>
