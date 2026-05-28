<template>
  <CustomerLayout>
    <div class="menu-wrapper">

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
          <div class="product-rating" v-if="product.averageRating > 0">
            ⭐ {{ product.averageRating }}
          </div>
          <div class="product-rating" v-else>
            <span style="color: #666; font-size: 0.8rem">Chưa có đánh giá</span>
          </div>
          <p class="price">{{ product.price.toLocaleString() }} VNĐ</p>
          <button v-if="!isAdminOrManager" class="btn-add" @click="addToCart(product)">+ Thêm vào giỏ</button>
          <button v-else class="btn-add btn-disabled" disabled>Chỉ xem (Admin)</button>
        </div>
      </div>
      
      <!-- Floating Cart Button -->
      <div v-if="cart.length > 0 && !isAdminOrManager" class="floating-cart" @click="showCheckoutModal = true">
        <span class="cart-icon">🛒</span>
        <span class="cart-count">{{ cart.length }} món</span>
        <span class="cart-total">{{ cartTotal.toLocaleString() }}đ</span>
        <span class="cart-checkout">Thanh toán →</span>
      </div>
    </main>

    <div v-if="showCheckoutModal" class="g-modal-overlay" @click.self="showCheckoutModal = false">
      <div class="g-modal-box" style="max-width: 600px;">
        <h3>Thông Tin Giao Hàng & Thanh Toán</h3>
        
        <div class="checkout-scroll-area" style="max-height: 400px; overflow-y: auto; padding-right: 10px;">
          <div class="form-group mt-3">
            <label>Họ tên người nhận (*):</label>
            <input v-model="orderInfo.fullname" type="text" placeholder="Nguyễn Văn A..." class="g-form-control" />
          </div>
          <div class="form-group mt-3">
            <label>Số điện thoại (*):</label>
            <input v-model="orderInfo.phone" type="text" placeholder="0905..." class="g-form-control" />
          </div>
          <div class="form-group mt-3">
            <label>Địa chỉ nhận hàng (*):</label>
            <input v-model="orderInfo.address" type="text" placeholder="Số nhà, tên đường..." class="g-form-control" />
          </div>

          <div class="payment-banking-box mt-4" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.1); padding: 15px; border-radius: 12px;">
            <h4 style="color: var(--primary); margin: 0 0 10px 0;">💳 Chuyển khoản qua VietQR</h4>
            <div style="display: flex; gap: 15px; align-items: center;">
              <div style="flex: 1;">
                <p>Tổng tiền: <strong style="color: #ff4757; font-size: 1.2rem;">{{ cartTotal.toLocaleString() }}đ</strong></p>
                <div style="font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin-top: 10px;">
                  <p>Ngân hàng: <strong>Vietcombank</strong></p>
                  <p>STK: <strong>1047187126</strong></p>
                  <p>Tên TK: <strong>NGUYEN QUANG NHAT</strong></p>
                  <p>Nội dung: <strong>{{ orderInfo.phone || 'SDT' }} SHIP</strong></p>
                </div>
              </div>
              <div>
                <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.jpg?amount=${cartTotal}&addInfo=${orderInfo.phone || 'SHIP'}&accountName=NGUYEN QUANG NHAT`" alt="QR" style="width: 140px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.5);" />
              </div>
            </div>
            
            <div class="form-group mt-3">
              <label style="color: #ff4757;">Dán Mã giao dịch sau khi CK (*):</label>
              <input v-model="orderInfo.txCode" type="text" placeholder="Ví dụ: FT24..." class="g-form-control" style="border-color: #ff4757;" />
            </div>
          </div>
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="showCheckoutModal = false" class="g-btn-outline" style="flex: 1;">Đóng</button>
          <button v-if="cart.length > 0" @click="submitShipOrder" class="g-btn-primary" style="flex: 1;">Xác Nhận & Đặt Hàng</button>
        </div>
      </div>
    </div>
  </div>
  </CustomerLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import CustomerLayout from '@/components/CustomerLayout.vue';

const products = ref([]);
const categories = ref([]);
const cart = ref([]);
const router = useRouter();
const isLoggedIn = ref(false);
const userRoles = ref([]);

const isAdminOrManager = computed(() => {
  return userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_MANAGER');
});

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
</script>

<style scoped>
.menu-wrapper { background-color: var(--bg-root); min-height: 100vh; color: var(--text-primary); }

.navbar {
  background: rgba(13, 27, 42, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky; top: 0; z-index: 100;
  padding: 12px 40px;
}
.nav-container { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo h2 span { color: var(--primary); }
.logo p { margin: 0; font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(0,212,170,0.1); }

.nav-right { display: flex; gap: 10px; }
.btn-nav-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-outline:hover { border-color: var(--primary); color: var(--primary); background: rgba(0,212,170,0.1); }
.btn-cart {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #040914; border: none; padding: 10px 24px; border-radius: 100px;
  font-weight: 800; cursor: pointer; transition: var(--transition);
}
.btn-cart:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }

.menu-content { max-width: 1400px; margin: 60px auto; padding: 0 20px; text-align: center;}
.page-title { font-size: 3rem; color: var(--text-heading); font-weight: 900; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 10px; }
.page-subtitle { color: var(--primary); font-size: 1.2rem; margin-bottom: 40px; font-weight: 600; }

.category-filter { display: flex; gap: 12px; justify-content: center; margin-bottom: 50px; flex-wrap: wrap; }
.category-filter button {
  background: rgba(13,27,42,0.6); border: 1px solid rgba(255,255,255,0.05);
  color: var(--text-secondary); padding: 10px 24px; border-radius: 100px;
  cursor: pointer; font-weight: 600; transition: var(--transition);
}
.category-filter button:hover { border-color: var(--primary); color: var(--primary); }
.category-filter button.active {
  background: var(--primary); color: #040914; border-color: var(--primary); font-weight: 800;
  box-shadow: 0 0 20px rgba(0,212,170,0.3);
}

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 30px; }
.product-card {
  background: rgba(13,27,42,0.5); padding: 30px 20px; border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.05); transition: var(--transition);
  position: relative; overflow: hidden;
}
.product-card::before {
  content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: radial-gradient(circle, var(--primary-glow2) 0%, transparent 60%);
  opacity: 0; transition: var(--transition); z-index: 0;
}
.product-card:hover { transform: translateY(-8px); border-color: var(--border-focus); box-shadow: var(--shadow-lg); }
.product-card:hover::before { opacity: 1; }

.product-card > * { position: relative; z-index: 1; }
.product-card img { width: 150px; height: 150px; border-radius: 50%; object-fit: cover; margin-bottom: 20px; border: 4px solid rgba(13,27,42,0.8); box-shadow: var(--shadow-md); transition: var(--transition); }
.product-card:hover img { transform: scale(1.05) rotate(5deg); border-color: var(--primary); }
.product-card h3 { color: var(--text-heading); font-size: 1.2rem; margin-bottom: 8px; font-weight: 800; }
.product-rating { margin-bottom: 12px; color: #f1c40f; font-weight: 700; font-size: 0.95rem; }
.price { color: var(--primary); font-weight: 900; margin-bottom: 20px; font-size: 1.3rem; }
.btn-add {
  background: rgba(0,212,170,0.1); color: var(--primary); border: 1px solid var(--primary);
  width: 100%; padding: 12px; border-radius: 100px; cursor: pointer;
  font-weight: 800; transition: var(--transition);
}
.btn-add:hover { background: var(--primary); color: #040914; box-shadow: 0 5px 15px rgba(0,212,170,0.4); }
.btn-disabled { opacity: 0.5; cursor: not-allowed !important; }
.btn-disabled:hover { background: rgba(0,212,170,0.1); color: var(--primary); box-shadow: none; }

/* Floating Cart */
.floating-cart {
  position: fixed; bottom: 30px; right: 30px; z-index: 99;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  padding: 15px 25px; border-radius: 50px;
  display: flex; align-items: center; gap: 15px;
  cursor: pointer; box-shadow: 0 10px 30px rgba(0,212,170,0.4);
  color: #040914; font-weight: 800; transition: var(--transition);
}
.floating-cart:hover { transform: translateY(-5px); box-shadow: 0 15px 40px rgba(0,212,170,0.6); }
.cart-icon { font-size: 1.5rem; }
.cart-count { background: #040914; color: var(--primary); padding: 4px 10px; border-radius: 20px; font-size: 0.85rem; }
.cart-total { font-size: 1.1rem; }
.cart-checkout { margin-left: 10px; background: rgba(4,9,20,0.1); padding: 5px 15px; border-radius: 20px; }
</style>