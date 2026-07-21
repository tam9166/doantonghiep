<template>
  <CustomerLayout>
  <div class="dine-in-wrapper">
    

    <main class="main-content">
      <div style="margin-bottom: 20px;">
        <button v-if="userRoles.includes('ROLE_WAITER')" @click="$router.push('/waiter')" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← Quay Lại Phục Vụ
        </button>
        <button v-else @click="$router.back()" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← Quay Lại
        </button>
      </div>
      <div class="table-selection-box">
        <label>📍 Bạn đang ngồi ở bàn nào?</label>
        <select v-model="selectedTable" class="form-control table-select" :disabled="isTableLocked">
          <option value="" disabled>-- Vui lòng chọn bàn của bạn --</option>
          <optgroup v-for="(tables, floor) in groupedTables" :key="floor" :label="floor">
          <!-- ĐÃ NÂNG CẤP: Hiện trạng thái và chặn khách chọn bàn đã có người -->
          <option 
            v-for="t in tables" 
            :key="t.id" 
            :value="t.name" 
            :disabled="t.isOccupied !== 0 && t.name !== selectedTable"
          >
            {{ t.name }} {{ t.isOccupied === 0 ? '(🟢 Trống)' : (t.isOccupied === 1 ? '(🟡 Đã cọc)' : '(🔴 Có khách)') }}
          </option>
        </optgroup>
        </select>
        <p v-if="isTableLocked" style="color:var(--primary); font-size: 0.85rem; margin-top: 5px;">🔒 Bạn đã quét mã QR cho bàn này. Không thể đổi bàn.</p>
      </div>

      <div class="product-list" v-if="selectedTable">
        <!-- AI Suggestion Section -->
        <div class="ai-suggestion-box">
          <div class="ai-header">
            <h3>🤖 Smart Suggestion</h3>
            <span class="ai-badge">AI Gợi Ý</span>
          </div>
          
          <div v-if="aiCombo.length === 0 && !isFetchingAI">
            <p class="ai-desc">Để đưa ra gợi ý hợp lý nhất, bạn đi mấy người?</p>
            <div class="party-size-row" style="display: flex; gap: 10px; margin-bottom: 10px;">
              <input type="number" v-model="partySize" min="1" placeholder="Nhập số người" class="form-control" style="width: 150px; background: rgba(0,0,0,0.2); color: #FFFFFF;" />
              <button class="btn-add-item" @click="fetchComboForParty">Nhận gợi ý</button>
            </div>
          </div>
          <div v-else-if="isFetchingAI">
            <p class="ai-desc">Đang phân tích và thiết kế thực đơn cho {{ partySize }} người...</p>
          </div>
          <div v-else>
            <p class="ai-desc">{{ aiRecommendationReason }}</p>
            
            <div class="combo-grid">
              <div v-for="product in aiCombo" :key="'ai-'+product.id" class="combo-item">
                <img :src="foodImage(product.image)" :alt="product.name" loading="lazy" @error="replaceFoodImage" />
                <div class="product-info">
                  <h4>{{ product.name }} <span v-if="product.suggestedQuantity > 1" style="color: var(--primary);">x{{ product.suggestedQuantity }}</span></h4>
                  <p class="price">{{ product.price.toLocaleString() }}đ</p>
                </div>
                <button v-if="!isAdminOrManager" class="btn-add-item" @click="addToCart(product, product.suggestedQuantity || 1)">Thêm</button>
                <button v-else class="btn-add-item btn-disabled" disabled>Chỉ xem</button>
              </div>
            </div>
            <div class="ai-action" style="display: flex; gap: 10px; justify-content: center;">
              <button v-if="!isAdminOrManager" class="btn-add-combo" @click="addComboToCart">🛒 Thêm Cả Combo</button>
              <button v-else class="btn-add-combo btn-disabled" disabled>Chỉ xem (Admin)</button>
              <button class="btn-cancel" style="padding: 10px 20px; border-radius: 20px;" @click="aiCombo = []">Thử lại</button>
            </div>
          </div>
        </div>

        <!-- Món ăn bán chạy / Gợi ý -->
        <div v-if="suggestedProducts.length > 0" class="suggested-section">
          <h3 class="section-title"><span style="color: #B98229">🌟</span> Gợi Ý Cho Bạn (Bán Chạy)</h3>
          <div class="suggested-grid">
            <div v-for="product in suggestedProducts" :key="'sugg-'+product.id" class="suggested-card">
              <div class="sugg-badge">HOT</div>
              <img :src="foodImage(product.image)" :alt="product.name" loading="lazy" @error="replaceFoodImage" />
              <div class="sugg-info">
                <h4>{{ product.name }}</h4>
                <p class="price">{{ product.price.toLocaleString() }}đ</p>
              </div>
              <button v-if="!isAdminOrManager" class="btn-sugg-add" @click="addToCart(product, 1)">Thêm Ngay</button>
            </div>
          </div>
        </div>

        <h3 class="section-title">Thực Đơn Đầy Đủ</h3>
        <div v-for="product in activeProducts" :key="product.id" class="product-item">
          <img :src="foodImage(product.image)" :alt="product.name" loading="lazy" @error="replaceFoodImage" />
          <div class="product-info">
            <h4>{{ product.name }}</h4>
            <p class="price">{{ product.price.toLocaleString() }}đ</p>
          </div>
          <button v-if="!isAdminOrManager" class="btn-add-item" @click="addToCart(product)">Thêm</button>
          <button v-else class="btn-add-item btn-disabled" disabled>Chỉ xem</button>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>Vui lòng chọn bàn để xem thực đơn và gọi món nhé!</p>
      </div>

      <!-- FAB Voice Order -->
      <div v-if="selectedTable && !isAdminOrManager" class="fab-mic" @click="startVoiceOrder" :class="{'recording': isListening}">
        🎙️
      </div>

      <!-- Voice Modal -->
      <div v-if="showVoiceModal" class="modal-overlay voice-modal">
        <div class="voice-box">
          <div class="mic-icon" :class="{'pulse': isListening}">🎙️</div>
          <h3>Trợ lý Gọi Món AI</h3>
          <p class="voice-text">{{ voiceText }}</p>
          <button class="btn-cancel" style="margin-top:20px;" @click="closeVoiceModal">Hủy</button>
        </div>
      </div>
    </main>

    <div class="sticky-bottom-cart" v-if="cart.length > 0">
      <div class="cart-summary" @click="showModal = true">
        <span class="cart-icon">🛒 <span class="badge">{{ totalItems }}</span></span>
        <div class="cart-text">
          <span class="cart-price">{{ cartSubtotal.toLocaleString() }}đ</span>
        </div>
      </div>
      <button class="btn-checkout" @click="showModal = true">🍳 Gửi Bếp</button>
    </div>

    <!-- Xác nhận gọi món Modal -->
    <div v-if="showModal" class="g-modal-overlay" @click.self="showModal = false">
      <div class="g-modal-box" style="max-width: 550px; max-height: 90vh; overflow-y: auto;">
        <h3>🍳 Xác Nhận Gọi Món</h3>
        
        <div class="cart-details" style="max-height: 300px; overflow-y: auto; margin-bottom: 15px; padding-right: 10px;">
          <div v-for="(item, idx) in cart" :key="idx" class="cart-item-row">
            <div class="cart-item-info">
              <span class="cart-item-name">{{ item.name }}</span>
              <span class="cart-item-price">{{ (item.price * item.quantity).toLocaleString() }}đ</span>
            </div>
            <div class="cart-item-controls">
              <button class="qty-btn qty-minus" @click="decreaseQty(idx)">−</button>
              <span class="qty-display">{{ item.quantity }}</span>
              <button class="qty-btn qty-plus" @click="increaseQty(idx)">+</button>
              <button class="qty-btn qty-remove" @click="cart.splice(idx, 1)">✖</button>
            </div>
          </div>
        </div>

        <div class="cart-total" style="margin-top: 20px; text-align: center;">
          <h4 style="color: var(--primary); font-size: 1.1rem; border-top: 1px dashed rgba(90, 110, 69, 0.3); padding-top: 10px;">Tạm tính: {{ cartSubtotal.toLocaleString() }}đ</h4>
          <h4 style="color: var(--primary); font-size: 1.1rem;">Thuế GTGT: {{ cartTax.toLocaleString() }}đ</h4>
          <h4 style="color: #B23B2E; font-size: 1.4rem; margin-top: 5px;">Tổng cộng: {{ finalTotal.toLocaleString() }}đ</h4>
          <p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 5px;">💡 Thanh toán sau khi dùng bữa xong</p>
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="showModal = false" class="g-btn-outline" style="flex:1;">Quay lại</button>
          <button @click="submitOrder" class="g-btn-primary" style="flex:1;">🍳 Gửi Bếp Ngay</button>
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

import { ref, computed, onMounted } from 'vue';
import api, { externalApi } from '@/services/api';
import { useRoute } from 'vue-router';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';

const route = useRoute();
const products = ref([]);
const suggestedProducts = ref([]);
const allTables = ref([]);
const cart = ref([]);
const selectedTable = ref("");
const isTableLocked = ref(false);
const showModal = ref(false);
const toastMsg = ref('');
const userRoles = ref([]);

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

const fetchComboForParty = async () => {
  if (!partySize.value || partySize.value < 1) {
    alert("Vui lòng nhập số người hợp lệ!");
    return;
  }
  isFetchingAI.value = true;
  try {
    // Gọi API thời tiết Đà Nẵng
    const wRes = await externalApi.get('https://api.open-meteo.com/v1/forecast?latitude=16.0678&longitude=108.2208&current_weather=true');
    const weather = wRes.data.current_weather;
    const weatherCode = weather.weathercode;
    let weatherStr = `Trời quang, nhiệt độ ${weather.temperature}°C`;
    if (weatherCode >= 50 && weatherCode <= 69) weatherStr = `Trời đang mưa lất phất, nhiệt độ ${weather.temperature}°C`;
    else if (weatherCode >= 70) weatherStr = `Trời mưa to/tuyết, nhiệt độ ${weather.temperature}°C`;
    else if (weather.temperature > 30) weatherStr = `Trời nắng nóng, nhiệt độ ${weather.temperature}°C`;

    // Gọi Backend AI
    const menuStr = activeProducts.value.map(p => `${p.id}-${p.name}`).join(', ');
    const message = `Khách đi ${partySize.value} người. Thời tiết hiện tại: ${weatherStr}`;
    
    const aiRes = await api.post('/api/chatbot/chat', {
      type: 'COMBO_RECOMMEND',
      message: message,
      menu: menuStr
    });

    let reply = aiRes.data.reply;
    reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
    
    // Kiểm tra xem phản hồi có phải là JSON hợp lệ không để tránh lỗi console
    if (!reply.startsWith('[') && !reply.startsWith('{')) {
      throw new Error("AI trả về định dạng không hợp lệ: " + reply);
    }
    
    const suggestions = JSON.parse(reply);

    aiCombo.value = suggestions.map(s => {
      const p = activeProducts.value.find(prod => prod.id == s.id);
      if (p) return { ...p, suggestedQuantity: s.quantity || 1 };
      return null;
    }).filter(p => p != null);
    
    if(suggestions.length > 0) aiRecommendationReason.value = suggestions[0].reason;

  } catch(e) {
    console.error("Lỗi AI Recommend:", e);
    // Fallback thông minh theo số lượng người khi AI bị lỗi
    if (activeProducts.value.length >= 2) {
      let pSize = parseInt(partySize.value) || 2;
      
      // Phân loại món ăn và nước uống
      let drinks = activeProducts.value.filter(p => p.category && (p.category.name.toLowerCase().includes('nước') || p.category.name.toLowerCase().includes('uống') || p.category.name.toLowerCase().includes('trà') || p.category.name.toLowerCase().includes('cafe')));
      let foods = activeProducts.value.filter(p => !p.category || (!p.category.name.toLowerCase().includes('nước') && !p.category.name.toLowerCase().includes('uống') && !p.category.name.toLowerCase().includes('trà') && !p.category.name.toLowerCase().includes('cafe')));
      
      // Số món ăn khác nhau: ceil(số người * 0.7), tối thiểu 2, tối đa số món có sẵn
      let numFoodTypes = Math.max(2, Math.ceil(pSize * 0.7));
      numFoodTypes = Math.min(numFoodTypes, foods.length);
      
      // Chọn ngẫu nhiên các món ăn khác nhau
      let shuffledFoods = [...foods].sort(() => Math.random() - 0.5);
      let selectedFoods = shuffledFoods.slice(0, numFoodTypes);
      
      // Chọn 1-2 loại nước uống khác nhau
      let numDrinkTypes = Math.min(Math.max(1, Math.ceil(pSize / 3)), drinks.length || 1);
      let shuffledDrinks = [...drinks].sort(() => Math.random() - 0.5);
      let selectedDrinks = shuffledDrinks.slice(0, numDrinkTypes);
      
      // Tính số lượng mỗi món
      let combo = [];
      selectedFoods.forEach((food, idx) => {
        let qty = idx === 0 ? Math.ceil(pSize / numFoodTypes) + 1 : Math.ceil(pSize / numFoodTypes);
        combo.push({ ...food, suggestedQuantity: Math.max(1, qty) });
      });
      
      // Mỗi người 1 nước, chia đều cho các loại nước
      let drinksPerType = Math.ceil(pSize / numDrinkTypes);
      selectedDrinks.forEach(drink => {
        combo.push({ ...drink, suggestedQuantity: drinksPerType });
      });
      
      // Nếu không có nước riêng, chọn bất kỳ sản phẩm nào chưa được chọn
      if (selectedDrinks.length === 0 && activeProducts.value.length > numFoodTypes) {
        let remaining = activeProducts.value.filter(p => !selectedFoods.find(f => f.id === p.id));
        if (remaining.length > 0) {
          combo.push({ ...remaining[0], suggestedQuantity: pSize });
        }
      }

      aiCombo.value = combo;
      aiRecommendationReason.value = `(Hệ thống AI đang bảo trì) Gợi ý Combo dự phòng ${combo.length} món đa dạng cho ${pSize} người (Gồm ${selectedFoods.length} món ăn + ${selectedDrinks.length || 1} loại nước uống).`;
    }
  } finally {
    isFetchingAI.value = false;
  }
};

const addComboToCart = () => {
  aiCombo.value.forEach(p => addToCart(p, p.suggestedQuantity || 1));
  alert('Đã thêm Combo Gợi ý vào giỏ hàng!');
};

const loadData = async () => {
  try {
    const [resProd, resTable] = await Promise.all([
      api.get('/api/products'),
      api.get('/api/tables') // Lấy danh sách bàn
    ]);
    products.value = resProd.data;
    allTables.value = resTable.data;
    
    // Tự động chọn bàn nếu có truyền query param ?table=
    if (route.query.table) {
      selectedTable.value = route.query.table;
      isTableLocked.value = true;
    }

    // Lấy thông tin User để áp dụng hạng thẻ
    const token = localStorage.getItem('token');
    if (token) {
      const resProfile = await api.get('/api/auth/profile', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      userProfile.value = resProfile.data;
      if (userProfile.value.membershipTier === 'Kim Cương') tierDiscount.value = 0.15;
      else if (userProfile.value.membershipTier === 'Vàng') tierDiscount.value = 0.10;
      else if (userProfile.value.membershipTier === 'Bạc') tierDiscount.value = 0.05;
    }
    
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser);
        if (parsed && parsed.roles) {
          userRoles.value = parsed.roles;
        }
      } catch (e) {}
    }
    
    // Tải Món Gợi Ý (Bán Chạy)
    try {
      const response = await api.get('/api/admin/popular-items/products?limit=4', {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
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
  if (existing) existing.quantity += qty;
  else cart.value.push({ productId: product.id, name: product.name, price: product.price, quantity: qty, taxRate: product.taxRate || 8 });
};

const increaseQty = (idx) => {
  cart.value[idx].quantity++;
};

const decreaseQty = (idx) => {
  if (cart.value[idx].quantity > 1) {
    cart.value[idx].quantity--;
  } else {
    cart.value.splice(idx, 1);
  }
};

const closeVoiceModal = () => {
  showVoiceModal.value = false;
  isListening.value = false;
  if (recognition) recognition.stop();
};

const startVoiceOrder = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    alert("Trình duyệt của bạn không hỗ trợ nhận diện giọng nói. Vui lòng dùng Google Chrome hoặc Edge.");
    return;
  }
  
  if (!recognition) {
    recognition = new SpeechRecognition();
    recognition.lang = 'vi-VN';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      isListening.value = true;
      showVoiceModal.value = true;
      voiceText.value = 'Đang lắng nghe... Hãy nói: "Cho mình 2 trà đào và 1 mì ý".';
    };

    recognition.onresult = async (event) => {
      isListening.value = false;
      const transcript = event.results[0][0].transcript;
      voiceText.value = `Bạn vừa nói: "${transcript}"\n\nĐang nhờ AI phân tích... 🤖`;
      
      try {
        // Gửi danh sách tên món cho AI
        const menuStr = activeProducts.value.map(p => `${p.id}: ${p.name}`).join(', ');
        const res = await api.post('/api/chatbot/chat', {
          message: transcript,
          type: 'VOICE_ORDER',
          menu: menuStr
        });
        
        let reply = res.data.reply || '';
        reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
        
        const items = JSON.parse(reply);
        if (!Array.isArray(items) || items.length === 0) {
          voiceText.value = 'AI không tìm thấy món ăn nào khớp với menu. Vui lòng thử lại!';
          setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
          return;
        }
        
        let addedNames = [];
        items.forEach(item => {
          const prod = activeProducts.value.find(p => p.id === item.productId);
          if (prod) {
            addToCart(prod, item.quantity || 1);
            addedNames.push(`${item.quantity || 1} ${prod.name}`);
          }
        });
        
        if (addedNames.length > 0) {
          voiceText.value = `✅ Đã thêm vào giỏ: ${addedNames.join(', ')}`;
        } else {
          voiceText.value = 'AI không tìm thấy món ăn nào khớp với menu.';
        }
        setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3500);
        
      } catch (e) {
        voiceText.value = 'Lỗi xử lý AI hoặc định dạng trả về không đúng!';
        setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
      }
    };

    recognition.onerror = (event) => {
      voiceText.value = `Lỗi: ${event.error}`;
      isListening.value = false;
      setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 3000);
    };

    recognition.onend = () => {
      isListening.value = false;
      if(showVoiceModal.value && voiceText.value.includes('Đang lắng nghe')) {
          voiceText.value = 'Không nghe thấy gì. Đã tự động tắt mic.';
          setTimeout(() => { if(!isListening.value) showVoiceModal.value = false; }, 2000);
      }
    };
  }

  recognition.start();
};

const submitOrder = async () => {
  if (cart.value.length === 0) return alert("Giỏ hàng trống!");
  
  const token = localStorage.getItem('token') || ''; 
  const today = new Date().toLocaleDateString('en-CA');
  const now = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const infoFull = `[TẠI QUÁN] Bàn: ${selectedTable.value} | Lúc: ${now} ngày ${today}`;
  const formattedItems = cart.value.map(item => ({ productId: item.productId, quantity: item.quantity }));

  try {
    await api.post('/api/orders/checkout', {
      address: infoFull,
      paymentOption: 'PAY_AT_RESTAURANT',
      items: formattedItems
    }, { headers: token ? { 'Authorization': `Bearer ${token}` } : {} });

    cart.value = [];
    showModal.value = false;
    toastMsg.value = 'Đã ghi nhận đơn. Nhân viên sẽ xác nhận trước khi chuyển xuống bếp.';
    setTimeout(() => { toastMsg.value = ''; }, 4000);
  } catch (error) {
    alert("Lỗi: Vui lòng thử lại!");
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
  background: rgba(90, 110, 69, 0.1);
}
.qty-minus:hover {
  border-color: #B23B2E;
  color: #B23B2E;
  background: rgba(178,59,46,0.1);
}
.qty-remove {
  background: none;
  border: none;
  color: #B23B2E;
  font-size: 0.9rem;
  margin-left: 4px;
}
.qty-remove:hover {
  color: #B23B2E;
  background: rgba(178,59,46,0.15);
}
.qty-display {
  min-width: 28px;
  text-align: center;
  font-weight: 800;
  font-size: 1rem;
  color: var(--text-heading);
}
.dine-in-wrapper { font-family: var(--font-primary); background-color: var(--bg-root); min-height: 100vh; padding-bottom: 80px; color: var(--text-primary); }

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
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(90, 110, 69, 0.1); }

.nav-right { display: flex; align-items: center; gap: 10px; }
.btn-nav-dinein {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-dinein:hover { border-color: var(--primary); color: var(--primary); background: rgba(90, 110, 69, 0.1); }

.main-content { padding: 15px; max-width: 600px; margin: 0 auto; }
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
.suggested-section { margin-bottom: 25px; text-align: left; background: rgba(185, 130, 41, 0.05); padding: 15px; border-radius: 12px; border: 1px solid rgba(185, 130, 41, 0.2); }
.suggested-section .section-title { font-size: 1.2rem; margin-bottom: 15px; color: #B98229; border-bottom: none; }
.suggested-grid { display: flex; gap: 15px; overflow-x: auto; padding-bottom: 10px; }
.suggested-grid::-webkit-scrollbar { height: 6px; }
.suggested-grid::-webkit-scrollbar-thumb { background: #B98229; border-radius: 10px; }
.suggested-card { min-width: 160px; background: rgba(0,0,0,0.5); border-radius: 10px; padding: 10px; display: flex; flex-direction: column; position: relative; border: 1px solid rgba(185, 130, 41, 0.3); transition: 0.3s; }
.suggested-card:hover { transform: translateY(-3px); box-shadow: 0 5px 15px rgba(185, 130, 41, 0.2); }
.sugg-badge { position: absolute; top: -5px; right: -5px; background: #B23B2E; color: #FFFFFF; padding: 3px 6px; border-radius: 6px; font-weight: 900; font-size: 0.7rem; transform: rotate(10deg); box-shadow: 0 2px 5px rgba(178,59,46,0.5); }
.suggested-card img { width: 100%; height: 100px; object-fit: cover; border-radius: 8px; margin-bottom: 10px; }
.sugg-info { flex: 1; }
.sugg-info h4 { margin: 0 0 5px 0; font-size: 0.95rem; color: #FFFFFF; }
.sugg-info .price { color: #B98229; font-weight: bold; font-size: 1rem; margin: 0; }
.btn-sugg-add { background: #B98229; color: #201D14; border: none; padding: 6px; border-radius: 6px; font-weight: bold; margin-top: 10px; cursor: pointer; transition: 0.3s; font-size: 0.85rem;}
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
.ai-badge { background: linear-gradient(135deg, var(--primary), #5A6E45); color: var(--bg-dark); padding: 3px 8px; border-radius: 12px; font-size: 0.7rem; font-weight: bold; }
.ai-desc { margin: 0 0 15px 0; font-size: 0.85rem; color: var(--text-secondary); font-style: italic; }
.combo-grid { display: flex; flex-direction: column; gap: 10px; }
.combo-item {
  display: flex; background: var(--bg-card); padding: 10px; border-radius: 8px; align-items: center; gap: 12px;
  border: 1px solid var(--border);
}
.combo-item img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; border: 1px solid var(--border); }
.ai-action { margin-top: 15px; text-align: center; }
.btn-add-combo {
  background: linear-gradient(135deg, var(--primary), #5A6E45); color: var(--bg-dark); border: none; padding: 10px 20px; border-radius: 20px;
  font-weight: bold; width: 100%; cursor: pointer; box-shadow: 0 4px 10px var(--primary-glow);
}

.sticky-bottom-cart { position: fixed; bottom: 0; left: 0; right: 0; background: var(--bg-nav); padding: 15px 20px; box-shadow: 0 -4px 20px rgba(0,0,0,0.5); display: flex; justify-content: space-between; align-items: center; z-index: 101; max-width: 600px; margin: 0 auto; border-top-left-radius: 15px; border-top-right-radius: 15px; border-top: 1px solid var(--border);}
.cart-summary { display: flex; align-items: center; gap: 15px; cursor: pointer; flex: 1;}
.cart-icon { font-size: 1.8rem; position: relative; }
.cart-icon .badge {
  position: absolute; top: -5px; right: -10px; background: #B23B2E; color: #FFFFFF;
  font-size: 0.75rem; font-weight: bold; padding: 2px 6px; border-radius: 50%;
}
.cart-text { display: flex; flex-direction: column; }
.cart-price { font-size: 1.2rem; font-weight: 800; color: #FFFFFF; }
.cart-discount-badge { font-size: 0.75rem; background: rgba(47, 143, 91, 0.2); color: #2F8F5B; padding: 2px 6px; border-radius: 4px; margin-top: 2px; border: 1px solid rgba(47, 143, 91, 0.4);}
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
  background: linear-gradient(135deg, var(--primary), #5A6E45);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem; box-shadow: 0 4px 20px rgba(90, 110, 69, 0.5);
  cursor: pointer; z-index: 99; transition: 0.3s;
}
.fab-mic:hover { transform: scale(1.1); }
.fab-mic.recording { background: #B23B2E; box-shadow: 0 4px 20px rgba(178,59,46,0.5); animation: mic-pulse 1.5s infinite; }
@keyframes mic-pulse { 0% {box-shadow: 0 0 0 0 rgba(178,59,46,0.7);} 70% {box-shadow: 0 0 0 20px rgba(178,59,46,0);} 100% {box-shadow: 0 0 0 0 rgba(178,59,46,0);} }

.voice-box {
  background: var(--bg-card); padding: 30px; border-radius: 20px;
  text-align: center; max-width: 400px; width: 90%;
  border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.8);
  margin-bottom: 20vh;
}
.voice-box .mic-icon { font-size: 3.5rem; margin-bottom: 15px; display: inline-block; padding: 10px; }
.voice-box .pulse { animation: mic-pulse 1.5s infinite; border-radius: 50%; background: rgba(178,59,46,0.2); }
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
  box-shadow: 0 0 30px rgba(90, 110, 69, 0.3);
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
