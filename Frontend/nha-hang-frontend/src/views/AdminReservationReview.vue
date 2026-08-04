<template>
  <AdminLayout>
    <div class="review-page">
      <div class="page-header">
        <div>
          <h1>Đánh Giá Đặt Bàn</h1>
          <p>Theo dõi phản hồi sau khi khách dùng bữa và phản hồi công khai khi cần.</p>
        </div>
        <button class="g-btn-primary" @click="fetchReviews">Tải lại</button>
      </div>

      <div class="stats-row">
        <div><span>Tổng đánh giá</span><strong>{{ reviews.length }}</strong></div>
        <div><span>Điểm trung bình</span><strong>{{ averageRating }}/5</strong></div>
        <div><span>Đang hiển thị</span><strong>{{ visibleCount }}</strong></div>
        <div><span>Đã ẩn</span><strong>{{ hiddenCount }}</strong></div>
      </div>

      <div v-if="loading" class="empty-state">Đang tải đánh giá...</div>
      <div v-else-if="reviews.length === 0" class="empty-state">Chưa có đánh giá đặt bàn.</div>
      <div v-else class="review-list">
        <article v-for="review in reviews" :key="review.id" class="review-card" :class="{ hidden: review.hidden }">
          <div class="review-main">
            <div class="review-title">
              <div>
                <h3>{{ review.reservationCode }}</h3>
                <p>{{ formatDate(review.createdAt) }}</p>
              </div>
              <span class="rating">{{ review.overallRating }}/5</span>
            </div>
            <p class="content">{{ review.content || 'Khách không để lại bình luận.' }}</p>
            <div class="rating-breakdown">
              <span>Món ăn: {{ review.foodRating || '-' }}</span>
              <span>Phục vụ: {{ review.serviceRating || '-' }}</span>
              <span>Không gian: {{ review.ambienceRating || '-' }}</span>
              <span>Vệ sinh: {{ review.cleanlinessRating || '-' }}</span>
            </div>
            <div v-if="review.adminReply" class="reply-box">
              <strong>Phản hồi:</strong> {{ review.adminReply }}
            </div>
            <div v-if="review.hidden" class="hidden-reason">
              Đã ẩn: {{ review.hiddenReason || 'Không có lý do' }}
            </div>
          </div>
          <div class="review-actions">
            <textarea v-model="replyDrafts[review.id]" rows="4" placeholder="Nhập phản hồi của nhà hàng" />
            <button class="g-btn-primary" @click="replyReview(review)">Lưu phản hồi</button>
            <button class="btn-secondary" @click="toggleVisibility(review)">
              {{ review.hidden ? 'Hiện lại' : 'Ẩn đánh giá' }}
            </button>
          </div>
        </article>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import api from '@/services/api';
import AdminLayout from '@/components/AdminLayout.vue';

const reviews = ref([]);
const replyDrafts = ref({});
const loading = ref(false);

const authHeader = () => ({
  headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
});

const visibleCount = computed(() => reviews.value.filter(review => !review.hidden).length);
const hiddenCount = computed(() => reviews.value.filter(review => review.hidden).length);
const averageRating = computed(() => {
  const visible = reviews.value.filter(review => !review.hidden);
  if (!visible.length) return '0.0';
  return (visible.reduce((sum, review) => sum + Number(review.overallRating || 0), 0) / visible.length).toFixed(1);
});

const formatDate = value => value ? new Date(value).toLocaleString('vi-VN') : '-';

const fetchReviews = async () => {
  loading.value = true;
  try {
    const res = await api.get('/api/reservation-reviews/admin', authHeader());
    reviews.value = Array.isArray(res.data) ? res.data : [];
    replyDrafts.value = {};
    reviews.value.forEach(review => {
      replyDrafts.value[review.id] = review.adminReply || '';
    });
  } catch {
    alert('Không thể tải danh sách đánh giá.');
  } finally {
    loading.value = false;
  }
};

const replyReview = async (review) => {
  try {
    await api.put(`/api/reservation-reviews/admin/${review.id}/reply`, {
      adminReply: replyDrafts.value[review.id] || ''
    }, authHeader());
    await fetchReviews();
  } catch {
    alert('Không thể lưu phản hồi.');
  }
};

const toggleVisibility = async (review) => {
  const hidden = !review.hidden;
  const hiddenReason = hidden ? prompt('Lý do ẩn đánh giá:', review.hiddenReason || '') : '';
  if (hidden && hiddenReason === null) return;
  try {
    await api.put(`/api/reservation-reviews/admin/${review.id}/visibility`, {
      hidden,
      hiddenReason
    }, authHeader());
    await fetchReviews();
  } catch {
    alert('Không thể cập nhật hiển thị đánh giá.');
  }
};

onMounted(fetchReviews);
</script>

<style scoped>
.review-page { max-width: 1280px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; margin-bottom: 22px; }
.page-header h1 { margin: 0 0 6px; color: var(--text-heading); font-size: 1.9rem; font-weight: 900; }
.page-header p { margin: 0; color: var(--text-muted); }
.stats-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 18px; }
.stats-row div { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; display: grid; gap: 6px; }
.stats-row span { color: var(--text-muted); font-size: 0.8rem; font-weight: 800; text-transform: uppercase; }
.stats-row strong { color: var(--primary); font-size: 1.5rem; }
.review-list { display: grid; gap: 14px; }
.review-card { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 18px; box-shadow: var(--shadow-md); }
.review-card.hidden { opacity: 0.65; }
.review-title { display: flex; justify-content: space-between; gap: 12px; }
.review-title h3, .review-title p { margin: 0; }
.review-title h3 { color: var(--text-heading); }
.review-title p, .content, .rating-breakdown { color: var(--text-muted); }
.rating { border-radius: 999px; background: rgba(90, 110, 69, 0.12); color: var(--primary); padding: 6px 10px; font-weight: 900; height: fit-content; }
.rating-breakdown { display: flex; flex-wrap: wrap; gap: 10px; font-size: 0.84rem; font-weight: 700; }
.reply-box, .hidden-reason { margin-top: 12px; border-radius: var(--radius-md); padding: 10px; background: var(--bg-card2); color: var(--text-secondary); }
.hidden-reason { color: #B23B2E; }
.review-actions { display: grid; gap: 10px; align-content: start; }
.review-actions textarea { width: 100%; border: 1px solid var(--border-light); border-radius: var(--radius-md); background: var(--bg-input); color: var(--text-primary); padding: 10px; font: inherit; }
.btn-secondary { border: 1px solid var(--border-light); border-radius: var(--radius-md); background: transparent; color: var(--text-secondary); padding: 10px 14px; font-weight: 800; cursor: pointer; }
.btn-secondary:hover { color: var(--primary); border-color: var(--primary); }
.empty-state { border: 1px dashed var(--border-light); border-radius: var(--radius-lg); padding: 46px; color: var(--text-muted); text-align: center; }
@media (max-width: 900px) {
  .stats-row, .review-card { grid-template-columns: 1fr; }
  .page-header { display: grid; }
}
</style>
