<template>
  <div class="dine-in-wrapper">
    <header class="dinein-navbar">
      <div class="nav-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🍽️</span>
          <div>
            <h2>NHÀ HÀNG FPOLY</h2>
            <p>ĐÀ NẴNG</p>
          </div>
        </div>
        <nav class="nav-links">
          <router-link to="/">Trang chủ</router-link>
          <router-link to="/menu">Thực đơn</router-link>
          <router-link to="/reservation">Đặt chỗ</router-link>
          <a href="#" class="active">Tại bàn</a>
        </nav>
        <div class="nav-right">
          <button @click="$router.push('/history')" class="btn-nav-dinein">📜 Lịch Sử</button>
        </div>
      </div>
    </header>

    <main class="main-content">
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
        <div v-if="aiCombo.length > 0" class="ai-suggestion-box">
          <div class="ai-header">
            <h3>🤖 Smart Suggestion</h3>
            <span class="ai-badge">AI Gợi Ý</span>
          </div>
          <p class="ai-desc">{{ aiRecommendationReason || 'Đang phân tích thời tiết và thực đơn...' }}</p>
          
          <div class="combo-grid">
            <div v-for="product in aiCombo" :key="'ai-'+product.id" class="combo-item">
              <img :src="product.image || 'https://via.placeholder.com/100'" alt="food" />
              <div class="product-info">
                <h4>{{ product.name }}</h4>
                <p class="price">{{ product.price.toLocaleString() }}đ</p>
              </div>
              <button class="btn-add-item" @click="addToCart(product)">Thêm</button>
            </div>
          </div>
          <div class="ai-action">
            <button class="btn-add-combo" @click="addComboToCart">🛒 Thêm Cả Combo</button>
          </div>
        </div>

        <h3 class="section-title">Thực Đơn Đầy Đủ</h3>
        <div v-for="product in activeProducts" :key="product.id" class="product-item">
          <img :src="product.image || 'https://via.placeholder.com/100'" alt="food" />
          <div class="product-info">
            <h4>{{ product.name }}</h4>
            <p class="price">{{ product.price.toLocaleString() }}đ</p>
          </div>
          <button class="btn-add-item" @click="addToCart(product)">Thêm</button>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>Vui lòng chọn bàn để xem thực đơn và gọi món nhé!</p>
      </div>

      <!-- FAB Voice Order -->
      <div v-if="selectedTable" class="fab-mic" @click="startVoiceOrder" :class="{'recording': isListening}">
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
          <span class="cart-price">{{ cartTotal.toLocaleString() }}đ</span>
          <span v-if="tierDiscount > 0" class="cart-discount-badge">
            🏷️ Thẻ {{ userProfile?.membershipTier }}: Giảm {{ tierDiscount * 100 }}%
          </span>
        </div>
      </div>
      <button class="btn-checkout" @click="showModal = true">Thanh Toán</button>
    </div>

    <!-- Giỏ hàng Modal -->
    <div v-if="showModal" class="g-modal-overlay" @click.self="showModal = false">
      <div class="g-modal-box" style="max-width: 550px; max-height: 90vh; overflow-y: auto;">
        <h3>Xác Nhận Đặt Món</h3>
        
        <div class="cart-details" style="max-height: 150px; overflow-y: auto; margin-bottom: 15px; padding-right: 10px;">
          <div v-for="(item, idx) in cart" :key="idx" style="display: flex; justify-content: space-between; border-bottom: 1px dashed rgba(255,255,255,0.1); padding: 5px 0;">
            <span>{{ item.name }} <strong>(x{{ item.quantity }})</strong></span>
            <span>{{ (item.price * item.quantity).toLocaleString() }}đ</span>
          </div>
        </div>

        <div class="form-group mt-3">
          <label>Nhập mã Voucher (nếu có)</label>
          <div style="display: flex; gap: 10px;">
            <input v-model="voucherCode" type="text" class="g-form-control" placeholder="MAGIAMGIA..." />
            <button @click="applyVoucher" class="g-btn-primary">Áp dụng</button>
          </div>
        </div>

        <div class="cart-total" style="margin-top: 20px; text-align: center;">
          <p style="color: var(--text-primary); font-weight: bold; margin-bottom: 5px;">Tổng cộng: {{ cartSubtotal.toLocaleString() }}đ</p>
          <p v-if="discountAmount > 0" style="color: #2ecc71; font-weight: bold; margin-bottom: 5px;">
            Giảm giá: -{{ discountAmount.toLocaleString() }}đ
          </p>
          <h4 style="color: var(--primary); font-size: 1.4rem; margin-top: 10px; border-top: 1px dashed rgba(0,212,170,0.3); padding-top: 10px;">Thành tiền: {{ finalTotal.toLocaleString() }}đ</h4>
        </div>

        <div class="qr-container" style="text-align: center; margin: 20px 0;">
          <img :src="vietQrUrl" alt="VietQR" style="width: 180px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.5);" />
        </div>

        <div class="form-group mt-3">
          <label>Mã giao dịch (Sau khi CK) *:</label>
          <input v-model="txCode" type="text" class="g-form-control" placeholder="Nhập mã GD..." />
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="showModal = false" class="g-btn-outline" style="flex:1;">Quay lại</button>
          <button @click="submitOrder" class="g-btn-primary" style="flex:1;">Xác nhận đặt</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();
const products = ref([]);
const allTables = ref([]);
const cart = ref([]);
const selectedTable = ref("");
const isTableLocked = ref(false);
const txCode = ref("");
const showModal = ref(false);

// AI Voice
const isListening = ref(false);
const showVoiceModal = ref(false);
const voiceText = ref('');
let recognition = null;

// Membership Tier Logic
const userProfile = ref(null);
const tierDiscount = ref(0);
const voucherCode = ref('');
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

const finalTotal = computed(() => {
  return cartSubtotal.value - discountAmount.value;
});

const cartTotal = computed(() => finalTotal.value);

const vietQrUrl = computed(() => {
  const bank = 'vietcombank';
  const accountNo = '1047187126';
  const accountName = 'NGUYEN QUANG NHAT';
  const amount = finalTotal.value;
  const addInfo = encodeURIComponent(`Thanh toan ban ${selectedTable.value || 'KH'}`);
  return `https://img.vietqr.io/image/${bank}-${accountNo}-compact2.png?amount=${amount}&addInfo=${addInfo}&accountName=${encodeURIComponent(accountName)}`;
});

// AI Suggestion Logic
const aiCombo = ref([]);
const aiRecommendationReason = ref('');

const fetchWeatherAI = async () => {
  try {
    // Gọi API thời tiết Đà Nẵng
    const wRes = await axios.get('https://api.open-meteo.com/v1/forecast?latitude=16.0678&longitude=108.2208&current_weather=true');
    const weather = wRes.data.current_weather;
    const weatherCode = weather.weathercode;
    let weatherStr = `Trời quang, nhiệt độ ${weather.temperature}°C`;
    if (weatherCode >= 50 && weatherCode <= 69) weatherStr = `Trời đang mưa lất phất, nhiệt độ ${weather.temperature}°C`;
    else if (weatherCode >= 70) weatherStr = `Trời mưa to/tuyết, nhiệt độ ${weather.temperature}°C`;
    else if (weather.temperature > 30) weatherStr = `Trời nắng nóng, nhiệt độ ${weather.temperature}°C`;

    // Gọi Backend AI
    const menuStr = activeProducts.value.map(p => `${p.id}-${p.name}`).join(', ');
    const aiRes = await axios.post('http://localhost:8080/api/chatbot/chat', {
      type: 'WEATHER_RECOMMEND',
      message: weatherStr,
      menu: menuStr
    });

    let reply = aiRes.data.reply;
    reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
    const suggestions = JSON.parse(reply);

    aiCombo.value = suggestions.map(s => activeProducts.value.find(p => p.id == s.id)).filter(p => p != null);
    if(suggestions.length > 0) aiRecommendationReason.value = suggestions[0].reason;

  } catch(e) {
    console.error("Lỗi AI Weather:", e);
    // Fallback
    if (activeProducts.value.length >= 2) {
      aiCombo.value = activeProducts.value.slice(0, 2);
      aiRecommendationReason.value = "Combo Gợi Ý Mặc Định";
    }
  }
};

const addComboToCart = () => {
  aiCombo.value.forEach(p => addToCart(p));
  alert('Đã thêm Combo Gợi ý vào giỏ hàng!');
};

const loadData = async () => {
  try {
    const [resProd, resTable] = await Promise.all([
      axios.get('http://localhost:8080/api/products'),
      axios.get('http://localhost:8080/api/tables') // Lấy danh sách bàn
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
      const resProfile = await axios.get('http://localhost:8080/api/auth/profile', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      userProfile.value = resProfile.data;
      if (userProfile.value.membershipTier === 'Kim Cương') tierDiscount.value = 0.15;
      else if (userProfile.value.membershipTier === 'Vàng') tierDiscount.value = 0.10;
      else if (userProfile.value.membershipTier === 'Bạc') tierDiscount.value = 0.05;
    }
    
    // Gọi Weather AI sau khi load xong products
    fetchWeatherAI();
    
  } catch (error) { console.error(error); }
};

const addToCart = (product, qty = 1) => {
  const existing = cart.value.find(item => item.productId === product.id);
  if (existing) existing.quantity += qty;
  else cart.value.push({ productId: product.id, name: product.name, price: product.price, quantity: qty });
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
        const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
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

// 🌟 LOGIC CHECK VOUCHER
const applyVoucher = async () => {
  if (!voucherCode.value) return alert("Vui lòng nhập mã!");
  try {
    const token = localStorage.getItem('token');
    const res = await axios.post('http://localhost:8080/api/vouchers/check', { code: voucherCode.value }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    voucherDiscountPercent.value = res.data.discountPercent;
    alert(`Áp dụng thành công! Giảm ${voucherDiscountPercent.value}%`);
  } catch (error) {
    alert(error.response?.data || "Mã không hợp lệ!");
    voucherDiscountPercent.value = 0;
  }
};

const submitOrder = async () => {
  if (!txCode.value) return alert("Vui lòng nhập Mã giao dịch để nhà hàng xác nhận!");
  
  const token = localStorage.getItem('token') || ''; 
  const today = new Date().toLocaleDateString('en-CA');
  const now = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const infoFull = `[TẠI QUÁN] Bàn: ${selectedTable.value} | Lúc: ${now} ngày ${today} | MãGD: ${txCode.value}`;
  const formattedItems = cart.value.map(item => ({ productId: item.productId, quantity: item.quantity }));

  try {
    await axios.post('http://localhost:8080/api/orders/checkout', {
      address: infoFull,
      voucherCode: voucherDiscountPercent.value > 0 ? voucherCode.value : null,
      items: formattedItems
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    alert("🎉 Chúc mừng bạn đã đặt món thành công!");
    cart.value = [];
    showModal.value = false;
    router.push('/'); 
  } catch (error) {
    alert("Lỗi: Vui lòng thử lại!");
  }
};

onMounted(loadData);
</script>

<style scoped>
.dine-in-wrapper { font-family: 'Outfit', -apple-system, sans-serif; background-color: var(--bg-root); min-height: 100vh; padding-bottom: 80px; color: var(--text-primary); }

/* Navbar */
.dinein-navbar {
  background: rgba(13, 27, 42, 0.4);
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
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(0,212,170,0.1); }

.nav-right { display: flex; align-items: center; gap: 10px; }
.btn-nav-dinein {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-dinein:hover { border-color: var(--primary); color: var(--primary); background: rgba(0,212,170,0.1); }

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

.empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); background: var(--bg-card); border-radius: 10px; border: 1px dashed var(--border); }

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
.ai-badge { background: linear-gradient(135deg, var(--primary), #3498db); color: var(--bg-dark); padding: 3px 8px; border-radius: 12px; font-size: 0.7rem; font-weight: bold; }
.ai-desc { margin: 0 0 15px 0; font-size: 0.85rem; color: var(--text-secondary); font-style: italic; }
.combo-grid { display: flex; flex-direction: column; gap: 10px; }
.combo-item {
  display: flex; background: var(--bg-card); padding: 10px; border-radius: 8px; align-items: center; gap: 12px;
  border: 1px solid var(--border);
}
.combo-item img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; border: 1px solid var(--border); }
.ai-action { margin-top: 15px; text-align: center; }
.btn-add-combo {
  background: linear-gradient(135deg, var(--primary), #3498db); color: var(--bg-dark); border: none; padding: 10px 20px; border-radius: 20px;
  font-weight: bold; width: 100%; cursor: pointer; box-shadow: 0 4px 10px var(--primary-glow);
}

.sticky-bottom-cart { position: fixed; bottom: 0; left: 0; right: 0; background: var(--bg-nav); padding: 15px 20px; box-shadow: 0 -4px 20px rgba(0,0,0,0.5); display: flex; justify-content: space-between; align-items: center; z-index: 101; max-width: 600px; margin: 0 auto; border-top-left-radius: 15px; border-top-right-radius: 15px; border-top: 1px solid var(--border);}
.cart-summary { display: flex; align-items: center; gap: 15px; cursor: pointer; flex: 1;}
.cart-icon { font-size: 1.8rem; position: relative; }
.cart-icon .badge {
  position: absolute; top: -5px; right: -10px; background: #e74c3c; color: #fff;
  font-size: 0.75rem; font-weight: bold; padding: 2px 6px; border-radius: 50%;
}
.cart-text { display: flex; flex-direction: column; }
.cart-price { font-size: 1.2rem; font-weight: 800; color: #fff; }
.cart-discount-badge { font-size: 0.75rem; background: rgba(46, 204, 113, 0.2); color: #2ecc71; padding: 2px 6px; border-radius: 4px; margin-top: 2px; border: 1px solid rgba(46, 204, 113, 0.4);}
.btn-checkout {
  background: var(--primary); color: #fff; border: none; padding: 12px 24px;
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
  background: linear-gradient(135deg, var(--primary), #3498db);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem; box-shadow: 0 4px 20px rgba(0,212,170,0.5);
  cursor: pointer; z-index: 99; transition: 0.3s;
}
.fab-mic:hover { transform: scale(1.1); }
.fab-mic.recording { background: #e74c3c; box-shadow: 0 4px 20px rgba(231,76,60,0.5); animation: mic-pulse 1.5s infinite; }
@keyframes mic-pulse { 0% {box-shadow: 0 0 0 0 rgba(231,76,60,0.7);} 70% {box-shadow: 0 0 0 20px rgba(231,76,60,0);} 100% {box-shadow: 0 0 0 0 rgba(231,76,60,0);} }

.voice-box {
  background: var(--bg-card); padding: 30px; border-radius: 20px;
  text-align: center; max-width: 400px; width: 90%;
  border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.8);
  margin-bottom: 20vh;
}
.voice-box .mic-icon { font-size: 3.5rem; margin-bottom: 15px; display: inline-block; padding: 10px; }
.voice-box .pulse { animation: mic-pulse 1.5s infinite; border-radius: 50%; background: rgba(231,76,60,0.2); }
.voice-text { margin: 15px 0; font-size: 1rem; color: var(--text-secondary); white-space: pre-line; }
</style>