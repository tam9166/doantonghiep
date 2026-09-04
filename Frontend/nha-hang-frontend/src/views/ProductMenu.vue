<template>
  <CustomerLayout>
    <div class="menu-wrapper">

    <main class="menu-content">
      <h1 class="page-title">{{ text.title }}</h1>
      <p class="page-subtitle">{{ text.subtitle }}</p>
      <div v-if="!isLoading && !loadError" class="menu-search">
        <label class="sr-only" for="menu-search-input">{{ text.searchLabel }}</label>
        <span aria-hidden="true">⌕</span>
        <input id="menu-search-input" v-model.trim="menuQuery" type="search" :placeholder="text.searchPlaceholder" @keydown.esc="menuQuery = ''" />
        <button v-if="menuQuery" type="button" :aria-label="text.clearSearch" @click="menuQuery = ''">×</button>
      </div>

      <!-- Món ăn gợi ý -->
      <div v-if="!isLoading && !loadError && suggestedProducts.length > 0" class="suggested-section">
      <h2 class="section-title"><span style="color: var(--color-tertiary)"><UiIcon name="sparkles" /></span> {{ text.suggestions }}</h2>
        <div class="suggested-grid">
          <div v-for="product in suggestedProducts" :key="'sugg-'+product.id" class="suggested-card">
            <div class="sugg-badge">HOT</div>
            <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
            <div class="sugg-info">
              <h3>{{ productName(product) }}</h3>
              <p class="price">{{ formatCurrency(product.price) }}</p>
            </div>
            <button v-if="!isAdminOrManager" class="btn-sugg-add" :disabled="product.availableQuantity <= 0" @click="addToCart(product)">
            {{ product.availableQuantity > 0 ? text.addNow : text.soldOut }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="!isLoading && !loadError" class="category-filter">
        <button :class="{'active': selectedCategory === null}" @click="selectedCategory = null">{{ text.allItems }}</button>
        <button v-for="c in categories" :key="c.id" :class="{'active': selectedCategory === c.id}" @click="selectedCategory = c.id">
          {{ categoryName(c) }}
        </button>
      </div>

      <div v-if="isLoading" class="menu-loading-grid" :aria-label="text.loading">
        <SkeletonLoader v-for="item in 6" :key="item" variant="card" />
      </div>

      <div v-else-if="loadError" class="menu-state menu-error" role="alert">
        <strong>{{ text.loadFailed }}</strong>
        <span>{{ loadError }}</span>
        <button class="g-btn-outline" type="button" @click="loadMenu">{{ text.retry }}</button>
      </div>

      <div v-else-if="filteredProducts.length === 0" class="menu-state">
        <strong>{{ text.noItems }}</strong>
        <span>{{ text.noItemsHint }}</span>
      </div>

      <div v-else ref="menuSectionRef" class="product-grid">
        <div v-for="product in paginatedProducts" :key="product.id" class="product-card">
          <img :src="foodImage(product.image)" :alt="productName(product)" loading="lazy" @error="replaceFoodImage" />
          <h3>{{ productName(product) }}</h3>
            <div class="menu-tags" :aria-label="text.tagsLabel">
              <span v-if="product.isSignatureDish">{{ text.signature }}</span>
              <span v-if="product.dietType === 'CHAY'">{{ text.vegetarian }}</span>
              <span v-if="product.cookingMethod === 'NUONG'">{{ text.grilled }}</span>
            <span v-if="product.spicyLevel > 0"> {{ product.spicyLevel }}</span>
          </div>
          <div class="product-rating" v-if="product.averageRating > 0">
             {{ product.averageRating }}
          </div>
          <div class="product-rating" v-else>
            <span style="color: var(--text-secondary); font-size: 0.8rem">{{ text.noRatings }}</span>
          </div>
          <p class="price">{{ formatCurrency(product.price) }}</p>
          <small class="remaining-servings" :class="{ low: product.availableQuantity > 0 && product.availableQuantity <= 5, soldout: product.availableQuantity <= 0 }">
              {{ product.availableQuantity > 0 ? t('menu.remaining', { count: product.availableQuantity }) : text.soldOut }}
          </small>
          <button v-if="!isAdminOrManager" class="btn-add" :disabled="product.availableQuantity <= 0" @click="addToCart(product)">
              {{ product.availableQuantity > 0 ? `+ ${text.addToCart}` : text.soldOut }}
          </button>
          <button v-else class="btn-add btn-disabled" disabled>{{ text.viewOnly }}</button>
        </div>
      </div>

      <nav v-if="filteredProducts.length > 0 && totalPages > 1" class="menu-pagination" :aria-label="text.menuPagination">
        <button type="button" :disabled="currentPage === 1" :aria-label="text.previousPage" @click="goToPage(currentPage - 1)">‹</button>
        <button v-for="page in totalPages" :key="page" type="button" :class="{ active: currentPage === page }"
          :aria-current="currentPage === page ? 'page' : undefined" @click="goToPage(page)">{{ page }}</button>
        <button type="button" :disabled="currentPage === totalPages" :aria-label="text.nextPage" @click="goToPage(currentPage + 1)">›</button>
      </nav>

      <!-- Floating Cart Button -->
      <div v-if="cart.length > 0 && !isAdminOrManager" class="floating-cart" @click="openCheckout">
          <span class="cart-icon"><UiIcon name="dish" /></span>
        <span class="cart-count">{{ t('menu.itemCount', { count: cart.length }) }}</span>
        <span class="cart-total">{{ formatCurrency(cartTotal) }}</span>
        <span class="cart-checkout">{{ text.checkout }} →</span>
      </div>
    </main>

    <div v-if="showCheckoutModal" class="g-modal-overlay" @click.self="showCheckoutModal = false">
      <div class="g-modal-box checkout-modal">
        <h3 class="checkout-header">{{ text.checkoutTitle }}</h3>

        <div v-if="!checkoutResult" class="checkout-scroll-area">
          <section v-if="cart.length > 0" class="cart-recommendations" aria-live="polite">
            <div class="recommendation-heading">
              <h4> {{ text.cartRecommendations }}</h4>
              <button type="button" class="recommendation-retry" :disabled="recommendationLoading" @click="loadCartRecommendations">
                ↻
              </button>
            </div>
            <div class="recommendation-controls">
              <label>
                  <span>{{ text.guests }}</span>
                <input v-model.number="recommendationProfile.guestCount" type="number" min="1" max="100" />
              </label>
              <label>
                  <span>{{ text.maximumBudget }}</span>
                  <input v-model.number="recommendationProfile.maxBudget" type="number" min="0" max="100000000" step="50000" :placeholder="text.budgetPlaceholder" />
              </label>
              <div class="preference-field">
                  <span>{{ text.preferences }}</span>
                <div class="preference-chips">
                  <button
                    v-for="option in recommendationPreferences"
                    :key="option.value"
                    type="button"
                    :class="{ active: recommendationProfile.preferences.includes(option.value) }"
                    :aria-pressed="recommendationProfile.preferences.includes(option.value)"
                    @click="toggleRecommendationPreference(option.value)"
                  >{{ option.label }}</button>
                </div>
              </div>
            </div>
            <p v-if="recommendationLoading" class="recommendation-copy">{{ text.recommendationLoading }}</p>
            <template v-else-if="cartRecommendations.length">
              <p class="recommendation-copy">{{ recommendationMessage || text.recommendationFallback }}</p>
              <div class="recommendation-list">
                <article v-for="item in cartRecommendations" :key="item.productId" class="recommendation-item">
                  <img :src="foodImage(item.image)" :alt="item.name" @error="replaceFoodImage" />
                  <div>
                    <strong>{{ item.name }}</strong>
                    <small>{{ text.recommendationReason?.[item.reasonCode] || item.reasonCode }}</small>
                    <span>{{ formatCurrency(item.price) }}</span>
                  </div>
                  <button type="button" @click="addRecommendedItem(item)">{{ text.recommendationAdd }}</button>
                </article>
              </div>
            </template>
            <p v-else-if="recommendationError" class="recommendation-error">
              {{ recommendationError }}
              <button type="button" @click="loadCartRecommendations">{{ text.recommendationRetry }}</button>
            </p>
          </section>

          <div class="form-group mt-3">
            <label>{{ text.recipientName }}</label>
            <input v-model="orderInfo.fullname" type="text" :placeholder="text.recipientNamePlaceholder" class="g-form-control" />
          </div>
          <div class="form-group mt-3">
            <label>{{ text.phone }}</label>
            <input v-model="orderInfo.phone" type="text" placeholder="0905..." class="g-form-control" />
          </div>
          <div class="form-group mt-3">
            <label>{{ text.address }}</label>
            <input v-model="orderInfo.address" type="text" :placeholder="text.addressPlaceholder" class="g-form-control" />
          </div>
          <div class="form-group mt-3">
            <label>{{ text.deliveryNote }}</label>
            <textarea v-model="orderInfo.note" :placeholder="text.deliveryNotePlaceholder" class="g-form-control" maxlength="500"></textarea>
          </div>

          <div class="payment-choice-box mt-4">
            <h4>{{ text.deliveryPayment }}</h4>
            <p class="payment-choice-note">{{ text.deliveryPaymentHint }}</p>
            <div class="payment-choice-grid">
              <button
                v-for="option in deliveryPaymentOptions"
                :key="option.key"
                type="button"
                class="payment-choice-btn"
                :class="{ selected: selectedDeliveryPaymentOption === option.key }"
                :aria-pressed="selectedDeliveryPaymentOption === option.key"
                @click="selectedDeliveryPaymentOption = option.key"
              >
                <strong>{{ option.label }}</strong>
                <small>{{ option.hint }}</small>
              </button>
            </div>
          </div>

          <div v-if="selectedDeliveryPaymentOption === 'PREPAID_TRANSFER'" class="payment-banking-box mt-4" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.1); padding: 15px; border-radius: 12px;">
            <h4 style="color: var(--primary); margin: 0 0 10px 0;">{{ text.bankTransfer }}</h4>
            <p style="margin-bottom: 5px;">{{ text.subtotal }}: <strong>{{ formatCurrency(cartSubtotal) }}</strong></p>
            <p style="margin-bottom: 5px;">{{ text.tax }}: <strong>{{ formatCurrency(cartTax) }}</strong></p>
            <p>{{ text.estimatedTotal }}: <strong style="color: var(--primary); font-size: 1.2rem;">{{ formatCurrency(cartTotal) }}</strong></p>
            <p style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 10px;">
              {{ text.qrHint }}
            </p>
          </div>

          <div v-else class="payment-banking-box mt-4 payment-cash-box" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.1); padding: 15px; border-radius: 12px;">
            <h4 style="color: var(--secondary); margin: 0 0 10px 0;">{{ paymentOptionText(selectedDeliveryPaymentOption) }}</h4>
            <p style="margin-bottom: 5px;">{{ text.subtotal }}: <strong>{{ formatCurrency(cartSubtotal) }}</strong></p>
            <p style="margin-bottom: 5px;">{{ text.tax }}: <strong>{{ formatCurrency(cartTax) }}</strong></p>
            <p>{{ text.estimatedTotal }}: <strong style="color: var(--secondary); font-size: 1.2rem;">{{ formatCurrency(cartTotal) }}</strong></p>
            <p style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 10px;">
              {{ text.cashOnDeliveryHint }}
            </p>
          </div>
        </div>

        <div v-else class="payment-banking-box payment-result checkout-scroll-area">
          <h4>{{ text.orderSuccessTitle }}</h4>
          <p class="checkout-success-lead">{{ text.orderCreatedSuccess }}</p>
          <div class="checkout-success-grid">
            <div>
              <span>{{ text.orderCode }}</span>
              <strong>{{ checkoutResult.orderCode }}</strong>
            </div>
            <div>
              <span>{{ text.orderStatus }}</span>
              <strong>{{ checkoutStatusText(checkoutResult.status) }}</strong>
            </div>
            <div>
              <span>{{ text.paymentMethod }}</span>
              <strong>{{ paymentOptionText(checkoutResult.paymentOption) }}</strong>
            </div>
            <div>
              <span>{{ text.paymentStatus }}</span>
              <strong>{{ paymentStatusText(checkoutResult.paymentStatus, checkoutResult.paymentOption) }}</strong>
            </div>
            <div>
              <span>{{ text.amount }}</span>
              <strong>{{ formatCurrency(Number(checkoutResult.totalAmount || 0)) }}</strong>
            </div>
          </div>
          <button type="button" class="checkout-copy-btn" @click="copyCheckoutOrderCode">
            {{ text.copyOrderCode }}
          </button>
          <template v-if="checkoutPayment">
            <h4 style="margin-top: 18px;">{{ text.scanQr }}</h4>
            <img :src="checkoutPayment.qrUrl" :alt="text.paymentQrAlt" />
            <p>{{ text.amount }}: <strong>{{ formatCurrency(Number(checkoutPayment.amount)) }}</strong></p>
            <p>{{ text.bank }}: <strong>{{ checkoutPayment.bankCode }}</strong></p>
            <p>{{ text.accountNumber }}: <strong>{{ checkoutPayment.accountNumber }}</strong></p>
            <p>{{ text.accountHolder }}: <strong>{{ checkoutPayment.accountHolder }}</strong></p>
            <p>{{ text.transferContent }}: <strong>{{ checkoutPayment.transferContent }}</strong></p>
            <small>{{ text.paymentConfirmedHint }}</small>
          </template>
          <template v-else>
            <p class="checkout-cash-note">{{ text.cashOnDeliveryHint }}</p>
          </template>
        </div>

        <div class="modal-actions checkout-actions mt-4">
          <button @click="closeCheckout" class="g-btn-outline" style="flex: 1;">{{ checkoutResult ? text.viewHistory : text.close }}</button>
          <button v-if="cart.length > 0 && !checkoutResult" @click="submitShipOrder" :disabled="checkoutSubmitting" class="g-btn-primary" style="flex: 1;">
            {{ checkoutSubmitting ? text.creatingOrder : text.createOrder }}
          </button>
        </div>
      </div>
    </div>
  </div>
  </CustomerLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import api from '@/services/api';
import { createDeliveryCheckoutRequest } from '@/services/orderCheckout';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import CustomerLayout from '@/components/CustomerLayout.vue';
import SkeletonLoader from '@/components/SkeletonLoader.vue';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';
import { useFormatters } from '@/composables/useFormatters';
import { MENU_PAGE_SIZE, paginateMenu } from '@/utils/menuPagination';
import { useToast } from '@/composables/useToast';
import { getApiErrorMessage } from '@/services/errorMessage';

const products = ref([]);
const suggestedProducts = ref([]);
const categories = ref([]);
const cart = ref([]);
const router = useRouter();
const { locale, tm, t } = useI18n();
const { formatCurrency } = useFormatters();
const text = computed(() => tm('menu'));
const isLoggedIn = ref(false);
const userRoles = ref([]);
const isLoading = ref(true);
const loadError = ref('');
const toast = useToast();

const isAdminOrManager = computed(() => {
  return userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_MANAGER');
});

const showCheckoutModal = ref(false);
const checkoutResult = ref(null);
const selectedDeliveryPaymentOption = ref('PREPAID_TRANSFER');
const selectedCategory = ref(null);
const menuQuery = ref('');
const currentPage = ref(1);
const menuSectionRef = ref(null);

const orderInfo = ref({ fullname: '', phone: '', address: '', note: '' });
const paymentQr = ref(null);
const checkoutSubmitting = ref(false);
const checkoutIdempotencyKey = ref(crypto.randomUUID());
const cartRecommendations = ref([]);
const recommendationMessage = ref('');
const recommendationLoading = ref(false);
const recommendationError = ref('');
const recommendationProfile = ref({ guestCount: 2, maxBudget: null, preferences: [] });
const recommendationPreferences = computed(() => [
  ['chay', 'vegetarian'], ['mặn', 'savory'], ['nướng', 'grilled'], ['hấp', 'steamed'],
  ['lẩu', 'hotpot'], ['hải sản', 'seafood'], ['ít cay', 'mild'], ['không cay', 'nonSpicy'],
  ['khai vị', 'appetizer'], ['món chính', 'main'], ['tráng miệng', 'dessert'], ['signature', 'signature'],
].map(([value, key]) => ({ value, label: text.value.preferenceLabels[key] })));
const deliveryPaymentOptions = computed(() => Object.entries(text.value.deliveryPaymentOptions || {}).map(([key, value]) => ({
  key,
  label: value[0],
  hint: value[1]
})));
const checkoutPayment = computed(() => checkoutResult.value?.payment || paymentQr.value || null);
let recommendationRequestId = 0;

const cartSubtotal = computed(() => {
  return cart.value.reduce((total, item) => total + (item.price * item.quantity), 0);
});

const cartTax = computed(() => {
  return cart.value.reduce((total, item) => total + ((item.price * item.quantity) * (item.taxRate || 8) / 100), 0);
});

const cartTotal = computed(() => {
  return cartSubtotal.value + cartTax.value;
});

const cartRecommendationKey = computed(() => cart.value
  .map(item => `${item.productId}:${item.quantity}`)
  .sort()
  .join('|'));
const recommendationProfileKey = computed(() => [
  recommendationProfile.value.guestCount,
  recommendationProfile.value.maxBudget,
  ...recommendationProfile.value.preferences.slice().sort()
].join('|'));

const productName = (product) => locale.value === 'en'
  ? (product.nameEn || product.nameVi || product.name)
  : (product.nameVi || product.name || product.nameEn);

const categoryName = (category) => locale.value === 'en'
  ? (category.nameEn || category.nameVi || category.name)
  : (category.nameVi || category.name || category.nameEn);

const fetchProducts = async () => {
  const response = await api.get('/api/products');
  if (!Array.isArray(response.data)) {
    throw new Error('Menu payload is not a list');
  }
  products.value = response.data;
};

const fetchCategories = async () => {
  const response = await api.get('/api/categories');
  if (!Array.isArray(response.data)) {
    throw new Error('Category payload is not a list');
  }
  categories.value = response.data;
};

const fetchSuggested = async () => {
  try {
    const response = await api.get('/api/menu/hot?limit=4');
    if (Array.isArray(response.data) && response.data.length > 0) {
       suggestedProducts.value = response.data
         .map(item => products.value.find(p => p.id === item.productId))
         .filter(p => p != null).slice(0, 4);
    }
  } catch (error) {
    suggestedProducts.value = [];
    console.error('Lỗi lấy gợi ý:', error);
  }
};

const filteredProducts = computed(() => {
  const activeProducts = products.value.filter(p => p.status !== false);
  const byCategory = selectedCategory.value === null
    ? activeProducts
    : activeProducts.filter(p => p.category && p.category.id === selectedCategory.value);
  const query = menuQuery.value.toLocaleLowerCase('vi-VN').trim();
  const matches = !query ? byCategory : byCategory.filter((product) => {
    const content = [
      productName(product),
      product.descriptionVi,
      product.descriptionEn,
      product.category && categoryName(product.category)
    ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN');
    return content.includes(query);
  });
  return [...matches].sort((a, b) => Number(isProductOrderable(b)) - Number(isProductOrderable(a))
    || Number(isAlcoholicProduct(a)) - Number(isAlcoholicProduct(b))
    || productName(a).localeCompare(productName(b), 'vi'));
});

const isProductOrderable = product => product?.available !== false && Number(product?.availableQuantity || 0) > 0;
const alcoholKeywords = ['bia', 'rượu', 'beer', 'lager', 'wine', 'vang', 'whisky', 'whiskey', 'vodka', 'gin', 'rum', 'soju', 'sake', 'cocktail', 'champagne', 'cognac', 'hennessy', 'chivas', 'johnnie', 'martell', 'remy'];
const isAlcoholicProduct = product => {
  const content = [
    productName(product),
    product.description,
    product.descriptionVi,
    product.descriptionEn,
    product.category && categoryName(product.category)
  ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN');
  return alcoholKeywords.some(keyword => content.includes(keyword));
};

const pagination = computed(() => paginateMenu(filteredProducts.value, currentPage.value, MENU_PAGE_SIZE));
const totalPages = computed(() => pagination.value.totalPages);
const paginatedProducts = computed(() => pagination.value.items);

watch([selectedCategory, menuQuery], () => {
  currentPage.value = 1;
});

const goToPage = (page) => {
  currentPage.value = Math.min(totalPages.value, Math.max(1, page));
  requestAnimationFrame(() => menuSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
};

const addToCart = (product) => {
  const existing = cart.value.find(item => item.productId === product.id);
  const availableQuantity = Math.max(0, Number(product.availableQuantity || 0));
  const requestedQuantity = (existing?.quantity || 0) + 1;
  if (requestedQuantity > availableQuantity) {
    toast.warning(t('menu.quantityLimit', { count: availableQuantity }));
    return;
  }
  if (existing) {
    existing.quantity++;
  } else {
    cart.value.push({ productId: product.id, quantity: 1, name: productName(product), price: product.price,
      taxRate: product.taxRate || 8, availableQuantity });
  }
  checkoutIdempotencyKey.value = crypto.randomUUID();
  toast.success(t('menu.addedToCart', { name: productName(product) }));
};

const addRecommendedItem = (item) => {
  const product = products.value.find(candidate => candidate.id === item.productId);
  if (product) addToCart(product);
};

const toggleRecommendationPreference = (value) => {
  const selected = recommendationProfile.value.preferences;
  recommendationProfile.value.preferences = selected.includes(value)
    ? selected.filter(item => item !== value)
    : [...selected, value];
};

const loadCartRecommendations = async () => {
  const requestId = ++recommendationRequestId;
  const productIds = cart.value.map(item => item.productId);
  if (!productIds.length) {
    cartRecommendations.value = [];
    recommendationMessage.value = '';
    recommendationError.value = '';
    return;
  }

  recommendationLoading.value = true;
  recommendationError.value = '';
  try {
    const response = await api.post('/api/customer/ai/menu-suggestion', {
      productIds,
      guestCount: recommendationProfile.value.guestCount || 1,
      maxBudget: recommendationProfile.value.maxBudget || null,
      preferences: recommendationProfile.value.preferences
    });
    if (requestId !== recommendationRequestId) return;
    cartRecommendations.value = response.data?.suggestions || [];
    recommendationMessage.value = response.data?.message || '';
  } catch (error) {
    if (requestId !== recommendationRequestId) return;
    cartRecommendations.value = [];
    recommendationMessage.value = '';
    recommendationError.value = error.response?.data?.message || text.value.connectionError;
  } finally {
    if (requestId === recommendationRequestId) recommendationLoading.value = false;
  }
};

const openCheckout = () => {
  showCheckoutModal.value = true;
  checkoutResult.value = null;
  paymentQr.value = null;
  selectedDeliveryPaymentOption.value = 'PREPAID_TRANSFER';
  loadCartRecommendations();
};

watch([cartRecommendationKey, recommendationProfileKey], () => {
  loadCartRecommendations();
});

const submitShipOrder = async () => {
  const token = sessionStorage.getItem('token');

  if(!orderInfo.value.fullname || !orderInfo.value.phone || !orderInfo.value.address) {
    toast.warning(t('menu.requiredDeliveryInfo'));
    return;
  }

  const formattedItems = cart.value.map(item => ({
    productId: item.productId,
    quantity: item.quantity
  }));

  try {
    checkoutSubmitting.value = true;
    const response = await api.post('/api/orders/checkout', createDeliveryCheckoutRequest({
      recipientName: orderInfo.value.fullname.trim(),
      recipientPhone: orderInfo.value.phone.trim(),
      deliveryAddress: orderInfo.value.address.trim(),
      deliveryNote: orderInfo.value.note,
      paymentOption: selectedDeliveryPaymentOption.value,
      items: formattedItems
    }), { headers: {
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      'X-Idempotency-Key': checkoutIdempotencyKey.value
    } });

    checkoutResult.value = response.data;
    paymentQr.value = response.data?.payment || null;
    cart.value = [];
    checkoutIdempotencyKey.value = crypto.randomUUID();
  } catch (error) {
    const payload = error.response?.data;
    const message = payload?.message || t('menu.checkoutError');
    const affectedItems = payload?.fieldErrors && Object.keys(payload.fieldErrors).length
      ? `\n${t('menu.affectedItems', { items: Object.entries(payload.fieldErrors).map(([name, reason]) => `${name}: ${reason}`).join('; ') })}`
      : '';
    toast.error(getApiErrorMessage(error, `${message}${affectedItems}`));
    if (error.response?.status === 409) await loadMenu();
  } finally {
    checkoutSubmitting.value = false;
  }
};

const closeCheckout = () => {
  showCheckoutModal.value = false;
  if (checkoutResult.value) {
    router.push('/history');
  }
  checkoutResult.value = null;
  paymentQr.value = null;
};

const copyCheckoutOrderCode = async () => {
  const code = checkoutResult.value?.orderCode;
  if (!code) return;
  try {
    if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable');
    await navigator.clipboard.writeText(code);
    toast.success(text.value.copiedOrderCode);
  } catch (_) {
    toast.info(code);
  }
};

const paymentOptionText = (option) => {
  if (!option) return '---';
  return text.value.deliveryPaymentOptions?.[option]?.[0] || option;
};

const paymentStatusText = (status, paymentOption) => {
  const en = locale.value === 'en';
  const map = en ? {
    UNPAID: paymentOption === 'COD' ? 'Cash on delivery - unpaid' : 'Awaiting confirmation',
    PENDING: 'Awaiting confirmation',
    PARTIALLY_PAID: 'Partially paid',
    PAID: 'Paid',
    OVERPAID: 'Overpaid',
  } : {
    UNPAID: paymentOption === 'COD' ? 'Chưa thu tiền' : 'Chờ xác nhận thanh toán',
    PENDING: 'Đang chờ xác nhận',
    PARTIALLY_PAID: 'Đã thanh toán một phần',
    PAID: 'Đã thanh toán',
    OVERPAID: 'Thanh toán dư',
  };
  return map[status] || status || '---';
};

const checkoutStatusText = (status) => {
  const en = locale.value === 'en';
  const map = en ? {
    0: 'Processing',
    1: 'In progress',
    2: 'Served',
    3: 'Cancelled',
    4: 'Completed',
    5: 'Scheduled',
    6: 'In progress',
    7: 'Served'
  } : {
    0: 'Chờ xử lý',
    1: 'Đang làm',
    2: 'Đã phục vụ',
    3: 'Đã hủy',
    4: 'Hoàn thành',
    5: 'Chờ hẹn giờ',
    6: 'Đang làm',
    7: 'Đã phục vụ'
  };
  return map[status] || 'Chờ xử lý';
};

const loadMenu = async () => {
  isLoading.value = true;
  loadError.value = '';

  const [productsResult, categoriesResult] = await Promise.allSettled([
    fetchProducts(),
    fetchCategories()
  ]);

  if (productsResult.status === 'rejected') {
    loadError.value = productsResult.reason?.response?.data?.message || t('menu.connectionError');
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
  const token = sessionStorage.getItem('token');
  if (token) isLoggedIn.value = true;

  const storedUser = sessionStorage.getItem('user');
  if (storedUser) {
    try {
      const parsed = JSON.parse(storedUser);
      if (parsed && parsed.roles) {
        userRoles.value = parsed.roles;
      }
    } catch (error) {
      console.warn('Không thể đọc vai trò khách hàng đã lưu.', error)
    }
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
.nav-links a:hover, .nav-links a.active { color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }

.nav-right { display: flex; gap: 10px; }
.btn-nav-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-outline:hover { border-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }
.btn-cart {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--text-primary); border: none; padding: 10px 24px; border-radius: 100px;
  font-weight: 800; cursor: pointer; transition: var(--transition);
}
.btn-cart:hover { transform: translateY(-2px); box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent); }

.menu-content { max-width: 1400px; margin: 60px auto; padding: 0 20px; text-align: center;}
.page-title { font-size: 3rem; color: var(--text-heading); font-weight: 900; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 10px; }
.page-subtitle { color: var(--primary); font-size: 1.2rem; margin-bottom: 30px; font-weight: 600; }

.suggested-section { margin-bottom: 50px; text-align: left; background: color-mix(in srgb, var(--color-tertiary) 5%, transparent); padding: 25px; border-radius: 20px; border: 1px solid color-mix(in srgb, var(--color-tertiary) 20%, transparent); }
.suggested-section .section-title { font-size: 1.8rem; margin-bottom: 20px; color: var(--color-tertiary); font-weight: 900; }
.suggested-grid { display: flex; gap: 20px; overflow-x: auto; padding-bottom: 15px; }
.suggested-grid::-webkit-scrollbar { height: 8px; }
.suggested-grid::-webkit-scrollbar-thumb { background: var(--color-tertiary); border-radius: 10px; }
.suggested-card { min-width: 250px; background: rgba(0,0,0,0.5); border-radius: 15px; padding: 15px; display: flex; flex-direction: column; position: relative; border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent); transition: 0.3s; }
.suggested-card:hover { transform: translateY(-5px); box-shadow: 0 5px 15px color-mix(in srgb, var(--color-tertiary) 20%, transparent); }
.sugg-badge { position: absolute; top: -10px; right: -10px; background: var(--primary); color: #FFFFFF; padding: 5px 10px; border-radius: 10px; font-weight: 900; font-size: 0.8rem; transform: rotate(10deg); box-shadow: 0 2px 10px color-mix(in srgb, var(--primary) 50%, transparent); }
.suggested-card img { width: 100%; height: 140px; object-fit: cover; border-radius: 10px; margin-bottom: 15px; }
.sugg-info { flex: 1; }
.sugg-info h3 { margin: 0 0 5px 0; font-size: 1.1rem; color: #FFFFFF; }
.sugg-info .price { color: var(--color-tertiary); font-weight: bold; font-size: 1.2rem; margin: 0; }
.btn-sugg-add { background: var(--color-tertiary); color: var(--text-primary); border: none; padding: 10px; border-radius: 8px; font-weight: bold; margin-top: 15px; cursor: pointer; transition: 0.3s; }
.btn-sugg-add:hover { background: #FFFFFF; }

.category-filter { display: flex; gap: 12px; justify-content: center; margin-bottom: 50px; flex-wrap: wrap; }
.category-filter button {
  background: rgba(255, 255, 255, 0.78); border: 1px solid rgba(255,255,255,0.05);
  color: var(--text-secondary); padding: 10px 24px; border-radius: 100px;
  cursor: pointer; font-weight: 600; transition: var(--transition);
}
.category-filter button:hover { border-color: var(--primary); color: var(--primary); }
.category-filter button.active {
  background: var(--primary); color: var(--color-on-primary); border-color: var(--primary); font-weight: 800;
  box-shadow: 0 0 20px color-mix(in srgb, var(--secondary) 30%, transparent);
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
.menu-tags { display: flex; flex-wrap: wrap; gap: 5px; margin: 0 14px 9px; }
.menu-tags span { padding: 3px 7px; border-radius: 999px; background: var(--color-surface-container); color: var(--text-secondary); font-size: 0.72rem; font-weight: 700; }

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
.product-rating { margin-bottom: 12px; color: var(--color-tertiary); font-weight: 700; font-size: 0.95rem; }
.price { color: var(--primary); font-weight: 900; margin-bottom: 20px; font-size: 1.3rem; }
.btn-add {
  background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--primary); border: 1px solid var(--primary);
  width: 100%; padding: 12px; border-radius: 100px; cursor: pointer;
  font-weight: 800; transition: var(--transition);
}
.btn-add:hover { background: var(--primary); color: var(--color-on-primary); box-shadow: 0 5px 15px color-mix(in srgb, var(--secondary) 40%, transparent); }
.btn-disabled { opacity: 0.5; cursor: not-allowed !important; }
.btn-disabled:hover { background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--primary); box-shadow: none; }

/* Floating Cart */
.floating-cart {
  position: fixed; bottom: 30px; right: 30px; z-index: 99;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  padding: 15px 25px; border-radius: 50px;
  display: flex; align-items: center; gap: 15px;
  cursor: pointer; box-shadow: 0 10px 30px color-mix(in srgb, var(--secondary) 40%, transparent);
  color: var(--text-primary); font-weight: 800; transition: var(--transition);
}
.floating-cart:hover { transform: translateY(-5px); box-shadow: 0 15px 40px color-mix(in srgb, var(--secondary) 60%, transparent); }
.cart-icon { font-size: 1.5rem; }
.cart-count { background: var(--color-inverse-surface); color: var(--primary); padding: 4px 10px; border-radius: 20px; font-size: 0.85rem; }
.cart-total { font-size: 1.1rem; }
.cart-checkout { margin-left: 10px; background: color-mix(in srgb, var(--color-on-background) 10%, transparent); padding: 5px 15px; border-radius: 20px; }

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
.cart-recommendations { margin: 4px 0 20px; padding: 14px; border: 1px solid var(--color-outline-variant); border-radius: var(--radius-md); background: var(--color-surface-container-low); }
.recommendation-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.recommendation-heading h4 { margin: 0; color: var(--text-primary); font-family: var(--font-display); }
.recommendation-controls { display: grid; grid-template-columns: 120px minmax(180px, 1fr); gap: 10px; margin: 12px 0; }
.recommendation-controls label, .preference-field { display: grid; gap: 5px; color: var(--text-secondary); font-size: 0.8rem; font-weight: 700; }
.recommendation-controls input { width: 100%; min-height: 38px; padding: 7px 9px; border: 1px solid var(--color-outline-variant); border-radius: var(--radius-sm); background: var(--bg-card); color: var(--text-primary); font: inherit; }
.preference-field { grid-column: 1 / -1; }
.preference-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.preference-chips button { min-height: 32px; padding: 5px 9px; border: 1px solid var(--color-outline-variant); border-radius: 999px; background: var(--bg-card); color: var(--text-secondary); cursor: pointer; font: inherit; font-size: 0.78rem; }
.preference-chips button.active { border-color: var(--primary); background: var(--primary); color: var(--color-on-primary); }
.recommendation-retry { border: 0; background: transparent; color: var(--primary); cursor: pointer; font-size: 1.15rem; }
.recommendation-retry:disabled { cursor: wait; opacity: 0.6; }
.recommendation-copy { margin: 8px 0 12px; color: var(--text-secondary); font-size: 0.9rem; }
.recommendation-list { display: grid; gap: 8px; }
.recommendation-item { display: grid; grid-template-columns: 44px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 8px; border-radius: var(--radius-sm); background: var(--bg-card); }
.recommendation-item img { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
.recommendation-item div { min-width: 0; display: grid; gap: 2px; }
.recommendation-item strong { color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recommendation-item small { color: var(--text-secondary); }
.recommendation-item span { color: var(--primary); font-size: 0.84rem; font-weight: 700; }
.recommendation-item button, .recommendation-error button { border: 1px solid var(--color-outline-variant); border-radius: var(--radius-sm); background: var(--color-surface-container); color: var(--primary); cursor: pointer; font: inherit; font-size: 0.82rem; font-weight: 700; padding: 7px 10px; }
.recommendation-error { margin: 8px 0 0; color: var(--danger); font-size: 0.88rem; }
.recommendation-error button { margin-left: 8px; }
.payment-choice-box {
  padding: 18px;
  border: 1px solid var(--color-outline-variant);
  border-radius: var(--radius-md);
  background: var(--color-surface-container-low);
  text-align: left;
}
.payment-choice-box h4 { margin: 0 0 6px; color: var(--text-primary); }
.payment-choice-note { margin: 0 0 12px; color: var(--text-secondary); font-size: 0.9rem; }
.payment-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.payment-choice-btn {
  display: grid;
  gap: 4px;
  text-align: left;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-outline-variant);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
}
.payment-choice-btn strong { color: var(--text-primary); }
.payment-choice-btn small { color: var(--text-muted); }
.payment-choice-btn.selected {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary) 20%, transparent);
}
.checkout-success-lead { margin: 0 0 16px; color: var(--text-secondary); }
.checkout-success-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  text-align: left;
  margin-bottom: 14px;
}
.checkout-success-grid div {
  padding: 10px 12px;
  border: 1px solid var(--color-outline-variant);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
}
.checkout-success-grid span {
  display: block;
  color: var(--text-muted);
  font-size: 0.78rem;
  margin-bottom: 4px;
}
.checkout-success-grid strong {
  color: var(--text-primary);
  word-break: break-word;
}
.checkout-copy-btn {
  border: 1px solid var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  color: var(--primary);
  border-radius: var(--radius-sm);
  padding: 9px 14px;
  cursor: pointer;
  font-weight: 700;
  margin-bottom: 14px;
}
.checkout-copy-btn:hover { background: color-mix(in srgb, var(--primary) 18%, transparent); }
.checkout-cash-note { margin: 12px 0 0; color: var(--text-secondary); font-size: 0.92rem; }

@media (max-width: 720px) {
  .payment-choice-grid,
  .checkout-success-grid {
    grid-template-columns: 1fr;
  }
}

/* GustoPro menu composition: simple header, warm cards, clear primary actions. */
.menu-wrapper { background: var(--color-surface); }
.menu-content { max-width: 1240px; margin: 0 auto; padding: 58px 24px 90px; text-align: left; }
.page-title { font-family: var(--font-display); color: var(--text-primary); font-size: 2.5rem; letter-spacing: 0; text-transform: none; margin: 0 0 6px; }
.page-subtitle { color: var(--text-secondary); font-size: 1rem; font-weight: 400; margin-bottom: 18px; }
.menu-search { position: relative; width: min(100%, 480px); margin: 0 0 34px; }
.menu-search > span { position: absolute; top: 50%; left: 14px; color: var(--text-muted); font-size: 1.3rem; transform: translateY(-52%); pointer-events: none; }
.menu-search input { width: 100%; min-height: 46px; padding: 0 42px 0 42px; border: 1px solid var(--color-outline-variant); border-radius: 999px; background: var(--color-surface-container-low); color: var(--text-primary); font: inherit; }
.menu-search input:focus { outline: 2px solid var(--primary-glow); border-color: var(--primary); }
.menu-search button { position: absolute; top: 50%; right: 8px; width: 30px; height: 30px; padding: 0; border: 0; border-radius: 50%; background: transparent; color: var(--text-muted); font-size: 1.4rem; cursor: pointer; transform: translateY(-50%); }
.menu-search button:hover { color: var(--primary); background: var(--color-primary-fixed); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.suggested-section { margin: 0 0 36px; padding: 0; background: transparent; border: 0; border-radius: 0; }
.suggested-section .section-title { color: var(--text-primary); font-family: var(--font-display); font-size: 1.6rem; margin-bottom: 16px; }
.suggested-section .section-title span { color: var(--primary) !important; }
.suggested-grid { gap: 16px; padding-bottom: 6px; }
.suggested-grid::-webkit-scrollbar-thumb { background: var(--primary); }
.suggested-card { min-width: 232px; padding: 0; overflow: hidden; background: var(--bg-card); border: 1px solid var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.suggested-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); transform: translateY(-2px); }
.suggested-card img { width: 100%; height: 142px; margin: 0; border-radius: 0; }
.sugg-info { padding: 14px 14px 4px; }
.sugg-info h3 { color: var(--text-primary); font-family: var(--font-display); }
.sugg-info .price { color: var(--primary); font-size: 1rem; }
.sugg-badge { top: 8px; right: 8px; background: var(--primary); color: var(--color-on-primary); border-radius: 999px; transform: none; box-shadow: none; }
.btn-sugg-add { width: calc(100% - 28px); margin: 10px 14px 14px; background: var(--color-surface-container); color: var(--primary); border: 1px solid var(--color-outline-variant); border-radius: var(--radius-sm); }
.btn-sugg-add:hover { background: var(--primary); color: var(--color-on-primary); }
.category-filter { justify-content: flex-start; gap: 8px; margin-bottom: 30px; }
.category-filter button { background: var(--bg-card); border-color: var(--color-outline-variant); color: var(--text-secondary); border-radius: 999px; }
.category-filter button.active { background: var(--primary); color: var(--color-on-primary); border-color: var(--primary); box-shadow: none; }
.product-grid { grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 14px; scroll-margin-top: 90px; }
.product-card { display: flex; flex-direction: column; align-items: flex-start; padding: 0 0 14px; background: var(--bg-card); border: 1px solid var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); text-align: left; }
.product-card::before { display: none; }
.product-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); transform: translateY(-2px); }
.product-card img { width: 100%; aspect-ratio: 4 / 3; height: auto; margin: 0 0 11px; border: 0; border-radius: 0; box-shadow: none; }
.product-card:hover img { transform: none; border: 0; }
.product-card h3, .product-card .product-rating, .product-card .price { margin-right: 14px; margin-left: 14px; }
.product-card h3 { color: var(--text-primary); font-family: var(--font-display); font-size: .94rem; line-height: 1.35; margin-bottom: 4px; min-height: 2.55em; }
.product-rating { color: var(--warning); margin-bottom: 8px; }
.price { color: var(--primary); font-size: 1rem; margin-bottom: 14px; }
.remaining-servings { width: calc(100% - 20px); margin: -5px 10px 11px; color: var(--text-muted); font-size: 12.5px; font-weight: 500; line-height: 1.4; text-align: center; }
.remaining-servings.low, .remaining-servings.soldout { color: var(--danger); font-weight: 750; }
.btn-add { width: calc(100% - 28px); margin: auto 14px 0; min-height: 40px; background: var(--color-surface-container); border-color: var(--color-outline-variant); border-radius: var(--radius-sm); color: var(--primary); }
.btn-add:hover { background: var(--primary); color: var(--color-on-primary); box-shadow: none; }
.floating-cart { background: var(--primary); color: var(--color-on-primary); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); }
.cart-count { background: var(--color-on-primary); color: var(--primary); }
.cart-checkout { background: rgba(255, 255, 255, 0.18); }
.menu-pagination { display: flex; justify-content: center; flex-wrap: wrap; gap: 7px; margin: 30px 0 0; }
.menu-pagination button { min-width: 38px; min-height: 38px; border: 1px solid var(--color-outline-variant); border-radius: 9px; background: var(--bg-card); color: var(--text-primary); font: inherit; font-weight: 750; cursor: pointer; }
.menu-pagination button.active { background: var(--primary); border-color: var(--primary); color: var(--color-on-primary); }
.menu-pagination button:disabled { opacity: .45; cursor: not-allowed; }
.checkout-modal { display: flex; flex-direction: column; width: min(600px, calc(100vw - 32px)); max-height: min(760px, calc(100dvh - 32px)); padding: 0; overflow: hidden; }
.checkout-header { flex: 0 0 auto; margin: 0; padding: 22px 24px 16px; border-bottom: 1px solid var(--border); }
.checkout-scroll-area { min-height: 0; max-height: none; overflow-y: auto; overscroll-behavior: contain; padding: 18px 24px; }
.checkout-actions { flex: 0 0 auto; display: flex; gap: 10px; margin: 0; padding: 16px 24px; border-top: 1px solid var(--border); background: var(--bg-card); }

@media (max-width: 1024px) {
  .menu-content { margin: 44px auto; }
  .page-title { font-size: 2.4rem; }
  .product-grid,
  .menu-loading-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
  .floating-cart { right: 20px; bottom: 20px; }
  .menu-content { padding-right: 20px; padding-left: 20px; }
  .suggested-card { min-width: 220px; }
}

@media (max-width: 760px) and (min-width: 641px) {
  .product-grid, .menu-loading-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 640px) {
  .menu-wrapper,
  .menu-wrapper * { box-sizing: border-box; }
  .menu-wrapper { overflow-x: hidden; }
  .menu-content { margin: 32px auto 96px; padding: 0 16px; }
  .menu-search { width: 100%; margin-bottom: 24px; }
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
  .menu-loading-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .menu-state { box-sizing: border-box; width: 100%; min-height: 180px; padding: 22px 16px; }
  .menu-state span { max-width: 100%; overflow-wrap: anywhere; text-align: center; }
  .menu-error .g-btn-outline { min-height: 44px; width: 100%; }
  .product-card { padding: 0 0 14px; }
  .product-card img { width: 100%; aspect-ratio: 4 / 3; height: auto; object-fit: cover; }
  .product-card h3 { font-size: .9rem; }
  .product-card h3 { overflow-wrap: anywhere; }
  .suggested-section { overflow: hidden; }
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
  .recommendation-item { grid-template-columns: 44px minmax(0, 1fr); }
  .recommendation-controls { grid-template-columns: 1fr; }
  .preference-field { grid-column: auto; }
  .recommendation-item button { grid-column: 1 / -1; width: 100%; }
  .g-modal-overlay:has(.checkout-modal) { align-items: stretch; padding: 0; }
  .checkout-modal { width: 100vw; max-width: none; max-height: 100dvh; min-height: 100dvh; border-radius: 0; }
  .checkout-header { position: sticky; top: 0; z-index: 2; padding: 16px; background: var(--bg-card); }
  .checkout-scroll-area { flex: 1 1 auto; padding: 16px; }
  .checkout-actions { position: sticky; bottom: 0; z-index: 2; padding: 12px 16px calc(12px + env(safe-area-inset-bottom)); }
  .checkout-actions button { width: 100%; }
}
</style>
