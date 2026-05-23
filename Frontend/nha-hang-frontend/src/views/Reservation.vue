<template>
  <div class="reservation-luxury">
    <header class="luxury-navbar">
      <div class="nav-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🍽️</span>
          <h2>FPOLY <span class="gold-text">RESTAURANT</span></h2>
        </div>
        <nav class="nav-links">
          <router-link to="/">Trang chủ</router-link>
          <router-link to="/menu">Thực đơn</router-link>
          <a href="#" class="active">Đặt chỗ</a>
          <router-link to="/dine-in">Tại bàn</router-link>
        </nav>
        <div class="nav-right-rsv">
          <button @click="$router.push('/history')" class="btn-rsv-nav">📜 Lịch Sử</button>
          <button @click="$router.push('/login')" class="btn-rsv-nav">🔐 Đăng nhập</button>
        </div>
      </div>
    </header>

    <main class="content-wrap">
      <div class="booking-card">
        <h1 class="elegant-title">RESERVATION</h1>
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
              <button :class="{'active': selectedFloor === 'Tầng 2'}" @click="selectedFloor = 'Tầng 2'">Tầng 2 (VIP)</button>
              <button :class="{'active': selectedFloor === 'Tầng 3'}" @click="selectedFloor = 'Tầng 3'">Tầng 3 (Standard)</button>
              <button :class="{'active': selectedFloor === 'Sân thượng'}" @click="selectedFloor = 'Sân thượng'">Rooftop</button>
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
                  <span>{{ item.name }} (x{{ item.quantity }})</span>
                  <span @click="preOrderCart.splice(idx,1)" class="del">✖</span>
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
              <button @click="submitReservation" class="lux-btn">HOÀN TẤT ĐẶT BÀN</button>
            </div>
          </div>
        </transition>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const step = ref(1);
const showMenu = ref(false);
const form = ref({ date: '', time: '', phone: '', txCode: '' });

const tablesList = ref([]);
const products = ref([]);
const selectedFloor = ref('Tầng 2');
const selectedTable = ref(null);
const preOrderCart = ref([]);

const foodTotal = computed(() => preOrderCart.value.reduce((total, item) => total + (item.price * item.quantity), 0));
const finalPayAmount = computed(() => preOrderCart.value.length > 0 ? foodTotal.value : 500000);

const filteredTables = computed(() => tablesList.value.filter(t => String(t.floor).trim() === String(selectedFloor.value).trim()));

onMounted(async () => {
  try {
    const resProd = await axios.get('http://localhost:8080/api/products');
    products.value = resProd.data;
  } catch (err) {}
});

const nextToTable = async () => {
  if(!form.value.date || !form.value.time) return alert("Vui lòng chọn thời gian!");
  try {
    const res = await axios.get(`http://localhost:8080/api/tables/check-availability?date=${form.value.date}&time=${form.value.time}`);
    tablesList.value = res.data;
    step.value = 2;
  } catch (err) { alert("Lỗi tải sơ đồ bàn!"); }
};

const selectTable = (table) => { if (table.isOccupied === 0) selectedTable.value = table; };

const addToPreOrder = (p) => {
  const ex = preOrderCart.value.find(i => i.productId === p.id);
  if (ex) ex.quantity++; else preOrderCart.value.push({ productId: p.id, name: p.name, price: p.price, quantity: 1 });
};

const submitReservation = async () => {
  const token = localStorage.getItem('token');
  if(!token || !selectedTable.value || !form.value.phone || !form.value.txCode) return alert("Vui lòng điền đủ thông tin!");

  // GỬI CHUẨN ĐỊNH DẠNG "Bàn: " ĐỂ BACKEND NHẬN DIỆN KHÓA BÀN
  const type = preOrderCart.value.length > 0 ? 'Có món' : 'Giữ chỗ';
  const infoFull = `Bàn: ${selectedTable.value.name} | SĐT: ${form.value.phone} | Lúc: ${form.value.time} ngày ${form.value.date} | ${type} | MãGD: ${form.value.txCode}`;
  
  const formattedItems = preOrderCart.value.map(i => ({ productId: i.productId, quantity: i.quantity }));

  try {
    await axios.post('http://localhost:8080/api/orders/checkout', { address: infoFull, items: formattedItems }, 
    { headers: { 'Authorization': `Bearer ${token}` } });
    alert("🎉 Cảm ơn quý khách! Bàn đã được đặt thành công.");
    router.push('/history'); 
  } catch (error) { alert("Lỗi đặt bàn!"); }
};
</script>

<style scoped>
.reservation-luxury { background: var(--bg-root); min-height: 100vh; font-family: 'Inter', sans-serif; color: var(--text-primary); }
.luxury-navbar { background: var(--bg-nav); padding: 15px 0; border-bottom: 1px solid var(--border); box-shadow: 0 4px 15px rgba(0,0,0,0.5); }
.nav-container { max-width: 1100px; margin: auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px;}
.logo h2 { color: var(--primary); margin: 0; font-size: 1.5rem; letter-spacing: 2px; font-weight: 900;}
.gold-text { color: var(--primary); }
.nav-links a { color: var(--text-secondary); text-decoration: none; margin-left: 25px; text-transform: uppercase; font-size: 0.85rem; font-weight: 600; letter-spacing: 1px; transition: 0.3s;}
.nav-links a:hover, .nav-links a.active { color: var(--primary); }

.logo { display: flex; align-items: center; gap: 10px; }
.logo-icon { font-size: 1.5rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.nav-right-rsv { display: flex; gap: 8px; }
.btn-rsv-nav {
  background: transparent; border: 1px solid var(--border); color: var(--text-secondary);
  padding: 7px 14px; border-radius: 20px; cursor: pointer; font-size: 0.83rem;
  font-weight: 600; font-family: inherit; transition: 0.3s;
}
.btn-rsv-nav:hover { border-color: var(--primary); color: var(--primary); }

.content-wrap { display: flex; justify-content: center; align-items: center; min-height: 80vh; padding: 40px 20px; }
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
.menu-item-lux { display: flex; gap: 15px; margin-bottom: 15px; border-bottom: 1px dashed var(--border); padding-bottom: 10px; align-items: center;}
.menu-item-lux img { width: 60px; height: 60px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border);}
.menu-item-lux .info h5 { margin: 0 0 5px 0; font-size: 1rem; color: var(--text-heading);}
.menu-item-lux .info p { margin: 0; color: var(--primary); font-weight: bold;}
.add-btn { margin-left: auto; background: var(--primary-glow); color: var(--primary); border: 1px solid var(--primary); width: 30px; height: 30px; border-radius: 50%; cursor: pointer; transition: 0.3s;}
.add-btn:hover { background: var(--primary); color: var(--bg-dark); }
.cart-lux { background: var(--bg-card2); padding: 15px; border-radius: 10px; border: 1px solid var(--border);}
.cart-lux h4 { margin-top: 0; border-bottom: 1px dashed var(--border); padding-bottom: 10px; color: var(--text-heading);}
.c-item { display: flex; justify-content: space-between; font-size: 0.9rem; margin-bottom: 8px; color: var(--text-primary);}
.del { color: #e74c3c; cursor: pointer; font-size: 0.8rem;}

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