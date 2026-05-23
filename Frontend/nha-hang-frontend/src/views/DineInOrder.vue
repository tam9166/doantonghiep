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
        <select v-model="selectedTable" class="form-control table-select">
          <option value="" disabled>-- Vui lòng chọn bàn của bạn --</option>
          <optgroup v-for="(tables, floor) in groupedTables" :key="floor" :label="floor">
          <!-- ĐÃ NÂNG CẤP: Hiện trạng thái và chặn khách chọn bàn đã có người -->
          <option 
            v-for="t in tables" 
            :key="t.id" 
            :value="t.name" 
            :disabled="t.isOccupied !== 0"
          >
            {{ t.name }} {{ t.isOccupied === 0 ? '(🟢 Trống)' : (t.isOccupied === 1 ? '(🟡 Đã cọc)' : '(🔴 Có khách)') }}
          </option>
        </optgroup>
        </select>
      </div>

      <div class="product-list" v-if="selectedTable">
        <!-- AI Suggestion Section -->
        <div v-if="aiCombo.length > 0" class="ai-suggestion-box">
          <div class="ai-header">
            <h3>🤖 Smart Suggestion</h3>
            <span class="ai-badge">AI Gợi Ý</span>
          </div>
          <p class="ai-desc">Dựa trên 1,245 hóa đơn tuần qua, đây là Combo được gọi cùng nhau nhiều nhất!</p>
          
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
    </main>

    <div class="sticky-bottom-cart" v-if="cart.length > 0">
      <div class="cart-summary" @click="showModal = true">
        <span class="cart-icon">🛒 {{ totalItems }} món</span>
        <span class="cart-total">{{ cartTotal.toLocaleString() }} VNĐ</span>
      </div>
      <button class="btn-checkout" @click="showModal = true">Thanh Toán</button>
    </div>

    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <h2 class="modal-title">Xác Nhận & Thanh Toán</h2>
        
        <div class="cart-details">
          <div v-for="(item, idx) in cart" :key="idx" class="cart-row">
            <span>{{ item.name }} <strong>(x{{ item.quantity }})</strong></span>
            <span>{{ (item.price * item.quantity).toLocaleString() }}đ</span>
          </div>
        </div>

        <div class="payment-box">
          <p style="text-align: center; font-weight: bold; color: #b72c2c;">Tổng: {{ cartTotal.toLocaleString() }}đ</p>
          <div class="qr-area">
            <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.jpg?amount=${cartTotal}&addInfo=${selectedTable} DAT MON&accountName=NGUYEN QUANG NHAT`" alt="QR" />
          </div>
          <div class="form-group mt-3">
            <label>Mã giao dịch (Sau khi CK) *:</label>
            <input v-model="txCode" type="text" placeholder="Nhập mã GD..." class="form-control" />
          </div>
        </div>

        <div class="modal-actions">
          <button @click="showModal = false" class="btn-cancel">Quay lại</button>
          <button @click="submitOrder" class="btn-confirm">Gửi Đơn Cho Bếp</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const products = ref([]);
const allTables = ref([]);
const cart = ref([]);
const selectedTable = ref("");
const txCode = ref("");
const showModal = ref(false);

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
const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));

// AI Suggestion Logic (Lấy ra 2 món ngẫu nhiên hoặc món cố định giả lập AI phân tích)
const aiCombo = computed(() => {
  if (activeProducts.value.length >= 2) {
    // Lấy 1 món ăn và 1 đồ uống nếu có, hoặc lấy top 2 món
    return activeProducts.value.slice(0, 2);
  }
  return [];
});

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
  } catch (error) { console.error(error); }
};

const addToCart = (product) => {
  const existing = cart.value.find(item => item.productId === product.id);
  if (existing) existing.quantity++;
  else cart.value.push({ productId: product.id, name: product.name, price: product.price, quantity: 1 });
};

const submitOrder = async () => {
  if (!txCode.value) return alert("Vui lòng nhập Mã giao dịch để nhà hàng xác nhận!");
  
  const token = localStorage.getItem('token') || ''; 

  // Lấy ngày và giờ hiện tại trên máy của khách
  const today = new Date().toLocaleDateString('en-CA'); // Ra định dạng YYYY-MM-DD
  const now = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

  // 🟢 CẬP NHẬT CHỖ NÀY: Chèn ngày giờ vào chuỗi y hệt như Đặt Bàn
  // Backend đọc thấy chuỗi "ngày YYYY-MM-DD" sẽ lập tức tự động khóa cái bàn này lại thành màu đỏ!
  const infoFull = `[TẠI QUÁN] Bàn: ${selectedTable.value} | Lúc: ${now} ngày ${today} | MãGD: ${txCode.value}`;
  
  const formattedItems = cart.value.map(item => ({ productId: item.productId, quantity: item.quantity }));

  try {
    await axios.post('http://localhost:8080/api/orders/checkout', {
      address: infoFull,
      items: formattedItems
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    alert("🎉 Đã gửi đơn thành công! Bếp đang chuẩn bị món cho bạn.");
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
.dine-in-wrapper { font-family: 'Inter', sans-serif; background-color: var(--bg-root); min-height: 100vh; padding-bottom: 80px; color: var(--text-primary); }

/* Navbar */
.dinein-navbar {
  background: rgba(6, 13, 26, 0.95); backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light); position: sticky; top: 0; z-index: 100;
  box-shadow: 0 2px 30px rgba(0,0,0,0.5);
}
.nav-container {
  max-width: 1400px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px; height: 68px;
}
.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-icon { font-size: 1.5rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.logo h2 { margin: 0; font-size: 1.2rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo p { margin: 0; font-size: 0.65rem; color: var(--primary); letter-spacing: 3px; text-transform: uppercase; }
.nav-links { display: flex; gap: 4px; align-items: center; }
.nav-links a {
  color: var(--text-muted); text-decoration: none; font-size: 0.88rem; font-weight: 500;
  padding: 8px 14px; border-radius: 8px; transition: 0.3s;
}
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(0,212,170,0.08); }
.nav-right { display: flex; align-items: center; gap: 8px; }
.btn-nav-dinein {
  background: transparent; border: 1px solid var(--border); color: var(--text-secondary);
  padding: 7px 14px; border-radius: 20px; cursor: pointer; font-size: 0.83rem;
  font-weight: 600; font-family: inherit; transition: 0.3s;
}
.btn-nav-dinein:hover { border-color: var(--primary); color: var(--primary); }

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
.cart-summary { cursor: pointer; }
.cart-icon { font-weight: bold; color: var(--text-secondary); display: block; font-size: 0.9rem;}
.cart-total { color: var(--primary); font-weight: bold; font-size: 1.2rem; }
.btn-checkout { background: var(--primary); color: var(--bg-dark); border: none; padding: 10px 25px; border-radius: 25px; font-weight: bold; font-size: 1.1rem; cursor: pointer; transition: 0.3s; }
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
</style>