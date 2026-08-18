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
          >Giờ<input v-model="form.arrivalTime" type="time" required /></label
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
        <div v-if="form.preorderEnabled">
          <strong>Chọn món trước</strong
          ><button
            v-for="item in menu"
            :key="item.id"
            type="button"
            @click="add(item)"
          >
            {{ item.nameVi }} · {{ money(item.price) }}
          </button>
          <p>
            Món đã chọn: {{ cart.map((x) => x.name).join(", ") || "Chưa chọn" }}
          </p>
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
        <RouterLink :to="`/reservation-lookup?code=${result.reservationCode}`"
          >Theo dõi yêu cầu</RouterLink
        >
      </section>
    </section>
  </main>
</template>
<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import api from "@/services/api";
const form = ref({
  customerName: "",
  customerPhone: "",
  customerEmail: "",
  areaId: null,
  eventType: "WEDDING",
  guestCount: 30,
  reservationDate: new Date().toISOString().slice(0, 10),
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
const error = ref("");
const submitting = ref(false);
const result = ref(null);
const halls = computed(() =>
  areas.value.filter(
    (a) => a.areaType === "EVENT_HALL" && a.status === "ACTIVE",
  ),
);
const selectedHall = computed(() => halls.value.find((area) => area.id === form.value.areaId));
const money = (v) =>
  Number(v || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  });
function add(i) {
  const x = cart.value.find((x) => x.productId === i.id);
  if (x) x.quantity++;
  else cart.value.push({ productId: i.id, name: i.nameVi, quantity: 1 });
}
onMounted(async () => {
  try {
    const draft = JSON.parse(
      sessionStorage.getItem("event-booking-draft") || "null",
    );
    if (draft) form.value = { ...form.value, ...draft };
    areas.value = (await api.get("/api/areas")).data || [];
    menu.value = (await api.get("/api/menu-items/preorder")).data || [];
  } catch {
    error.value = "Không tải được danh sách sảnh sự kiện.";
  }
});
async function submit() {
  submitting.value = true;
  error.value = "";
  try {
    result.value = (
      await api.post("/api/event-bookings", {
        ...form.value,
        preorderItems: cart.value.map((x) => ({
          productId: x.productId,
          quantity: x.quantity,
        })),
      })
    ).data;
  } catch (e) {
    error.value = e.response?.data?.message || "Không thể gửi yêu cầu.";
  } finally {
    submitting.value = false;
  }
}
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
  content: "❧";
  top: 22px;
  right: 4%;
  transform: rotate(-20deg);
}
.event-page::after {
  content: "❦";
  bottom: 20px;
  left: 3%;
  transform: rotate(18deg);
}
.event-page > section {
  position: relative;
  z-index: 1;
  max-width: 900px;
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
  form > *,
  label:has(input[type="checkbox"]) {
    grid-column: 1 !important;
  }
  .event-page::before,
  .event-page::after {
    font-size: 120px;
  }
}
</style>
