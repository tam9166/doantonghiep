<template>
  <main class="event-page">
    <section>
      <p class="eyebrow">MỘC VỊ EVENT HALL</p>
      <h1>Đặt sảnh sự kiện</h1>
      <p>
        Yêu cầu tiệc cưới, hội nghị hoặc sinh nhật được tiếp nhận riêng, không
        cần chọn bàn.
      </p>
      <article v-if="selectedHall" class="hall-preview">
        <img v-if="selectedHall.imageUrl" :src="selectedHall.imageUrl" :alt="selectedHall.nameVi" />
        <div><h2>{{ selectedHall.nameVi }}</h2><p>{{ selectedHall.descriptionVi }}</p><strong>{{ selectedHall.minGuestCount }}–{{ selectedHall.maxGuestCount }} khách · tối đa {{ selectedHall.maxTables || "-" }} bàn × {{ selectedHall.defaultGuestsPerTable || 10 }} khách</strong>
          <div class="hall-gallery"><img v-for="url in selectedHall.gallery || []" :key="url" :src="url" alt="Ảnh sảnh sự kiện" /></div>
        </div>
      </article>
      <form @submit.prevent="submit">
        <label>Họ tên<input v-model.trim="form.customerName" required /></label
        ><label
          >Số điện thoại<input
            v-model.trim="form.customerPhone"
            required /></label
        ><label
          >Email<input v-model.trim="form.customerEmail" type="email" /></label
        ><label
          >Sảnh sự kiện<select v-model.number="form.areaId" required>
            <option :value="null">Chọn sảnh</option>
            <option v-for="area in halls" :key="area.id" :value="area.id">
              {{ area.nameVi }} · {{ area.minGuestCount }}–{{
                area.maxGuestCount
              }}
              khách
            </option>
          </select></label
        ><label
          >Loại sự kiện<select v-model="form.eventType">
            <option value="WEDDING">Tiệc cưới</option>
            <option value="ENGAGEMENT">Ăn hỏi</option>
            <option value="BIRTHDAY">Sinh nhật</option>
            <option value="REUNION">Họp lớp / Liên hoan</option>
            <option value="CORPORATE">Tiệc công ty</option>
            <option value="CONFERENCE">Hội nghị</option>
            <option value="OTHER">Khác</option>
          </select></label
        ><label
          >Số khách<input
            v-model.number="form.guestCount"
            min="1"
            type="number"
            required /></label
        ><label
          >Ngày<input
            v-model="form.reservationDate"
            type="date"
            required /></label
        ><label
          >Giờ<input v-model="form.arrivalTime" type="time" :min="businessHours.openingTime" :max="latestArrivalTime" required /></label
        ><label
          >Số giờ thuê<input
            v-model.number="form.durationHours"
            min="1"
            max="72"
            type="number"
            required /></label
        ><label
          ><input v-model="form.decorationRequired" type="checkbox" /> Cần trang
          trí</label
        ><label
          ><input v-model="form.mcRequired" type="checkbox" /> Cần MC</label
        ><label
          ><input v-model="form.preorderEnabled" type="checkbox" /> Đặt món
          trước cho sự kiện</label
        >
        <div v-if="form.preorderEnabled" class="event-menu-picker">
          <div class="event-menu-main">
            <div class="event-menu-toolbar">
              <input v-model.trim="menuSearch" type="search" placeholder="Tìm món cho sự kiện" />
              <select v-model="menuCategory">
                <option value="">Tất cả nhóm món</option>
                <option v-for="category in menuCategories" :key="category" :value="category">{{ category }}</option>
              </select>
            </div>
            <div class="event-dish-grid">
              <article v-for="item in pagedMenu" :key="item.id" :class="['event-dish-card', { unavailable: !isAvailable(item) }]">
                <strong>{{ item.nameVi }}</strong>
                <span>{{ item.categoryNameVi || "Món ăn" }}</span>
                <b>{{ money(item.price) }}</b>
                <small v-if="item.inventoryManaged">{{ Math.max(0, Number(item.availableQuantity || 0)) }} phần còn lại</small>
                <small v-if="!isAvailable(item)" class="unavailable-note">Không khả dụng</small>
                <button type="button" :disabled="!isAvailable(item)" @click="add(item)">
                  {{ isAvailable(item) ? "Thêm món" : "Tạm hết" }}
                </button>
              </article>
            </div>
            <nav v-if="menuTotalPages > 1" class="event-menu-pagination" aria-label="Phân trang món sự kiện">
              <button type="button" :disabled="menuPage === 1" @click="menuPage--">‹</button>
              <button
                v-for="page in menuPageButtons"
                :key="page.key"
                type="button"
                :disabled="page.ellipsis"
                :class="{ active: page.value === menuPage, ellipsis: page.ellipsis }"
                @click="!page.ellipsis && (menuPage = page.value)"
              >{{ page.label }}</button>
              <button type="button" :disabled="menuPage === menuTotalPages" @click="menuPage++">›</button>
            </nav>
          </div>
          <aside class="event-selected-panel">
            <h3>Món đã chọn</h3>
            <p v-if="!cart.length" class="empty-cart">Chưa chọn món nào.</p>
            <div v-for="item in cart" :key="item.productId" :class="['event-cart-row', { invalid: !isCartItemValid(item) }]">
              <strong>{{ item.name }}</strong>
              <small v-if="item.invalidReason" class="unavailable-note">{{ item.invalidReason }}</small>
              <div class="event-qty">
                <button type="button" :disabled="!isCartItemValid(item)" @click="changeQty(item.productId, -1)">-</button>
                <input :value="item.quantity" :disabled="!isCartItemValid(item)" type="number" min="1" :max="item.availableQuantity || undefined" @change="setQty(item, $event.target.value)" />
                <button type="button" :disabled="!isCartItemValid(item)" @click="changeQty(item.productId, 1)">+</button>
              </div>
              <span>{{ isCartItemValid(item) ? money(item.price * item.quantity) : money(0) }}</span>
              <button type="button" class="remove-dish" @click="remove(item.productId)">Xóa</button>
            </div>
            <strong class="event-cart-total">Tổng: {{ money(cartTotal) }}</strong>
          </aside>
        </div>
        <label
          >Ghi chú<textarea v-model.trim="form.eventNote" maxlength="500" />
        </label>
        <p v-if="error" class="error">{{ error }}</p>
        <button :disabled="submitting">
          {{ submitting ? "Đang gửi..." : "Gửi yêu cầu đặt sự kiện" }}
        </button>
      </form>
      <section v-if="result" class="result">
        <h2>Đã tiếp nhận yêu cầu</h2>
        <p>
          Mã đặt chỗ: <strong>{{ result.reservationCode }}</strong>
        </p>
        <p>
          Tiền cọc cần thanh toán:
          <strong>{{ money(result.depositAmount) }}</strong>
        </p>
        <article v-if="paymentQr" class="event-payment-qr">
          <div>
            <h3>QR thanh toán tiền cọc</h3>
            <p>Nội dung: <strong>{{ paymentQr.transferContent }}</strong></p>
            <p>Hết hạn: {{ new Date(paymentQr.expiresAt).toLocaleString('vi-VN') }}</p>
          </div>
          <img :src="paymentQr.qrUrl" alt="QR thanh toán tiền cọc sự kiện" />
        </article>
        <p v-if="paymentError" class="error">{{ paymentError }}</p>
        <RouterLink :to="`/reservation-lookup?code=${result.reservationCode}`"
          >Theo dõi yêu cầu</RouterLink
        >
      </section>
    </section>
  </main>
</template>
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import api from "@/services/api";
import { toBusinessDate } from "@/utils/businessDate";
import { minuteBefore } from "@/utils/businessHours";
const form = ref({
  customerName: "",
  customerPhone: "",
  customerEmail: "",
  areaId: null,
  eventType: "WEDDING",
  guestCount: 30,
  reservationDate: toBusinessDate(),
  arrivalTime: "",
  durationHours: 4,
  decorationRequired: false,
  mcRequired: false,
  eventNote: "",
  preorderEnabled: false,
});
const areas = ref([]);
const menu = ref([]);
const cart = ref([]);
const menuSearch = ref("");
const menuCategory = ref("");
const menuPage = ref(1);
const MENU_PAGE_SIZE = 10;
const error = ref("");
const submitting = ref(false);
const result = ref(null);
const paymentQr = ref(null);
const paymentError = ref("");
const idempotencyKey = ref(crypto.randomUUID());
const businessHours = ref({ openingTime: "09:00", lastOrderTime: "21:30" });
const latestArrivalTime = computed(() => minuteBefore(businessHours.value.lastOrderTime));
const halls = computed(() =>
  areas.value.filter(
    (a) => a.areaType === "EVENT_HALL" && a.status === "ACTIVE",
  ),
);
const selectedHall = computed(() => halls.value.find((area) => area.id === form.value.areaId));
const menuCategories = computed(() => [...new Set(menu.value.map((item) => item.categoryNameVi).filter(Boolean))]);
const alcoholKeywords = ['bia', 'rượu', 'beer', 'lager', 'wine', 'vang', 'whisky', 'whiskey', 'vodka', 'gin', 'rum', 'soju', 'sake', 'cocktail', 'champagne', 'cognac', 'hennessy', 'chivas', 'johnnie', 'martell', 'remy'];
const isAlcoholicItem = item => {
  const content = `${item?.nameVi || ""} ${item?.descriptionVi || ""} ${item?.categoryNameVi || ""}`.toLocaleLowerCase("vi-VN");
  return alcoholKeywords.some(keyword => content.includes(keyword));
};
const filteredMenu = computed(() => {
  const keyword = menuSearch.value.toLowerCase();
  return menu.value.filter((item) => {
    const haystack = `${item.nameVi || ""} ${item.descriptionVi || ""} ${item.categoryNameVi || ""}`.toLowerCase();
    return (!keyword || haystack.includes(keyword)) && (!menuCategory.value || item.categoryNameVi === menuCategory.value);
  }).sort((a, b) => Number(isAvailable(b)) - Number(isAvailable(a))
    || Number(isAlcoholicItem(a)) - Number(isAlcoholicItem(b))
    || String(a.nameVi || "").localeCompare(String(b.nameVi || ""), "vi"));
});
const menuTotalPages = computed(() => Math.max(1, Math.ceil(filteredMenu.value.length / MENU_PAGE_SIZE)));
const pagedMenu = computed(() => {
  const start = (menuPage.value - 1) * MENU_PAGE_SIZE;
  return filteredMenu.value.slice(start, start + MENU_PAGE_SIZE);
});
const menuPageButtons = computed(() => compactPageButtons(menuPage.value, menuTotalPages.value));
const validCart = computed(() => cart.value.filter(isCartItemValid));
const cartTotal = computed(() => validCart.value.reduce((total, item) => total + item.price * item.quantity, 0));
const money = (v) =>
  Number(v || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  });
function add(i) {
  if (!isAvailable(i)) {
    error.value = `${i.nameVi} không còn khả dụng để đặt trước.`;
    markCartItemInvalid(i.id, error.value);
    return;
  }
  const x = cart.value.find((x) => x.productId === i.id);
  const limit = i.inventoryManaged ? Number(i.availableQuantity || 0) : Infinity;
  if (x) {
    if (x.quantity >= limit) {
      error.value = `${i.nameVi} chỉ còn ${Math.max(0, limit)} phần.`;
      return;
    }
    x.quantity++;
    x.invalidReason = "";
  } else {
    cart.value.push({
      productId: i.id,
      name: i.nameVi,
      price: Number(i.price || 0),
      quantity: 1,
      availableQuantity: i.inventoryManaged ? Number(i.availableQuantity || 0) : -1,
      invalidReason: "",
    });
  }
}
function changeQty(productId, delta) {
  const item = cart.value.find((row) => row.productId === productId);
  if (!item || !isCartItemValid(item)) return;
  const next = item.quantity + delta;
  if (next <= 0) {
    remove(productId);
    return;
  }
  const limit = item.availableQuantity == null || item.availableQuantity < 0 ? Infinity : Number(item.availableQuantity);
  if (next > limit) {
    error.value = `${item.name} chỉ còn ${limit} phần.`;
    return;
  }
  item.quantity = next;
}
function setQty(item, rawValue) {
  if (!isCartItemValid(item)) return;
  const parsed = Number.parseInt(rawValue, 10);
  const limit = item.availableQuantity == null || item.availableQuantity < 0 ? Infinity : Number(item.availableQuantity);
  if (!Number.isInteger(parsed) || parsed < 1) {
    item.quantity = 1;
    return;
  }
  if (parsed > limit) {
    item.quantity = limit;
    error.value = `${item.name} chỉ còn ${limit} phần.`;
    return;
  }
  item.quantity = parsed;
}
function remove(productId) {
  cart.value = cart.value.filter((item) => item.productId !== productId);
}
function isAvailable(item) {
  if (!item || item.available === false) return false;
  if (item.inventoryManaged) return Number(item.availableQuantity || 0) > 0;
  return true;
}
function isCartItemValid(item) {
  return Boolean(item && !item.invalidReason && (item.availableQuantity == null || item.availableQuantity < 0 || item.quantity <= item.availableQuantity));
}
function markCartItemInvalid(productId, reason) {
  const item = cart.value.find((row) => row.productId === productId);
  if (item) item.invalidReason = reason;
}
function syncCartAvailability() {
  cart.value.forEach((item) => {
    const dish = menu.value.find((row) => row.id === item.productId);
    if (!dish || !isAvailable(dish)) {
      item.availableQuantity = 0;
      item.invalidReason = `${item.name} không còn khả dụng để đặt trước.`;
      return;
    }
    item.availableQuantity = dish.inventoryManaged ? Number(dish.availableQuantity || 0) : -1;
    if (dish.inventoryManaged && item.quantity > item.availableQuantity) {
      item.quantity = Math.max(1, item.availableQuantity);
      item.invalidReason = `${item.name} chỉ còn ${item.availableQuantity} phần.`;
      return;
    }
    item.invalidReason = "";
  });
}
function validateCart() {
  syncCartAvailability();
  const invalid = cart.value.find((item) => !isCartItemValid(item));
  if (!invalid) return true;
  error.value = invalid.invalidReason || "Có món không còn khả dụng.";
  return false;
}
function compactPageButtons(current, total) {
  const pages = new Set([1, total, current - 1, current, current + 1]);
  const valid = [...pages].filter((page) => page >= 1 && page <= total).sort((a, b) => a - b);
  const result = [];
  valid.forEach((page, index) => {
    const previous = valid[index - 1];
    if (previous && page - previous > 1) result.push({ key: `ellipsis-${previous}-${page}`, label: "…", ellipsis: true });
    result.push({ key: `page-${page}`, label: String(page), value: page, ellipsis: false });
  });
  return result;
}
onMounted(async () => {
  try {
    const draft = JSON.parse(
      sessionStorage.getItem("event-booking-draft") || "null",
    );
    if (draft) form.value = { ...form.value, ...draft };
    const settings = (await api.get("/api/settings/public")).data || {};
    businessHours.value = {
      openingTime: settings.openingTime || businessHours.value.openingTime,
      lastOrderTime: settings.lastOrderTime || businessHours.value.lastOrderTime,
    };
    areas.value = (await api.get("/api/areas")).data || [];
    menu.value = (await api.get("/api/menu-items/preorder")).data || [];
    syncCartAvailability();
  } catch {
    error.value = "Không tải được danh sách sảnh sự kiện.";
  }
});
async function submit() {
  if (submitting.value) return;
  if (form.value.preorderEnabled && !validateCart()) return;
  submitting.value = true;
  error.value = "";
  try {
    result.value = (
      await api.post("/api/event-bookings", {
        ...form.value,
        preorderItems: validCart.value.map((x) => ({
          productId: x.productId,
          quantity: x.quantity,
        })),
      }, {
        headers: { 'X-Idempotency-Key': idempotencyKey.value }
      })
    ).data;
    const capability = result.value.paymentCapabilityToken || "";
    if (capability && result.value.reservationCode) {
      sessionStorage.setItem(`reservation-capability:${result.value.reservationCode}`, capability);
    }
    if (capability && Number(result.value.depositAmount || 0) > 0) {
      try {
        paymentQr.value = (await api.post("/api/payments/qr", {
          reservationCode: result.value.reservationCode,
          paymentOption: result.value.paymentOption,
        }, { headers: {
          "X-Payment-Capability": capability,
          "X-Idempotency-Key": crypto.randomUUID(),
        } })).data;
      } catch (paymentFailure) {
        paymentError.value = paymentFailure.response?.data?.message
          || "Yêu cầu đã được lưu nhưng chưa tạo được QR. Bạn có thể tạo QR tại trang theo dõi.";
      }
    }
    idempotencyKey.value = crypto.randomUUID();
  } catch (e) {
    error.value = e.response?.data?.message || "Không thể gửi yêu cầu.";
  } finally {
    submitting.value = false;
  }
}
watch([menuSearch, menuCategory], () => { menuPage.value = 1; });
watch(filteredMenu, () => { menuPage.value = Math.max(1, Math.min(menuPage.value, menuTotalPages.value)); });
</script>
<style scoped>
.event-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 58px 18px 80px;
  color: #241719;
  background:
    radial-gradient(
      circle at 10% 12%,
      rgba(190, 11, 47, 0.075),
      transparent 24%
    ),
    radial-gradient(
      circle at 90% 28%,
      rgba(214, 164, 103, 0.09),
      transparent 27%
    ),
    linear-gradient(145deg, #fffdfc 0%, #fff7f6 52%, #fffaf6 100%);
}
.event-page::before,
.event-page::after {
  position: absolute;
  z-index: 0;
  color: rgba(190, 11, 47, 0.075);
  font-family: Georgia, serif;
  font-size: 190px;
  line-height: 1;
  pointer-events: none;
}
.event-page::before {
  content: "";
  top: 22px;
  right: 4%;
  transform: rotate(-20deg);
}
.event-page::after {
  content: "";
  bottom: 20px;
  left: 3%;
  transform: rotate(18deg);
}
.event-page > section {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: auto;
  padding: 38px 42px;
  border: 1px solid #efd4d4;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 45px rgba(90, 35, 43, 0.09);
  backdrop-filter: blur(8px);
}
.event-page h1 {
  margin: 8px 0;
  font-size: clamp(2rem, 4vw, 3rem);
  letter-spacing: -0.035em;
}
.event-page > section > p:not(.eyebrow) {
  color: #6e5e60;
}
.eyebrow {
  margin: 0;
  color: #b70f2b;
  font-weight: 900;
  letter-spacing: 0.06em;
}
.hall-preview{display:grid;grid-template-columns:220px 1fr;gap:18px;margin:22px 0;padding:16px;border:1px solid #efd4d4;border-radius:12px;background:#fff9f8}.hall-preview>img{width:100%;height:180px;object-fit:cover;border-radius:9px}.hall-preview h2{margin:0}.hall-gallery{display:flex;gap:8px;margin-top:12px;overflow:auto}.hall-gallery img{width:92px;height:68px;object-fit:cover;border-radius:7px}
form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 22px;
  margin-top: 28px;
}
label {
  display: grid;
  gap: 8px;
  font-weight: 750;
}
label:nth-of-type(1),
label:nth-of-type(2),
label:nth-of-type(3),
label:nth-of-type(4),
label:nth-of-type(5),
label:nth-of-type(6),
label:nth-of-type(7),
label:nth-of-type(8),
label:nth-of-type(9),
label:last-of-type {
  grid-column: span 1;
}
input,
select,
textarea {
  min-height: 48px;
  padding: 11px 13px;
  border: 1px solid #e8cdcc;
  border-radius: 9px;
  background: #fffdfd;
  color: #241719;
  font: inherit;
}
input:focus,
select:focus,
textarea:focus {
  outline: 3px solid rgba(190, 11, 47, 0.09);
  border-color: #be0b2f;
}
input[type="checkbox"] {
  width: 20px;
  min-height: 20px;
  accent-color: #be0b2f;
}
label:has(input[type="checkbox"]) {
  display: flex;
  grid-column: span 1;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #f0dddd;
  border-radius: 9px;
  background: #fff9f8;
}
form > div,
form > .error,
form > button,
label:last-of-type {
  grid-column: 1 / -1;
}
.event-menu-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 340px);
  gap: 18px;
  align-items: start;
}
.event-menu-main {
  min-width: 0;
}
.event-menu-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 240px);
  gap: 12px;
  margin-bottom: 14px;
}
.event-dish-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.event-dish-card {
  display: grid;
  gap: 7px;
  padding: 14px;
  border: 1px solid #efd4d4;
  border-radius: 12px;
  background: #fffdfd;
}
.event-dish-card.unavailable {
  opacity: .68;
}
.event-dish-card span,
.event-dish-card small {
  color: #766568;
}
.event-dish-card b,
.event-cart-total {
  color: #be0b2f;
}
.unavailable-note {
  color: #a32626;
  font-weight: 850;
}
.event-selected-panel {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid #efd4d4;
  border-radius: 12px;
  background: #fff9f8;
}
.event-selected-panel h3 {
  margin: 0;
}
.empty-cart {
  margin: 0;
  color: #766568;
}
.event-cart-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 10px 0;
  border-top: 1px dashed #efd4d4;
}
.event-cart-row.invalid {
  padding: 10px;
  border-radius: 10px;
  background: #fff0ef;
}
.event-cart-row strong,
.event-cart-row small,
.event-cart-row .event-qty,
.event-cart-row .remove-dish {
  grid-column: 1 / -1;
}
.event-qty {
  display: inline-grid;
  grid-template-columns: 32px 52px 32px;
  width: max-content;
  height: 36px;
}
.event-qty button,
.event-qty input {
  width: auto;
  min-width: 0;
  min-height: 36px;
  height: 36px;
  padding: 0;
  text-align: center;
}
.event-qty input {
  border-inline: 0;
  border-radius: 0;
}
.remove-dish {
  min-height: 38px;
  background: #fff;
  color: #be0b2f;
  border: 1px solid #efd4d4;
}
.event-menu-pagination {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
}
.event-menu-pagination button {
  min-width: 38px;
  min-height: 38px;
  padding: 6px 10px;
  background: #fff;
  color: #be0b2f;
  border: 1px solid #efd4d4;
}
.event-menu-pagination button.active {
  background: #be0b2f;
  color: #fff;
}
.event-menu-pagination button.ellipsis {
  background: transparent;
  border-color: transparent;
  color: #766568;
  cursor: default;
}
textarea {
  min-height: 110px;
  resize: vertical;
}
button {
  min-height: 50px;
  padding: 13px 18px;
  border: 0;
  border-radius: 9px;
  background: linear-gradient(135deg, #b90b2d, #d30d3a);
  color: #fff;
  font-weight: 850;
  cursor: pointer;
}
button:hover:not(:disabled) {
  background: #9d0825;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error {
  color: #a32626;
}
.result {
  margin-top: 24px;
  padding: 20px;
  border: 1px solid #efd4d4;
  border-radius: 10px;
  background: #fff7f6;
}
.event-payment-qr {
  display: grid;
  grid-template-columns: 1fr 190px;
  gap: 18px;
  align-items: center;
  margin: 18px 0;
  padding: 16px;
  border: 1px solid var(--color-outline-variant);
  border-radius: 10px;
  background: var(--color-surface);
}
.event-payment-qr img {
  width: 190px;
  height: 190px;
  object-fit: contain;
}
@media (max-width: 720px) {
  .hall-preview{grid-template-columns:1fr}
  .event-page {
    padding: 28px 12px 50px;
  }
  .event-page > section {
    padding: 26px 18px;
  }
  form {
    grid-template-columns: 1fr;
  }
  .event-menu-picker,
  .event-menu-toolbar {
    grid-template-columns: 1fr;
  }
  .event-selected-panel {
    position: static;
  }
  .event-payment-qr { grid-template-columns: 1fr; }
  .event-payment-qr img { width: 100%; height: auto; }
  form > *,
  label:has(input[type="checkbox"]) {
    grid-column: 1 !important;
  }
  .event-page::before,
  .event-page::after {
    font-size: 120px;
  }
}
.event-menu-picker .event-menu-pagination button,
.event-menu-picker .event-qty button,
.event-menu-picker .remove-dish {
  border: 1px solid #efd4d4;
  background: #fff;
  color: #be0b2f;
}
.event-menu-picker .event-menu-pagination button.active,
.event-menu-picker .event-dish-card button {
  background: linear-gradient(135deg, #b90b2d, #d30d3a);
  color: #fff;
  border-color: transparent;
}
.event-menu-picker .event-menu-pagination button.ellipsis {
  background: transparent;
  border-color: transparent;
  color: #766568;
}
.event-menu-picker .event-qty input {
  width: 52px;
  min-width: 0;
  min-height: 36px;
  height: 36px;
  padding: 0 4px;
  text-align: center;
}
</style>
