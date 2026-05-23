<template>
  <div class="menu-wrapper">
    <header class="navbar">
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
          <a href="#" class="active">Thực đơn</a>
          <router-link to="/reservation">Đặt chỗ</router-link>
          <router-link to="/dine-in">Tại bàn</router-link>
        </nav>
        <div class="nav-right">
          <template v-if="!isLoggedIn">
            <button @click="$router.push('/login')" class="btn-nav-outline">Đăng nhập</button>
          </template>
          <template v-else>
            <button @click="$router.push('/history')" class="btn-nav-outline">📜 Lịch Sử</button>
            <button @click="showCheckoutModal = true" class="btn-cart">
              🛒 Giỏ hàng ({{ cart.length }})
            </button>
          </template>
        </div>
      </div>
    </header>

    <main class="menu-content">
      <h1 class="page-title">Thực Đơn Giao Hàng</h1>
      <p class="page-subtitle">Chọn món ngon - Giao nóng hổi tận nhà</p>

      <div class="category-filter">
        <button :class="{'active': selectedCategory === null}" @click="selectedCategory = null">Tất cả món</button>
        <button v-for="c in categories" :key="c.id" :class="{'active': selectedCategory === c.id}" @click="selectedCategory = c.id">
          {{ c.name }}
        </button>
      </div>

      <div class="product-grid">
        <div v-for="product in filteredProducts" :key="product.id" class="product-card">
          <img :src="product.image || 'https://via.placeholder.com/150'" alt="food" />
          <h3>{{ product.name }}</h3>
          <p class="price">{{ product.price.toLocaleString() }} VNĐ</p>
          <button class="btn-add" @click="addToCart(product)">+ Thêm vào giỏ</button>
        </div>
      </div>
    </main>

    <div v-if="showCheckoutModal" class="modal-overlay">
      <div class="modal-content checkout-modal">
        <h2 class="modal-header-title">Thông Tin Giao Hàng & Thanh Toán</h2>
        
        <div class="checkout-scroll-area">
          <div class="form-group">
            <label>Họ tên người nhận (*):</label>
            <input v-model="orderInfo.fullname" type="text" placeholder="Nguyễn Văn A..." class="form-control" />
          </div>
          <div class="form-group">
            <label>Số điện thoại (*):</label>
            <input v-model="orderInfo.phone" type="text" placeholder="0905..." class="form-control" />
          </div>
          <div class="form-group">
            <label>Địa chỉ nhận hàng (*):</label>
            <input v-model="orderInfo.address" type="text" placeholder="Số nhà, tên đường..." class="form-control" />
          </div>

          <div class="payment-banking-box">
            <h3 class="payment-title">💳 Chuyển khoản qua VietQR</h3>
            <div class="payment-layout">
              <div class="payment-info-text">
                <p>Tổng tiền: <strong style="color: #b72c2c; font-size: 1.2rem;">{{ cartTotal.toLocaleString() }}đ</strong></p>
                <div class="bank-card">
                  <p>Ngân hàng: <strong>Vietcombank</strong></p>
                  <p>STK: <strong>1047187126</strong></p>
                  <p>Tên TK: <strong>NGUYEN QUANG NHAT</strong></p>
                  <p>Nội dung: <strong>{{ orderInfo.phone || 'SDT' }} SHIP</strong></p>
                </div>
              </div>
              <div class="qr-code-image">
                <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.jpg?amount=${cartTotal}&addInfo=${orderInfo.phone || 'SHIP'}&accountName=NGUYEN QUANG NHAT`" alt="QR" />
              </div>
            </div>
            
            <div class="form-group mt-2">
              <label style="color: #b72c2c;">Dán Mã giao dịch sau khi CK (*):</label>
              <input v-model="orderInfo.txCode" type="text" placeholder="Ví dụ: FT24..." class="form-control tx-input" />
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button @click="showCheckoutModal = false" class="btn-cancel">Đóng</button>
          <button v-if="cart.length > 0" @click="submitShipOrder" class="btn-confirm">Xác Nhận & Đặt Hàng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const products = ref([]);
const categories = ref([]);
const cart = ref([]);
const router = useRouter();
const isLoggedIn = ref(false);

const showCheckoutModal = ref(false);
const selectedCategory = ref(null); 

// Cập nhật orderInfo để bỏ paymentMethod, thêm fullname và txCode
const orderInfo = ref({ fullname: '', phone: '', address: '', txCode: '' });

// Tính tổng tiền giỏ hàng để hiện lên QR
const cartTotal = computed(() => {
  return cart.value.reduce((total, item) => total + (item.price * item.quantity), 0);
});

const fetchProducts = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/products');
    products.value = response.data;
  } catch (error) { console.error(error); }
};

const fetchCategories = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/categories');
    categories.value = response.data;
  } catch (error) { console.error(error); }
};

const filteredProducts = computed(() => {
  const activeProducts = products.value.filter(p => p.status !== false);
  if (selectedCategory.value === null) return activeProducts;
  return activeProducts.filter(p => p.category && p.category.id === selectedCategory.value);
});

const addToCart = (product) => {
  const existing = cart.value.find(item => item.productId === product.id);
  if (existing) {
    existing.quantity++;
  } else {
    cart.value.push({ productId: product.id, quantity: 1, name: product.name, price: product.price });
  }
  alert(`Đã thêm ${product.name} vào giỏ!`);
};

const submitShipOrder = async () => {
  const token = localStorage.getItem('token');
  
  if(!orderInfo.value.fullname || !orderInfo.value.phone || !orderInfo.value.address || !orderInfo.value.txCode) {
    alert("Vui lòng điền đầy đủ thông tin nhận hàng và Mã giao dịch!");
    return;
  }

  // Cấu trúc infoFull để Admin dùng In hóa đơn sau này
  const infoFull = `[GIAO HÀNG] Khách: ${orderInfo.value.fullname} | SĐT: ${orderInfo.value.phone} | ĐC: ${orderInfo.value.address} | MãGD: ${orderInfo.value.txCode}`;
  
  const formattedItems = cart.value.map(item => ({ 
    productId: item.productId, 
    quantity: item.quantity 
  }));

  try {
    await axios.post('http://localhost:8080/api/orders/checkout', {
      address: infoFull,
      items: formattedItems
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    alert("🚀 Đặt hàng thành công! Cửa hàng sẽ kiểm tra và giao món ngay.");
    showCheckoutModal.value = false;
    cart.value = [];
    router.push('/history');
  } catch (error) {
    alert("Lỗi: " + (error.response?.data || "Vui lòng thử lại"));
  }
};

onMounted(() => {
  fetchProducts();
  fetchCategories();
  const token = localStorage.getItem('token');
  if (token) isLoggedIn.value = true;
});
</script>

<style scoped>
.menu-wrapper { background-color: var(--bg-root); min-height: 100vh; font-family: 'Inter', sans-serif; color: var(--text-primary); }
.navbar { background-color: var(--bg-nav); color: white; padding: 15px 0; position: sticky; top: 0; z-index: 100; font-family: 'Inter', sans-serif; border-bottom: 1px solid var(--border); box-shadow: 0 4px 15px rgba(0,0,0,0.5); }
.nav-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.logo { cursor: pointer; text-align: center; display: flex; align-items: center; gap: 10px; }
.logo-icon { font-size: 1.5rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.logo h2 { margin: 0; font-size: 1.5rem; letter-spacing: 1px; color: var(--primary); font-weight: 900; }
.logo p { margin: 0; font-size: 0.8rem; letter-spacing: 3px; color: var(--text-secondary); }
.nav-links a { color: var(--text-secondary); text-decoration: none; margin-left: 20px; font-weight: 600; transition: 0.3s; }
.nav-links a:hover { color: var(--primary); }
.nav-links a.active { border-bottom: 2px solid var(--primary); padding-bottom: 5px; color: var(--primary); }
.btn-nav-outline { background: transparent; border: 1px solid var(--border); color: var(--text-secondary); padding: 8px 15px; border-radius: 20px; cursor: pointer; transition: 0.3s; font-weight: bold; }
.btn-nav-outline:hover { border-color: var(--primary); color: var(--primary); }
.btn-cart { background: linear-gradient(135deg, var(--primary), #3498db); color: var(--bg-dark); border: none; padding: 8px 15px; border-radius: 20px; font-weight: bold; cursor: pointer; transition: 0.3s; margin-left: 10px; }
.btn-cart:hover { box-shadow: 0 4px 15px var(--primary-glow); transform: translateY(-2px); }

.menu-content { max-width: 1200px; margin: 40px auto; padding: 0 20px; text-align: center;}
.page-title { font-family: 'Inter', sans-serif; font-size: 2.5rem; color: var(--primary); font-weight: 900; text-transform: uppercase; letter-spacing: 1px; }
.page-subtitle { color: var(--text-secondary); font-size: 1.1rem; margin-bottom: 30px; }
.category-filter { display: flex; gap: 10px; justify-content: center; margin-bottom: 30px; flex-wrap: wrap; }
.category-filter button { background: var(--bg-card); border: 1px solid var(--border); color: var(--text-secondary); padding: 8px 20px; border-radius: 20px; cursor: pointer; font-weight: bold; transition: 0.3s;}
.category-filter button:hover { border-color: var(--primary); color: var(--primary); }
.category-filter button.active { background: var(--primary); color: var(--bg-dark); border-color: var(--primary); }

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 30px; }
.product-card { background: var(--bg-card); padding: 20px; border-radius: 12px; box-shadow: var(--shadow-md); border: 1px solid var(--border-light); transition: 0.3s; }
.product-card:hover { transform: translateY(-5px); border-color: var(--primary); box-shadow: 0 8px 25px rgba(0,212,170,0.2); }
.product-card img { width: 120px; height: 120px; border-radius: 50%; object-fit: cover; margin-bottom: 15px; border: 2px solid var(--border); }
.product-card h3 { color: var(--text-heading); font-size: 1.1rem; margin-bottom: 10px; }
.price { color: var(--primary); font-weight: bold; margin-bottom: 15px; font-size: 1.1rem; }
.btn-add { background: var(--primary-glow); color: var(--primary); border: 1px solid var(--primary); width: 100%; padding: 10px; border-radius: 20px; cursor: pointer; font-weight: bold; transition: 0.3s; }
.btn-add:hover { background: var(--primary); color: var(--bg-dark); }

/* Modal Checkout Style */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.checkout-modal { background: var(--bg-card); width: 500px; max-height: 90vh; border-radius: 15px; display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--border); }
.modal-header-title { background: var(--bg-nav); color: var(--primary); margin: 0; padding: 15px; font-size: 1.3rem; text-align: center; font-family: 'Inter', sans-serif; border-bottom: 1px solid var(--border);}
.checkout-scroll-area { padding: 20px; overflow-y: auto; flex: 1; }
.form-group { margin-bottom: 12px; text-align: left; }
.form-group label { display: block; font-weight: bold; margin-bottom: 5px; font-size: 0.9rem; color: var(--text-heading); }
.form-control { width: 100%; padding: 10px; border: 1px solid var(--border); background: var(--bg-input); color: var(--text-primary); border-radius: 6px; box-sizing: border-box; }

.payment-banking-box { background: var(--bg-card2); border: 1px solid var(--primary); padding: 15px; border-radius: 10px; margin-top: 15px; }
.payment-title { margin: 0 0 10px 0; font-size: 1rem; color: var(--primary); border-bottom: 1px solid var(--border); padding-bottom: 5px; }
.payment-layout { display: flex; gap: 15px; align-items: center; }
.bank-card { background: var(--bg-root); padding: 10px; border-radius: 5px; font-size: 0.85rem; line-height: 1.4; border-left: 3px solid var(--primary); color: var(--text-secondary); }
.bank-card strong { color: var(--text-primary); }
.qr-code-image img { width: 140px; border-radius: 8px; border: 1px solid var(--border); }
.tx-input { border: 2px solid var(--primary); background: var(--bg-input); font-weight: bold; color: var(--primary); }

.modal-actions { padding: 15px; border-top: 1px solid var(--border); display: flex; justify-content: flex-end; gap: 10px; background: var(--bg-nav); }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-muted); border-radius: 5px; cursor: pointer; transition: 0.3s; }
.btn-cancel:hover { background: rgba(255,255,255,0.1); }
.btn-confirm { padding: 10px 20px; border: none; background: var(--primary); color: var(--bg-dark); border-radius: 5px; font-weight: bold; cursor: pointer; transition: 0.3s; }
.btn-confirm:hover { box-shadow: 0 4px 15px var(--primary-glow); }
</style>