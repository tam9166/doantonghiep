<template>
  <AdminLayout>
  <div class="proposal-page">
    <div class="page-header"><div><h1>Đề xuất từ Bếp</h1><p>Kiểm duyệt nguyên liệu, món và công thức trước khi đưa vào dữ liệu chính thức.</p></div><button class="refresh" @click="load">Làm mới</button></div>
    <div v-if="loading" class="state">Đang tải đề xuất...</div>
    <div v-else-if="proposals.length === 0" class="state">Chưa có đề xuất nào.</div>
    <div v-else class="table-wrap"><table><thead><tr><th>Loại</th><th>Người đề xuất</th><th>Nội dung</th><th>Lý do</th><th>Trạng thái</th><th>Thời gian</th><th>Thao tác</th></tr></thead><tbody><tr v-for="item in proposals" :key="item.id"><td>{{ labels[item.proposalType] || item.proposalType }}</td><td>{{ item.proposedBy }}<small>{{ item.proposerRole }}</small></td><td><pre>{{ item.payload }}</pre></td><td>{{ item.reason }}</td><td><span :class="['status', item.status.toLowerCase()]">{{ item.status }}</span><small v-if="item.reviewNote">{{ item.reviewNote }}</small></td><td>{{ formatDate(item.createdAt) }}</td><td class="actions"><button v-if="item.status === 'PENDING'" class="approve" @click="approve(item)">Duyệt</button><button v-if="item.status === 'PENDING'" class="reject" @click="reject(item)">Từ chối</button><span v-else>Đã xử lý</span></td></tr></tbody></table></div>
  </div>
  </AdminLayout>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import AdminLayout from '@/components/AdminLayout.vue';
import api from '@/services/api';
import { useToast } from '@/composables/useToast';
const proposals = ref([]); const loading = ref(false); const toast = useToast();
const labels = { INGREDIENT: 'Nguyên liệu', DISH: 'Món', RECIPE: 'Công thức' };
const headers = () => ({ headers: { Authorization: `Bearer ${sessionStorage.getItem('staff_token')}` } });
const formatDate = value => value ? new Date(value).toLocaleString('vi-VN') : '-';
const load = async () => { loading.value = true; try { const res = await api.get('/api/admin/kitchen-proposals', headers()); proposals.value = Array.isArray(res.data) ? res.data : []; } catch (e) { toast.error(e.response?.data?.message || 'Không tải được đề xuất.'); } finally { loading.value = false; } };
const approve = async item => { try { await api.post(`/api/admin/kitchen-proposals/${item.id}/approve`, {}, headers()); item.status = 'APPROVED'; toast.success('Đã duyệt đề xuất và tạo bản ghi nháp.'); } catch (e) { toast.error(e.response?.data?.message || 'Không thể duyệt đề xuất.'); } };
const reject = async item => { const note = window.prompt('Nhập lý do từ chối:'); if (!note?.trim()) return toast.warning('Bắt buộc nhập lý do từ chối.'); try { await api.post(`/api/admin/kitchen-proposals/${item.id}/reject`, { note: note.trim() }, headers()); item.status = 'REJECTED'; item.reviewNote = note.trim(); toast.success('Đã từ chối đề xuất.'); } catch (e) { toast.error(e.response?.data?.message || 'Không thể từ chối đề xuất.'); } };
onMounted(load);
</script>
<style scoped>
.proposal-page{padding:28px;color:var(--text-primary);}.page-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.page-header h1{color:var(--primary);margin:0 0 6px}.page-header p{margin:0;color:var(--text-muted)}button{border:0;border-radius:8px;padding:9px 14px;cursor:pointer}.refresh{background:var(--primary);color:#fff}.table-wrap{overflow:auto;background:var(--bg-card);border:1px solid var(--border);border-radius:12px}table{width:100%;border-collapse:collapse}th,td{padding:12px;border-bottom:1px solid var(--border);text-align:left;vertical-align:top}th{color:var(--primary);white-space:nowrap}small{display:block;color:var(--text-muted);margin-top:4px}pre{white-space:pre-wrap;max-width:280px;margin:0;font:inherit}.status{font-weight:700}.status.pending{color:#b7791f}.status.approved{color:#16834b}.status.rejected{color:#c53030}.actions{white-space:nowrap}.approve{background:#16834b;color:#fff;margin-right:6px}.reject{background:#c53030;color:#fff}.state{padding:50px;text-align:center;background:var(--bg-card);border-radius:12px}
</style>
