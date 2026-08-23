<template>
  <AdminLayout>
  <div class="admin-wrapper luxury-theme">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">📝 Quản Lý Bài Đăng</h1>
        <p class="page-subtitle">Đăng tin tức nhà hàng, tuyển dụng nhân sự</p>
      </div>

      <div class="content-grid">
        <!-- FORM -->
        <div class="g-card form-card">
          <h3 class="card-title">{{ editingPost ? '✏️ Sửa Bài Đăng' : '➕ Tạo Bài Đăng Mới' }}</h3>

          <div class="form-group">
            <label>Tiêu đề *</label>
            <input v-model="form.title" type="text" class="g-form-control" placeholder="VD: Tuyển đầu bếp chính..." />
          </div>

          <div class="form-group">
            <label>Nội dung *</label>
            <textarea v-model="form.content" class="g-form-control" rows="5" placeholder="Nội dung chi tiết bài đăng..."></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Hình ảnh (URL)</label>
              <input v-model="form.image" type="text" class="g-form-control" placeholder="https://..." />
            </div>
            <div class="form-group">
              <label>Loại bài</label>
              <select v-model="form.type" class="g-form-control">
                <option value="NEWS">📰 Tin Tức</option>
                <option value="RECRUITMENT">💼 Tuyển Dụng</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="toggle-label">
              <input type="checkbox" v-model="form.active" />
              <span class="toggle-text">{{ form.active ? '🟢 Đang hiển thị' : '🔴 Đã ẩn' }}</span>
            </label>
          </div>

          <div class="form-actions">
            <button @click="submitPost" class="btn-save">
              {{ editingPost ? '💾 Cập Nhật' : '🚀 Đăng Bài' }}
            </button>
            <button v-if="editingPost" @click="cancelEdit" class="btn-cancel">✕ Hủy</button>
          </div>

          <!-- Preview -->
          <div v-if="form.image" class="preview-box">
            <p class="preview-label">Xem trước ảnh:</p>
            <img :src="form.image" class="preview-img" />
          </div>
        </div>

        <!-- TABLE -->
        <div class="g-card table-card">
          <div class="table-header">
            <h3 class="card-title">📋 Danh Sách Bài Đăng ({{ posts.length }})</h3>
            <div class="filter-tabs">
              <button :class="['tab', filterType === 'ALL' && 'active']" @click="filterType = 'ALL'">Tất cả</button>
              <button :class="['tab', filterType === 'NEWS' && 'active']" @click="filterType = 'NEWS'">📰 Tin Tức</button>
              <button :class="['tab', filterType === 'RECRUITMENT' && 'active']" @click="filterType = 'RECRUITMENT'">💼 Tuyển Dụng</button>
              <button :class="['tab', filterType === 'APPLICATIONS' && 'active']" @click="filterType = 'APPLICATIONS'">📋 Đơn Ứng Tuyển</button>
            </div>
          </div>

          <div class="post-list" v-if="filterType !== 'APPLICATIONS'">
            <div v-for="post in filteredPosts" :key="post.id" class="post-item" :class="{ 'inactive': !post.active }">
              <img v-if="post.image" :src="post.image" class="post-thumb" />
              <div v-else class="post-thumb-placeholder">📄</div>
              <div class="post-info">
                <div class="post-meta">
                  <span :class="['type-badge', post.type === 'NEWS' ? 'type-news' : 'type-recruit']">
                    {{ post.type === 'NEWS' ? '📰 Tin Tức' : '💼 Tuyển Dụng' }}
                  </span>
                  <span class="post-date">{{ formatDate(post.createDate) }}</span>
                  <span v-if="!post.active" class="hidden-badge">🔴 Đã ẩn</span>
                  <span class="likes-badge">❤️ {{ post.likes || 0 }}</span>
                </div>
                <h4>{{ post.title }}</h4>
                <p>{{ truncate(post.content, 120) }}</p>
              </div>
              <div class="post-actions">
                <button @click="editPost(post)" class="btn-edit">✏️</button>
                <button @click="deletePost(post.id)" class="btn-delete">🗑️</button>
              </div>
            </div>

            <div v-if="filteredPosts.length === 0" class="empty-state">
              <p>📭 Chưa có bài đăng nào.</p>
            </div>
          </div>

          <!-- APPLICATION LIST -->
          <div class="post-list" v-if="filterType === 'APPLICATIONS'">
            <div v-for="app in applications" :key="app.id" class="post-item app-item">
              <div class="post-info">
                <div class="post-meta">
                  <span class="type-badge type-recruit">📋 Ứng viên</span>
                  <span class="post-date">{{ formatDate(app.createDate) }}</span>
                  <span class="post-date" v-if="app.postId">ID Bài Đăng: {{ app.postId }}</span>
                </div>
                <h4>🧑‍💼 {{ app.fullname }}</h4>
                <p>📞 {{ app.phone }} | ✉️ {{ app.email || 'Không có email' }}</p>
                <p class="app-message"><strong>Lời nhắn:</strong> {{ app.message || 'Không có lời nhắn' }}</p>
                <div v-if="app.cvFile" style="margin-top: 10px;">
                  <button type="button" @click="downloadCv(app)" class="btn-download-cv">📄 Xem / Tải CV</button>
                </div>
              </div>
            </div>
            
            <div v-if="applications.length === 0" class="empty-state">
              <p>📭 Chưa có đơn ứng tuyển nào.</p>
            </div>
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

const posts = ref([]);
const applications = ref([]);
const filterType = ref('ALL');
const editingPost = ref(null);

const form = ref({
  title: '', content: '', image: '', type: 'NEWS', active: true
});

const configHeader = () => {
  const token = sessionStorage.getItem('staff_token');
  return { headers: { 'Authorization': `Bearer ${token}` } };
};

const loadPosts = async () => {
  try {
    const res = await api.get('/api/admin/posts', configHeader());
    posts.value = res.data;
  } catch (err) { console.error('Lỗi tải bài đăng', err); }
};

const loadApplications = async () => {
  try {
    const res = await api.get('/api/applications', configHeader());
    applications.value = res.data;
  } catch (err) { console.error('Lỗi tải đơn ứng tuyển', err); }
};

const downloadCv = async (app) => {
  try {
    const res = await api.get(app.cvFile, {
      ...configHeader(),
      responseType: 'blob'
    });
    const blobUrl = URL.createObjectURL(res.data);
    window.open(blobUrl, '_blank', 'noopener');
    setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
  } catch (err) {
    console.error('Không thể tải CV', err);
    alert('Không thể tải CV. Vui lòng kiểm tra quyền truy cập.');
  }
};

const filteredPosts = computed(() => {
  if (filterType.value === 'ALL') return posts.value;
  return posts.value.filter(p => p.type === filterType.value);
});

const submitPost = async () => {
  if (!form.value.title || !form.value.content) {
    return alert('Vui lòng nhập tiêu đề và nội dung!');
  }
  try {
    if (editingPost.value) {
      await api.put(`/api/admin/posts/${editingPost.value.id}`, form.value, configHeader());
      alert('Cập nhật bài đăng thành công!');
    } else {
      await api.post('/api/admin/posts', form.value, configHeader());
      alert('Đăng bài thành công!');
    }
    resetForm();
    loadPosts();
  } catch (err) {
    alert('Lỗi: ' + (err.response?.data || err.message));
  }
};

const editPost = (post) => {
  editingPost.value = post;
  form.value = { title: post.title, content: post.content, image: post.image || '', type: post.type, active: post.active };
};

const cancelEdit = () => { resetForm(); };

const resetForm = () => {
  editingPost.value = null;
  form.value = { title: '', content: '', image: '', type: 'NEWS', active: true };
};

const deletePost = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa bài đăng này?')) return;
  try {
    await api.delete(`/api/admin/posts/${id}`, configHeader());
    loadPosts();
  } catch (err) {
    console.error('Lỗi xóa bài đăng', err);
    alert('Lỗi xóa bài đăng!');
  }
};

const formatDate = (d) => d ? new Date(d).toLocaleDateString('vi-VN') : '---';
const truncate = (str, len) => str && str.length > len ? str.substring(0, len) + '...' : str;

onMounted(() => {
  loadPosts();
  loadApplications();
});
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1500px; margin: 0 auto; padding: 36px 24px; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.9rem; margin: 0; }

.content-grid { display: grid; grid-template-columns: 420px 1fr; gap: 28px; align-items: start; }

/* Form */
.form-card { position: sticky; top: 90px; }
.card-title { margin: 0 0 20px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading); padding-bottom: 14px; border-bottom: 1px solid var(--border-light); }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); margin-bottom: 6px; }
.form-group textarea { resize: vertical; min-height: 100px; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }

.toggle-label { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.toggle-label input { width: 18px; height: 18px; accent-color: var(--primary); }
.toggle-text { font-size: 0.88rem; font-weight: 600; color: var(--text-secondary); }

.form-actions { display: flex; gap: 10px; margin-top: 8px; }
.btn-save {
  flex: 1; background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 13px; border-radius: var(--radius-md);
  font-weight: 800; font-size: 0.95rem; cursor: pointer; font-family: inherit;
  transition: var(--transition);
}
.btn-save:hover { transform: translateY(-2px); box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent); }
.btn-cancel {
  background: color-mix(in srgb, var(--primary) 15%, transparent); border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  color: var(--primary); padding: 13px 20px; border-radius: var(--radius-md);
  font-weight: 700; cursor: pointer; font-family: inherit; transition: var(--transition);
}

.preview-box { margin-top: 16px; padding: 12px; background: var(--bg-card2); border-radius: var(--radius-md); border: 1px dashed var(--border); }
.preview-label { font-size: 0.8rem; color: var(--text-muted); margin: 0 0 8px 0; }
.preview-img { width: 100%; max-height: 200px; object-fit: cover; border-radius: var(--radius-sm); }

/* Post List */
.table-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.filter-tabs { display: flex; gap: 6px; }
.tab {
  background: var(--bg-input); border: 1px solid var(--border-light);
  color: var(--text-muted); padding: 7px 14px; border-radius: 20px;
  cursor: pointer; font-size: 0.82rem; font-weight: 600;
  font-family: inherit; transition: var(--transition);
}
.tab.active { background: var(--primary-glow); border-color: var(--primary); color: var(--primary); }

.post-list { display: flex; flex-direction: column; gap: 12px; }

.post-item {
  display: flex; align-items: center; gap: 16px;
  padding: 16px; background: var(--bg-card2);
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  transition: var(--transition);
}
.post-item:hover { border-color: var(--border); transform: translateX(4px); }
.post-item.inactive { opacity: 0.5; }

.btn-download-cv {
  display: inline-block;
  padding: 6px 12px;
  background: var(--secondary);
  color: #FFFFFF;
  text-decoration: none;
  border-radius: 4px;
  font-weight: bold;
  font-size: 0.9rem;
  transition: 0.2s;
}
.btn-download-cv:hover {
  background: var(--secondary);
  transform: translateY(-2px);
}

.post-thumb {
  width: 80px; height: 80px; border-radius: var(--radius-md);
  object-fit: cover; flex-shrink: 0; border: 1px solid var(--border);
}
.post-thumb-placeholder {
  width: 80px; height: 80px; border-radius: var(--radius-md);
  background: var(--bg-input); display: flex; align-items: center; justify-content: center;
  font-size: 2rem; flex-shrink: 0; border: 1px solid var(--border-light);
}

.post-info { flex: 1; min-width: 0; }
.post-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.type-badge {
  font-size: 0.72rem; font-weight: 700; padding: 3px 10px;
  border-radius: 12px; letter-spacing: 0.5px;
}
.type-news { background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--secondary); }
.type-recruit { background: color-mix(in srgb, var(--color-tertiary) 15%, transparent); color: var(--color-tertiary); }
.post-date { font-size: 0.78rem; color: var(--text-muted); }
.hidden-badge { font-size: 0.72rem; font-weight: 700; color: var(--primary); }

.post-info h4 { margin: 0 0 4px 0; font-size: 0.95rem; color: var(--text-heading); font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.post-info p { margin: 0; font-size: 0.82rem; color: var(--text-muted); line-height: 1.4; }

.post-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.btn-edit, .btn-delete {
  width: 36px; height: 36px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: transparent;
  cursor: pointer; font-size: 1rem; transition: var(--transition);
}
.btn-edit:hover { background: color-mix(in srgb, var(--secondary) 15%, transparent); border-color: var(--primary); }
.btn-delete:hover { background: color-mix(in srgb, var(--primary) 15%, transparent); border-color: var(--primary); }

.empty-state { text-align: center; padding: 50px 20px; color: var(--text-muted); font-style: italic; }

.app-item { align-items: flex-start; }
.app-message { background: var(--bg-root); padding: 10px; border-radius: var(--radius-sm); margin-top: 8px !important; font-size: 0.8rem !important; }
.likes-badge { font-size: 0.72rem; font-weight: 700; color: var(--primary); background: color-mix(in srgb, var(--primary) 10%, transparent); padding: 2px 8px; border-radius: 10px; }
</style>

