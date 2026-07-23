<template>
  <CustomerLayout transparent-nav>
    <div class="home-wrapper">

    <section class="hero-banner">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-tag">✦ {{ $t('home.title') }}</div>
        <h1>Nhà Hàng<br><span>Mộc Vị</span></h1>
        <p>{{ $t('home.subtitle') }}</p>
        <div class="hero-actions">
          <button @click="$router.push('/dine-in')" class="g-btn-primary home-hero-btn">
            🍽️ Gọi Món Tại Quán
          </button>
          <button @click="$router.push('/reservation')" class="g-btn-outline home-hero-btn home-hero-outline">
            📅 Đặt Bàn Trước
          </button>
        </div>

        <!-- Stats bar -->
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">100+</span>
            <span class="stat-lbl">Món ăn</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">5★</span>
            <span class="stat-lbl">Đánh giá</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">10+</span>
            <span class="stat-lbl">Năm kinh nghiệm</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Main Content -->
    <main class="main-content">
      <div class="content-container">
        <!-- Left col - Article -->
        <article class="left-col">
          <div class="meta-info">
            <span class="meta-tag">Tin nổi bật</span>
            <span class="meta-date">🕒 {{ currentDate }}</span>
          </div>
          <h2 class="article-title">Trải nghiệm ẩm thực Việt tại NHÀ HÀNG MỘC VỊ Đà Nẵng</h2>
          <p class="article-excerpt">
            Với không gian thoáng đãng, đậm chất kiến trúc truyền thống và thực đơn phong phú
            quy tụ tinh hoa ẩm thực ba miền, nhà hàng chúng tôi mang đến cho thực khách những
            trải nghiệm trọn vẹn nhất...
          </p>
          <div class="article-actions">
            <button @click="$router.push('/menu')" class="g-btn-primary home-action-btn">
              📖 Xem Thực Đơn
            </button>
            <button @click="$router.push('/reservation')" class="g-btn-outline home-action-btn">
              📅 Đặt Bàn Ngay
            </button>
          </div>

          <!-- Feature Cards -->
          <div class="feature-grid">
            <div class="feature-card">
              <div class="feature-icon">🚚</div>
              <h4>Giao Hàng Tận Nơi</h4>
              <p>Đặt món và nhận hàng ngay tại nhà của bạn</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon">🎉</div>
              <h4>Đặt Tiệc & Họp Mặt</h4>
              <p>Phòng VIP riêng cho sự kiện của bạn</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon">⭐</div>
              <h4>Không Gian VIP</h4>
              <p>Trải nghiệm không gian đẳng cấp 5 sao</p>
            </div>
          </div>
        </article>

        <!-- Right col - Sidebar -->
        <aside class="right-col">
          <div class="sidebar-card">
            <h3>- Dịch vụ của chúng tôi</h3>
            <ul class="service-list">
              <li>
                <router-link to="/menu">
                  <span class="svc-icon">🚚</span>
                  <span>Giao hàng tận nơi</span>
                  <span class="svc-badge new">Mới</span>
                </router-link>
              </li>
              <li>
                <router-link to="/reservation">
                  <span class="svc-icon">🎉</span>
                  <span>Đặt tiệc, họp mặt</span>
                  <span class="svc-badge hot">Hot</span>
                </router-link>
              </li>
              <li>
                <a href="#">
                  <span class="svc-icon">⭐</span>
                  <span>Trải nghiệm không gian VIP</span>
                </a>
              </li>
              <li>
                <a href="#">
                  <span class="svc-icon">🌿</span>
                  <span>Thực đơn chay</span>
                </a>
              </li>
            </ul>
          </div>

          <div class="sidebar-card hours-card">
            <h3>🕒 Giờ Mở Cửa</h3>
            <div class="hours-list">
              <div class="hours-item">
                <span>Thứ 2 - Thứ 6</span>
                <span class="hours-time">10:00 - 22:00</span>
              </div>
              <div class="hours-item">
                <span>Thứ 7 - CN</span>
                <span class="hours-time">09:00 - 23:00</span>
              </div>
            </div>
            <div class="open-status">
              <span class="open-dot"></span>
              Đang mở cửa
            </div>
          </div>
        </aside>
      </div>
    </main>

    <!-- Tin Tức Nhà Hàng -->
    <section v-if="newsPosts.length > 0" class="posts-section">
      <div class="section-container">
        <div class="section-header-block">
          <span class="section-tag">📰 Tin Tức</span>
          <h2>Tin Tức Nhà Hàng</h2>
          <p>Cập nhật những thông tin mới nhất về nhà hàng, món ăn và ưu đãi đặc biệt</p>
        </div>
        <div class="news-grid">
          <div v-for="post in newsPosts" :key="post.id" class="news-card">
            <div class="news-img-wrap">
              <img v-if="post.image" :src="post.image" :alt="post.title" loading="lazy" />
              <div v-else class="news-img-placeholder">📰</div>
              <span class="news-date-badge">{{ formatPostDate(post.createDate) }}</span>
            </div>
            <div class="news-body">
              <h3>{{ post.title }}</h3>
              <p>{{ truncateText(post.content, 150) }}</p>
              <div class="news-footer">
                <button 
                  class="btn-like" 
                  :class="{'liked': isLiked(post.id)}" 
                  @click="likePost(post)"
                >
                  ❤️ {{ post.likes || 0 }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Tuyển Dụng -->
    <section v-if="recruitPosts.length > 0" class="posts-section recruit-section">
      <div class="section-container">
        <div class="section-header-block">
          <span class="section-tag recruit-tag">💼 Tuyển Dụng</span>
          <h2>Cơ Hội Nghề Nghiệp</h2>
          <p>Gia nhập đội ngũ MỘC VỊ RESTAURANT — nơi kiến tạo sự nghiệp ẩm thực</p>
        </div>
        <div class="recruit-list">
          <div v-for="post in recruitPosts" :key="post.id" class="recruit-card">
            <div class="recruit-icon">💼</div>
            <div class="recruit-info">
              <h4>{{ post.title }}</h4>
              <p>{{ truncateText(post.content, 200) }}</p>
              <div class="recruit-meta">
                <span class="recruit-date">📅 Đăng ngày {{ formatPostDate(post.createDate) }}</span>
                <button 
                  class="btn-like btn-like-small" 
                  :class="{'liked': isLiked(post.id)}" 
                  @click.stop="likePost(post)"
                >
                  ❤️ {{ post.likes || 0 }}
                </button>
              </div>
            </div>
            <button class="btn-apply" @click="showRecruitDetail(post)">Xem Chi Tiết →</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Recruit Detail Modal -->
    <div v-if="selectedRecruit" class="modal-overlay" @click.self="selectedRecruit = null">
      <div class="recruit-modal">
        <div class="modal-top">
          <h2>{{ selectedRecruit.title }}</h2>
          <button @click="selectedRecruit = null" class="btn-close-modal" aria-label="Đóng chi tiết tuyển dụng">✖</button>
        </div>
        <img v-if="selectedRecruit.image" :src="selectedRecruit.image" :alt="selectedRecruit.title" class="modal-img" loading="lazy" />
        <div class="modal-content">
          <pre class="modal-text">{{ selectedRecruit.content }}</pre>
          <div class="modal-footer">
            <p class="modal-date">📅 Đăng ngày {{ formatPostDate(selectedRecruit.createDate) }}</p>
            <div class="modal-actions">
              <button class="btn-interview" @click="startInterview">🎤 Thử Phỏng Vấn</button>
              <button class="btn-submit-app" @click="openApplicationForm">📋 Ứng Tuyển Ngay</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Application Form Modal -->
    <div v-if="showAppForm" class="modal-overlay" @click.self="showAppForm = false">
      <div class="app-modal">
        <h3>📋 Đơn Ứng Tuyển</h3>
        <p class="app-subtitle">Vị trí: <strong>{{ selectedRecruit?.title }}</strong></p>
        
        <div class="form-group">
          <label>Họ và Tên *</label>
          <input v-model="appForm.fullname" type="text" class="g-input" placeholder="VD: Nguyễn Văn A" />
        </div>
        <div class="form-group">
          <label>Số Điện Thoại *</label>
          <input v-model="appForm.phone" type="text" class="g-input" placeholder="VD: 0987654321" />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input v-model="appForm.email" type="email" class="g-input" placeholder="nguyenvana@gmail.com" />
        </div>
        <div class="form-group">
          <label>Lời Nhắn</label>
          <textarea v-model="appForm.message" class="g-input" rows="3" placeholder="Kinh nghiệm của bạn..."></textarea>
        </div>
        <div class="form-group">
          <label>CV Đính Kèm (PDF, DOCX, Ảnh)</label>
          <input type="file" @change="handleCvUpload" class="g-input" style="padding: 10px;" />
        </div>
        
        <div class="app-actions">
          <button class="btn-submit-app w-100" @click="submitApplication">Gửi Đơn Ứng Tuyển</button>
          <button class="btn-close" @click="showAppForm = false">Hủy</button>
        </div>
      </div>
    </div>

    <!-- Chatbots -->
    <div class="chatbots-container">
      <!-- Chat Widget: Interview -->
      <div v-if="showInterviewChat" class="chat-widget interview-chat">
        <div class="chat-header interview-header">
          <span>🎤 Bot Phỏng Vấn</span>
          <button @click="showInterviewChat = false" aria-label="Đóng bot phỏng vấn">✖</button>
        </div>
        <div class="chat-body" ref="interviewChatBody">
          <div v-for="(msg, i) in interviewMessages" :key="i" :class="['chat-msg', msg.type]">
            {{ msg.text }}
          </div>
        </div>
        <div class="chat-input-area" v-if="interviewStep < interviewQuestions.length">
          <input v-model="interviewInput" @keyup.enter="sendInterviewMessage" placeholder="Trả lời câu hỏi..." />
          <button @click="sendInterviewMessage">Gửi</button>
        </div>
        <div class="chat-input-area chat-done-area" v-else>
          <button class="btn-submit-app w-100" @click="openApplicationFormFromInterview">📋 Ứng Tuyển Ngay</button>
        </div>
      </div>

      <!-- Chat Widget: Customer Support -->
      <div v-if="showSupportChat" class="chat-widget support-chat">
        <div class="chat-header support-header">
          <span>💬 Bot Hỗ Trợ Khách Hàng</span>
          <button @click="showSupportChat = false" aria-label="Đóng bot hỗ trợ">✖</button>
        </div>
        <div class="chat-body" ref="supportChatBody">
          <div v-for="(msg, i) in supportMessages" :key="i" :class="['chat-msg', msg.type, { 'msg-booking': msg.isTableBooking, 'msg-menu': msg.isMenuLink }]">
            <template v-if="!msg.isTableBooking && !msg.isMenuLink">
              {{ msg.text }}
            </template>
            <template v-else-if="msg.isMenuLink">
              <div class="menu-link-widget">
                <p>👉 Bạn có thể xem toàn bộ thực đơn và đặt món tại đây:</p>
                <button @click="$router.push('/menu')" class="b-btn">📖 Xem Thực Đơn</button>
              </div>
            </template>
            <template v-else>
              <div class="booking-widget">
                <div v-if="!msg.bookingState.tableConfirmed">
                  <p>Các bàn trống phù hợp yêu cầu của bạn:</p>
                  <div class="b-table-list">
                    <div v-for="t in msg.bookingState.tables" :key="t.id" class="b-table-card">
                      <strong>{{ t.name }}</strong> ({{ t.capacity }} người)<br>
                      <span>{{ t.description }}</span>
                      <button @click="selectBookingTable(msg, t)" class="b-btn-small">Chọn bàn này</button>
                    </div>
                    <div v-if="msg.bookingState.tables.length === 0" style="color: var(--danger); font-size:0.8rem">Không tìm thấy bàn trống phù hợp!</div>
                  </div>
                </div>
                
                <div v-else-if="!msg.bookingState.paymentMethod">
                  <p>Đã chọn: <strong>{{ msg.bookingState.selectedTable.name }}</strong></p>
                  <div class="b-actions">
                    <button @click="choosePaymentMethod(msg, 'cọc')" class="b-btn">Chỉ đặt bàn (Cọc 100K)</button>
                    <button @click="choosePaymentMethod(msg, 'món')" class="b-btn-outline">Đặt món trước</button>
                  </div>
                </div>

                <div v-else-if="msg.bookingState.paymentMethod === 'cọc' && !msg.bookingState.paid" class="b-success">
                  <p>Tiếp tục trong quy trình đặt bàn an toàn để nhận báo giá, chính sách cọc và QR riêng.</p>
                  <button @click="continueSecureReservation" class="b-btn">Tiếp tục đặt bàn</button>
                </div>

                <div v-else-if="msg.bookingState.paymentMethod === 'món'" class="b-success">
                  <p>Đã lưu bàn. Đang chuyển tới Thực Đơn...</p>
                </div>

                <div v-else-if="msg.bookingState.paid" class="b-success">
                  ✅ Đặt bàn thành công!<br>Mã đơn: <strong>#{{ msg.bookingState.orderCode }}</strong>
                </div>
              </div>
            </template>
          </div>
          <div v-if="isSupportTyping" class="chat-msg bot typing-indicator">
            Đang suy nghĩ...
          </div>
        </div>
        <div class="chat-input-area">
          <input v-model="supportInput" @keyup.enter="sendSupportMessage" placeholder="Bạn cần hỏi gì? VD: Giờ mở cửa" />
          <button @click="sendSupportMessage">Gửi</button>
        </div>
      </div>
      
      <!-- FAB Buttons -->
      <div class="fab-group">
        <div class="fab-chat fab-wheel" @click="openLuckyWheel">🎁</div>
        <div class="fab-chat" @click="toggleSupportChat">💬</div>
        <a href="tel:0123456789" class="fab-phone" style="text-decoration: none;">📞</a>
      </div>
    </div>

    <!-- Lucky Wheel Modal -->
    <div v-if="showLuckyWheel" class="modal-overlay" @click.self="showLuckyWheel = false">
      <div class="wheel-modal">
        <div class="wheel-header">
          <h2>🎡 Vòng Quay May Mắn</h2>
          <button class="btn-close-modal" aria-label="Đóng vòng quay may mắn" @click="showLuckyWheel = false">✖</button>
        </div>
        <p class="wheel-desc">Quay mỗi ngày để nhận điểm thưởng VIP nhé!</p>
        <div class="wheel-container">
          <div class="wheel-pointer">▼</div>
          <div class="wheel-board" :style="{ transform: `rotate(${wheelRotation}deg)`, background: 'conic-gradient(#C08A2E 0 60deg, #33422A 60deg 120deg, #B98229 120deg 180deg, #7A7460 180deg 240deg, #5A6E45 240deg 300deg, #22301B 300deg 360deg)' }">
            <div v-for="(p, i) in prizes" :key="i" class="wheel-text" :style="{ transform: `rotate(${p.deg + 30}deg)` }">
              <span class="slice-label" :style="{ color: i === 0 ? '#201D14' : '#FFFFFF' }">{{ p.label }}</span>
            </div>
          </div>
        </div>
        <button class="btn-spin" @click="spinWheel" :disabled="isSpinning">
          {{ isSpinning ? 'Đang quay...' : 'QUAY NGAY' }}
        </button>
        <div v-if="spinResult" class="spin-result" :class="{ 'win': spinResult.type !== 'miss' }">
          <p v-if="spinResult.type !== 'miss'">🎉 Chúc mừng! Bạn trúng <strong>{{ spinResult.label }}</strong></p>
          <p v-else>😢 Rất tiếc! Chúc bạn may mắn lần sau.</p>
        </div>
      </div>
    </div>

  </div>
  </CustomerLayout>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/services/api';
import CustomerLayout from '@/components/CustomerLayout.vue';

const router = useRouter();

const isLoggedIn = ref(false);
const user = ref(null);
const newsPosts = ref([]);
const recruitPosts = ref([]);
const selectedRecruit = ref(null);

const today = new Date();
const currentDate = ref(`${today.getDate()}-${today.getMonth() + 1}-${today.getFullYear()}`);

const formatPostDate = (d) => d ? new Date(d).toLocaleDateString('vi-VN') : '---';
const truncateText = (str, len) => str && str.length > len ? str.substring(0, len) + '...' : str;
const showRecruitDetail = (post) => { selectedRecruit.value = post; };

// === LUCKY WHEEL ===
const showLuckyWheel = ref(false);
const isSpinning = ref(false);
const wheelRotation = ref(0);
const spinResult = ref(null);

const prizes = [
  { label: '5 ĐIỂM', value: 5, type: 'points', deg: 0, color: '#C08A2E' },
  { label: '10 ĐIỂM', value: 10, type: 'points', deg: 60, color: '#33422A' },
  { label: 'GIẢM 5%', value: 5, type: 'discount', deg: 120, color: '#B98229' },
  { label: 'TRƯỢT', value: 0, type: 'miss', deg: 180, color: '#7A7460' },
  { label: 'TẶNG MÓN', value: 0, type: 'gift', deg: 240, color: '#5A6E45' },
  { label: '50 ĐIỂM', value: 50, type: 'points', deg: 300, color: '#22301B' }
];

const openLuckyWheel = async () => {
  if (!isLoggedIn.value) {
    alert("Vui lòng đăng nhập để quay thưởng!");
    router.push('/login');
    return;
  }
  
  showLuckyWheel.value = true;
};

const spinWheel = async () => {
  if (isSpinning.value) return;

  isSpinning.value = true;
  spinResult.value = null;

  let reward;
  try {
    const token = localStorage.getItem('token');
    const response = await api.post('/api/vouchers/spin', null, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    reward = response.data;
  } catch (error) {
    isSpinning.value = false;
    if (error.response?.status === 409) {
      alert('Hôm nay bạn đã quay rồi. Hãy quay lại vào ngày mai nhé!');
    } else if (error.response?.status === 403) {
      alert('Hôm nay bạn cần có hóa đơn đã thanh toán và hoàn tất từ 3.000.000 VNĐ để được quay thưởng.');
    } else {
      alert('Không thể thực hiện vòng quay lúc này. Vui lòng thử lại sau.');
      console.error('Lỗi vòng quay may mắn', error);
    }
    return;
  }

  const prizeIndex = prizes.findIndex((prize) =>
    prize.type === reward.type && prize.value === reward.value
  );
  if (prizeIndex < 0) {
    isSpinning.value = false;
    console.error('Phần thưởng từ máy chủ không khớp cấu hình giao diện', reward);
    alert('Kết quả vòng quay không hợp lệ. Vui lòng liên hệ nhà hàng.');
    return;
  }

  const targetDeg = (360 - prizes[prizeIndex].deg) + 360 * 5; 

  wheelRotation.value += targetDeg;

  setTimeout(() => {
    isSpinning.value = false;
    const won = { ...prizes[prizeIndex], ...reward };
    spinResult.value = won;

    if (won.type === 'points') {
      alert(`🎉 Chúc mừng bạn đã trúng ${won.label}! Điểm hiện tại: ${won.currentPoints} PT (Hạng: ${won.membershipTier})`);
    } else if (won.type === 'discount') {
      alert(`🎉 Chúc mừng bạn đã trúng Giảm ${won.value}%!\nMã Voucher của bạn: ${won.voucherCode}\nHãy lưu lại để dùng cho lần ăn tiếp theo nhé!`);
    } else if (won.type === 'gift') {
      alert(`🎉 Chúc mừng bạn đã trúng ${won.label}! Vui lòng đưa thông báo này cho nhân viên để nhận quà nhé!`);
    }
  }, 7000); // 7 seconds
};

// === LIKE POSTS ===
const likedPosts = ref(JSON.parse(localStorage.getItem('likedPosts') || '[]'));
const isLiked = (id) => likedPosts.value.includes(id);
const likePost = async (post) => {
  if (isLiked(post.id)) return;
  try {
    await api.put(`/api/posts/${post.id}/like`);
    post.likes = (post.likes || 0) + 1;
    likedPosts.value.push(post.id);
    localStorage.setItem('likedPosts', JSON.stringify(likedPosts.value));
  } catch (err) { console.error('Like failed', err); }
};

// === APPLICATION FORM ===
const showAppForm = ref(false);
const appForm = ref({ fullname: '', phone: '', email: '', message: '', postId: null, cvFile: null });
const openApplicationForm = () => {
  showAppForm.value = true;
  appForm.value = { fullname: '', phone: '', email: '', message: '', postId: selectedRecruit.value?.id };
};
const openApplicationFormFromInterview = () => {
  showInterviewChat.value = false;
  openApplicationForm();
};
const handleCvUpload = (event) => {
  if (event.target.files && event.target.files[0]) {
    appForm.value.cvFile = event.target.files[0];
  }
};

const submitApplication = async () => {
  if (!appForm.value.fullname || !appForm.value.phone) return alert('Vui lòng nhập họ tên và SĐT!');
  try {
    const formData = new FormData();
    formData.append('fullname', appForm.value.fullname);
    formData.append('phone', appForm.value.phone);
    if (appForm.value.email) formData.append('email', appForm.value.email);
    if (appForm.value.message) formData.append('message', appForm.value.message);
    if (appForm.value.postId) formData.append('postId', appForm.value.postId);
    if (appForm.value.cvFile) formData.append('file', appForm.value.cvFile);

    await api.post('/api/applications/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    alert('Đã gửi đơn ứng tuyển thành công! Nhà hàng sẽ liên hệ sớm nhất.');
    showAppForm.value = false;
  } catch (err) { alert('Lỗi: ' + (err.response?.data || err.message)); }
};

// === SUPPORT CHATBOT ===
const { locale } = useI18n()
const showSupportChat = ref(false);
const supportInput = ref('');
const supportMessages = ref([
  { type: 'bot', text: 'Chào bạn! Mình là trợ lý ảo của MỘC VỊ RESTAURANT. Mình có thể giúp gì cho bạn? (VD: giờ mở cửa, đặt bàn, địa chỉ)' }
]);
const supportChatBody = ref(null);

const isSupportTyping = ref(false);

const toggleSupportChat = () => {
  showSupportChat.value = !showSupportChat.value;
  if (showSupportChat.value) setTimeout(scrollToBottomSupport, 100);
};

const scrollToBottomSupport = () => {
  setTimeout(() => {
    if (supportChatBody.value) supportChatBody.value.scrollTop = supportChatBody.value.scrollHeight;
  }, 100);
};

const sendSupportMessage = async () => {
  const text = supportInput.value.trim();
  if (!text) return;
  
  supportMessages.value.push({ type: 'user', text });
  supportInput.value = '';
  isSupportTyping.value = true;
  scrollToBottomSupport();

  try {
    let historyStr = '';
    for (let i = 0; i < supportMessages.value.length - 1; i++) {
      const m = supportMessages.value[i];
      if (!m.isTableBooking) {
        historyStr += `${m.type === 'bot' ? 'Bot' : 'Khách'}: ${m.text}\n`;
      }
    }

    const res = await api.post('/api/chatbot/chat', { 
      message: text,
      type: 'SUPPORT',
      history: historyStr,
      locale: locale.value
    });
    
    let reply = res.data.reply || '';
    
    // Check for Booking Tag
    const bookRegex = /\[ACTION:BOOK_TABLE\|time=([^|]+)\|pax=([^|]+)\|view=([^\]]+)\]/i;
    const match = reply.match(bookRegex);
    
    // Check for Show Menu Tag
    const menuRegex = /\[ACTION:SHOW_MENU\]/i;
    const menuMatch = reply.match(menuRegex);

    if (match) {
      const time = match[1];
      const pax = parseInt(match[2]);
      const view = match[3];

      // Fetch tables
      const tRes = await api.get('/api/tables');
      
      const available = tRes.data.filter(t => {
        if (t.isOccupied !== 0) return false;
        if (t.capacity && t.capacity < pax) return false; // Sức chứa phải >= số người
        
        // Nếu khách yêu cầu view cụ thể (phố, sông, sân vườn)
        if (view && view.toLowerCase() !== 'không' && view.toLowerCase() !== 'none') {
           // Nếu bàn không có viewType hoặc không khớp từ khóa view
           const v = view.toLowerCase();
           if (!t.viewType || (!t.viewType.toLowerCase().includes('phố') && v.includes('phố')) && (!t.viewType.toLowerCase().includes('sông') && v.includes('sông')) && (!t.viewType.toLowerCase().includes('vườn') && v.includes('vườn'))) {
             // Bỏ qua lọc view khắt khe để tránh báo hết bàn nếu AI phân tích sai, nhưng nếu AI bảo rõ view thì ưu tiên lọc
             if (t.viewType) {
               return t.viewType.toLowerCase().includes(v.replace('view ', ''));
             }
           }
        }
        return true;
      });

      supportMessages.value.push({ 
        type: 'bot', 
        isTableBooking: true,
        bookingState: {
          time, pax, view,
          tables: available.slice(0, 3), // Show max 3 tables
          tableConfirmed: false,
          selectedTable: null,
          paymentMethod: null,
          guestName: '', guestPhone: '',
          paid: false, orderCode: ''
        }
      });
    } else if (menuMatch) {
      // Remove the tag from the text
      const cleanReply = reply.replace(menuRegex, '').trim();
      supportMessages.value.push({ type: 'bot', text: cleanReply });
      supportMessages.value.push({ type: 'bot', isMenuLink: true });
    } else {
      supportMessages.value.push({ type: 'bot', text: reply });
    }
  } catch (err) {
    supportMessages.value.push({ type: 'bot', text: 'Xin lỗi, AI hiện đang mất kết nối. Vui lòng gọi Hotline để được hỗ trợ!' });
  } finally {
    isSupportTyping.value = false;
    scrollToBottomSupport();
  }
};

const selectBookingTable = (msg, table) => {
  msg.bookingState.selectedTable = table;
  msg.bookingState.tableConfirmed = true;
  scrollToBottomSupport();
};

const choosePaymentMethod = (msg, method) => {
  msg.bookingState.paymentMethod = method;
  if (method === 'món') {
    localStorage.setItem('bookedTable', JSON.stringify({
      id: msg.bookingState.selectedTable.id,
      name: msg.bookingState.selectedTable.name,
      time: msg.bookingState.time
    }));
    setTimeout(() => {
      router.push('/dine-in');
    }, 1500);
  }
  scrollToBottomSupport();
};

const continueSecureReservation = () => {
  router.push('/reservation');
};

// === INTERVIEW CHATBOT ===
const showInterviewChat = ref(false);
const interviewInput = ref('');
const interviewStep = ref(0);
const interviewMessages = ref([]);
const interviewChatBody = ref(null);

const interviewQuestions = [
  "Chào bạn, rất vui vì bạn quan tâm đến MỘC VỊ RESTAURANT. Bạn hãy giới thiệu ngắn gọn về bản thân nhé?",
  "Cảm ơn bạn! Bạn đã có kinh nghiệm làm việc trong lĩnh vực F&B (Nhà hàng/Cafe) chưa?",
  "Rất tốt! Vậy tại sao bạn lại muốn ứng tuyển vào vị trí này tại MỘC VỊ RESTAURANT?",
  "Nếu gặp một khách hàng khó tính phàn nàn về món ăn, bạn sẽ xử lý như thế nào?",
  "Câu hỏi cuối cùng: Bạn có thể làm việc xoay ca (sáng/tối) không?"
];

const startInterview = () => {
  selectedRecruit.value = null;
  showInterviewChat.value = true;
  interviewMessages.value = [];
  setTimeout(() => {
    interviewMessages.value.push({ type: 'bot', text: "Chào bạn, mình là Giám đốc Nhân sự của MỘC VỊ RESTAURANT. Rất vui được trao đổi với bạn hôm nay! Bạn có thể giới thiệu sơ qua về bản thân và vị trí mà bạn mong muốn ứng tuyển được không?" });
  }, 500);
};

const scrollInterview = () => {
  setTimeout(() => {
    if (interviewChatBody.value) interviewChatBody.value.scrollTop = interviewChatBody.value.scrollHeight;
  }, 50);
};

const sendInterviewMessage = async () => {
  const text = interviewInput.value.trim();
  if (!text) return;
  interviewMessages.value.push({ type: 'user', text });
  interviewInput.value = '';
  scrollInterview();

  interviewMessages.value.push({ type: 'bot', text: 'Đang suy nghĩ...', isTyping: true });
  scrollInterview();

  try {
    let historyStr = '';
    for (let i = 0; i < interviewMessages.value.length - 2; i++) {
      const msg = interviewMessages.value[i];
      historyStr += `${msg.type === 'bot' ? 'HR' : 'Ứng viên'}: ${msg.text}\n`;
    }

    const res = await api.post('/api/chatbot/chat', {
      message: text,
      type: 'INTERVIEW',
      history: historyStr
    });
    
    interviewMessages.value.pop(); // Xóa chữ 'Đang suy nghĩ...'
    interviewMessages.value.push({ type: 'bot', text: res.data.reply });
    scrollInterview();
  } catch (err) {
    interviewMessages.value.pop();
    interviewMessages.value.push({ type: 'bot', text: 'Xin lỗi, hệ thống AI tuyển dụng đang quá tải. Hãy để lại thông tin của bạn!' });
    scrollInterview();
  }
};

onMounted(async () => {
  const token = localStorage.getItem('token');
  const storedUser = localStorage.getItem('user');
  if (token && storedUser) {
    isLoggedIn.value = true;
    user.value = JSON.parse(storedUser);
  }

  // Fetch posts
  try {
    const [newsRes, recruitRes] = await Promise.all([
      api.get('/api/posts/news'),
      api.get('/api/posts/recruitment')
    ]);
    newsPosts.value = newsRes.data.slice(0, 6); // Max 6 tin
    recruitPosts.value = recruitRes.data.slice(0, 5); // Max 5 tuyển dụng
  } catch (err) { /* silent - posts are optional */ }
});

</script>

<style scoped>
.home-wrapper {
  background: var(--bg-root);
  min-height: 100vh;
  font-family: var(--font-primary);
}

/* ===== NAVBAR ===== */
/* ===== NAVBAR ===== */
.navbar {
  position: absolute;
  top: 0; left: 0; right: 0;
  z-index: 100;
  padding: 20px 40px;
}
.nav-container {
  max-width: 1400px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
  background: rgba(245, 243, 234, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 220, 198, 0.75);
  border-radius: 100px;
  padding: 12px 30px;
  box-shadow: var(--shadow-md);
}

.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; text-decoration: none; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo-text h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo-text h2 span { color: var(--primary); }
.logo-text p { margin: 0; font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover, .nav-links a.router-link-active, .nav-links a.active {
  color: var(--primary); background: rgba(90, 110, 69, 0.1);
}

.nav-right { display: flex; align-items: center; gap: 12px; }

.lang-switch {
  background: rgba(231, 239, 233, 0.55);
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  outline: none;
  font-family: inherit;
  font-weight: 600;
  font-size: 0.85rem;
  transition: var(--transition);
}
.lang-switch:hover { border-color: var(--primary); }

.btn-nav {
  background: transparent; color: var(--text-secondary);
  border: 1px solid var(--border); padding: 7px 14px;
  border-radius: 20px; cursor: pointer;
  font-size: 0.83rem; font-weight: 600; font-family: inherit;
  transition: var(--transition);
}
.btn-nav:hover { border-color: var(--primary); color: var(--primary); }
.btn-nav-filled { background: var(--primary); color: #FFFFFF; border-color: var(--primary); font-weight: 700; }
.btn-nav-filled:hover { background: var(--primary-dark); border-color: var(--primary-dark); color: #FFFFFF; }
.btn-admin { border-color: rgba(192, 138, 46, 0.45); color: var(--secondary); }
.btn-admin:hover { background: rgba(192, 138, 46, 0.14); }
.btn-kitchen { border-color: rgba(90, 110, 69, 0.4); color: var(--primary); }
.btn-waiter { border-color: rgba(90, 110, 69, 0.45); color: #5A6E45; }
.btn-logout-nav { border-color: rgba(178,59,46,0.4); color: #B23B2E; }
.btn-logout-nav:hover { background: rgba(178,59,46,0.15); }

/* ===== HERO ===== */
.hero-banner {
  position: relative;
  background: linear-gradient(135deg, #33422A 0%, #22301B 100%);
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  overflow: hidden;
}
.hero-overlay {
  position: absolute; inset: 0;
  background:
    radial-gradient(circle at 30% 20%, rgba(242,196,109,0.18) 0%, transparent 55%),
    linear-gradient(180deg, rgba(20,28,15,0.10), rgba(20,28,15,0.34));
}
.hero-content {
  position: relative; z-index: 1;
  padding: 0 20px; max-width: 700px;
}
.hero-tag {
  display: inline-block;
  background: rgba(242, 196, 109, 0.14);
  border: 1px solid rgba(242, 196, 109, 0.42);
  color: #F7D98B;
  padding: 6px 20px; border-radius: 20px;
  font-size: 0.83rem; font-weight: 600; letter-spacing: 1px;
  margin-bottom: 24px;
  text-shadow: 0 2px 10px rgba(0,0,0,0.35);
}
.hero-content h1 {
  font-size: 4rem; font-weight: 900;
  color: #FFFFFF; line-height: 1.1;
  margin: 0 0 16px 0;
  text-shadow: 0 4px 20px rgba(0,0,0,0.5);
}
.hero-content h1 span {
  color: #F2C46D;
  text-shadow: 0 4px 22px rgba(0,0,0,0.55);
}
.hero-content p {
  color: rgba(255,255,255,0.90); font-size: 1.1rem;
  margin: 0 0 32px 0; letter-spacing: 0.5px;
  text-shadow: 0 3px 14px rgba(0,0,0,0.45);
}

.hero-actions { display: flex; gap: 14px; justify-content: center; flex-wrap: wrap; margin-bottom: 48px; }
.home-hero-btn {
  min-height: 48px;
  padding: 14px 30px;
  border-radius: 30px;
  font-size: 1rem;
}
.home-hero-outline {
  background: rgba(255,255,255,0.08); color: #FFFFFF;
  border: 2px solid rgba(255,255,255,0.55);
  backdrop-filter: blur(10px);
}
.home-hero-outline:hover { background: rgba(255,255,255,0.18); border-color: #FFFFFF; color: #FFFFFF; }

/* Stats */
.hero-stats {
  display: inline-flex; align-items: center; gap: 0;
  background: rgba(20, 28, 15, 0.66);
  border: 1px solid rgba(242, 196, 109, 0.28);
  border-radius: 16px; padding: 16px 32px;
  backdrop-filter: blur(15px);
}
.stat-item { text-align: center; padding: 0 20px; }
.stat-num { display: block; font-size: 1.6rem; font-weight: 900; color: #F2C46D; }
.stat-lbl { font-size: 0.78rem; color: rgba(255,255,255,0.88); letter-spacing: 0.5px; }
.stat-divider { width: 1px; height: 40px; background: rgba(255,255,255,0.22); }

/* ===== MAIN CONTENT ===== */
.main-content { padding: 80px 24px; }
.content-container {
  max-width: 1200px; margin: 0 auto;
  display: grid; grid-template-columns: 1fr 360px; gap: 60px;
}

/* Left col */
.left-col {}
.meta-info { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.meta-tag {
  background: var(--primary-glow); color: var(--primary);
  border: 1px solid var(--border); padding: 4px 12px;
  border-radius: 20px; font-size: 0.78rem; font-weight: 700;
}
.meta-date { color: var(--text-muted); font-size: 0.83rem; }

.article-title {
  font-size: 2.1rem; font-weight: 900; color: var(--text-heading);
  line-height: 1.3; margin: 0 0 20px 0;
}
.article-excerpt {
  font-size: 1.05rem; color: var(--text-secondary);
  line-height: 1.8; margin: 0 0 28px 0;
}

.article-actions { display: flex; gap: 14px; margin-bottom: 48px; flex-wrap: wrap; }
.home-action-btn { min-height: 44px; padding: 12px 26px; }

/* Feature Grid */
.feature-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.feature-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
  transition: var(--transition);
}
.feature-card:hover { border-color: var(--border); transform: translateY(-3px); box-shadow: var(--shadow-md); }
.feature-icon { font-size: 1.8rem; margin-bottom: 10px; }
.feature-card h4 { margin: 0 0 6px 0; color: var(--text-heading); font-size: 0.9rem; font-weight: 700; }
.feature-card p { margin: 0; color: var(--text-muted); font-size: 0.8rem; line-height: 1.5; }

/* Right col - Sidebar */
.right-col { display: flex; flex-direction: column; gap: 20px; }
.sidebar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.sidebar-card h3 {
  margin: 0 0 18px 0; font-size: 1rem; font-weight: 700;
  color: var(--text-heading); padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.service-list { list-style: none; padding: 0; margin: 0; }
.service-list li { margin-bottom: 4px; }
.service-list a {
  display: flex; align-items: center; gap: 10px;
  color: var(--text-secondary); text-decoration: none;
  padding: 10px 12px; border-radius: var(--radius-md);
  font-size: 0.9rem; transition: var(--transition);
}
.service-list a:hover { background: var(--primary-glow2); color: var(--primary); }
.svc-icon { font-size: 1.1rem; }
.service-list a span:nth-child(2) { flex: 1; }
.svc-badge {
  font-size: 0.7rem; font-weight: 700; padding: 2px 8px; border-radius: 10px;
}
.svc-badge.new { background: rgba(90, 110, 69, 0.15); color: var(--primary); }
.svc-badge.hot { background: rgba(178,59,46,0.15); color: #B23B2E; }

/* Hours */
.hours-card {}
.hours-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.hours-item { display: flex; justify-content: space-between; align-items: center; }
.hours-item span:first-child { color: var(--text-muted); font-size: 0.88rem; }
.hours-time { color: var(--text-heading); font-weight: 700; font-size: 0.9rem; }
.open-status {
  display: flex; align-items: center; gap: 8px;
  color: var(--primary); font-size: 0.85rem; font-weight: 700;
}
.open-dot {
  width: 8px; height: 8px; background: var(--primary); border-radius: 50%;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* FAB */
.fab-phone {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF;
}
.msg-booking {
  background: transparent !important;
  border: 1px solid var(--border-light);
  width: 100%;
}
.booking-widget { display: flex; flex-direction: column; gap: 10px; font-size: 0.85rem; }
.b-table-list { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.b-table-card { background: var(--bg-card); border: 1px solid var(--border); padding: 8px 12px; border-radius: 6px; }
.b-table-card strong { color: var(--primary); }
.b-btn-small { background: var(--primary); border: none; color: #FFFFFF; padding: 4px 10px; border-radius: 4px; font-size: 0.75rem; cursor: pointer; margin-top: 6px; font-weight: bold; }
.b-actions { display: flex; gap: 6px; margin-top: 8px; }
.b-btn { background: var(--primary); border: none; color: #FFFFFF; padding: 8px; border-radius: 6px; cursor: pointer; font-weight: bold; flex: 1; text-align: center; font-size: 0.8rem; }
.b-btn-outline { background: transparent; border: 1px solid var(--primary); color: var(--primary); padding: 8px; border-radius: 6px; cursor: pointer; font-weight: bold; flex: 1; text-align: center; font-size: 0.8rem; }
.b-input { background: var(--bg-input); border: 1px solid var(--border); color: var(--text-heading); padding: 8px; border-radius: 4px; width: 100%; box-sizing: border-box; }
.b-qr-box { background: #FFFFFF; padding: 10px; border-radius: 8px; text-align: center; margin-top: 8px; }
.b-qr-box img { width: 100%; max-width: 150px; border-radius: 6px; }
.b-qr-box p { color: var(--text-heading); font-weight: bold; margin: 5px 0 10px 0; }
.b-success { background: rgba(90, 110, 69, 0.1); border: 1px solid var(--primary); padding: 12px; border-radius: 6px; color: var(--primary); font-weight: bold; text-align: center; }

.fab-phone {
  width: 60px; height: 60px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.6rem; cursor: pointer;
  box-shadow: 0 6px 25px rgba(90, 110, 69, 0.4);
  animation: fab-pulse 2.5s ease-in-out infinite;
  transition: var(--transition);
}
.fab-phone:hover { transform: scale(1.1); box-shadow: 0 8px 30px rgba(90, 110, 69, 0.6); }
@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 6px 25px rgba(90, 110, 69, 0.4); }
  50% { box-shadow: 0 6px 25px rgba(90, 110, 69, 0.4), 0 0 0 15px rgba(90, 110, 69, 0); }
}

/* ===== POSTS SECTIONS ===== */
.posts-section {
  padding: 80px 24px;
  border-top: 1px solid var(--border-light);
}
.section-container { max-width: 1200px; margin: 0 auto; }
.section-header-block { text-align: center; margin-bottom: 48px; }
.section-tag {
  display: inline-block; background: var(--primary-glow);
  border: 1px solid var(--border); color: var(--primary);
  padding: 6px 20px; border-radius: 20px;
  font-size: 0.83rem; font-weight: 600; letter-spacing: 1px;
  margin-bottom: 16px;
}
.recruit-tag { background: rgba(192, 138, 46, 0.1); color: #C08A2E; border-color: rgba(192, 138, 46, 0.3); }
.section-header-block h2 {
  font-size: 2.2rem; font-weight: 900; color: var(--text-heading);
  margin: 0 0 12px 0;
}
.section-header-block p { color: var(--text-muted); font-size: 1rem; margin: 0; }

/* News Grid */
.news-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px;
}
.news-card {
  background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: var(--radius-lg); overflow: hidden;
  transition: var(--transition);
}
.news-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); border-color: var(--border); }
.news-img-wrap {
  position: relative; height: 200px; overflow: hidden;
}
.news-img-wrap img { width: 100%; height: 100%; object-fit: cover; }
.news-img-placeholder {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  background: var(--bg-card2); font-size: 3rem;
}
.news-date-badge {
  position: absolute; top: 12px; right: 12px;
  background: var(--overlay-darker); color: #FFFFFF;
  padding: 4px 12px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600; backdrop-filter: blur(10px);
}
.news-body { padding: 20px; }
.news-body h3 {
  margin: 0 0 10px 0; font-size: 1.05rem; font-weight: 800;
  color: var(--text-heading); line-height: 1.4;
}
.news-body p { margin: 0; font-size: 0.88rem; color: var(--text-muted); line-height: 1.6; }

/* Recruit List */
.recruit-section { background: var(--bg-card2); }
.recruit-list { display: flex; flex-direction: column; gap: 16px; }
.recruit-card {
  display: flex; align-items: center; gap: 20px;
  padding: 24px; background: var(--bg-card);
  border: 1px solid var(--border-light); border-radius: var(--radius-lg);
  transition: var(--transition);
}
.recruit-card:hover { border-color: rgba(192, 138, 46, 0.4); transform: translateX(6px); box-shadow: var(--shadow-md); }
.recruit-icon {
  font-size: 2rem; width: 60px; height: 60px; flex-shrink: 0;
  background: rgba(192, 138, 46, 0.1); border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
}
.recruit-info { flex: 1; min-width: 0; }
.recruit-info h4 { margin: 0 0 6px 0; font-size: 1.1rem; font-weight: 800; color: var(--text-heading); }
.recruit-info p { margin: 0 0 8px 0; font-size: 0.88rem; color: var(--text-muted); line-height: 1.5; }
.recruit-date { font-size: 0.78rem; color: var(--text-muted); }
.btn-apply {
  background: rgba(192, 138, 46, 0.15); border: 1px solid rgba(192, 138, 46, 0.3);
  color: #C08A2E; padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.88rem; font-family: inherit;
  white-space: nowrap; transition: var(--transition); flex-shrink: 0;
}
.btn-apply:hover { background: #C08A2E; color: #FFFFFF; }

/* Recruit Modal */
.modal-overlay {
  position: fixed; inset: 0; background: var(--overlay-darker);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; backdrop-filter: blur(6px); padding: 20px;
}
.recruit-modal {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-xl); width: 100%; max-width: 700px;
  max-height: 85vh; overflow-y: auto; box-shadow: var(--shadow-lg);
}
.modal-top {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px; border-bottom: 1px solid var(--border-light);
}
.modal-top h2 { margin: 0; font-size: 1.2rem; font-weight: 800; color: var(--text-heading); }
.btn-close-modal {
  background: transparent; border: none; color: var(--text-muted);
  font-size: 1.3rem; cursor: pointer; transition: var(--transition);
}
.btn-close-modal:hover { color: var(--secondary); }
.modal-img { width: 100%; max-height: 300px; object-fit: cover; }
.modal-content { padding: 24px; }
.modal-text {
  white-space: pre-wrap; word-wrap: break-word; font-family: var(--font-primary);
  color: var(--text-secondary); font-size: 0.95rem; line-height: 1.8; margin: 0 0 16px 0;
}
.modal-date { color: var(--text-muted); font-size: 0.85rem; margin: 0; }

/* ===== APP FORM MODAL ===== */
.app-modal {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-xl); width: 100%; max-width: 500px;
  padding: 30px; box-shadow: var(--shadow-lg);
}
.app-modal h3 { margin: 0 0 5px 0; font-size: 1.5rem; color: var(--primary); }
.app-subtitle { color: var(--text-muted); margin: 0 0 20px 0; font-size: 0.9rem; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); margin-bottom: 6px; }
.g-input {
  width: 100%; padding: 12px; background: var(--bg-input);
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  color: var(--text-heading); font-family: inherit; font-size: 0.95rem;
  transition: var(--transition);
}
.g-input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(90, 110, 69, 0.1); }
.app-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-submit-app {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF; border: none; padding: 14px; border-radius: var(--radius-md);
  font-weight: 800; font-size: 1rem; cursor: pointer; transition: var(--transition);
}
.btn-submit-app:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(90, 110, 69, 0.4); }
.btn-close {
  background: transparent; border: 1px solid var(--border);
  color: var(--text-muted); padding: 12px; border-radius: var(--radius-md);
  font-weight: 600; cursor: pointer; transition: var(--transition);
}
.btn-close:hover { border-color: var(--secondary); color: var(--secondary); }
.w-100 { width: 100%; }

/* LIKES & INTERVIEW BTN */
.news-footer { margin-top: 16px; display: flex; justify-content: flex-end; }
.recruit-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.btn-like {
  background: rgba(192, 138, 46, 0.1); border: 1px solid rgba(192, 138, 46, 0.24);
  color: var(--secondary); padding: 6px 14px; border-radius: 20px;
  cursor: pointer; font-weight: 700; font-size: 0.85rem;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.btn-like:hover { background: rgba(192, 138, 46, 0.2); transform: scale(1.05); }
.btn-like.liked { background: var(--secondary); color: #FFFFFF; border-color: var(--secondary); animation: heartBeat 0.5s; }
.btn-like-small { padding: 4px 10px; font-size: 0.8rem; }
@keyframes heartBeat {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}
.modal-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border-light); }
.modal-actions { display: flex; gap: 12px; }
.btn-interview {
  background: rgba(192, 138, 46, 0.12); border: 1px solid rgba(192, 138, 46, 0.3);
  color: var(--secondary); padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.9rem; transition: var(--transition);
}
.btn-interview:hover { background: var(--secondary); color: #FFFFFF; }

/* ===== CHATBOTS ===== */
.chatbots-container { position: fixed; bottom: 30px; right: 30px; z-index: 999; display: flex; flex-direction: column; align-items: flex-end; gap: 16px; }
.fab-group { display: flex; gap: 12px; }
.fab-chat {
  width: 60px; height: 60px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF; font-size: 1.8rem; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 6px 20px rgba(90, 110, 69, 0.4);
  transition: transform 0.3s;
}
.fab-chat:hover { transform: scale(1.1); }

.chat-widget {
  width: 350px; height: 500px; background: var(--bg-card);
  border-radius: 20px; border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  display: flex; flex-direction: column; overflow: hidden;
  animation: slideUp 0.3s ease-out;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

.chat-header {
  padding: 16px; display: flex; justify-content: space-between; align-items: center;
  color: #FFFFFF; font-weight: 700;
}
.support-header { background: linear-gradient(135deg, var(--primary), var(--primary-dark)); }
.interview-header { background: linear-gradient(135deg, #C08A2E, #8A641F); }
.chat-header button { background: transparent; border: none; color: #FFFFFF; font-size: 1.2rem; cursor: pointer; }

.chat-body {
  flex: 1; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px;
  background: var(--bg-root);
}
.chat-msg { max-width: 80%; padding: 12px 16px; border-radius: 18px; font-size: 0.9rem; line-height: 1.4; word-wrap: break-word; }
.chat-msg.bot { background: var(--bg-input); border: 1px solid var(--border-light); color: var(--text-heading); border-bottom-left-radius: 4px; align-self: flex-start; }
.chat-msg.user { background: var(--primary); color: #FFFFFF; border-bottom-right-radius: 4px; align-self: flex-end; font-weight: 600; }

.chat-input-area {
  padding: 12px; border-top: 1px solid var(--border-light); display: flex; gap: 8px; background: var(--bg-card);
}
.chat-input-area input {
  flex: 1; padding: 10px 14px; border-radius: 20px; border: 1px solid var(--border-light);
  background: var(--bg-input); color: var(--text-heading); font-family: inherit;
}
.chat-input-area input:focus { outline: none; border-color: var(--primary); }
.chat-input-area button {
  background: var(--primary); color: #FFFFFF; border: none;
  padding: 0 16px; border-radius: 20px; font-weight: 700; cursor: pointer; transition: 0.3s;
}
.chat-input-area button:hover { background: var(--primary-dark); }
.chat-done-area { padding: 16px; }

.typing-indicator {
  font-style: italic; color: var(--text-muted) !important;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { opacity: 0.5; }
  50% { opacity: 1; }
  100% { opacity: 0.5; }
}

.fab-wheel { background: linear-gradient(45deg, #B98229, #C08A2E); font-size: 1.5rem; display:flex; align-items:center; justify-content:center; box-shadow: 0 5px 20px rgba(185,130,41,0.5); }
.fab-wheel:hover { transform: scale(1.15) rotate(10deg); }

/* Lucky Wheel */
.wheel-modal { background: var(--bg-card); padding: 30px; border-radius: 20px; width: 400px; max-width: 90%; text-align: center; border: 1px solid var(--border); box-shadow: var(--shadow-lg); }
.wheel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.wheel-header h2 { color: #B98229; margin: 0; font-size: 1.5rem; text-transform: uppercase; font-weight: 900; }
.wheel-desc { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 20px; }
.wheel-container { position: relative; width: 300px; height: 300px; margin: 0 auto 30px; border-radius: 50%; box-shadow: 0 0 20px rgba(185,130,41,0.2); border: 8px solid var(--bg-root); overflow: hidden; }
.wheel-pointer { position: absolute; top: -10px; left: 50%; transform: translateX(-50%); font-size: 2rem; color: var(--secondary); z-index: 10; text-shadow: 0 2px 5px rgba(0,0,0,0.35); }
.wheel-board { width: 100%; height: 100%; border-radius: 50%; position: relative; transition: transform 7s cubic-bezier(0.1, 0.8, 0.1, 1); }
.wheel-text { position: absolute; width: 100%; height: 100%; top: 0; left: 0; display: flex; justify-content: center; align-items: flex-start; padding-top: 15px; transform-origin: 50% 50%; }
.slice-label { font-weight: 900; font-size: 1.1rem; text-shadow: 1px 1px 3px rgba(0,0,0,0.8); }

.btn-spin { background: linear-gradient(45deg, #B98229, #C08A2E); color: #FFFFFF; font-size: 1.2rem; font-weight: 900; border: none; padding: 15px 40px; border-radius: 30px; cursor: pointer; transition: 0.3s; box-shadow: 0 5px 15px rgba(192, 138, 46, 0.4); }
.btn-spin:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(192, 138, 46, 0.6); }
.btn-spin:disabled { opacity: 0.7; cursor: not-allowed; background: #7A7460; box-shadow: none; }
.spin-result { margin-top: 20px; font-size: 1.1rem; padding: 10px; border-radius: 8px; background: var(--primary-glow2); }
.spin-result.win { background: var(--primary-glow2); color: var(--primary); font-weight: bold; border: 1px solid var(--primary-glow); }

/* Animation Utils */
@media (max-width: 1024px) {
  .hero-banner { min-height: 560px; }
  .hero-content h1 { font-size: 3.2rem; }
  .main-content,
  .posts-section { padding: 64px 24px; }
  .content-container { grid-template-columns: 1fr; gap: 40px; }
  .right-col { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .news-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chatbots-container { right: 20px; bottom: 20px; }
}

@media (max-width: 640px) {
  .home-wrapper { overflow-x: hidden; }
  .home-wrapper,
  .home-wrapper * { box-sizing: border-box; }
  .hero-content,
  .content-container,
  .left-col,
  .right-col,
  .section-container { min-width: 0; }
  .hero-banner { min-height: 600px; padding: 104px 16px 40px; }
  .hero-content {
    flex: 0 1 calc(100vw - 32px);
    width: calc(100vw - 32px);
    max-width: calc(100vw - 32px);
    padding: 0;
  }
  .hero-tag {
    display: block;
    width: 100%;
    max-width: 100%;
    margin-bottom: 18px;
    padding: 6px 10px;
    white-space: normal;
    overflow-wrap: break-word;
    font-size: 0.76rem;
    line-height: 1.4;
  }
  .hero-content h1 { font-size: 2.45rem; line-height: 1.12; }
  .hero-content p { max-width: 100%; font-size: 1rem; line-height: 1.5; margin-bottom: 26px; overflow-wrap: break-word; }
  .hero-actions { flex-direction: column; margin-bottom: 28px; }
  .hero-actions button,
  .article-actions button,
  .btn-apply,
  .btn-submit-app,
  .btn-close,
  .btn-interview,
  .btn-spin { min-height: 44px; }
  .hero-actions button { width: 100%; padding: 12px 18px; }
  .hero-stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
    max-width: 100%;
    padding: 14px 6px;
  }
  .stat-item { padding: 0 6px; }
  .stat-num { font-size: 1.3rem; }
  .stat-lbl { display: block; font-size: 0.68rem; }
  .stat-divider { display: none; }
  .main-content,
  .posts-section { padding: 48px 16px; }
  .content-container {
    width: calc(100vw - 32px);
    max-width: calc(100vw - 32px);
    gap: 32px;
  }
  .left-col { width: 100%; max-width: 100%; }
  .meta-info { flex-wrap: wrap; }
  .article-title,
  .section-header-block h2 { font-size: 1.65rem; }
  .article-title { overflow-wrap: anywhere; }
  .article-excerpt { font-size: 0.98rem; line-height: 1.7; }
  .article-actions { flex-direction: column; margin-bottom: 32px; }
  .article-actions button { width: 100%; }
  .feature-grid,
  .right-col,
  .news-grid { grid-template-columns: 1fr; }
  .sidebar-card { padding: 20px; }
  .section-header-block { margin-bottom: 32px; }
  .recruit-card { align-items: flex-start; flex-wrap: wrap; padding: 20px; }
  .recruit-icon { width: 48px; height: 48px; }
  .recruit-info { min-width: calc(100% - 68px); }
  .recruit-meta,
  .modal-footer { align-items: flex-start; flex-direction: column; gap: 12px; }
  .btn-apply { width: 100%; }
  .modal-top,
  .modal-content { padding: 18px; }
  .modal-actions { flex-direction: column; width: 100%; }
  .modal-actions button { width: 100%; }
  .app-modal { padding: 22px 18px; }
  .chatbots-container { right: 12px; bottom: 12px; left: 12px; max-width: calc(100vw - 24px); }
  .chat-widget { width: 100%; height: min(500px, 72vh); }
  .fab-group { align-self: stretch; justify-content: flex-end; flex-wrap: wrap; max-width: 100%; }
  .fab-chat,
  .fab-phone { width: 52px; height: 52px; }
  .wheel-modal { padding: 22px 14px; }
  .wheel-container { width: 260px; height: 260px; }
}
</style>
