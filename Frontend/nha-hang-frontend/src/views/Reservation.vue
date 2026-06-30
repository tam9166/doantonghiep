<template>
  <CustomerLayout>
  <div class="reservation-luxury">
    

    <main class="content-wrap">
      <div style="margin-bottom: 20px; width: 100%; max-width: 800px; display: flex;">
        <button @click="$router.back()" class="lux-btn-outline" style="border-radius: 100px; padding: 8px 20px;">
          ← Quay Lại
        </button>
      </div>
      <div class="booking-card">
        <h1 class="elegant-title text-gradient">RESERVATION</h1>
        <p class="elegant-subtitle">Trải nghiệm không gian ẩm thực thượng lưu</p>

        <div class="step-progress">
          <div :class="['step', step >= 1 ? 'active' : '']">1. Thời gian</div>
          <div :class="['step', step >= 2 ? 'active' : '']">2. Vị trí</div>
          <div :class="['step', step >= 3 ? 'active' : '']">3. Tùy chọn</div>
          <div :class="['step', step >= 4 ? 'active' : '']">4. Hoàn tất</div>
        </div>

        <transition name="fade" mode="out-in">
          <div v-if="step === 1" class="step-panel">
            <h3 class="panel-title">Quý khách dự định đến vào lúc?</h3>
            <div class="input-grid">
              <div class="input-box">
                <label>Ngày phục vụ</label>
                <input v-model="form.date" type="date" class="lux-input" />
              </div>
              <div class="input-box">
                <label>Giờ phục vụ</label>
                <input v-model="form.time" type="time" class="lux-input" />
              </div>
            </div>
            <button @click="nextToTable" class="lux-btn block-btn mt-20">TÌM VỊ TRÍ ĐẸP</button>
          </div>

          <div v-else-if="step === 2" class="step-panel">
            <h3 class="panel-title">Sơ đồ bàn khả dụng</h3>
            <div class="floor-tabs">
              <button 
                v-for="floor in uniqueFloors" 
                :key="floor"
                :class="{'active': selectedFloor === floor}" 
                @click="selectedFloor = floor">
                {{ floor }}
              </button>
            </div>

            <div class="table-grid-lux">
              <div v-for="t in filteredTables" :key="t.id" 
                :class="['table-card-lux', t.isOccupied !== 0 ? 'disabled' : '', selectedTable?.id === t.id ? 'selected' : '']"
                @click="selectTable(t)">
                <div class="t-view" v-if="t.hasView">City View</div>
                <h4>{{ t.name }}</h4>
                <span class="status-dot" :class="t.isOccupied === 0 ? 'green' : 'red'"></span>
              </div>
            </div>
            
            <div class="btn-group mt-20">
              <button @click="step = 1" class="lux-btn-outline">QUAY LẠI</button>
              <button v-if="selectedTable" @click="step = 3" class="lux-btn">TIẾP TỤC</button>
            </div>
          </div>

          <div v-else-if="step === 3" class="step-panel">
            <h3 class="panel-title">Chuẩn bị cho bữa tiệc của bạn</h3>
            <div class="service-options" v-if="!showMenu">
              <div class="opt-card" @click="step = 4">
                <div class="icon">🍷</div>
                <h4>Chỉ Giữ Bàn</h4>
                <p>Cọc 500k. Chọn món sau tại nhà hàng.</p>
              </div>
              <div class="opt-card highlight" @click="showMenu = true">
                <div class="icon">🍱</div>
                <h4>Đặt Món Trước</h4>
                <p>Món ăn sẽ sẵn sàng ngay khi bạn đến.</p>
              </div>
            </div>

            <div v-if="showMenu" class="menu-preorder">
              <!-- AI Suggestion Section -->
              <div class="ai-suggestion-box">
                <div class="ai-header">
                  <h3>🤖 Smart Suggestion</h3>
                  <span class="ai-badge">AI Gợi Ý</span>
                </div>
                
                <div v-if="aiCombo.length === 0 && !isFetchingAI">
                  <p class="ai-desc">Để đưa ra gợi ý hợp lý nhất, bạn đi mấy người?</p>
                  <div style="display: flex; gap: 10px; margin-bottom: 10px;">
                    <input type="number" v-model="partySize" min="1" placeholder="Nhập số người" class="form-control lux-input" style="width: 150px; background: rgba(0,0,0,0.2); color: white;" />
                    <button class="lux-btn" style="padding: 10px; border-radius: 6px;" @click="fetchComboForParty">Nhận gợi ý</button>
                  </div>
                </div>
                <div v-else-if="isFetchingAI">
                  <p class="ai-desc">Đang phân tích và thiết kế thực đơn cho {{ partySize }} người...</p>
                </div>
                <div v-else>
                  <p class="ai-desc">{{ aiRecommendationReason }}</p>
                  
                  <div class="combo-grid">
                    <div v-for="product in aiCombo" :key="'ai-'+product.id" class="combo-item" style="display: flex; gap:10px; align-items:center; background: rgba(255,255,255,0.05); padding: 10px; border-radius: 8px; margin-bottom: 5px;">
                      <img :src="product.image || 'https://via.placeholder.com/100'" alt="food" style="width: 50px; height: 50px; border-radius: 6px; object-fit: cover;" />
                      <div class="product-info" style="flex:1;">
                        <h4 style="margin:0; font-size: 1rem;">{{ product.name }} <span v-if="product.suggestedQuantity > 1" style="color: var(--primary);">x{{product.suggestedQuantity}}</span></h4>
                        <p class="price" style="margin:0; color: #bbb;">{{ product.price.toLocaleString() }}đ</p>
                      </div>
                      <button class="lux-btn" style="padding: 5px 15px; border-radius:6px; min-width: auto;" @click="addToPreOrder(product, product.suggestedQuantity || 1)">Thêm</button>
                    </div>
                  </div>
                  <div class="ai-action" style="display: flex; gap: 10px; justify-content: center; margin-top: 15px;">
                    <button class="lux-btn" @click="addComboToCart">🛒 Thêm Cả Combo</button>
                    <button class="lux-btn-outline" @click="aiCombo = []">Thử lại</button>
                  </div>
                </div>
              </div>

              <h4 style="margin: 20px 0 10px; color: var(--primary);">Thực Đơn Đầy Đủ</h4>
              <div class="menu-list">
                <div v-for="p in products" :key="p.id" class="menu-item-lux" v-show="p.status !== false">
                  <img :src="p.image" />
                  <div class="info">
                    <h5>{{ p.name }}</h5>
                    <p>{{ p.price.toLocaleString() }}đ</p>
                  </div>
                  <button @click="addToPreOrder(p)" class="add-btn">+</button>
                </div>
              </div>
              <div class="cart-lux" v-if="preOrderCart.length > 0">
                <h4>Giỏ Hàng</h4>
                <div v-for="(item, idx) in preOrderCart" :key="idx" class="c-item">
                  <div style="flex: 1; min-width: 0;">
                    <span style="font-weight: 600;">{{ item.name }}</span>
                    <div style="font-size: 0.85rem; color: var(--primary); font-weight: 700;">{{ (item.price * item.quantity).toLocaleString() }}đ</div>
                  </div>
                  <div style="display: flex; align-items: center; gap: 6px; flex-shrink: 0;">
                    <button @click="decreasePreOrderQty(idx)" class="qty-btn-rsv qty-minus-rsv">−</button>
                    <span style="min-width: 24px; text-align: center; font-weight: 800;">{{ item.quantity }}</span>
                    <button @click="increasePreOrderQty(idx)" class="qty-btn-rsv qty-plus-rsv">+</button>
                    <button @click="preOrderCart.splice(idx,1)" class="del">✖</button>
                  </div>
                </div>
                <div style="font-size: 0.9rem; margin-top: 10px; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 10px; margin-bottom: 10px;">
                  <p style="margin: 0 0 5px 0;">Tạm tính: {{ foodSubtotal.toLocaleString() }}đ</p>
                  <p style="margin: 0;">Thuế GTGT: {{ foodTax.toLocaleString() }}đ</p>
                </div>
                <button @click="step = 4" class="lux-btn block-btn mt-10">THANH TOÁN: {{ foodTotal.toLocaleString() }}đ</button>
              </div>
            </div>
            <button v-if="showMenu" @click="showMenu = false" class="lux-btn-outline mt-20">Hủy đặt món</button>
          </div>

          <div v-else-if="step === 4" class="step-panel">
            <h3 class="panel-title">Thanh toán & Xác nhận</h3>
            <div class="payment-box-lux">
              <div class="p-info">
                <label>SĐT Liên Hệ (*)</label>
                <input v-model="form.phone" type="text" class="lux-input" placeholder="090..." />
                
                <div class="bill-summary">
                  <p>Thanh toán: <span class="gold-text">{{ finalPayAmount.toLocaleString() }}đ</span></p>
                  <p>Ngân hàng: Vietcombank (NGUYEN QUANG NHAT)</p>
                  <p>STK: 1047187126</p>
                </div>

                <label class="mt-10">Mã Giao Dịch (Bắt buộc)</label>
                <input v-model="form.txCode" type="text" class="lux-input" placeholder="Nhập mã sau khi CK" />
              </div>
              <div class="p-qr">
                <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.jpg?amount=${finalPayAmount}&addInfo=${form.phone || 'DAT BAN'}&accountName=NGUYEN QUANG NHAT`" />
                <p>Quét mã để thanh toán</p>
              </div>
            </div>
            <div class="btn-group mt-20">
              <button @click="step = 3" class="lux-btn-outline">QUAY LẠI</button>
              <button v-if="!isAdminOrManager" @click="submitReservation" class="lux-btn">HOÀN TẤT ĐẶT BÀN</button>
              <button v-else class="lux-btn btn-disabled" disabled>Chỉ xem (Admin)</button>
            </div>
          </div>
        </transition>
      </div>
    </main>
  </div>
  </CustomerLayout>
</template>

<script setup>
import CustomerLayout from '@/components/CustomerLayout.vue';

import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const step = ref(1);
const showMenu = ref(false);
const form = ref({ date: '', time: '', phone: '', txCode: '' });

const tablesList = ref([]);
const products = ref([]);
const selectedFloor = ref('');
const selectedTable = ref(null);
const preOrderCart = ref([]);
const userRoles = ref([]);

const aiCombo = ref([]);
const aiRecommendationReason = ref('');
const partySize = ref('');
const isFetchingAI = ref(false);

const isAdminOrManager = computed(() => {
  return userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_MANAGER');
});

const foodSubtotal = computed(() => preOrderCart.value.reduce((total, item) => total + (item.price * item.quantity), 0));
const foodTax = computed(() => preOrderCart.value.reduce((total, item) => total + ((item.price * item.quantity) * (item.taxRate || 8) / 100), 0));
const foodTotal = computed(() => foodSubtotal.value + foodTax.value);
const finalPayAmount = computed(() => preOrderCart.value.length > 0 ? foodTotal.value : 500000);

const uniqueFloors = computed(() => {
  const floors = new Set();
  tablesList.value.forEach(t => {
    if (t.floor) floors.add(t.floor.trim());
  });
  return Array.from(floors).sort();
});

const filteredTables = computed(() => tablesList.value.filter(t => String(t.floor).trim() === String(selectedFloor.value).trim()));

onMounted(async () => {
  try {
    const resProd = await axios.get('http://localhost:8080/api/products');
    products.value = resProd.data;
  } catch (err) {}
  
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    try {
      const parsed = JSON.parse(storedUser);
      if (parsed && parsed.roles) {
        userRoles.value = parsed.roles;
      }
    } catch (e) {}
  }
});

const nextToTable = async () => {
  if(!form.value.date || !form.value.time) return alert("Vui lòng chọn thời gian!");
  try {
    const res = await axios.get(`http://localhost:8080/api/tables/check-availability?date=${form.value.date}&time=${form.value.time}`);
    tablesList.value = res.data;
    if (tablesList.value.length > 0 && uniqueFloors.value.length > 0) {
      selectedFloor.value = uniqueFloors.value[0];
    }
    step.value = 2;
  } catch (err) { alert("Lỗi tải sơ đồ bàn!"); }
};

const selectTable = (table) => { if (table.isOccupied === 0) selectedTable.value = table; };

const fetchComboForParty = async () => {
  if (!partySize.value || partySize.value < 1) {
    alert("Vui lòng nhập số người hợp lệ!");
    return;
  }
  isFetchingAI.value = true;
  try {
    const wRes = await axios.get('https://api.open-meteo.com/v1/forecast?latitude=16.0678&longitude=108.2208&current_weather=true');
    const weather = wRes.data.current_weather;
    const weatherCode = weather.weathercode;
    let weatherStr = `Trời quang, nhiệt độ ${weather.temperature}°C`;
    if (weatherCode >= 50 && weatherCode <= 69) weatherStr = `Trời đang mưa lất phất, nhiệt độ ${weather.temperature}°C`;
    else if (weatherCode >= 70) weatherStr = `Trời mưa to/tuyết, nhiệt độ ${weather.temperature}°C`;
    else if (weather.temperature > 30) weatherStr = `Trời nắng nóng, nhiệt độ ${weather.temperature}°C`;

    const activeProds = products.value.filter(p => p.status !== false);
    const menuStr = activeProds.map(p => `${p.id}-${p.name}`).join(', ');
    const message = `Khách đi ${partySize.value} người. Thời tiết hiện tại: ${weatherStr}`;
    
    const aiRes = await axios.post('http://localhost:8080/api/chatbot/chat', {
      type: 'COMBO_RECOMMEND',
      message: message,
      menu: menuStr
    });

    let reply = aiRes.data.reply;
    reply = reply.replace(/```json/g, '').replace(/```/g, '').trim();
    
    if (!reply.startsWith('[') && !reply.startsWith('{')) {
      throw new Error("AI trả về định dạng không hợp lệ: " + reply);
    }
    
    const suggestions = JSON.parse(reply);

    aiCombo.value = suggestions.map(s => {
      const p = activeProds.find(prod => prod.id == s.id);
      if (p) return { ...p, suggestedQuantity: s.quantity || 1 };
      return null;
    }).filter(p => p != null);
    
    if(suggestions.length > 0) aiRecommendationReason.value = suggestions[0].reason;

  } catch(e) {
    console.error("Lỗi AI Recommend:", e);
    const activeProds = products.value.filter(p => p.status !== false);
    if (activeProds.length >= 2) {
      let pSize = parseInt(partySize.value) || 2;
      
      // Phân loại món ăn và nước uống
      let drinks = activeProds.filter(p => p.category && (p.category.name.toLowerCase().includes('nước') || p.category.name.toLowerCase().includes('uống') || p.category.name.toLowerCase().includes('trà') || p.category.name.toLowerCase().includes('cafe')));
      let foods = activeProds.filter(p => !p.category || (!p.category.name.toLowerCase().includes('nước') && !p.category.name.toLowerCase().includes('uống') && !p.category.name.toLowerCase().includes('trà') && !p.category.name.toLowerCase().includes('cafe')));
      
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
        // Chia đều số lượng cho các món, mỗi món ít nhất 1 phần
        let qty = idx === 0 ? Math.ceil(pSize / numFoodTypes) + 1 : Math.ceil(pSize / numFoodTypes);
        combo.push({ ...food, suggestedQuantity: Math.max(1, qty) });
      });
      
      // Mỗi người 1 nước, chia đều cho các loại nước
      let drinksPerType = Math.ceil(pSize / numDrinkTypes);
      selectedDrinks.forEach(drink => {
        combo.push({ ...drink, suggestedQuantity: drinksPerType });
      });
      
      // Nếu không có nước riêng, chọn bất kỳ sản phẩm nào chưa được chọn
      if (selectedDrinks.length === 0 && activeProds.length > numFoodTypes) {
        let remaining = activeProds.filter(p => !selectedFoods.find(f => f.id === p.id));
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
  aiCombo.value.forEach(p => addToPreOrder(p, p.suggestedQuantity || 1));
  alert('Đã thêm combo vào giỏ hàng!');
};

const addToPreOrder = (p, qty = 1) => {
  const ex = preOrderCart.value.find(i => i.productId === p.id);
  if (ex) ex.quantity += qty; else preOrderCart.value.push({ productId: p.id, name: p.name, price: p.price, quantity: qty, taxRate: p.taxRate || 8 });
};

const increasePreOrderQty = (idx) => {
  preOrderCart.value[idx].quantity++;
};

const decreasePreOrderQty = (idx) => {
  if (preOrderCart.value[idx].quantity > 1) {
    preOrderCart.value[idx].quantity--;
  } else {
    preOrderCart.value.splice(idx, 1);
  }
};

const submitReservation = async () => {
  const token = localStorage.getItem('token');
  if(!token || !selectedTable.value || !form.value.phone || !form.value.txCode) return alert("Vui lòng điền đủ thông tin!");

  // GỬI CHUẨN ĐỊNH DẠNG "Bàn: " ĐỂ BACKEND NHẬN DIỆN KHÓA BÀN
  const type = preOrderCart.value.length > 0 ? 'Có món' : 'Giữ chỗ';
  const infoFull = `Bàn: ${selectedTable.value.name} | SĐT: ${form.value.phone} | Lúc: ${form.value.time} ngày ${form.value.date} | ${type} | MãGD: ${form.value.txCode}`;
  
  const formattedItems = preOrderCart.value.map(i => ({ productId: i.productId, quantity: i.quantity }));

  try {
    await axios.post('http://localhost:8080/api/orders/checkout', { 
      address: infoFull, 
      items: formattedItems,
      deposit: finalPayAmount.value 
    }, { headers: { 'Authorization': `Bearer ${token}` } });
    alert("🎉 Cảm ơn quý khách! Bàn đã được đặt thành công.");
    router.push('/history'); 
  } catch (error) { alert("Lỗi đặt bàn!"); }
};
</script>

<style scoped>
.reservation-luxury { background: var(--bg-root); min-height: 100vh; font-family: 'Outfit', -apple-system, sans-serif; color: var(--text-primary); }

.luxury-navbar {
  background: rgba(13, 27, 42, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky; top: 0; z-index: 100;
  padding: 12px 40px;
}
.nav-container { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; text-decoration: none; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.gold-text { color: var(--secondary); }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(0,212,170,0.1); }

.nav-right-rsv { display: flex; gap: 10px; }
.btn-rsv-nav {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-rsv-nav:hover { border-color: var(--primary); color: var(--primary); background: rgba(0,212,170,0.1); }

.content-wrap { display: flex; flex-direction: column; justify-content: center; align-items: center; min-height: 80vh; padding: 40px 20px; }
.booking-card { background: var(--bg-card); max-width: 800px; width: 100%; border-radius: 15px; padding: 40px; box-shadow: var(--shadow-lg); border: 1px solid var(--border-light); }
.elegant-title { text-align: center; color: var(--primary); font-size: 2.5rem; margin-bottom: 5px; font-weight: 900; letter-spacing: 2px;}
.elegant-subtitle { text-align: center; color: var(--text-secondary); margin-bottom: 30px;}

.step-progress { display: flex; justify-content: space-between; border-bottom: 2px solid var(--border); padding-bottom: 15px; margin-bottom: 30px;}
.step { color: var(--text-muted); font-weight: 600; font-size: 0.9rem;}
.step.active { color: var(--primary); border-bottom: 2px solid var(--primary); padding-bottom: 15px; margin-bottom: -17px; text-shadow: 0 0 10px var(--primary-glow); }

.panel-title { color: var(--text-heading); margin-bottom: 20px; font-size: 1.3rem;}
.input-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px;}
.lux-input { width: 100%; padding: 12px 15px; border: 1px solid var(--border); border-radius: 5px; font-size: 1rem; background: var(--bg-input); color: var(--text-primary); box-sizing: border-box; transition: 0.3s;}
.lux-input:focus { border-color: var(--primary); outline: none; box-shadow: 0 0 5px var(--primary-glow);}

.lux-btn { background: linear-gradient(135deg, var(--primary), #3498db); color: var(--bg-dark); border: none; padding: 12px 25px; border-radius: 5px; font-weight: bold; letter-spacing: 1px; cursor: pointer; transition: 0.3s;}
.lux-btn:hover { box-shadow: 0 5px 15px var(--primary-glow); transform: translateY(-2px); }
.lux-btn-outline { background: transparent; border: 1px solid var(--border); color: var(--text-secondary); padding: 12px 25px; border-radius: 5px; font-weight: bold; cursor: pointer; transition: 0.3s;}
.lux-btn-outline:hover { border-color: var(--primary); color: var(--primary); }
.block-btn { width: 100%; }
.mt-20 { margin-top: 20px;} .mt-10 { margin-top: 10px;}
.btn-group { display: flex; justify-content: space-between;}
.btn-disabled { opacity: 0.5; cursor: not-allowed !important; background: var(--bg-card); color: var(--text-muted); border: 1px solid var(--border); }

.floor-tabs { display: flex; gap: 10px; margin-bottom: 20px; justify-content: center;}
.floor-tabs button { background: var(--bg-input); border: 1px solid var(--border); padding: 10px 20px; border-radius: 20px; cursor: pointer; font-weight: 600; color: var(--text-secondary); transition: 0.3s;}
.floor-tabs button.active { background: var(--primary); color: var(--bg-dark); border-color: var(--primary); box-shadow: 0 0 10px var(--primary-glow);}

.table-grid-lux { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 15px;}
.table-card-lux { background: var(--bg-card2); border: 1px solid var(--border); border-radius: 10px; padding: 20px 10px; text-align: center; cursor: pointer; position: relative; transition: 0.2s;}
.table-card-lux:hover:not(.disabled) { border-color: var(--primary); transform: translateY(-3px); box-shadow: 0 5px 15px var(--primary-glow);}
.table-card-lux.selected { border: 2px solid var(--primary); background: rgba(0, 212, 170, 0.1);}
.table-card-lux.disabled { opacity: 0.5; cursor: not-allowed; background: var(--bg-root);}
.status-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-top: 10px;}
.green { background: #2ecc71; box-shadow: 0 0 5px #2ecc71;}
.red { background: #e74c3c; box-shadow: 0 0 5px #e74c3c;}
.t-view { position: absolute; top: 5px; right: 5px; background: var(--primary); color: var(--bg-dark); font-size: 0.65rem; padding: 2px 5px; border-radius: 5px; font-weight: bold;}

.service-options { display: flex; gap: 20px;}
.opt-card { flex: 1; border: 1px solid var(--border); border-radius: 10px; padding: 30px 20px; text-align: center; cursor: pointer; transition: 0.3s; background: var(--bg-card2);}
.opt-card:hover { border-color: var(--primary); box-shadow: 0 5px 15px var(--primary-glow);}
.opt-card.highlight { background: rgba(0, 212, 170, 0.05); border-color: var(--primary);}
.opt-card .icon { font-size: 2.5rem; margin-bottom: 10px;}

.menu-preorder { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-top: 20px;}
.menu-list { max-height: 350px; overflow-y: auto; padding-right: 10px;}
.btn-group { display: flex; gap: 15px; margin-top: 30px; }

/* AI Suggestion Styles */
.ai-suggestion-box {
  background: rgba(13, 27, 42, 0.6);
  border: 1px solid rgba(0, 255, 170, 0.2);
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 255, 170, 0.05);
}
.ai-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.ai-header h3 { margin: 0; font-size: 1.1rem; color: var(--primary); display: flex; align-items: center; gap: 8px; }
.ai-badge { background: var(--primary); color: var(--bg-dark); font-size: 0.75rem; font-weight: bold; padding: 3px 8px; border-radius: 12px; }
.ai-desc { font-size: 0.9rem; color: #ccc; margin-bottom: 10px; font-style: italic; }
.menu-item-lux { display: flex; gap: 15px; margin-bottom: 15px; border-bottom: 1px dashed var(--border); padding-bottom: 10px; align-items: center;}
.menu-item-lux img { width: 60px; height: 60px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border);}
.menu-item-lux .info h5 { margin: 0 0 5px 0; font-size: 1rem; color: var(--text-heading);}
.menu-item-lux .info p { margin: 0; color: var(--primary); font-weight: bold;}
.add-btn { margin-left: auto; background: var(--primary-glow); color: var(--primary); border: 1px solid var(--primary); width: 30px; height: 30px; border-radius: 50%; cursor: pointer; transition: 0.3s;}
.add-btn:hover { background: var(--primary); color: var(--bg-dark); }
.cart-lux { background: var(--bg-card2); padding: 15px; border-radius: 10px; border: 1px solid var(--border);}
.cart-lux h4 { margin-top: 0; border-bottom: 1px dashed var(--border); padding-bottom: 10px; color: var(--text-heading);}
.c-item { display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; margin-bottom: 8px; color: var(--text-primary); padding: 6px 0; border-bottom: 1px dashed rgba(255,255,255,0.06);}
.del { color: #e74c3c; cursor: pointer; font-size: 0.8rem; background: none; border: none; margin-left: 4px; transition: 0.2s; }
.del:hover { color: #ff6b6b; }
.qty-btn-rsv {
  width: 28px; height: 28px; border-radius: 6px;
  border: 1px solid var(--border); background: var(--bg-input);
  color: var(--text-primary); font-size: 1rem; font-weight: 700;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s ease;
}
.qty-btn-rsv:hover { border-color: var(--primary); color: var(--primary); background: rgba(0,212,170,0.1); }
.qty-minus-rsv:hover { border-color: #e74c3c; color: #e74c3c; background: rgba(231,76,60,0.1); }

.payment-box-lux { display: flex; gap: 30px; background: var(--bg-card2); padding: 20px; border-radius: 10px; border: 1px solid var(--border);}
.p-info { flex: 1;}
.p-qr { text-align: center;}
.p-qr img { width: 160px; border-radius: 10px; border: 2px solid var(--primary); box-shadow: 0 5px 15px var(--primary-glow);}
.bill-summary { background: var(--bg-root); padding: 15px; border-radius: 5px; margin-top: 15px; border-left: 3px solid var(--primary);}
.bill-summary p { margin: 5px 0; font-size: 0.9rem; color: var(--text-secondary);}
.gold-text { font-size: 1.2rem; font-weight: bold; color: var(--primary);}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>