<template>
  <CustomerLayout>
  <div class="dine-in-wrapper">
    <main class="main-content">
      <div style="margin-bottom: 20px;">
        <button v-if="userRoles.includes('ROLE_WAITER')" @click="$router.push('/waiter')" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← {{ text.backWaiter }}
        </button>
        <button v-else @click="$router.back()" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← {{ text.back }}
        </button>
      </div>
      <div class="table-selection-box">
        <label>{{ text.tableQuestion }}</label>
        <select v-model="selectedTable" class="form-control table-select" :disabled="isTableLocked">
          <option value="" disabled>-- {{ text.selectTable }} --</option>
          <optgroup v-for="(tables, floor) in groupedTables" :key="floor" :label="floor">
          <!-- ĐÃ NÂNG CẤP: Hiện trạng thái và chặn khách chọn bàn đã có người -->
          <option 
              v-for="table in tables"
              :key="table.id"
              :value="table.name"
              :disabled="table.isOccupied !== 0 && table.name !== selectedTable"
            >
              {{ tableName(table) }} {{ table.isOccupied === 0 ? `(${text.empty})` : (table.isOccupied === 1 ? `(${text.reserved})` : `(${text.occupied})`) }}
          </option>
        </optgroup>
        </select>
        <p v-if="isTableLocked" style="color:var(--primary); font-size: 0.85rem; margin-top: 5px;">{{ text.qrLocked }}</p>
      </div>
      <div class="product-list" v-if="selectedTable">
        <div class="menu-column">
        <!-- AI Suggestion Section -->
        <div class="ai-suggestion-box">
          <div class="ai-header">
                  <h3>{{ text.smartSuggestion }}</h3>
                  <span class="ai-badge">{{ text.aiSuggestion }}</span>
          </div>
          
          <div v-if="aiCombo.length === 0 && !isFetchingAI" class="smart-profile">
                <p class="ai-desc">{{ text.aiDescription }}</p>
            <label class="smart-field">
                    <span>{{ text.people }}</span>
                    <input type="number" v-model.number="partySize" min="1" max="100" :placeholder="text.guestPlaceholder" class="form-control" />
            </label>
            <div class="smart-field">
                    <span>{{ text.favorite }}</span>
              <div class="smart-chips">
                <button v-for="option in favoriteOptions" :key="option.value" type="button"
                  :class="{ active: favoritePreferences.includes(option.value) }"
                  @click="toggleFavorite(option.value)">{{ option.label }}</button>
              </div>
            </div>
            <div class="smart-field">
                    <span>{{ text.palate }}</span>
              <div class="smart-chips">
                <button v-for="option in palateOptions" :key="option.value" type="button"
                  :class="{ active: palatePreference === option.value }"
                  @click="palatePreference = option.value">{{ option.label }}</button>
              </div>
            </div>
            <label class="smart-field">
                    <span>{{ text.allergies }}</span>
                    <input v-model.trim="allergyInput" maxlength="160" class="form-control" :placeholder="text.allergyPlaceholder" />
            </label>
                <button class="btn-add-item smart-submit" @click="fetchComboForParty">{{ text.suggest }}</button>
          </div>
          <div v-else-if="isFetchingAI">
                <p class="ai-desc">{{ t('dineIn.analyzing', { count: partySize }) }}</p>
          </div>
          <div v-else>
            <p class="ai-desc">{{ aiRecommendationReason }}</p>
            
            <div class="combo-grid">
              <div v-for="product in aiCombo" :key="'ai-'+product.id" class="combo-item">
                    <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
                <div class="product-info">
                      <h4>{{ productName(product) }} <span v-if="product.suggestedQuantity > 1" style="color: var(--primary);">x{{ product.suggestedQuantity }}</span></h4>
                      <p class="price">{{ formatCurrency(product.price) }}</p>
                  <small>{{ product.suggestionReason }}</small>
                </div>
                    <button v-if="!isAdminOrManager" class="btn-add-item" :disabled="product.availableQuantity <= 0" @click="addToCart(product, product.suggestedQuantity || 1)">{{ product.availableQuantity > 0 ? text.add : text.soldOut }}</button>
                    <button v-else class="btn-add-item btn-disabled" disabled>{{ text.viewOnly }}</button>
              </div>
            </div>
            <div class="ai-action" style="display: flex; gap: 10px; justify-content: center;">
                  <button v-if="!isAdminOrManager" class="btn-add-combo" @click="addComboToCart">{{ text.addCombo }}</button>
                  <button v-else class="btn-add-combo btn-disabled" disabled>{{ text.viewOnly }}</button>
                  <button class="btn-cancel" style="padding: 10px 20px; border-radius: 20px;" @click="aiCombo = []">{{ text.retry }}</button>
            </div>
          </div>
        </div>

        <!-- Món ăn bán chạy / Gợi ý -->
        <div v-if="suggestedProducts.length > 0" class="suggested-section">
            <h3 class="section-title"><span style="color: var(--color-tertiary)"><UiIcon name="sparkles" /></span> {{ text.popular }}</h3>
          <div class="suggested-grid">
            <div v-for="product in suggestedProducts" :key="'sugg-'+product.id" class="suggested-card">
              <div class="sugg-badge">HOT</div>
                <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
              <div class="sugg-info">
                  <h4>{{ productName(product) }}</h4>
                  <p class="price">{{ formatCurrency(product.price) }}</p>
              </div>
                <button v-if="!isAdminOrManager" class="btn-sugg-add" :disabled="product.availableQuantity <= 0" @click="addToCart(product, 1)">{{ product.availableQuantity > 0 ? text.addNow : text.soldOut }}</button>
            </div>
          </div>
        </div>

          <h3 class="section-title">{{ text.fullMenu }}</h3>
        <div v-for="product in activeProducts" :key="product.id" class="product-item">
            <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
          <div class="product-info">
              <h4>{{ productName(product) }}</h4>
              <p class="price">{{ formatCurrency(product.price) }}</p>
              <small v-if="product.availableQuantity > 0">{{ t('dineIn.remaining', { count: product.availableQuantity }) }}</small>
          </div>
            <button v-if="!isAdminOrManager" class="btn-add-item" :disabled="product.availableQuantity <= 0" @click="addToCart(product)">{{ product.availableQuantity > 0 ? text.add : text.soldOut }}</button>
            <button v-else class="btn-add-item btn-disabled" disabled>{{ text.viewOnly }}</button>
        </div>
        </div>
        <aside v-if="cart.length > 0" class="inline-cart" :aria-label="text.selectedOrder">
          <h3>{{ text.selectedOrder }}</h3>
          <div v-for="(item, idx) in cart" :key="item.productId" class="inline-cart-item">
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ formatCurrency(item.price * item.quantity) }}</span>
            </div>
            <div class="inline-cart-controls">
              <button type="button" :aria-label="text.decrease" @click="decreaseQty(idx)">−</button>
              <b>{{ item.quantity }}</b>
              <button type="button" :aria-label="text.increase" @click="increaseQty(idx)">+</button>
              <button type="button" class="inline-remove" :aria-label="text.remove" @click="cart.splice(idx, 1)">×</button>
            </div>
          </div>
          <div class="inline-cart-total">{{ text.subtotal }} <strong>{{ formatCurrency(cartSubtotal) }}</strong></div>
          <button type="button" class="btn-checkout" @click="showModal = true">{{ text.sendKitchen }}</button>
        </aside>
      </div>
      <div v-else class="empty-state">
        <p>{{ text.selectTableHint }}</p>
      </div>

      <!-- FAB Voice Order -->
      <div v-if="selectedTable && !isAdminOrManager" class="fab-mic" @click="startVoiceOrder" :class="{'recording': isListening}">
        <UiIcon name="mic" />
      </div>

      <!-- Voice Modal -->
      <div v-if="showVoiceModal" class="modal-overlay voice-modal">
        <div class="voice-box">
            <div class="mic-icon" :class="{'pulse': isListening}"><UiIcon name="mic" /></div>
          <h3>{{ text.voiceAssistant }}</h3>
          <p class="voice-text">{{ voiceText }}</p>
          <button class="btn-cancel" style="margin-top:20px;" @click="closeVoiceModal">{{ text.cancel }}</button>
        </div>
      </div>
    </main>

    <div class="sticky-bottom-cart" v-if="cart.length > 0">
      <div class="cart-summary" @click="showModal = true">
        <span class="cart-icon"> <span class="badge">{{ totalItems }}</span></span>
        <div class="cart-text">
          <span class="cart-price">{{ formatCurrency(cartSubtotal) }}</span>
        </div>
      </div>
      <button class="btn-checkout" @click="showModal = true">{{ text.sendKitchen }}</button>
    </div>

    <!-- Xác nhận gọi món Modal -->
    <div v-if="showModal" class="g-modal-overlay" @click.self="showModal = false">
      <div class="g-modal-box" style="max-width: 550px; max-height: 90vh; overflow-y: auto;">
        <h3>{{ text.confirmOrder }}</h3>
        
        <div class="cart-details" style="max-height: 300px; overflow-y: auto; margin-bottom: 15px; padding-right: 10px;">
          <div v-for="(item, idx) in cart" :key="idx" class="cart-item-row">
            <div class="cart-item-info">
              <span class="cart-item-name">{{ item.name }}</span>
              <span class="cart-item-price">{{ formatCurrency(item.price * item.quantity) }}</span>
              <label class="dish-note-label">
                {{ text.dishNote }}
                <input v-model.trim="item.note" maxlength="500" :placeholder="text.dishNotePlaceholder" />
              </label>
              <label class="dish-note-label allergy-note-label">
                {{ text.allergyNote }}
                <input v-model.trim="item.allergyNote" maxlength="500" :placeholder="text.allergyNotePlaceholder" />
              </label>
            </div>
            <div class="cart-item-controls">
              <button class="qty-btn qty-minus" @click="decreaseQty(idx)">−</button>
              <span class="qty-display">{{ item.quantity }}</span>
              <button class="qty-btn qty-plus" @click="increaseQty(idx)">+</button>
              <button class="qty-btn qty-remove" @click="cart.splice(idx, 1)"><UiIcon name="trash" /></button>
            </div>
          </div>
        </div>

        <div class="cart-total" style="margin-top: 20px; text-align: center;">
          <h4 style="color: var(--primary); font-size: 1.1rem; border-top: 1px dashed color-mix(in srgb, var(--secondary) 30%, transparent); padding-top: 10px;">{{ text.subtotal }}: {{ formatCurrency(cartSubtotal) }}</h4>
          <h4 style="color: var(--primary); font-size: 1.1rem;">{{ text.vat }}: {{ formatCurrency(cartTax) }}</h4>
          <h4 style="color: var(--primary); font-size: 1.4rem; margin-top: 5px;">{{ text.total }}: {{ formatCurrency(finalTotal) }}</h4>
          <p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 5px;">{{ text.payAfterMeal }}</p>
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="showModal = false" class="g-btn-outline" style="flex:1;">{{ text.back }}</button>
          <button @click="submitOrder" :disabled="isSubmitting" class="g-btn-primary" style="flex:1;">
            {{ isSubmitting ? text.sending : text.sendNow }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast thông báo -->
    <div v-if="toastMsg" class="toast-notification">{{ toastMsg }}</div>
  </div>
  </CustomerLayout>
</template>

<script setup>
import CustomerLayout from '@/components/CustomerLayout.vue';
import UiIcon from '@/components/UiIcon.vue';

import { ref, computed, onMounted } from 'vue';
import api from '@/services/api';
import { useRoute } from 'vue-router';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';
import { useI18n } from 'vue-i18n';
import { useFormatters } from '@/composables/useFormatters';

const route = useRoute();
const { locale, tm, t } = useI18n();
const { formatCurrency } = useFormatters();
const text = computed(() => tm('dineIn'));
const products = ref([]);
const suggestedProducts = ref([]);
const allTables = ref([]);
const cart = ref([]);
const selectedTable = ref("");
const isTableLocked = ref(false);
const showModal = ref(false);
const toastMsg = ref('');
const isSubmitting = ref(false);
const userRoles = ref([]);
const addItemsIdempotencyKey = ref(crypto.randomUUID());
const checkoutIdempotencyKey = ref(crypto.randomUUID());
const tableSessionToken = ref(typeof route.query.cap === 'string' ? route.query.cap : '');
const capabilityOrder = ref(null);

const isAdminOrManager = computed(() => {
  return userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_MANAGER');
});

// AI Voice
const isListening = ref(false);
const showVoiceModal = ref(false);
const voiceText = ref('');
let recognition = null;

// Membership Tier Logic
const userProfile = ref(null);
const tierDiscount = ref(0);
const voucherDiscountPercent = ref(0);

const activeProducts = computed(() => products.value.filter(p => p.status !== false));
const productName = product => locale.value === 'en'
  ? (product?.nameEn || product?.nameVi || product?.name)
  : (product?.nameVi || product?.name || product?.nameEn);
const tableName = table => table?.code || table?.name || '';

// Gom nhóm bàn theo tầng để khách dễ tìm trong thẻ <select>
const groupedTables = computed(() => {
  const groups = {};
  allTables.value.forEach(t => {
    if (!groups[t.floor]) groups[t.floor] = [];
    groups[t.floor].push(t);
  });
  return groups;
});

const totalItems = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0));
const cartSubtotal = computed(() => cart.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));

const discountAmount = computed(() => {
  let discount = tierDiscount.value;
  if (voucherDiscountPercent.value > 0) discount += (voucherDiscountPercent.value / 100);
  if (discount > 1) discount = 1;
  return cartSubtotal.value * discount;
});

const cartTax = computed(() => {
  let discount = tierDiscount.value;
  if (voucherDiscountPercent.value > 0) discount += (voucherDiscountPercent.value / 100);
  if (discount > 1) discount = 1;
  return cart.value.reduce((sum, item) => sum + ((item.price * item.quantity * (1 - discount)) * (item.taxRate || 8) / 100), 0);
});

const finalTotal = computed(() => {
  return cartSubtotal.value - discountAmount.value + cartTax.value;
});

// AI Suggestion Logic
const aiCombo = ref([]);
const aiRecommendationReason = ref('');
const partySize = ref('');
const isFetchingAI = ref(false);
const favoritePreferences = ref([]);
const palatePreference = ref('không yêu cầu');
const allergyInput = ref('');
const favoriteOptions = computed(() => [
  ['thịt bò', 'beef'], ['gà', 'chicken'], ['hải sản', 'seafood'], ['nướng', 'grilled'],
  ['món nước', 'soup'], ['rau', 'vegetables'], ['đồ uống', 'drinks'], ['không quan trọng', 'any'],
].map(([value, key]) => ({ value, label: text.value.preferences[key] })));
const palateOptions = computed(() => [
  ['không cay', 'notSpicy'], ['cay nhẹ', 'mild'], ['cay vừa', 'medium'], ['cay nhiều', 'hot'],
  ['thanh nhẹ', 'light'], ['đậm vị', 'rich'], ['ít dầu', 'lowOil'], ['không yêu cầu', 'none'],
].map(([value, key]) => ({ value, label: text.value.preferences[key] })));

const toggleFavorite = (value) => {
  if (value === 'không quan trọng') {
    favoritePreferences.value = favoritePreferences.value.includes(value) ? [] : [value];
    return;
  }
  favoritePreferences.value = favoritePreferences.value.filter(item => item !== 'không quan trọng');
  favoritePreferences.value = favoritePreferences.value.includes(value)
    ? favoritePreferences.value.filter(item => item !== value)
    : [...favoritePreferences.value, value];
};

const fetchComboForParty = async () => {
  if (!partySize.value || partySize.value < 1) {
    toastMsg.value = t('dineIn.invalidGuests');
    return;
  }
  isFetchingAI.value = true;
  try {
    const preferences = [
      ...favoritePreferences.value.filter(value => value !== 'không quan trọng'),
      ...(palatePreference.value === 'không yêu cầu' ? [] : [palatePreference.value])
    ];
    const allergies = allergyInput.value.split(',').map(value => value.trim()).filter(Boolean);
    const response = await api.post('/api/customer/ai/menu-suggestion', {
      productIds: cart.value.map(item => item.productId), guestCount: Number(partySize.value), preferences, maxBudget: null, allergies
    });
    aiCombo.value = (response.data?.suggestions || []).map((suggestion, index) => {
      const product = activeProducts.value.find(item => item.id === suggestion.productId && item.availableQuantity > 0);
      if (!product) return null;
      return {
        ...product,
        suggestedQuantity: Math.max(1, Math.ceil(Number(partySize.value) / Math.max(2, Math.min(4, response.data.suggestions.length))) - (index > 1 ? 1 : 0)),
        suggestionReason: t('dineIn.suggestionFallback', { count: 1 })
      };
    }).filter(Boolean).slice(0, 4);
    aiRecommendationReason.value = response.data?.message
      || t('dineIn.suggestionFallback', { count: aiCombo.value.length });
    if (!aiCombo.value.length) toastMsg.value = t('dineIn.noSuggestion');

  } catch(e) {
    console.error("Lỗi AI Recommend:", e);
    aiCombo.value = [];
    toastMsg.value = locale.value === 'vi' && e.response?.data?.message ? e.response.data.message : t('dineIn.suggestionFailed');
  } finally {
    isFetchingAI.value = false;
  }
};

const addComboToCart = () => {
  aiCombo.value.forEach(p => addToCart(p, p.suggestedQuantity || 1));
  toastMsg.value = t('dineIn.comboAdded');
};

const loadData = async () => {
  try {
    const [resProd, resTable] = await Promise.all([
      api.get('/api/products'),
      api.get('/api/tables') // Lấy danh sách bàn
    ]);
    products.value = resProd.data;
    allTables.value = resTable.data;

    if (tableSessionToken.value) {
      try {
        const resolved = (await api.get('/api/table-sessions/resolve', {
          headers: { 'X-Table-Session-Token': tableSessionToken.value }
        })).data;
        const table = allTables.value.find(item => Number(item.id) === Number(resolved.tableId));
        if (!table) throw new Error(t('dineIn.tableUnavailable'));
        selectedTable.value = table.name;
        capabilityOrder.value = resolved.currentOrder || null;
        isTableLocked.value = true;
      } catch (error) {
        tableSessionToken.value = '';
        toastMsg.value = locale.value === 'vi' && error.response?.data?.message ? error.response.data.message : (error.message || t('dineIn.invalidQr'));
      }
    } else if (route.query.table && sessionStorage.getItem('staff_token')) {
      // Staff may open a table directly; public customers must use a capability QR.
      selectedTable.value = route.query.table;
      isTableLocked.value = true;
    }

    // Lấy thông tin User để áp dụng hạng thẻ
    const token = sessionStorage.getItem('staff_token');
    if (token) {
      const resProfile = await api.get('/api/auth/profile', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      userProfile.value = resProfile.data;
      if (userProfile.value.membershipTier === 'Kim Cương') tierDiscount.value = 0.15;
      else if (userProfile.value.membershipTier === 'Vàng') tierDiscount.value = 0.10;
      else if (userProfile.value.membershipTier === 'Bạc') tierDiscount.value = 0.05;
    }
    
    const storedUser = sessionStorage.getItem('staff_user');
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser);
        if (parsed && parsed.roles) {
          userRoles.value = parsed.roles;
        }
      } catch (error) {
        console.warn('Không thể đọc vai trò nhân viên đã lưu.', error)
      }
    }
    
    // Tải Món Gợi Ý (Bán Chạy)
    try {
      const response = await api.get('/api/menu/hot?limit=4');
      if (response.data && response.data.length > 0) {
         suggestedProducts.value = response.data
           .map(item => products.value.find(p => p.id === item.productId))
           .filter(p => p != null).slice(0, 4);
      }
    } catch (err) { console.warn('Lỗi lấy gợi ý: ', err); }

  } catch (error) { console.error(error); }
};

const addToCart = (product, qty = 1) => {
  const existing = cart.value.find(item => item.productId === product.id);
  const availableQuantity = Math.max(0, Number(product.availableQuantity || 0));
  const requestedQuantity = (existing?.quantity || 0) + qty;
  if (requestedQuantity > availableQuantity) {
    toastMsg.value = t('dineIn.quantityLimit', { count: availableQuantity });
    return;
  }
  if (existing) existing.quantity = requestedQuantity;
  else cart.value.push({ productId: product.id, name: productName(product), price: product.price, quantity: qty,
    taxRate: product.taxRate || 8, note: '', allergyNote: '', availableQuantity });
  checkoutIdempotencyKey.value = crypto.randomUUID();
  addItemsIdempotencyKey.value = crypto.randomUUID();
};

const increaseQty = (idx) => {
  const item = cart.value[idx];
  if (item.quantity >= item.availableQuantity) {
    toastMsg.value = t('dineIn.quantityLimit', { count: item.availableQuantity });
    return;
  }
  item.quantity++;
  checkoutIdempotencyKey.value = crypto.randomUUID();
  addItemsIdempotencyKey.value = crypto.randomUUID();
};

const decreaseQty = (idx) => {
  if (cart.value[idx].quantity > 1) {
    cart.value[idx].quantity--;
  } else {
    cart.value.splice(idx, 1);
  }
  checkoutIdempotencyKey.value = crypto.randomUUID();
  addItemsIdempotencyKey.value = crypto.randomUUID();
};

const closeVoiceModal = () => {
  showVoiceModal.value = false;
  isListening.value = false;
  if (recognition) recognition.stop();
};

const startVoiceOrder = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    toastMsg.value = t('dineIn.speechUnsupported');
    return;
  }
  
  if (!recognition) {
    recognition = new SpeechRecognition();
    recognition.lang = locale.value === 'vi' ? 'vi-VN' : 'en-US';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      isListening.value = true;
      showVoiceModal.value = true;
      voiceText.value = t('dineIn.listening');
    };

    recognition.onresult = async (event) => {
      isListening.value = false;
      const transcript = event.results[0][0].transcript;
      voiceText.value = t('dineIn.heard', { text: transcript });
      
      try {
        // Gửi danh sách tên món cho AI
        const menuStr = activeProducts.value.map(p => `${p.id}: ${productName(p)}`).join(', ');
        const res = await api.post('/api/chatbot/chat', {
          message: transcript,
          type: 'VOICE_ORDER',
          menu: menuStr
        });
        
        let reply = res.data.reply || '';
        reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
        
        const items = JSON.parse(reply);
        if (!Array.isArray(items) || items.length === 0) {
          voiceText.value = t('dineIn.voiceNoMatch');
          setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
          return;
        }
        
        let addedNames = [];
        items.forEach(item => {
          const prod = activeProducts.value.find(p => p.id === item.productId);
          if (prod) {
            addToCart(prod, item.quantity || 1);
            addedNames.push(`${item.quantity || 1} ${productName(prod)}`);
          }
        });
        
        if (addedNames.length > 0) {
          voiceText.value = t('dineIn.voiceAdded', { items: addedNames.join(', ') });
        } else {
          voiceText.value = t('dineIn.voiceNoMatch');
        }
        setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3500);
        
      } catch (e) {
        voiceText.value = t('dineIn.voiceError');
        setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
      }
    };

    recognition.onerror = (event) => {
      voiceText.value = t('dineIn.speechError', { error: event.error });
      isListening.value = false;
      setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
    };

    recognition.onend = () => {
      isListening.value = false;
      if(showVoiceModal.value && voiceText.value === t('dineIn.listening')) {
          voiceText.value = t('dineIn.voiceNothing');
          setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 2000);
      }
    };
  }

  recognition.start();
};

const submitOrder = async () => {
  if (cart.value.length === 0) {
    toastMsg.value = t('dineIn.emptyCart');
    return;
  }
  if (!selectedTable.value) {
    toastMsg.value = t('dineIn.selectBeforeSend');
    return;
  }
  if (isSubmitting.value) return;
  
  const token = sessionStorage.getItem('staff_token') || '';
  const formattedItems = cart.value.map(item => ({
    productId: item.productId,
    quantity: item.quantity,
    note: item.note || null,
    allergyNote: item.allergyNote || null
  }));

  try {
    isSubmitting.value = true;
    const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
    let existingOrder = capabilityOrder.value;
    if (!existingOrder && token) {
      const selectedTableRecord = allTables.value.find(table => table.name === selectedTable.value);
      if (!selectedTableRecord) throw new Error(t('dineIn.tableNotFound'));
      try { existingOrder = (await api.get('/api/orders/open-by-table', { params: { tableId: selectedTableRecord.id }, headers })).data; } catch (lookupError) {
        if (lookupError.response?.status !== 404) throw lookupError;
      }
    }
    if (existingOrder?.id) {
      const addItemsUrl = tableSessionToken.value
        ? `/api/table-sessions/orders/${existingOrder.id}/add-items`
        : `/api/orders/${existingOrder.id}/add-items`;
      await api.put(addItemsUrl, { items: formattedItems }, {
        headers: {
          ...headers,
          ...(tableSessionToken.value ? { 'X-Table-Session-Token': tableSessionToken.value } : {}),
          'X-Idempotency-Key': addItemsIdempotencyKey.value
        }
      });
    } else {
      const selectedTableRecord = allTables.value.find(table => table.name === selectedTable.value);
      if (!selectedTableRecord) throw new Error(t('dineIn.tableNotFound'));
      const created = await api.post('/api/orders/checkout', {
      address: null,
      tableId: selectedTableRecord.id,
      orderType: 'DINE_IN',
      paymentOption: 'PAY_AT_RESTAURANT',
      items: formattedItems
      }, {
        headers: {
          ...headers,
          ...(tableSessionToken.value ? { 'X-Table-Session-Token': tableSessionToken.value } : {}),
          'X-Idempotency-Key': checkoutIdempotencyKey.value
        }
      });
      if (tableSessionToken.value && created.data?.orderId) {
        capabilityOrder.value = { id: created.data.orderId };
      }
    }

    cart.value = [];
    addItemsIdempotencyKey.value = crypto.randomUUID();
    checkoutIdempotencyKey.value = crypto.randomUUID();
    showModal.value = false;
    toastMsg.value = t('dineIn.orderRecorded');
    setTimeout(() => { toastMsg.value = ''; }, 4000);
  } catch (error) {
    const payload = error.response?.data;
    const message = typeof payload === 'string'
      ? payload
      : (locale.value === 'vi' ? payload?.message : null) || t('dineIn.orderFailed');
    const reference = payload?.correlationId ? ` ${t('dineIn.supportCode', { code: payload.correlationId })}` : '';
    toastMsg.value = `${message}${reference}`;
    setTimeout(() => { toastMsg.value = ''; }, 6000);
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(loadData);
</script>

<style scoped>
/* Cart Item Controls */
.cart-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed rgba(255,255,255,0.1);
  padding: 10px 0;
  gap: 10px;
}
.cart-item-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.cart-item-name {
  font-weight: 600;
  color: var(--text-heading);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cart-item-price {
  font-size: 0.85rem;
  color: var(--primary);
  font-weight: 700;
}
.dish-note-label {
  display: grid;
  gap: 4px;
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 600;
}
.dish-note-label input {
  width: 100%;
  min-height: 34px;
  box-sizing: border-box;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--bg-input);
  color: var(--text-primary);
}
.allergy-note-label { color: var(--danger); }
.cart-item-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.qty-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--secondary) 10%, transparent);
}
.qty-minus:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
}
.qty-remove {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 0.9rem;
  margin-left: 4px;
}
.qty-remove:hover {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 15%, transparent);
}
.qty-display {
  min-width: 28px;
  text-align: center;
  font-weight: 800;
  font-size: 1rem;
  color: var(--text-heading);
}
.dine-in-wrapper { font-family: var(--font-primary); background-color: var(--bg-root); min-height: 100vh; padding-bottom: 80px; color: var(--text-primary); }
.smart-profile { display: grid; gap: 14px; }
.smart-field { display: grid; gap: 7px; color: var(--text-secondary); font-size: .84rem; font-weight: 750; }
.smart-field input { max-width: 360px; background: var(--bg-input); color: var(--text-primary); border-color: var(--border); }
.smart-chips { display: flex; flex-wrap: wrap; gap: 7px; }
.smart-chips button { min-height: 34px; padding: 6px 11px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-card); color: var(--text-secondary); font: inherit; cursor: pointer; }
.smart-chips button.active { background: var(--primary); border-color: var(--primary); color: var(--color-on-primary); }
.smart-submit { justify-self: start; min-width: 160px; }
.combo-item small { display: block; margin-top: 4px; color: var(--text-muted); line-height: 1.4; }

/* Navbar */
.dinein-navbar {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky; top: 0; z-index: 100;
  padding: 12px 40px;
}
.nav-container {
  max-width: 1400px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
}
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo p { margin: 0; font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }

.nav-right { display: flex; align-items: center; gap: 10px; }
.btn-nav-dinein {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-dinein:hover { border-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }

.main-content { padding: 15px; max-width: 1120px; margin: 0 auto; }
.product-list { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 20px; align-items: start; }
.menu-column { min-width: 0; }
.inline-cart { position: sticky; top: 16px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 8px; padding: 16px; box-shadow: var(--shadow-md); }
.inline-cart h3 { margin: 0 0 12px; color: var(--text-heading); font-size: 1.1rem; }
.inline-cart-item { display: flex; justify-content: space-between; gap: 8px; padding: 10px 0; border-bottom: 1px solid var(--border); }
.inline-cart-item strong, .inline-cart-item span { display: block; max-width: 142px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.inline-cart-item span { color: var(--primary); font-size: .86rem; margin-top: 3px; }
.inline-cart-controls { display: flex; align-items: center; gap: 5px; flex-shrink: 0; }
.inline-cart-controls button { width: 30px; height: 30px; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-root); color: var(--text-primary); font-weight: 700; cursor: pointer; }
.inline-cart-controls .inline-remove { color: var(--primary); border-color: color-mix(in srgb, var(--primary) 40%, transparent); }
.inline-cart-total { display: flex; justify-content: space-between; margin: 14px 0; color: var(--text-heading); }
.inline-cart .btn-checkout { width: 100%; min-height: 44px; border-radius: 6px; }
.table-selection-box { background: var(--bg-card); padding: 15px; border-radius: 10px; box-shadow: var(--shadow-md); margin-bottom: 20px; border-left: 4px solid var(--primary); border: 1px solid var(--border-light); }
.table-selection-box label { font-weight: bold; display: block; margin-bottom: 10px; color: var(--text-heading); }
.table-select { width: 100%; padding: 12px; border: 1px solid var(--border); background: var(--bg-input); color: var(--text-primary); border-radius: 8px; font-size: 1rem; }

.section-title { color: var(--primary); border-bottom: 2px solid var(--border); padding-bottom: 10px; margin-bottom: 15px; text-transform: uppercase; letter-spacing: 1px; }
.product-item { display: flex; background: var(--bg-card); padding: 15px; border-radius: 10px; margin-bottom: 15px; box-shadow: var(--shadow-md); align-items: center; gap: 15px; border: 1px solid var(--border-light); transition: transform 0.2s;}
.product-item:hover { transform: translateY(-2px); border-color: var(--primary); box-shadow: 0 4px 15px var(--primary-glow); }
.product-item img { width: 80px; height: 80px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); }
.product-info { flex: 1; }
.product-info h4 { margin: 0 0 5px 0; color: var(--text-heading); font-size: 1.1rem; }
.price { color: var(--primary); font-weight: bold; margin: 0; font-size: 1rem; }
.btn-add-item { background: var(--primary-glow); color: var(--primary); border: 1px solid var(--primary); padding: 8px 15px; border-radius: 20px; font-weight: bold; cursor: pointer; transition: 0.3s;}
.btn-add-item:hover { background: var(--primary); color: var(--bg-dark); }
.btn-disabled { opacity: 0.5; cursor: not-allowed !important; background: var(--bg-root); color: var(--text-muted); border: 1px solid var(--border); }

.empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); background: var(--bg-card); border-radius: 10px; border: 1px dashed var(--border); }

/* Suggested Section */
.suggested-section { margin-bottom: 25px; text-align: left; background: color-mix(in srgb, var(--color-tertiary) 5%, transparent); padding: 15px; border-radius: 12px; border: 1px solid color-mix(in srgb, var(--color-tertiary) 20%, transparent); }
.suggested-section .section-title { font-size: 1.2rem; margin-bottom: 15px; color: var(--color-tertiary); border-bottom: none; }
.suggested-grid { display: flex; gap: 15px; overflow-x: auto; padding-bottom: 10px; }
.suggested-grid::-webkit-scrollbar { height: 6px; }
.suggested-grid::-webkit-scrollbar-thumb { background: var(--color-tertiary); border-radius: 10px; }
.suggested-card { min-width: 160px; background: rgba(0,0,0,0.5); border-radius: 10px; padding: 10px; display: flex; flex-direction: column; position: relative; border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent); transition: 0.3s; }
.suggested-card:hover { transform: translateY(-3px); box-shadow: 0 5px 15px color-mix(in srgb, var(--color-tertiary) 20%, transparent); }
.sugg-badge { position: absolute; top: -5px; right: -5px; background: var(--primary); color: #FFFFFF; padding: 3px 6px; border-radius: 6px; font-weight: 900; font-size: 0.7rem; transform: rotate(10deg); box-shadow: 0 2px 5px color-mix(in srgb, var(--primary) 50%, transparent); }
.suggested-card img { width: 100%; height: 100px; object-fit: cover; border-radius: 8px; margin-bottom: 10px; }
.sugg-info { flex: 1; }
.sugg-info h4 { margin: 0 0 5px 0; font-size: 0.95rem; color: #FFFFFF; }
.sugg-info .price { color: var(--color-tertiary); font-weight: bold; font-size: 1rem; margin: 0; }
.btn-sugg-add { background: var(--color-tertiary); color: var(--text-primary); border: none; padding: 6px; border-radius: 6px; font-weight: bold; margin-top: 10px; cursor: pointer; transition: 0.3s; font-size: 0.85rem;}
.btn-sugg-add:hover { background: #FFFFFF; }

/* AI Suggestion */
.ai-suggestion-box {
  background: var(--bg-card2);
  border: 1px solid var(--primary);
  padding: 15px;
  border-radius: 12px;
  margin-bottom: 25px;
  box-shadow: 0 4px 15px var(--primary-glow);
}
.ai-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.ai-header h3 { margin: 0; color: var(--primary); font-size: 1.1rem; font-weight: 900; }
.ai-badge { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: var(--bg-dark); padding: 3px 8px; border-radius: 12px; font-size: 0.7rem; font-weight: bold; }
.ai-desc { margin: 0 0 15px 0; font-size: 0.85rem; color: var(--text-secondary); font-style: italic; }
.combo-grid { display: flex; flex-direction: column; gap: 10px; }
.combo-item {
  display: flex; background: var(--bg-card); padding: 10px; border-radius: 8px; align-items: center; gap: 12px;
  border: 1px solid var(--border);
}
.combo-item img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; border: 1px solid var(--border); }
.ai-action { margin-top: 15px; text-align: center; }
.btn-add-combo {
  background: linear-gradient(135deg, var(--primary), var(--secondary)); color: var(--bg-dark); border: none; padding: 10px 20px; border-radius: 20px;
  font-weight: bold; width: 100%; cursor: pointer; box-shadow: 0 4px 10px var(--primary-glow);
}

.sticky-bottom-cart { position: fixed; bottom: 0; left: 0; right: 0; background: var(--bg-nav); padding: 15px 20px; box-shadow: 0 -4px 20px rgba(0,0,0,0.5); display: flex; justify-content: space-between; align-items: center; z-index: 101; max-width: 600px; margin: 0 auto; border-top-left-radius: 15px; border-top-right-radius: 15px; border-top: 1px solid var(--border);}
.cart-summary { display: flex; align-items: center; gap: 15px; cursor: pointer; flex: 1;}
.cart-icon { font-size: 1.8rem; position: relative; }
.cart-icon .badge {
  position: absolute; top: -5px; right: -10px; background: var(--primary); color: #FFFFFF;
  font-size: 0.75rem; font-weight: bold; padding: 2px 6px; border-radius: 50%;
}
.cart-text { display: flex; flex-direction: column; }
.cart-price { font-size: 1.2rem; font-weight: 800; color: #FFFFFF; }
.cart-discount-badge { font-size: 0.75rem; background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); padding: 2px 6px; border-radius: 4px; margin-top: 2px; border: 1px solid color-mix(in srgb, var(--success) 40%, transparent);}
.btn-checkout {
  background: var(--primary); color: #FFFFFF; border: none; padding: 12px 24px;
  border-radius: 25px; font-weight: 700; font-size: 1rem; cursor: pointer; transition: 0.3s;
}
.btn-checkout:hover { transform: translateY(-2px); box-shadow: 0 4px 15px var(--primary-glow); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); display: flex; justify-content: center; align-items: flex-end; z-index: 1000; }
.modal-content { background: var(--bg-card); width: 100%; max-width: 600px; border-top-left-radius: 20px; border-top-right-radius: 20px; padding: 20px; max-height: 85vh; overflow-y: auto; color: var(--text-primary); border-top: 1px solid var(--border);}
.modal-title { margin-top: 0; text-align: center; color: var(--primary); border-bottom: 1px solid var(--border); padding-bottom: 15px;}
.cart-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px dashed var(--border); font-size: 0.95rem;}
.payment-box { background: var(--bg-input); padding: 15px; border-radius: 10px; margin-top: 15px; border: 1px solid var(--border); }
.qr-area { text-align: center; margin: 15px 0; }
.qr-area img { width: 180px; border-radius: 10px; border: 2px solid var(--primary); box-shadow: 0 4px 15px var(--primary-glow); }
.form-control { width: 100%; padding: 10px; border: 1px solid var(--border); background: var(--bg-root); color: var(--text-primary); border-radius: 5px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 10px; margin-top: 20px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--border); background: transparent; color: var(--text-muted); border-radius: 8px; font-weight: bold; cursor: pointer;}
.btn-cancel:hover { background: rgba(255,255,255,0.1); }
.btn-confirm { flex: 2; padding: 12px; border: none; background: var(--primary); color: var(--bg-dark); border-radius: 8px; font-weight: bold; cursor: pointer; transition: 0.3s;}
.btn-confirm:hover { box-shadow: 0 4px 15px var(--primary-glow); }

/* Voice FAB & Modal */
.fab-mic {
  position: fixed; bottom: 100px; right: 20px;
  width: 60px; height: 60px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem; box-shadow: 0 4px 20px color-mix(in srgb, var(--secondary) 50%, transparent);
  cursor: pointer; z-index: 99; transition: 0.3s;
}
.fab-mic:hover { transform: scale(1.1); }
.fab-mic.recording { background: var(--primary); box-shadow: 0 4px 20px color-mix(in srgb, var(--primary) 50%, transparent); animation: mic-pulse 1.5s infinite; }
@keyframes mic-pulse { 0% {box-shadow: 0 0 0 0 color-mix(in srgb, var(--primary) 70%, transparent);} 70% {box-shadow: 0 0 0 20px transparent;} 100% {box-shadow: 0 0 0 0 transparent;} }

.voice-box {
  background: var(--bg-card); padding: 30px; border-radius: 20px;
  text-align: center; max-width: 400px; width: 90%;
  border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.8);
  margin-bottom: 20vh;
}
.voice-box .mic-icon { font-size: 3.5rem; margin-bottom: 15px; display: inline-block; padding: 10px; }
.voice-box .pulse { animation: mic-pulse 1.5s infinite; border-radius: 50%; background: color-mix(in srgb, var(--primary) 20%, transparent); }
.voice-text { margin: 15px 0; font-size: 1rem; color: var(--text-secondary); white-space: pre-line; }

/* Toast Notification */
.toast-notification {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-card);
  color: var(--primary);
  padding: 14px 28px;
  border-radius: 30px;
  border: 1px solid var(--primary);
  box-shadow: 0 0 30px color-mix(in srgb, var(--secondary) 30%, transparent);
  font-weight: bold;
  z-index: 1000;
  animation: toastSlideUp 0.3s ease;
  text-align: center;
  max-width: 90%;
}
@keyframes toastSlideUp {
  from { transform: translate(-50%, 20px); opacity: 0; }
  to { transform: translate(-50%, 0); opacity: 1; }
}

@media (max-width: 1024px) {
  .dinein-navbar { padding: 12px 24px; }
  .nav-links { display: none; }
  .main-content { max-width: 720px; padding: 18px; }
  .product-list { display: block; }
  .inline-cart { display: none; }
}

@media (max-width: 640px) {
  .dine-in-wrapper,
  .dine-in-wrapper * { box-sizing: border-box; }
  .dine-in-wrapper { overflow-x: hidden; padding-bottom: 88px; }
  .dinein-navbar { padding: 10px 12px; }
  .logo p { display: none; }
  .logo h2 { font-size: 1.05rem; }
  .btn-nav-dinein { min-height: 44px; padding: 8px 12px; }
  .main-content { padding: 12px; }
  .table-selection-box,
  .ai-suggestion-box,
  .suggested-section { padding: 12px; }
  .table-select,
  .form-control,
  .btn-add-item,
  .btn-sugg-add,
  .btn-add-combo,
  .btn-checkout,
  .btn-cancel,
  .btn-confirm,
  .modal-actions button { min-height: 44px; }
  .party-size-row,
  .ai-action { flex-wrap: wrap; }
  .party-size-row .form-control { flex: 1 1 140px; width: auto !important; min-width: 0; }
  .party-size-row .btn-add-item { flex: 1 1 130px; }
  .combo-item { align-items: flex-start; flex-wrap: wrap; }
  .combo-item .product-info { min-width: calc(100% - 72px); }
  .combo-item .btn-add-item { width: 100%; }
  .suggested-card { min-width: min(160px, 70vw); }
  .product-item { align-items: center; gap: 10px; padding: 12px; }
  .product-item img { width: 72px; height: 72px; flex-shrink: 0; }
  .product-info { min-width: 0; }
  .product-info h4 { overflow-wrap: anywhere; font-size: 1rem; }
  .product-item .btn-add-item { flex-shrink: 0; padding: 8px 12px; }
  .sticky-bottom-cart { min-height: 72px; padding: 10px 12px; border-radius: 12px 12px 0 0; }
  .cart-summary { gap: 10px; min-width: 0; }
  .cart-price { font-size: 1rem; }
  .btn-checkout { padding: 10px 16px; }
  .fab-mic { right: 12px; bottom: 90px; width: 52px; height: 52px; }
  .g-modal-box { width: calc(100vw - 24px); max-height: calc(100vh - 24px) !important; }
  .cart-item-row { align-items: flex-start; flex-wrap: wrap; }
  .cart-item-info { min-width: 120px; }
  .cart-item-controls { gap: 2px; }
  .qty-btn { width: 44px; height: 44px; }
  .modal-actions { flex-direction: column; }
  .modal-actions button { width: 100%; }
  .voice-box { width: calc(100% - 24px); padding: 24px 16px; }
  .toast-notification { width: calc(100% - 24px); padding: 12px 16px; }
}
</style>
