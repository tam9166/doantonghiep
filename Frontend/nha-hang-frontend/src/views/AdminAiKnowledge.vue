<template>
  <AdminLayout>
    <div class="page">
      <h1>Trung tâm tri thức AI</h1>
      <p class="muted">Quản lý nội dung đã duyệt mà chatbot được phép tham khảo và cấu hình sức chứa.</p>

      <section class="card">
        <h2>Cấu hình đặt bàn</h2>
        <div class="grid">
          <label>Ngưỡng đoàn đông<input v-model.number="settings.largePartyThreshold" type="number" min="2"></label>
          <label>Sức chứa tối đa<input v-model.number="settings.restaurantMaxCapacity" type="number" min="2"></label>
        </div>
        <button @click="saveSettings">Lưu cấu hình</button>
      </section>

      <section class="card">
        <h2>{{ editingId ? 'Sửa nguồn tri thức' : 'Thêm nguồn tri thức' }}</h2>
        <input v-model.trim="form.title" placeholder="Tiêu đề (FAQ, chính sách, giới thiệu...)">
        <textarea v-model.trim="form.content" rows="7" placeholder="Nội dung đã kiểm duyệt"></textarea>
        <label class="check"><input v-model="form.enabled" type="checkbox"> Đang sử dụng</label>
        <div class="actions"><button @click="saveSource">{{ editingId ? 'Cập nhật' : 'Thêm nguồn' }}</button><button v-if="editingId" class="secondary" @click="resetForm">Hủy</button></div>
        <hr><h3>Hoặc tải tài liệu</h3>
        <input v-model.trim="uploadTitle" placeholder="Tên tài liệu (không bắt buộc)">
        <input ref="fileInput" type="file" accept=".txt,.md,.pdf,.docx" @change="selectedFile = $event.target.files[0]">
        <button :disabled="uploading || !selectedFile" @click="uploadFile">{{ uploading ? 'Đang xử lý...' : 'Tải lên và lập chỉ mục' }}</button>
      </section>

      <section class="card">
        <h2>Nguồn hiện có</h2>
        <p v-if="!sources.length" class="muted">Chưa có nguồn tri thức.</p>
        <article v-for="source in sources" :key="source.id" class="source">
          <div><strong>{{ source.title }}</strong><span :class="source.enabled ? 'on' : 'off'">{{ source.enabled ? 'Đang dùng' : 'Đã tắt' }}</span></div>
          <p class="source-preview">{{ preview(source.content) }}</p>
          <div class="actions source-actions">
            <button class="secondary icon-button" title="Xem đầy đủ" @click="viewingSource = source"><UiIcon name="eye" /> Xem</button>
            <button class="secondary icon-button" title="Sửa" @click="edit(source)"><UiIcon name="edit" /> Sửa</button>
            <button class="danger icon-button" title="Xóa" @click="remove(source.id)"><UiIcon name="trash" /> Xóa</button>
          </div>
        </article>
      </section>
      <section class="card">
        <h2>Brand Brain</h2>
        <div class="grid"><label>Tên thương hiệu<input v-model="brand.brandName"></label><label>Cách xưng hô<input v-model="brand.addressing"></label></div>
        <label>Giọng điệu<input v-model="brand.toneOfVoice"></label>
        <label>Quy tắc khi không biết<textarea v-model="brand.unknownAnswerRule" rows="2"></textarea></label>
        <label>Quy tắc không bịa dữ liệu<textarea v-model="brand.noFabricationRule" rows="2"></textarea></label>
        <label>Quy tắc chuyển nhân viên<textarea v-model="brand.handoffRule" rows="2"></textarea></label>
        <button @click="saveBrand">Lưu Brand Brain</button>
      </section>
      <section class="card">
        <h2>FAQ / Training Examples</h2>
        <input v-model="faqForm.question" placeholder="Câu hỏi mẫu"><textarea v-model="faqForm.idealAnswer" rows="3" placeholder="Câu trả lời lý tưởng"></textarea>
        <button @click="saveFaq">Thêm FAQ</button>
        <article v-for="faq in faqs" :key="faq.id" class="source"><strong>{{ faq.question }}</strong><p>{{ faq.idealAnswer }}</p><button class="danger" @click="removeFaq(faq.id)">Xóa</button></article>
      </section>
      <section class="card">
        <h2>AI Test</h2><textarea v-model="testQuestion" rows="3" placeholder="Nhập câu hỏi để kiểm tra Knowledge Base và Brand Brain"></textarea>
        <button :disabled="testing" @click="testAi">{{ testing ? 'Đang hỏi...' : 'Kiểm thử chatbot' }}</button><p v-if="testAnswer" class="answer">{{ testAnswer }}</p>
      </section>
      <section class="card"><h2>Feedback & Logs</h2><p class="muted">Tối đa 200 tương tác gần nhất; email và số điện thoại được che trước khi lưu.</p>
        <article v-for="log in logs" :key="log.id" class="source"><div><strong>{{ log.source }} · {{ log.requestType }}</strong><span>{{ log.helpful === true ? ' Hữu ích' : log.helpful === false ? ' Chưa tốt' : 'Chưa đánh giá' }}</span></div><p>Hỏi: {{ log.question }}</p><p>Đáp: {{ log.response }}</p></article>
      </section>
      <p v-if="message" class="toast">{{ message }}</p>
      <div v-if="viewingSource" class="modal-overlay" @click.self="viewingSource = null">
        <section class="source-modal" role="dialog" aria-modal="true" aria-labelledby="knowledge-source-title">
          <header><div><h2 id="knowledge-source-title">{{ viewingSource.title }}</h2><span :class="viewingSource.enabled ? 'on' : 'off'">{{ viewingSource.enabled ? 'Đang dùng' : 'Đã tắt' }}</span></div><button class="secondary close-button" title="Đóng" @click="viewingSource = null"><UiIcon name="x" /></button></header>
          <div class="source-full">{{ viewingSource.content }}</div>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import api from '@/services/api'
import { useToast } from '@/composables/useToast'
import { useDialog } from '@/composables/useDialog'

const sources = ref([]), faqs = ref([]), logs = ref([]), editingId = ref(null), message = ref('')
const viewingSource = ref(null)
const toast = useToast()
const { confirmDialog } = useDialog()
const selectedFile = ref(null), fileInput = ref(null), uploading = ref(false), uploadTitle = ref('')
const testQuestion = ref(''), testAnswer = ref(''), testing = ref(false)
const settings = reactive({ largePartyThreshold: 10, restaurantMaxCapacity: 200 })
const brand = reactive({ brandName: '', addressing: '', toneOfVoice: '', unknownAnswerRule: '', noFabricationRule: '', handoffRule: '' })
const faqForm = reactive({ question: '', idealAnswer: '', enabled: true })
const form = reactive({ title: '', content: '', type: 'TEXT', enabled: true })
const notify = text => { message.value = text; setTimeout(() => message.value = '', 2500) }
const preview = content => {
  const normalized = String(content || '').replace(/\s+/g, ' ').trim()
  return normalized.length > 140 ? `${normalized.slice(0, 140)}…` : normalized
}
async function load() {
  const [knowledge, config, brandRes, faqRes, logRes] = await Promise.all([api.get('/api/admin/ai/knowledge'), api.get('/api/settings/admin'), api.get('/api/admin/ai/knowledge/brand'), api.get('/api/admin/ai/knowledge/faqs'), api.get('/api/admin/ai/logs')])
  sources.value = knowledge.data; faqs.value = faqRes.data; logs.value = logRes.data; Object.assign(settings, config.data); Object.assign(brand, brandRes.data)
}
async function saveSettings() { await api.put('/api/settings/admin', settings); notify('Đã lưu cấu hình đặt bàn') }
function resetForm() { editingId.value = null; Object.assign(form, { title: '', content: '', type: 'TEXT', enabled: true }) }
function edit(source) { editingId.value = source.id; Object.assign(form, { title: source.title, content: source.content, type: source.type, enabled: source.enabled }); scrollTo({ top: 0, behavior: 'smooth' }) }
async function saveSource() {
  if (!form.title || !form.content) return notify('Vui lòng nhập tiêu đề và nội dung')
  if (editingId.value) await api.put(`/api/admin/ai/knowledge/${editingId.value}`, form)
  else await api.post('/api/admin/ai/knowledge', form)
  resetForm(); await load(); notify('Đã lưu nguồn tri thức')
}
async function uploadFile() { uploading.value = true; try { const data = new FormData(); data.append('file', selectedFile.value); if (uploadTitle.value) data.append('title', uploadTitle.value); await api.post('/api/admin/ai/knowledge/upload', data); selectedFile.value=null; uploadTitle.value=''; if(fileInput.value) fileInput.value.value=''; await load(); notify('Đã lập chỉ mục tài liệu') } finally { uploading.value=false } }
async function saveBrand() { await api.put('/api/admin/ai/knowledge/brand', brand); notify('Đã lưu Brand Brain') }
async function saveFaq() { if(!faqForm.question || !faqForm.idealAnswer) return notify('Nhập đủ câu hỏi và câu trả lời'); await api.post('/api/admin/ai/knowledge/faqs', faqForm); Object.assign(faqForm,{question:'',idealAnswer:'',enabled:true}); await load(); notify('Đã thêm FAQ') }
async function removeFaq(id) { await api.delete(`/api/admin/ai/knowledge/faqs/${id}`); await load(); notify('Đã xóa FAQ') }
async function testAi() { if(!testQuestion.value.trim()) return; testing.value=true; try { const {data}=await api.post('/api/chatbot/chat',{type:'SUPPORT',message:testQuestion.value,locale:'vi',history:''}); testAnswer.value=data.reply } finally { testing.value=false } }
async function remove(id) {
  if (!await confirmDialog({ title: 'Xóa nguồn tri thức', message: 'Bạn có chắc muốn xóa nguồn tri thức này?', confirmLabel: 'Xóa', danger: true })) return
  try { await api.delete(`/api/admin/ai/knowledge/${id}`); await load(); notify('Đã xóa') }
  catch (error) { toast.error(error.response?.data?.message || 'Không thể xóa nguồn tri thức.') }
}
onMounted(() => load().catch(() => notify('Không thể tải dữ liệu')))
</script>

<style scoped>
.page{max-width:1050px;margin:auto;padding:24px}.muted{color:var(--text-muted)}.card{background:var(--bg-card);border:1px solid var(--border);border-radius:14px;padding:22px;margin:18px 0;box-shadow:var(--shadow-sm)}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}label{display:grid;gap:7px;font-weight:600}input,textarea{width:100%;border:1px solid var(--border);border-radius:8px;padding:11px;font:inherit;margin:8px 0}input:focus,textarea:focus{outline:2px solid var(--primary-glow);border-color:var(--primary)}textarea{resize:vertical}button{background:var(--primary);color:var(--color-on-primary);border:0;border-radius:8px;padding:10px 16px;cursor:pointer}button:hover{background:var(--primary-dark)}button:disabled{opacity:.55}.actions{display:flex;gap:8px;flex-wrap:wrap}.secondary{background:var(--bg-card);border:1px solid var(--border);color:var(--primary)}.danger{background:var(--danger)}.check{display:flex;grid-template-columns:auto 1fr;align-items:center}.check input{width:auto}.source{border-top:1px solid var(--border-light);padding:14px 0}.source-preview{display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;margin:8px 0 12px}.source p,.answer{white-space:pre-wrap;color:var(--text-secondary)}.answer{background:var(--bg-card2);padding:16px;border-radius:8px}.source span,.source-modal span{margin-left:10px;font-size:12px;padding:3px 7px;border-radius:10px}.on{background:color-mix(in srgb,var(--success) 14%,transparent);color:var(--success)}.off{background:var(--color-surface-container)}.icon-button{display:inline-flex;align-items:center;justify-content:center;gap:8px}.toast{position:fixed;right:25px;bottom:25px;background:var(--color-inverse-surface);color:var(--color-inverse-on-surface);padding:12px 18px;border-radius:8px}.modal-overlay{position:fixed;inset:0;z-index:1200;background:rgba(28,8,12,.6);display:flex;align-items:center;justify-content:center;padding:20px}.source-modal{width:min(760px,100%);max-height:85vh;overflow:auto;background:var(--bg-card);border:1px solid var(--border);border-radius:14px;box-shadow:var(--shadow-lg);padding:22px}.source-modal header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;border-bottom:1px solid var(--border-light);padding-bottom:14px}.source-modal h2{margin:0;display:inline}.close-button{width:40px;height:40px;padding:0;display:flex;align-items:center;justify-content:center}.source-full{white-space:pre-wrap;line-height:1.65;color:var(--text-primary);padding-top:18px}@media(max-width:650px){.grid{grid-template-columns:1fr}}
</style>
