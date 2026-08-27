<template>
  <AdminLayout>
    <main class="page"><h1>Xử lý thực phẩm hết hạn</h1><p class="lead">Theo dõi, tiêu hủy và kiểm soát các lô nguyên liệu theo cùng dữ liệu cảnh báo kho.</p>
      <div class="kpis"><div><b>{{ expired.length }}</b><span>Hết hạn</span></div><div><b>{{ expiring.length }}</b><span>Sắp hết hạn</span></div><div><b>{{ disposed.length }}</b><span>Đã xử lý</span></div></div>
      <section class="card"><h2>Lô cần xử lý</h2><table><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Hạn dùng</th><th>Thao tác</th></tr></thead><tbody><tr v-for="batch in expired" :key="batch.id"><td>{{ nameOf(batch.ingredientId) }}</td><td>{{ batch.quantity }}</td><td>{{ date(batch.expirationDate) }}</td><td><button @click="dispose(batch)">Tiêu hủy</button></td></tr><tr v-if="!expired.length"><td colspan="4">Không có lô hết hạn.</td></tr></tbody></table></section>
      <section class="card"><h2>Lô sắp hết hạn</h2><table><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Hạn dùng</th></tr></thead><tbody><tr v-for="batch in expiring" :key="batch.id"><td>{{ nameOf(batch.ingredientId) }}</td><td>{{ batch.quantity }}</td><td>{{ date(batch.expirationDate) }}</td></tr><tr v-if="!expiring.length"><td colspan="3">Không có lô sắp hết hạn.</td></tr></tbody></table></section>
      <section class="card"><h2>Lịch sử tiêu hủy</h2><table><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Lý do</th><th>Ngày</th></tr></thead><tbody><tr v-for="item in disposed" :key="item.id"><td>{{ item.ingredientName }}</td><td>{{ item.quantityDisposed }}</td><td>{{ item.reason }}</td><td>{{ date(item.disposalDate) }}</td></tr><tr v-if="!disposed.length"><td colspan="4">Chưa có lịch sử.</td></tr></tbody></table></section>
    </main>
  </AdminLayout>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import AdminLayout from '@/components/AdminLayout.vue';
import api from '@/services/api';
import { useToast } from '@/composables/useToast';
const toast = useToast(); const expired = ref([]); const expiring = ref([]); const disposed = ref([]); const ingredients = ref([]);
const headers = () => ({ headers: { Authorization: `Bearer ${sessionStorage.getItem('staff_token')}` } });
const load = async () => { try { const [i,e,x,d] = await Promise.all([api.get('/api/admin/ingredients', headers()), api.get('/api/admin/ingredients/expired-batches', headers()), api.get('/api/admin/ingredients/expiring-batches?daysThreshold=3', headers()), api.get('/api/admin/ingredients/disposed-batches', headers())]); ingredients.value=i.data; expired.value=e.data; expiring.value=x.data; disposed.value=d.data; } catch (_) { toast.error('Không thể tải dữ liệu xử lý hạn dùng.'); } };
const nameOf = id => ingredients.value.find(i => i.id === id)?.name || `#${id}`; const date = d => d ? new Date(d).toLocaleDateString('vi-VN') : '—';
const dispose = async batch => { const reason = window.prompt('Nhập lý do tiêu hủy lô nguyên liệu:'); if (!reason?.trim()) { toast.error('Vui lòng nhập lý do tiêu hủy trước khi xác nhận.'); return; } try { await api.post(`/api/admin/ingredients/batches/${batch.id}/dispose`, { reason: reason.trim() }, headers()); toast.success('Đã ghi nhận tiêu hủy.'); await load(); } catch (e) { toast.error(e.response?.data?.message || 'Không thể tiêu hủy lô.'); } };
onMounted(load);
</script>
<style scoped>
.page{padding:32px;max-width:1400px;margin:auto}.lead{color:var(--text-muted)}.kpis{display:flex;gap:16px;margin:20px 0}.kpis div,.card{background:var(--bg-card);border:1px solid var(--border-light);border-radius:12px;padding:18px}.kpis div{min-width:150px}.kpis b,.kpis span{display:block}.kpis b{font-size:28px;color:var(--primary)}.card{margin:18px 0;overflow:auto}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:12px;border-bottom:1px solid var(--border-light)}button{background:var(--primary);color:#fff;border:0;border-radius:8px;padding:8px 12px;cursor:pointer}
</style>
