<template>
  <AdminLayout>
  <div class="admin-wrapper page-3d-enter">
    <main class="admin-content">
      <div class="page-header parallax-header">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h1 class="page-title text-3d"> Đề Xuất Mua Hàng Tự Động</h1>
            <p class="page-subtitle">Hệ thống phân tích tốc độ tiêu thụ và đề xuất nhập kho thông minh</p>
          </div>
          <div style="display: flex; gap: 10px;">
            <button @click="fetchSuggestions" class="g-btn-primary btn-3d"> Làm Mới</button>
            <button @click="analyzeWithAI" class="btn-ai btn-3d" :disabled="aiLoading">{{ aiLoading ? 'Đang phân tích...' : 'AI Phân Tích Sâu' }}</button>
          </div>
        </div>
      </div>

      <div v-if="summary.expiredBatchesCount || summary.expiringBatchesCount || summary.handlingCount" class="inventory-analysis-alert" role="alert">
        <UiIcon name="warning" />
        <span><strong>Cần xử lý kho:</strong> {{ summary.expiredBatchesCount }} lô hết hạn · {{ summary.expiringBatchesCount }} lô sắp hết hạn · {{ summary.handlingCount }} nguyên liệu tạm không nhập thêm.</span>
      </div>

      <!-- Summary Cards -->
      <div class="summary-cards">
        <div class="summary-card depth-card float-card card-total">
          <div class="sc-icon"><UiIcon name="box" /></div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.totalItems }}</span>
            <span class="sc-label">Cần Nhập Kho</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-critical">
          <div class="sc-icon"><UiIcon name="x" /></div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.criticalCount }}</span>
            <span class="sc-label">Hết Hàng</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-warning">
          <div class="sc-icon"><UiIcon name="warning" /></div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.warningCount }}</span>
            <span class="sc-label">Sắp Hết</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-cost">
          <div class="sc-icon"><UiIcon name="currency" /></div>
          <div class="sc-info">
            <span class="sc-value">{{ formatMoney(summary.totalEstimatedCost) }}</span>
            <span class="sc-label">Chi Phí Ước Tính</span>
          </div>
        </div>
      </div>

      <!-- Suggestions Table -->
      <div class="suggestions-wrap depth-card" style="padding: 20px; border-radius: 16px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <h3 style="margin: 0; font-size: 1.1rem; color: var(--text-heading);"> Danh Sách Đề Xuất Nhập Kho</h3>
          <button v-if="summary.totalItems > 0" @click="openBatchReview" class="g-btn-primary" style="font-size: 0.85rem;" :disabled="batchSubmitting">
             Duyệt Tất Cả ({{ summary.totalItems }})
          </button>
        </div>

        <table class="g-table">
          <thead>
            <tr>
              <th>Nguyên Liệu</th>
              <th>Tồn Hiện Tại</th>
              <th>Định Mức</th>
              <th>Tiêu Thụ/Ngày</th>
              <th>Còn Dùng Được</th>
              <th>Đề Xuất Mua</th>
              <th>Chi Phí Ước Tính</th>
              <th>Mức Độ</th>
              <th>Thao Tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in suggestions" :key="item.ingredientId" class="suggestion-row">
              <td>
                <div style="display: flex; align-items: center; gap: 10px;">
                  <img v-if="item.image" :src="item.image" style="width: 36px; height: 36px; border-radius: 8px; object-fit: cover;" />
                  <span v-else style="font-size: 1.3rem;"><UiIcon name="box" /></span>
                  <strong>{{ item.name }}</strong>
                </div>
              </td>
              <td :style="{ color: item.currentStock <= 0 ? 'var(--primary)' : item.currentStock <= item.minStock ? 'var(--color-tertiary)' : 'var(--success)', fontWeight: 'bold' }">
                {{ item.currentStock }} {{ item.unit }}
              </td>
              <td style="color: var(--text-muted);">{{ item.minStock }} {{ item.unit }}</td>
              <td style="color: var(--primary); font-weight: 700;">{{ item.dailyConsumption }} {{ item.unit }}</td>
              <td>
                <span :style="{ color: item.daysLeft <= 1 ? 'var(--primary)' : item.daysLeft <= 3 ? 'var(--color-tertiary)' : 'var(--success)', fontWeight: 'bold' }">
                  {{ item.daysLeft >= 999 ? '∞' : item.daysLeft + ' ngày' }}
                </span>
              </td>
              <td style="font-weight: 900; color: var(--primary); font-size: 1.05rem;">
                {{ item.suggestedAmount }} {{ item.unit }}
              </td>
              <td style="color: var(--primary); font-weight: bold;">{{ formatMoney(item.estimatedCost) }}</td>
              <td>
                <span class="urgency-badge" :class="'urgency-' + item.urgency">
                  {{ item.urgencyLabel }}
                </span>
              </td>
              <td>
                <button v-if="Number(item.suggestedAmount) > 0" @click="openApproval(item)" class="g-btn-primary btn-3d" style="font-size: 0.8rem; padding: 6px 12px;" :disabled="item.approved || approvingId === item.ingredientId">
                  {{ item.approved ? 'Đã nhập' : approvingId === item.ingredientId ? 'Đang nhập...' : 'Duyệt' }}
                </button>
                <span v-else class="urgency-badge urgency-expiring">Không nhập · Cần xử lý</span>
              </td>
            </tr>
            <tr v-if="suggestions.length === 0">
              <td colspan="9" class="empty-td">
                <div style="text-align: center; padding: 50px;">
        <div style="font-size: 3rem; margin-bottom: 10px;"><UiIcon name="check" /></div>
                  <h3 style="color: var(--success);">Kho hàng đầy đủ!</h3>
                  <p style="color: var(--text-muted);">Tất cả nguyên liệu đang ở mức an toàn. Không cần nhập thêm.</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- AI Modal -->
      <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
        <section class="ai-modal" role="dialog" aria-modal="true" aria-labelledby="inventory-ai-title">
          <header class="modal-heading">
            <div><h2 id="inventory-ai-title">AI Phân Tích Sâu</h2><p>Dữ liệu tồn, tiêu thụ, công thức và hạn dùng hiện tại</p></div>
            <button class="icon-close" aria-label="Đóng phân tích" @click="showAiModal = false"><UiIcon name="x" /></button>
          </header>
          <div class="ai-modal-body">
            <div v-if="aiLoading" class="ai-loading">
              <div class="spinner"></div>
              <p>AI đang phân tích dữ liệu tồn kho...</p>
            </div>
            <div v-else-if="aiAnalysisItems.length" class="analysis-grid">
              <article v-for="item in aiAnalysisItems" :key="item.ingredientId" class="analysis-card">
                <div class="analysis-card-title"><h3>{{ item.name }}</h3><span class="urgency-badge" :class="'urgency-' + item.urgency">{{ item.urgencyLabel || item.urgency }}</span></div>
                <dl><div><dt>Tồn hiện tại</dt><dd>{{ item.currentStock }} {{ item.unit }}</dd></div><div><dt>Tiêu thụ/ngày</dt><dd>{{ item.dailyConsumption }} {{ item.unit }}</dd></div><div><dt>Còn dùng được</dt><dd>{{ item.daysLeft >= 999 ? 'Chưa xác định' : item.daysLeft + ' ngày' }}</dd></div><div><dt>Đề xuất mua</dt><dd>{{ item.suggestedAmount }} {{ item.unit }}</dd></div></dl>
                <p><strong>Lý do:</strong> {{ item.reason }}</p>
                <p><strong>Hành động:</strong> {{ item.action }}</p>
                <p v-if="item.aiInsight" class="ai-insight"><strong>AI bổ sung:</strong> {{ item.aiInsight }}</p>
              </article>
            </div>
            <p v-else class="empty-analysis">Kho đang an toàn, không có cảnh báo hoặc đề xuất cần xử lý.</p>
          </div>
        </section>
      </div>

      <div v-if="approvalItem" class="modal-overlay" @click.self="closeApproval">
        <section class="approval-modal" role="dialog" aria-modal="true" aria-labelledby="approval-title">
          <header class="modal-heading"><div><h2 id="approval-title">Nhập Lô Mới</h2><p>{{ approvalItem.name }} · đề xuất {{ approvalItem.suggestedAmount }} {{ approvalItem.unit }}</p></div><button class="icon-close" aria-label="Đóng" @click="closeApproval"><UiIcon name="x" /></button></header>
          <div class="approval-body">
            <label>Nhà cung cấp *<input v-model.trim="approvalForm.supplier" class="g-form-control" maxlength="255"></label>
            <label>Số lượng ({{ approvalItem.unit }}) *<input v-model.number="approvalForm.quantity" class="g-form-control" type="number" min="0.01" step="0.1"></label>
            <label>Đơn giá thực tế (đ/{{ approvalItem.unit }}) *<input v-model.number="approvalForm.unitPrice" class="g-form-control" type="number" min="1" step="500"></label>
            <label>Hạn sử dụng *<input v-model="approvalForm.expirationDate" class="g-form-control" type="date" :min="tomorrow"></label>
            <label class="wide">Ghi chú<textarea v-model.trim="approvalForm.note" class="g-form-control" rows="2" maxlength="500"></textarea></label>
          </div>
          <footer class="modal-footer"><button class="g-btn-secondary" :disabled="approvingId !== null" @click="closeApproval">Hủy</button><button class="g-btn-primary" :disabled="!approvalValid || approvingId !== null" @click="submitApproval">{{ approvingId !== null ? 'Đang nhập kho...' : 'Xác nhận nhập kho' }}</button></footer>
        </section>
      </div>

      <div v-if="showBatchModal" class="modal-overlay" @click.self="closeBatchReview">
        <section class="batch-modal" role="dialog" aria-modal="true" aria-labelledby="batch-title">
          <header class="modal-heading"><div><h2 id="batch-title">Duyệt đề xuất theo lô</h2><p>Bổ sung dữ liệu thật trước khi tạo lô kho; hệ thống không tự đặt giá, HSD hoặc nhà cung cấp.</p></div><button class="icon-close" aria-label="Đóng" @click="closeBatchReview"><UiIcon name="x" /></button></header>
          <div class="batch-body">
            <label class="batch-supplier">Nhà cung cấp dùng chung *<input v-model.trim="batchSupplier" class="g-form-control" maxlength="255"></label>
            <div class="batch-table-wrap"><table class="g-table"><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Đơn giá thực tế</th><th>Đơn giá nhập trước đó</th><th>Hạn sử dụng</th><th>Trạng thái</th></tr></thead><tbody><tr v-for="row in batchRows" :key="row.ingredientId"><td><strong>{{ row.name }}</strong><small>{{ row.unit }}</small></td><td><input v-model.number="row.quantity" type="number" min="0.01" step="0.1" class="g-form-control"></td><td><input v-model.number="row.unitPrice" type="number" min="1" step="500" class="g-form-control"><small v-if="priceDeltaText(row)" :class="priceDeltaClass(row)">{{ priceDeltaText(row) }}</small></td><td><strong>{{ formatVnd(row.previousUnitPrice) }}</strong></td><td><input v-model="row.expirationDate" type="date" :min="tomorrow" class="g-form-control"></td><td><span :class="rowValid(row) ? 'row-ready' : 'row-missing'">{{ rowValid(row) ? 'Sẵn sàng' : 'Thiếu thông tin' }}</span></td></tr></tbody></table></div>
            <div v-if="batchResult" class="batch-result"><strong>Đã xử lý {{ batchResult.successCount }}/{{ batchRows.length }} đề xuất.</strong><p v-if="batchResult.failureCount">{{ batchResult.failureCount }} đề xuất chưa thành công.</p><ul v-if="batchResult.failures?.length"><li v-for="failure in batchResult.failures" :key="failure.ingredientId">{{ ingredientName(failure.ingredientId) }}: {{ failure.reason }}</li></ul></div>
          </div>
          <footer class="modal-footer"><button class="g-btn-primary" :disabled="!batchCanSubmit || batchSubmitting" @click="submitBatchApproval">{{ batchSubmitting ? 'Đang xử lý...' : `Xác nhận ${batchRows.length} đề xuất` }}</button></footer>
        </section>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';
import UiIcon from '@/components/UiIcon.vue';
import { computed, ref, onMounted } from 'vue';
import api from '@/services/api';
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';
import { getApiErrorMessage } from '@/services/errorMessage';

const suggestions = ref([]);
const summary = ref({ totalItems: 0, handlingCount: 0, criticalCount: 0, warningCount: 0, expiredBatchesCount: 0, expiringBatchesCount: 0, totalEstimatedCost: 0 });
const showAiModal = ref(false);
const aiLoading = ref(false);
const aiAnalysisItems = ref([]);
const approvalItem = ref(null);
const approvalForm = ref({ supplier: '', quantity: null, unitPrice: null, expirationDate: '', note: '', requestId: '' });
const approvingId = ref(null);
const showBatchModal = ref(false);
const batchRows = ref([]);
const batchSupplier = ref('');
const batchSubmitting = ref(false);
const batchResult = ref(null);
const toast = useToast();
const { confirmDialog } = useDialog();

const getToken = () => sessionStorage.getItem('staff_token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });
const tomorrow = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);
const approvalValid = computed(() => approvalForm.value.supplier && Number(approvalForm.value.quantity) > 0
  && Number(approvalForm.value.unitPrice) > 0 && approvalForm.value.expirationDate >= tomorrow);
const rowValid = row => Number(row.quantity) > 0 && Number(row.unitPrice) > 0 && row.expirationDate >= tomorrow;
const batchCanSubmit = computed(() => batchSupplier.value && batchRows.value.length > 0 && batchRows.value.every(rowValid));
const requestId = prefix => `${prefix}-${globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`}`;
const expirationPayload = date => new Date(`${date}T12:00:00+07:00`).toISOString();

const fetchSuggestions = async () => {
  try {
    const res = await api.get('/api/admin/purchase-suggestions', configHeader());
    suggestions.value = (res.data.suggestions || []).map(s => ({ ...s, approved: false }));
    summary.value = {
      totalItems: res.data.totalItems || 0,
      criticalCount: res.data.criticalCount || 0,
      warningCount: res.data.warningCount || 0,
      handlingCount: res.data.handlingCount || 0,
      expiredBatchesCount: res.data.expiredBatchesCount || 0,
      expiringBatchesCount: res.data.expiringBatchesCount || 0,
      totalEstimatedCost: res.data.totalEstimatedCost || 0
    };
  } catch (err) { console.error('Lỗi lấy đề xuất', err); }
};

const openApproval = item => {
  if (item.approved || approvingId.value !== null) return;
  approvalItem.value = item;
  approvalForm.value = {
    supplier: '', quantity: Number(item.suggestedAmount), unitPrice: null, expirationDate: '',
    note: 'Duyệt đề xuất mua hàng', requestId: requestId(`suggestion-${item.ingredientId}`)
  };
};
const closeApproval = () => { if (approvingId.value === null) approvalItem.value = null; };
const submitApproval = async () => {
  const item = approvalItem.value;
  if (!item || !approvalValid.value || approvingId.value !== null) return;
  approvingId.value = item.ingredientId;
  try {
    await api.post(
      `/api/admin/purchase-suggestions/approve/${item.ingredientId}`,
      { ...approvalForm.value, expirationDate: expirationPayload(approvalForm.value.expirationDate) }, configHeader()
    );
    item.approved = true;
    toast.success(`Đã nhập kho ${approvalForm.value.quantity} ${item.unit} ${item.name}.`);
    approvalItem.value = null;
    await fetchSuggestions();
  } catch (err) { toast.error(getApiErrorMessage(err, 'Không thể duyệt đề xuất nhập kho.')); }
  finally { approvingId.value = null; }
};

const openBatchReview = () => {
  batchRows.value = suggestions.value.filter(item => Number(item.suggestedAmount) > 0 && !item.approved).map(item => ({
    ingredientId: item.ingredientId, name: item.name, unit: item.unit,
    quantity: Number(item.suggestedAmount), unitPrice: null, expirationDate: '',
    previousUnitPrice: item.previousUnitPrice ?? null,
    requestId: requestId(`batch-${item.ingredientId}`)
  }));
  batchSupplier.value = '';
  batchResult.value = null;
  showBatchModal.value = true;
};
const closeBatchReview = () => { if (!batchSubmitting.value) showBatchModal.value = false; };
const ingredientName = id => batchRows.value.find(row => row.ingredientId === id)?.name || `Nguyên liệu #${id}`;
const submitBatchApproval = async () => {
  if (!batchCanSubmit.value || batchSubmitting.value) return;
  if (!await confirmDialog({ title: 'Xác nhận nhập kho', message: `Tạo ${batchRows.value.length} lô nhập với dữ liệu vừa kiểm tra?`, confirmLabel: 'Xác nhận', danger: false })) return;
  batchSubmitting.value = true;
  batchResult.value = null;
  try {
    const { data } = await api.post('/api/admin/purchase-suggestions/approve-batch', { items: batchRows.value.map(row => ({
      ingredientId: row.ingredientId,
      approval: { quantity: row.quantity, unitPrice: row.unitPrice, expirationDate: expirationPayload(row.expirationDate), supplier: batchSupplier.value, note: 'Duyệt hàng loạt đề xuất mua hàng', requestId: row.requestId }
    })) }, configHeader());
    batchResult.value = data;
    toast[data.failureCount ? 'warning' : 'success'](`Đã xử lý ${data.successCount}/${batchRows.value.length} đề xuất.${data.failureCount ? ` ${data.failureCount} đề xuất cần kiểm tra.` : ''}`);
    await fetchSuggestions();
    if (!data.failureCount) showBatchModal.value = false;
  } catch (err) { toast.error(getApiErrorMessage(err, 'Không thể xử lý danh sách đề xuất.')); }
  finally { batchSubmitting.value = false; }
};

const formatMoney = (val) => {
  if (!val) return '0đ';
  if (val >= 1000000) return (val / 1000000).toFixed(1) + ' Triệu';
  if (val >= 1000) return Math.round(val / 1000) + 'K';
  return Math.round(val).toLocaleString() + 'đ';
};

const formatVnd = (value) => {
  const amount = Number(value || 0);
  return amount > 0 ? `${Math.round(amount).toLocaleString('vi-VN')}đ` : 'Chưa có';
};

const formatCompactVnd = (value) => {
  const amount = Math.round(Math.abs(Number(value || 0)));
  if (amount <= 0) return '0đ';
  if (amount < 1_000_000) return `${amount.toLocaleString('vi-VN')}đ`;
  const millions = Math.floor(amount / 1_000_000);
  const hundredThousands = Math.floor((amount % 1_000_000) / 100_000);
  return `${millions}tr${hundredThousands ? hundredThousands : ''}`;
};

const priceDeltaText = row => {
  const previous = Number(row.previousUnitPrice || 0);
  const current = Number(row.unitPrice || 0);
  if (previous <= 0 || current <= 0) return '';
  const delta = current - previous;
  if (delta === 0) return 'Bằng đơn giá trước';
  return `${delta > 0 ? 'Tăng' : 'Giảm'} ${formatCompactVnd(delta)} so với lần nhập trước`;
};

const priceDeltaClass = row => {
  const delta = Number(row.unitPrice || 0) - Number(row.previousUnitPrice || 0);
  return delta > 0 ? 'price-delta-up' : delta < 0 ? 'price-delta-down' : 'price-delta-flat';
};

const analyzeWithAI = async () => {
  if (aiLoading.value) return;
  showAiModal.value = true;
  aiLoading.value = true;
  aiAnalysisItems.value = suggestions.value.map(item => ({ ...item, aiInsight: '' }));

  try {
    const res = await api.post('/api/admin/ai/inventory', {
      // Backend bổ sung canonical InventoryAlertService context. Chỉ gửi yêu cầu ngắn
      // để không nhân đôi 24 lô và vượt giới hạn AiRequest.message (4.000 ký tự).
      message: 'Phân tích các cảnh báo kho hiện tại và đề xuất hành động ưu tiên theo từng nguyên liệu.'
    }, configHeader());
    const cleaned = String(res.data.reply || '').replace(/```json/g, '').replace(/```/g, '').trim();
    try {
      const parsed = JSON.parse(cleaned);
      if (Array.isArray(parsed) && parsed.length > 0) {
        const aiByName = new Map(parsed.map(item => [String(item.name || '').toLowerCase(), item]));
        aiAnalysisItems.value = suggestions.value.map(item => {
          const aiItem = aiByName.get(String(item.name || '').toLowerCase());
          return aiItem ? { ...item, aiInsight: aiItem.analysis || aiItem.reason || '' } : item;
        });
      }
    } catch {
      if (cleaned && aiAnalysisItems.value.length) aiAnalysisItems.value[0].aiInsight = cleaned;
    }
  } catch (err) {
    // Canonical cards remain usable when the external AI provider is unavailable.
  } finally {
    aiLoading.value = false;
  }
};

onMounted(fetchSuggestions);
</script>

<style scoped>
@import '@/assets/admin-3d.css';

.admin-content { max-width: 1400px; margin: 0 auto; padding: 28px 24px; }
.page-header { margin-bottom: 28px; padding: 20px 0; }
.page-title { margin: 0; font-size: 1.8rem; font-weight: 900; }
.page-subtitle { margin: 6px 0 0; color: var(--text-muted); font-size: 0.95rem; }

.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { display: flex; align-items: center; gap: 14px; padding: 20px; border-radius: 14px; }
.inventory-analysis-alert { display: flex; align-items: center; gap: 10px; margin: 0 0 20px; padding: 13px 16px; border: 1px solid #FBBF24; border-radius: 10px; background: #FFFBEB; color: #92400E; }
.sc-icon { font-size: 1.8rem; }
.sc-info { display: flex; flex-direction: column; }
.sc-value { font-size: 1.5rem; font-weight: 900; }
.sc-label { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }

.card-total { border-left: 3px solid var(--primary); }
.card-total .sc-value { color: var(--primary); }
.card-critical { border-left: 3px solid var(--primary); }
.card-critical .sc-value { color: var(--primary); }
.card-warning { border-left: 3px solid var(--color-tertiary); }
.card-warning .sc-value { color: var(--color-tertiary); }
.card-cost { border-left: 3px solid var(--secondary); }
.card-cost .sc-value { color: var(--secondary); font-size: 1.2rem; }

.suggestion-row { transition: background 0.2s; }
.suggestion-row:hover { background: color-mix(in srgb, var(--secondary) 4%, transparent); }

.urgency-badge { padding: 4px 12px; border-radius: 100px; font-size: 0.75rem; font-weight: 700; display: inline-block; }
.urgency-expired { background: #FFF1F2; color: #9F1239; }
.urgency-expiring { background: #FFFBEB; color: #92400E; }

.btn-ai { background: linear-gradient(135deg, var(--color-tertiary), var(--warning)); color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 10px; font-weight: bold; cursor: pointer; font-family: inherit; }
.btn-ai:hover { filter: brightness(1.1); }

.modal-overlay { position: fixed; inset: 0; padding: 20px; background: rgba(40,24,18,.48); display: flex; align-items: center; justify-content: center; z-index: 1400; }
.ai-modal, .approval-modal, .batch-modal { width: min(920px, 96vw); max-height: 88vh; display: flex; flex-direction: column; overflow: hidden; background: #fff; color: var(--text-primary); border: 1px solid var(--border); border-radius: 14px; box-shadow: 0 24px 70px rgba(39,20,14,.28); }
.approval-modal { width: min(650px, 96vw); }
.batch-modal { width: min(1180px, 97vw); }
.modal-heading { flex: 0 0 auto; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 20px 22px; border-bottom: 1px solid var(--border); background: #fffaf8; }
.modal-heading h2 { margin: 0; color: var(--text-heading); font-size: 1.3rem; }
.modal-heading p { margin: 5px 0 0; color: var(--text-muted); }
.icon-close { width: 36px; height: 36px; flex: 0 0 36px; display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 1px solid var(--border); border-radius: 8px; background: #fff; color: var(--text-secondary); cursor: pointer; }
.icon-close:hover { border-color: var(--primary); color: var(--primary); background: #fff3f1; }
.icon-close:focus-visible, .approval-body input:focus-visible, .approval-body textarea:focus-visible { outline: 3px solid color-mix(in srgb, var(--secondary) 28%, transparent); outline-offset: 2px; }
.ai-modal-body, .batch-body { min-height: 0; overflow-y: auto; padding: 20px 22px; }
.ai-loading { text-align: center; padding: 46px 20px; color: var(--primary); }
.analysis-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.analysis-card { border: 1px solid var(--border); border-radius: 12px; padding: 16px; background: #fff; }
.analysis-card-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.analysis-card h3 { margin: 0; color: var(--text-heading); font-size: 1.05rem; }
.analysis-card dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin: 14px 0; }
.analysis-card dl div { padding: 9px; border-radius: 8px; background: var(--bg-card2); }
.analysis-card dt { color: var(--text-muted); font-size: .74rem; }
.analysis-card dd { margin: 3px 0 0; font-weight: 800; }
.analysis-card p { margin: 8px 0 0; line-height: 1.5; }
.ai-insight { padding: 10px; border-left: 3px solid var(--secondary); background: #f2f8f4; }
.empty-analysis { margin: 0; padding: 40px; text-align: center; color: var(--text-muted); }
.approval-body { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; overflow-y: auto; padding: 20px 22px; }
.approval-body label, .batch-supplier { display: grid; gap: 7px; color: var(--text-secondary); font-size: .82rem; font-weight: 800; }
.approval-body .wide { grid-column: 1 / -1; }
.modal-footer { flex: 0 0 auto; display: flex; justify-content: flex-end; gap: 10px; padding: 16px 22px; border-top: 1px solid var(--border); background: #fffaf8; }
.batch-supplier { max-width: 480px; margin-bottom: 16px; }
.batch-table-wrap { overflow: auto; border: 1px solid var(--border); border-radius: 10px; }
.batch-table-wrap table { min-width: 840px; }
.batch-table-wrap input { min-width: 130px; }
.batch-table-wrap small { display: block; margin-top: 3px; color: var(--text-muted); }
.row-ready, .row-missing { display: inline-flex; padding: 4px 9px; border-radius: 999px; font-size: .75rem; font-weight: 800; white-space: nowrap; }
.row-ready { color: #17653b; background: #eaf7ef; }
.row-missing { color: #8a5a06; background: #fff6df; }
.price-delta-up, .price-delta-down, .price-delta-flat { margin-top: 4px; font-weight: 800; }
.price-delta-up { color: #BE123C; }
.price-delta-down { color: #17653b; }
.price-delta-flat { color: var(--text-muted); }
.batch-result { margin-top: 16px; padding: 14px; border: 1px solid var(--border); border-radius: 10px; background: #fffaf8; }
.batch-result p { margin: 4px 0; }
.batch-result ul { margin: 10px 0 0; padding-left: 20px; color: var(--primary); }
.spinner { width: 40px; height: 40px; border: 4px solid color-mix(in srgb, var(--secondary) 20%, transparent); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 992px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .summary-cards { grid-template-columns: 1fr; }
  .analysis-grid, .approval-body { grid-template-columns: 1fr; }
  .approval-body .wide { grid-column: auto; }
  .modal-overlay { padding: 10px; }
  .modal-footer { flex-wrap: wrap; }
}
</style>
