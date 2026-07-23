<template>
  <CustomerLayout>
    <div class="menu-wrapper">

    <main class="menu-content">
      <h1 class="page-title">Thực Đơn Giao Hàng</h1>
      <p class="page-subtitle">Chọn món ngon - Giao nóng hổi tận nhà</p>

      <!-- Món ăn gợi ý -->
      <div v-if="!isLoading && !loadError && suggestedProducts.length > 0" class="suggested-section">
        <h2 class="section-title"><span style="color: #B98229">🌟</span> Gợi Ý Cho Bạn</h2>
        <div class="suggested-grid">
          <div v-for="product in suggestedProducts" :key="'sugg-'+product.id" class="suggested-card">
            <div class="sugg-badge">HOT</div>
            <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
            <div class="sugg-info">
              <h3>{{ productName(product) }}</h3>
              <p class="price">{{ product.price.toLocaleString() }}đ</p>
            </div>
            <button v-if="!isAdminOrManager" class="btn-sugg-add" @click="addToCart(product)">Thêm Ngay</button>
          </div>
        </div>
      </div>

      <div v-if="!isLoading && !loadError" class="category-filter">
        <button :class="{'active': selectedCategory === null}" @click="selectedCategory = null">Tất cả món</button>
        <button v-for="c in categories" :key="c.id" :class="{'active': selectedCategory === c.id}" @click="selectedCategory = c.id">
          {{ categoryName(c) }}
        </button>
      </div>

      <div v-if="isLoading" class="menu-loading-grid" aria-label="Đang tải thực đơn">
        <SkeletonLoader v-for="item in 6" :key="item" variant="card" />
      </div>

      <div v-else-if="loadError" class="menu-state menu-error" role="alert">
        <strong>Không thể tải thực đơn.</strong>
        <span>{{ loadError }}</span>
        <button class="g-btn-outline" type="button" @click="loadMenu">Thử lại</button>
      </div>

      <div v-else-if="filteredProducts.length === 0" class="menu-state">
        <strong>Chưa có món ăn phù hợp.</strong>
        <span>Hãy chọn danh mục khác để tiếp tục.</span>
      </div>

      <div v-else class="product-grid">
        <div v-for="product in filteredProducts" :key="product.id" class="product-card">
          <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
          <h3>{{ productName(product) }}</h3>
          <div class="product-rating" v-if="product.averageRating > 0">
            ⭐ {{ product.averageRating }}
          </div>
          <div class="product-rating" v-else>
            <span style="color: #55503E; font-size: 0.8rem">Chưa có đánh giá</span>
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
        
        <div v-if="!paymentQr" class="checkout-scroll-area" style="max-height: 400px; overflow-y: auto; padding-right: 10px;">
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
            <h4 style="color: var(--primary); margin: 0 0 10px 0;">Chuyển khoản qua VietQR</h4>
            <p style="margin-bottom: 5px;">Tạm tính: <strong>{{ cartSubtotal.toLocaleString() }}đ</strong></p>
            <p style="margin-bottom: 5px;">Thuế GTGT dự kiến: <strong>{{ cartTax.toLocaleString() }}đ</strong></p>
            <p>Tổng dự kiến: <strong style="color: #B23B2E; font-size: 1.2rem;">{{ cartTotal.toLocaleString() }}đ</strong></p>
            <p style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 10px;">
              Mã QR chính xác sẽ được tạo sau khi hệ thống ghi nhận đơn và tính lại giá.
            </p>
          </div>
        </div>

        <div v-else class="payment-banking-box payment-result">
          <h4>Quét QR để thanh toán</h4>
          <img :src="paymentQr.qrUrl" alt="Mã QR thanh toán đơn giao hàng" />
          <p>Số tiền: <strong>{{ Number(paymentQr.amount).toLocaleString() }}đ</strong></p>
          <p>Ngân hàng: <strong>{{ paymentQr.bankCode }}</strong></p>
          <p>Số tài khoản: <strong>{{ paymentQr.accountNumber }}</strong></p>
          <p>Chủ tài khoản: <strong>{{ paymentQr.accountHolder }}</strong></p>
          <p>Nội dung: <strong>{{ paymentQr.transferContent }}</strong></p>
          <small>Đơn chỉ được chuyển xuống bếp sau khi ngân hàng xác nhận đủ tiền.</small>
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="closeCheckout" class="g-btn-outline" style="flex: 1;">{{ paymentQr ? 'Đã hiểu' : 'Đóng' }}</button>
          <button v-if="cart.length > 0 && !paymentQr" @click="submitShipOrder" :disabled="checkoutSubmitting" class="g-btn-primary" style="flex: 1;">
            {{ checkoutSubmitting ? 'Đang tạo đơn...' : 'Tạo đơn & lấy mã QR' }}
          </button>
        </div>
      </div>
    </div>
  </div>
  </CustomerLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '@/services/api';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import CustomerLayout from '@/components/CustomerLayout.vue';
import SkeletonLoader from '@/components/SkeletonLoader.vue';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';

const products = ref([]);
const suggestedProducts = ref([]);
const categories = ref([]);
const cart = ref([]);
const router = useRouter();
const { locale } = useI18n();
const isLoggedIn = ref(false);
const userRoles = ref([]);
const isLoading = ref(true);
const loadError = ref('');

const isAdminOrManager = computed(() => {
  return userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_MANAGER');
});

const showCheckoutModal = ref(false);
const selectedCategory = ref(null); 

const orderInfo = ref({ fullname: '', phone: '', address: '' });
const paymentQr = ref(null);
const checkoutSubmitting = ref(false);

const cartSubtotal = computed(() => {
  return cart.value.reduce((total, item) => total + (item.price * item.quantity), 0);
});

const cartTax = computed(() => {
  return cart.value.reduce((total, item) => total + ((item.price * item.quantity) * (item.taxRate || 8) / 100), 0);
});

const cartTotal = computed(() => {
  return cartSubtotal.value + cartTax.value;
});

const productName = (product) => locale.value === 'en'
  ? (product.nameEn || product.name)
  : (product.nameVi || product.name);

const categoryName = (category) => locale.value === 'en'
  ? (category.nameEn || category.name)
  : (category.nameVi || category.name);

const fetchProducts = async () => {
  const response = await api.get('/api/products');
  products.value = response.data;
};

const fetchCategories = async () => {
  const response = await api.get('/api/categories');
  categories.value = response.data;
};

const fetchSuggested = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await api.get('/api/admin/popular-items/products?limit=4', {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    // Lọc ra các món có isPopular = true hoặc lấy thẳng top 4
    if (response.data && response.data.length > 0) {
       // get full product info from id
       suggestedProducts.value = response.data
         .map(item => products.value.find(p => p.id === item.productId))
         .filter(p => p != null).slice(0, 4);
    }
  } catch (error) { console.error('Lỗi lấy gợi ý:', error); }
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
    cart.value.push({ productId: product.id, quantity: 1, name: productName(product), price: product.price, taxRate: product.taxRate || 8 });
  }
  alert(`Đã thêm ${productName(product)} vào giỏ!`);
};

const submitShipOrder = async () => {
  const token = localStorage.getItem('token');
  
  if(!orderInfo.value.fullname || !orderInfo.value.phone || !orderInfo.value.address) {
    alert("Vui lòng điền đầy đủ thông tin nhận hàng!");
    return;
  }

  // Cấu trúc infoFull để Admin dùng In hóa đơn sau này
  const infoFull = `[GIAO HÀNG] Khách: ${orderInfo.value.fullname} | SĐT: ${orderInfo.value.phone} | ĐC: ${orderInfo.value.address}`;
  
  const formattedItems = cart.value.map(item => ({ 
    productId: item.productId, 
    quantity: item.quantity 
  }));

  try {
    checkoutSubmitting.value = true;
    const response = await api.post('/api/orders/checkout', {
      address: infoFull,
      paymentOption: 'PREPAID_TRANSFER',
      items: formattedItems
    }, { headers: token ? { 'Authorization': `Bearer ${token}` } : {} });

    paymentQr.value = response.data.payment;
    cart.value = [];
  } catch (error) {
    alert("Lỗi: " + (error.response?.data?.message || "Vui lòng thử lại"));
  } finally {
    checkoutSubmitting.value = false;
  }
};

const closeCheckout = () => {
  showCheckoutModal.value = false;
  if (paymentQr.value) {
    paymentQr.value = null;
    router.push('/history');
  }
};

const loadMenu = async () => {
  isLoading.value = true;
  loadError.value = '';

  const [productsResult, categoriesResult] = await Promise.allSettled([
    fetchProducts(),
    fetchCategories()
  ]);

  if (productsResult.status === 'rejected') {
    loadError.value = productsResult.reason?.response?.data?.message || 'Vui lòng kiểm tra kết nối rồi thử lại.';
  } else {
    await fetchSuggested();
  }

  if (categoriesResult.status === 'rejected') {
    console.error('Lỗi lấy danh mục:', categoriesResult.reason);
  }

  isLoading.value = false;
};

onMounted(async () => {
  await loadMenu();
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
  background: rgba(255, 255, 255, 0.72);
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
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: rgba(90, 110, 69, 0.1); }

.nav-right { display: flex; gap: 10px; }
.btn-nav-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-outline:hover { border-color: var(--primary); color: var(--primary); background: rgba(90, 110, 69, 0.1); }
.btn-cart {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #1A170F; border: none; padding: 10px 24px; border-radius: 100px;
  font-weight: 800; cursor: pointer; transition: var(--transition);
}
.btn-cart:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(90, 110, 69, 0.4); }

.menu-content { max-width: 1400px; margin: 60px auto; padding: 0 20px; text-align: center;}
.page-title { font-size: 3rem; color: var(--text-heading); font-weight: 900; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 10px; }
.page-subtitle { color: var(--primary); font-size: 1.2rem; margin-bottom: 30px; font-weight: 600; }

.suggested-section { margin-bottom: 50px; text-align: left; background: rgba(185, 130, 41, 0.05); padding: 25px; border-radius: 20px; border: 1px solid rgba(185, 130, 41, 0.2); }
.suggested-section .section-title { font-size: 1.8rem; margin-bottom: 20px; color: #B98229; font-weight: 900; }
.suggested-grid { display: flex; gap: 20px; overflow-x: auto; padding-bottom: 15px; }
.suggested-grid::-webkit-scrollbar { height: 8px; }
.suggested-grid::-webkit-scrollbar-thumb { background: #B98229; border-radius: 10px; }
.suggested-card { min-width: 250px; background: rgba(0,0,0,0.5); border-radius: 15px; padding: 15px; display: flex; flex-direction: column; position: relative; border: 1px solid rgba(185, 130, 41, 0.3); transition: 0.3s; }
.suggested-card:hover { transform: translateY(-5px); box-shadow: 0 5px 15px rgba(185, 130, 41, 0.2); }
.sugg-badge { position: absolute; top: -10px; right: -10px; background: #B23B2E; color: #FFFFFF; padding: 5px 10px; border-radius: 10px; font-weight: 900; font-size: 0.8rem; transform: rotate(10deg); box-shadow: 0 2px 10px rgba(178,59,46,0.5); }
.suggested-card img { width: 100%; height: 140px; object-fit: cover; border-radius: 10px; margin-bottom: 15px; }
.sugg-info { flex: 1; }
.sugg-info h3 { margin: 0 0 5px 0; font-size: 1.1rem; color: #FFFFFF; }
.sugg-info .price { color: #B98229; font-weight: bold; font-size: 1.2rem; margin: 0; }
.btn-sugg-add { background: #B98229; color: #201D14; border: none; padding: 10px; border-radius: 8px; font-weight: bold; margin-top: 15px; cursor: pointer; transition: 0.3s; }
.btn-sugg-add:hover { background: #FFFFFF; }

.category-filter { display: flex; gap: 12px; justify-content: center; margin-bottom: 50px; flex-wrap: wrap; }
.category-filter button {
  background: rgba(255, 255, 255, 0.78); border: 1px solid rgba(255,255,255,0.05);
  color: var(--text-secondary); padding: 10px 24px; border-radius: 100px;
  cursor: pointer; font-weight: 600; transition: var(--transition);
}
.category-filter button:hover { border-color: var(--primary); color: var(--primary); }
.category-filter button.active {
  background: var(--primary); color: #1A170F; border-color: var(--primary); font-weight: 800;
  box-shadow: 0 0 20px rgba(90, 110, 69, 0.3);
}

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 30px; }
.menu-loading-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 30px;
  text-align: left;
}
.menu-state {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  padding: 28px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-muted);
}
.menu-state strong { color: var(--text-heading); font-size: 1.15rem; }
.menu-error { border-left: 4px solid var(--danger); }
.menu-error .g-btn-outline { margin-top: 4px; }
.product-card {
  background: rgba(255, 255, 255, 0.70); padding: 30px 20px; border-radius: 20px;
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
.product-card img { width: 150px; height: 150px; border-radius: 50%; object-fit: cover; margin-bottom: 20px; border: 4px solid rgba(255, 255, 255, 0.88); box-shadow: var(--shadow-md); transition: var(--transition); }
.product-card:hover img { transform: scale(1.05) rotate(5deg); border-color: var(--primary); }
.product-card h3 { color: var(--text-heading); font-size: 1.2rem; margin-bottom: 8px; font-weight: 800; }
.product-rating { margin-bottom: 12px; color: #B98229; font-weight: 700; font-size: 0.95rem; }
.price { color: var(--primary); font-weight: 900; margin-bottom: 20px; font-size: 1.3rem; }
.btn-add {
  background: rgba(90, 110, 69, 0.1); color: var(--primary); border: 1px solid var(--primary);
  width: 100%; padding: 12px; border-radius: 100px; cursor: pointer;
  font-weight: 800; transition: var(--transition);
}
.btn-add:hover { background: var(--primary); color: #1A170F; box-shadow: 0 5px 15px rgba(90, 110, 69, 0.4); }
.btn-disabled { opacity: 0.5; cursor: not-allowed !important; }
.btn-disabled:hover { background: rgba(90, 110, 69, 0.1); color: var(--primary); box-shadow: none; }

/* Floating Cart */
.floating-cart {
  position: fixed; bottom: 30px; right: 30px; z-index: 99;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  padding: 15px 25px; border-radius: 50px;
  display: flex; align-items: center; gap: 15px;
  cursor: pointer; box-shadow: 0 10px 30px rgba(90, 110, 69, 0.4);
  color: #1A170F; font-weight: 800; transition: var(--transition);
}
.floating-cart:hover { transform: translateY(-5px); box-shadow: 0 15px 40px rgba(90, 110, 69, 0.6); }
.cart-icon { font-size: 1.5rem; }
.cart-count { background: #1A170F; color: var(--primary); padding: 4px 10px; border-radius: 20px; font-size: 0.85rem; }
.cart-total { font-size: 1.1rem; }
.cart-checkout { margin-left: 10px; background: rgba(26, 23, 15, 0.1); padding: 5px 15px; border-radius: 20px; }

.payment-result {
  padding: 18px;
  text-align: center;
  border: 1px solid var(--border);
  background: var(--bg-input);
}
.payment-result h4 { margin: 0 0 12px; color: var(--primary); }
.payment-result img { display: block; width: min(220px, 100%); margin: 0 auto 14px; }
.payment-result p { margin: 6px 0; overflow-wrap: anywhere; }
.payment-result small { display: block; margin-top: 12px; color: var(--text-secondary); }

@media (max-width: 1024px) {
  .menu-content { margin: 44px auto; }
  .page-title { font-size: 2.4rem; }
  .product-grid,
  .menu-loading-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }
  .floating-cart { right: 20px; bottom: 20px; }
}

@media (max-width: 640px) {
  .menu-wrapper,
  .menu-wrapper * { box-sizing: border-box; }
  .menu-wrapper { overflow-x: hidden; }
  .menu-content { margin: 32px auto 96px; padding: 0 16px; }
  .page-title { font-size: 1.85rem; line-height: 1.2; letter-spacing: 0; }
  .page-subtitle { font-size: 1rem; }
  .suggested-section { margin-bottom: 32px; padding: 18px 14px; border-radius: 14px; }
  .suggested-section .section-title { font-size: 1.4rem; }
  .suggested-card { min-width: min(250px, 78vw); }
  .category-filter { justify-content: flex-start; gap: 8px; margin-bottom: 30px; }
  .category-filter button,
  .btn-sugg-add,
  .btn-add,
  .modal-actions button { min-height: 44px; }
  .category-filter button { flex: 1 1 calc(50% - 8px); padding: 10px 12px; }
  .product-grid,
  .menu-loading-grid { grid-template-columns: 1fr; gap: 18px; }
  .menu-state { min-height: 180px; padding: 22px 16px; }
  .menu-error .g-btn-outline { min-height: 44px; width: 100%; }
  .product-card { padding: 24px 16px; }
  .product-card img { width: 140px; height: 140px; object-fit: cover; }
  .floating-cart {
    right: 12px;
    bottom: 12px;
    left: 12px;
    justify-content: space-between;
    min-height: 56px;
    padding: 10px 14px;
    border-radius: 16px;
  }
  .cart-total { font-size: 0.95rem; }
  .cart-checkout { margin-left: 0; padding: 5px 8px; }
  .payment-banking-box > div { flex-direction: column; align-items: stretch !important; }
  .payment-banking-box img { display: block; width: min(180px, 100%) !important; margin: 0 auto; }
  .modal-actions { flex-direction: column; }
  .g-modal-box { width: calc(100vw - 24px); max-height: calc(100vh - 24px); overflow-y: auto; }
}
</style>
