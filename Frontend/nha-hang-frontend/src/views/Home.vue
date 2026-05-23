<template>
  <div class="home-wrapper">
    <!-- Navbar -->
    <header class="navbar">
      <div class="nav-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🍽️</span>
          <div class="logo-text">
            <h2>NHÀ HÀNG FPOLY</h2>
            <p>ĐÀ NẴNG</p>
          </div>
        </div>

        <nav class="nav-links">
          <a href="#" class="active">Trang chủ</a>
          <router-link to="/menu">Thực đơn</router-link>
          <router-link to="/reservation">Đặt chỗ</router-link>
          <router-link to="/dine-in">Tại bàn</router-link>
        </nav>

        <div class="nav-right">
          <template v-if="!isLoggedIn">
            <button @click="$router.push('/login')" class="btn-nav">Đăng nhập</button>
            <button @click="$router.push('/register')" class="btn-nav btn-nav-filled">Đăng ký</button>
          </template>

          <template v-else>
            <button @click="$router.push('/history')" class="btn-nav">📜 Lịch sử</button>

            <button
              v-if="user && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_MANAGER'))"
              @click="$router.push('/admin')"
              class="btn-nav btn-admin"
            >⚙️ Admin</button>

            <button
              v-if="user && user.roles.includes('ROLE_KITCHEN')"
              @click="$router.push('/kitchen')"
              class="btn-nav btn-kitchen"
            >👨‍🍳 Bếp</button>

            <button
              v-if="user && user.roles.includes('ROLE_WAITER')"
              @click="$router.push('/waiter')"
              class="btn-nav btn-waiter"
            >🏃 Phục vụ</button>

            <button @click="handleLogout" class="btn-nav btn-logout-nav">Đăng xuất</button>
          </template>
        </div>
      </div>
    </header>

    <!-- Hero Banner -->
    <section class="hero-banner">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-tag">✦ Trải nghiệm ẩm thực đỉnh cao</div>
        <h1>Nhà Hàng<br><span>FPOLY</span></h1>
        <p>Không gian đẳng cấp · Hương vị tinh tế · Phục vụ tận tâm</p>
        <div class="hero-actions">
          <button @click="$router.push('/dine-in')" class="btn-hero-primary">
            🍽️ Gọi Món Tại Quán
          </button>
          <button @click="$router.push('/reservation')" class="btn-hero-secondary">
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
          <h2 class="article-title">Trải nghiệm ẩm thực Việt tại nhà hàng FPoly Đà Nẵng</h2>
          <p class="article-excerpt">
            Với không gian thoáng đãng, đậm chất kiến trúc truyền thống và thực đơn phong phú
            quy tụ tinh hoa ẩm thực ba miền, nhà hàng chúng tôi mang đến cho thực khách những
            trải nghiệm trọn vẹn nhất...
          </p>
          <div class="article-actions">
            <button @click="$router.push('/menu')" class="btn-action-primary">
              📖 Xem Thực Đơn
            </button>
            <button @click="$router.push('/reservation')" class="btn-action-outline">
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
              <img v-if="post.image" :src="post.image" :alt="post.title" />
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
          <p>Gia nhập đội ngũ FPOLY Restaurant — nơi kiến tạo sự nghiệp ẩm thực</p>
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
          <button @click="selectedRecruit = null" class="btn-close-modal">✖</button>
        </div>
        <img v-if="selectedRecruit.image" :src="selectedRecruit.image" class="modal-img" />
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
          <label>Lời Nhắn / Link CV</label>
          <textarea v-model="appForm.message" class="g-input" rows="4" placeholder="Kinh nghiệm của bạn, link Google Drive chứa CV..."></textarea>
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
          <button @click="showInterviewChat = false">✖</button>
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
          <button @click="showSupportChat = false">✖</button>
        </div>
        <div class="chat-body" ref="supportChatBody">
          <div v-for="(msg, i) in supportMessages" :key="i" :class="['chat-msg', msg.type]">
            {{ msg.text }}
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
        <div class="fab-chat" @click="toggleSupportChat">💬</div>
        <a href="tel:0123456789" class="fab-phone" style="text-decoration: none;">📞</a>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

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

// === LIKE POSTS ===
const likedPosts = ref(JSON.parse(localStorage.getItem('likedPosts') || '[]'));
const isLiked = (id) => likedPosts.value.includes(id);
const likePost = async (post) => {
  if (isLiked(post.id)) return;
  try {
    await axios.put(`http://localhost:8080/api/posts/${post.id}/like`);
    post.likes = (post.likes || 0) + 1;
    likedPosts.value.push(post.id);
    localStorage.setItem('likedPosts', JSON.stringify(likedPosts.value));
  } catch (err) { console.error('Like failed', err); }
};

// === APPLICATION FORM ===
const showAppForm = ref(false);
const appForm = ref({ fullname: '', phone: '', email: '', message: '', postId: null });
const openApplicationForm = () => {
  showAppForm.value = true;
  appForm.value = { fullname: '', phone: '', email: '', message: '', postId: selectedRecruit.value?.id };
};
const openApplicationFormFromInterview = () => {
  showInterviewChat.value = false;
  openApplicationForm();
};
const submitApplication = async () => {
  if (!appForm.value.fullname || !appForm.value.phone) return alert('Vui lòng nhập họ tên và SĐT!');
  try {
    await axios.post('http://localhost:8080/api/applications', appForm.value);
    alert('Đã gửi đơn ứng tuyển thành công! Nhà hàng sẽ liên hệ sớm nhất.');
    showAppForm.value = false;
  } catch (err) { alert('Lỗi: ' + (err.response?.data || err.message)); }
};

// === SUPPORT CHATBOT ===
const showSupportChat = ref(false);
const supportInput = ref('');
const supportMessages = ref([
  { type: 'bot', text: 'Chào bạn! Mình là trợ lý ảo của FPOLY Restaurant. Mình có thể giúp gì cho bạn? (VD: giờ mở cửa, đặt bàn, địa chỉ)' }
]);
const supportChatBody = ref(null);

const isSupportTyping = ref(false);

const toggleSupportChat = () => {
  showSupportChat.value = !showSupportChat.value;
  if (showSupportChat.value) setTimeout(scrollToBottomSupport, 100);
};

const scrollToBottomSupport = () => {
  if (supportChatBody.value) supportChatBody.value.scrollTop = supportChatBody.value.scrollHeight;
};

const sendSupportMessage = async () => {
  const text = supportInput.value.trim();
  if (!text) return;
  
  supportMessages.value.push({ type: 'user', text });
  supportInput.value = '';
  isSupportTyping.value = true;
  scrollToBottomSupport();

  try {
    const res = await axios.post('http://localhost:8080/api/chatbot/chat', { message: text });
    supportMessages.value.push({ type: 'bot', text: res.data.reply });
  } catch (err) {
    supportMessages.value.push({ type: 'bot', text: 'Xin lỗi, AI hiện đang mất kết nối. Vui lòng gọi Hotline để được hỗ trợ!' });
  } finally {
    isSupportTyping.value = false;
    scrollToBottomSupport();
  }
};

// === INTERVIEW CHATBOT ===
const showInterviewChat = ref(false);
const interviewInput = ref('');
const interviewStep = ref(0);
const interviewMessages = ref([]);
const interviewChatBody = ref(null);

const interviewQuestions = [
  "Chào bạn, rất vui vì bạn quan tâm đến FPOLY Restaurant. Bạn hãy giới thiệu ngắn gọn về bản thân nhé?",
  "Cảm ơn bạn! Bạn đã có kinh nghiệm làm việc trong lĩnh vực F&B (Nhà hàng/Cafe) chưa?",
  "Rất tốt! Vậy tại sao bạn lại muốn ứng tuyển vào vị trí này tại FPOLY Restaurant?",
  "Nếu gặp một khách hàng khó tính phàn nàn về món ăn, bạn sẽ xử lý như thế nào?",
  "Câu hỏi cuối cùng: Bạn có thể làm việc xoay ca (sáng/tối) không?"
];

const startInterview = () => {
  selectedRecruit.value = null;
  showInterviewChat.value = true;
  interviewMessages.value = [];
  setTimeout(() => {
    interviewMessages.value.push({ type: 'bot', text: "Chào bạn, mình là Giám đốc Nhân sự của FPOLY Restaurant. Rất vui được trao đổi với bạn hôm nay! Bạn có thể giới thiệu sơ qua về bản thân và vị trí mà bạn mong muốn ứng tuyển được không?" });
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

    const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
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
      axios.get('http://localhost:8080/api/posts/news'),
      axios.get('http://localhost:8080/api/posts/recruitment')
    ]);
    newsPosts.value = newsRes.data.slice(0, 6); // Max 6 tin
    recruitPosts.value = recruitRes.data.slice(0, 5); // Max 5 tuyển dụng
  } catch (err) { /* silent - posts are optional */ }
});

const handleLogout = () => {
  if (confirm('Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?')) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    isLoggedIn.value = false;
    user.value = null;
    alert('Đã đăng xuất thành công!');
    router.push('/');
  }
};
</script>

<style scoped>
.home-wrapper {
  background: var(--bg-root);
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
}

/* ===== NAVBAR ===== */
.navbar {
  background: rgba(6, 13, 26, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 30px rgba(0,0,0,0.5);
}
.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 68px;
}

/* Logo */
.logo {
  display: flex; align-items: center; gap: 12px;
  cursor: pointer; text-decoration: none;
}
.logo-icon { font-size: 1.8rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.logo-text h2 {
  margin: 0; font-size: 1.2rem; font-weight: 900;
  color: var(--text-heading); letter-spacing: 1px;
}
.logo-text p {
  margin: 0; font-size: 0.65rem; color: var(--primary);
  letter-spacing: 3px; text-transform: uppercase;
}

/* Nav Links */
.nav-links {
  display: flex; gap: 4px; align-items: center;
}
.nav-links a {
  color: var(--text-muted); text-decoration: none;
  font-size: 0.88rem; font-weight: 500;
  padding: 8px 14px; border-radius: var(--radius-md);
  transition: var(--transition);
}
.nav-links a:hover { color: var(--primary); background: var(--primary-glow2); }
.nav-links a.active { color: var(--primary); background: var(--primary-glow2); }

/* Nav Buttons */
.nav-right { display: flex; align-items: center; gap: 8px; }
.btn-nav {
  background: transparent; color: var(--text-secondary);
  border: 1px solid var(--border); padding: 7px 14px;
  border-radius: 20px; cursor: pointer;
  font-size: 0.83rem; font-weight: 600; font-family: inherit;
  transition: var(--transition);
}
.btn-nav:hover { border-color: var(--primary); color: var(--primary); }
.btn-nav-filled { background: var(--primary); color: var(--bg-dark); border-color: var(--primary); font-weight: 700; }
.btn-nav-filled:hover { background: var(--primary-dark); border-color: var(--primary-dark); color: var(--bg-dark); }
.btn-admin { border-color: rgba(241,196,15,0.4); color: #f1c40f; }
.btn-admin:hover { background: rgba(241,196,15,0.15); }
.btn-kitchen { border-color: rgba(0,212,170,0.4); color: var(--primary); }
.btn-waiter { border-color: rgba(52,152,219,0.4); color: #3498db; }
.btn-logout-nav { border-color: rgba(231,76,60,0.4); color: #e74c3c; }
.btn-logout-nav:hover { background: rgba(231,76,60,0.15); }

/* ===== HERO ===== */
.hero-banner {
  position: relative;
  background-image: url('https://images.unsplash.com/photo-1555396273-367ea4eb4db5?q=80&w=1974&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.hero-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg,
    rgba(6,13,26,0.6) 0%,
    rgba(6,13,26,0.5) 50%,
    rgba(6,13,26,0.85) 100%
  );
}
.hero-content {
  position: relative; z-index: 1;
  padding: 0 20px; max-width: 700px;
}
.hero-tag {
  display: inline-block;
  background: var(--primary-glow);
  border: 1px solid var(--border);
  color: var(--primary);
  padding: 6px 20px; border-radius: 20px;
  font-size: 0.83rem; font-weight: 600; letter-spacing: 1px;
  margin-bottom: 24px;
}
.hero-content h1 {
  font-size: 4rem; font-weight: 900;
  color: white; line-height: 1.1;
  margin: 0 0 16px 0;
  text-shadow: 0 4px 20px rgba(0,0,0,0.5);
}
.hero-content h1 span { color: var(--primary); }
.hero-content p {
  color: rgba(255,255,255,0.75); font-size: 1.1rem;
  margin: 0 0 32px 0; letter-spacing: 0.5px;
}

.hero-actions { display: flex; gap: 14px; justify-content: center; flex-wrap: wrap; margin-bottom: 48px; }
.btn-hero-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 15px 30px;
  border-radius: 30px; font-size: 1rem; font-weight: 800;
  font-family: inherit; cursor: pointer; transition: var(--transition);
}
.btn-hero-primary:hover { transform: translateY(-3px); box-shadow: 0 10px 30px rgba(0,212,170,0.5); }
.btn-hero-secondary {
  background: rgba(255,255,255,0.08); color: white;
  border: 2px solid rgba(255,255,255,0.4); padding: 15px 30px;
  border-radius: 30px; font-size: 1rem; font-weight: 700;
  font-family: inherit; cursor: pointer; transition: var(--transition);
  backdrop-filter: blur(10px);
}
.btn-hero-secondary:hover { background: rgba(255,255,255,0.18); border-color: white; }

/* Stats */
.hero-stats {
  display: inline-flex; align-items: center; gap: 0;
  background: rgba(13, 27, 42, 0.7);
  border: 1px solid var(--border);
  border-radius: 16px; padding: 16px 32px;
  backdrop-filter: blur(15px);
}
.stat-item { text-align: center; padding: 0 20px; }
.stat-num { display: block; font-size: 1.6rem; font-weight: 900; color: var(--primary); }
.stat-lbl { font-size: 0.78rem; color: rgba(255,255,255,0.6); letter-spacing: 0.5px; }
.stat-divider { width: 1px; height: 40px; background: var(--border); }

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
.btn-action-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 13px 26px;
  border-radius: var(--radius-md); font-size: 0.95rem; font-weight: 800;
  font-family: inherit; cursor: pointer; transition: var(--transition);
}
.btn-action-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }
.btn-action-outline {
  background: transparent; border: 1px solid var(--border);
  color: var(--text-secondary); padding: 13px 26px;
  border-radius: var(--radius-md); font-size: 0.95rem; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: var(--transition);
}
.btn-action-outline:hover { border-color: var(--primary); color: var(--primary); }

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
.svc-badge.new { background: rgba(0,212,170,0.15); color: var(--primary); }
.svc-badge.hot { background: rgba(231,76,60,0.15); color: #e74c3c; }

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
  color: var(--bg-dark);
  width: 60px; height: 60px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.6rem; cursor: pointer;
  box-shadow: 0 6px 25px rgba(0, 212, 170, 0.4);
  animation: fab-pulse 2.5s ease-in-out infinite;
  transition: var(--transition);
}
.fab-phone:hover { transform: scale(1.1); box-shadow: 0 8px 30px rgba(0,212,170,0.6); }
@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 6px 25px rgba(0, 212, 170, 0.4); }
  50% { box-shadow: 0 6px 25px rgba(0, 212, 170, 0.4), 0 0 0 15px rgba(0,212,170,0); }
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
.recruit-tag { background: rgba(155,89,182,0.1); color: #9b59b6; border-color: rgba(155,89,182,0.3); }
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
  background: rgba(0,0,0,0.7); color: white;
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
.recruit-card:hover { border-color: rgba(155,89,182,0.4); transform: translateX(6px); box-shadow: var(--shadow-md); }
.recruit-icon {
  font-size: 2rem; width: 60px; height: 60px; flex-shrink: 0;
  background: rgba(155,89,182,0.1); border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
}
.recruit-info { flex: 1; min-width: 0; }
.recruit-info h4 { margin: 0 0 6px 0; font-size: 1.1rem; font-weight: 800; color: var(--text-heading); }
.recruit-info p { margin: 0 0 8px 0; font-size: 0.88rem; color: var(--text-muted); line-height: 1.5; }
.recruit-date { font-size: 0.78rem; color: var(--text-muted); }
.btn-apply {
  background: rgba(155,89,182,0.15); border: 1px solid rgba(155,89,182,0.3);
  color: #9b59b6; padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.88rem; font-family: inherit;
  white-space: nowrap; transition: var(--transition); flex-shrink: 0;
}
.btn-apply:hover { background: #9b59b6; color: white; }

/* Recruit Modal */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.75);
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
.btn-close-modal:hover { color: #e74c3c; }
.modal-img { width: 100%; max-height: 300px; object-fit: cover; }
.modal-content { padding: 24px; }
.modal-text {
  white-space: pre-wrap; word-wrap: break-word; font-family: 'Inter', sans-serif;
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
.g-input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(0,212,170,0.1); }
.app-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-submit-app {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 14px; border-radius: var(--radius-md);
  font-weight: 800; font-size: 1rem; cursor: pointer; transition: var(--transition);
}
.btn-submit-app:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }
.btn-close {
  background: transparent; border: 1px solid var(--border);
  color: var(--text-muted); padding: 12px; border-radius: var(--radius-md);
  font-weight: 600; cursor: pointer; transition: var(--transition);
}
.btn-close:hover { border-color: #e74c3c; color: #e74c3c; }
.w-100 { width: 100%; }

/* LIKES & INTERVIEW BTN */
.news-footer { margin-top: 16px; display: flex; justify-content: flex-end; }
.recruit-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.btn-like {
  background: rgba(231,76,60,0.1); border: 1px solid rgba(231,76,60,0.2);
  color: #e74c3c; padding: 6px 14px; border-radius: 20px;
  cursor: pointer; font-weight: 700; font-size: 0.85rem;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.btn-like:hover { background: rgba(231,76,60,0.2); transform: scale(1.05); }
.btn-like.liked { background: #e74c3c; color: white; border-color: #e74c3c; animation: heartBeat 0.5s; }
.btn-like-small { padding: 4px 10px; font-size: 0.8rem; }
@keyframes heartBeat {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}
.modal-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border-light); }
.modal-actions { display: flex; gap: 12px; }
.btn-interview {
  background: rgba(52,152,219,0.15); border: 1px solid rgba(52,152,219,0.3);
  color: #3498db; padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.9rem; transition: var(--transition);
}
.btn-interview:hover { background: #3498db; color: white; }

/* ===== CHATBOTS ===== */
.chatbots-container { position: fixed; bottom: 30px; right: 30px; z-index: 999; display: flex; flex-direction: column; align-items: flex-end; gap: 16px; }
.fab-group { display: flex; gap: 12px; }
.fab-chat {
  width: 60px; height: 60px; border-radius: 50%;
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: white; font-size: 1.8rem; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 6px 20px rgba(52,152,219,0.4);
  transition: transform 0.3s;
}
.fab-chat:hover { transform: scale(1.1); }

.chat-widget {
  width: 350px; height: 500px; background: var(--bg-card);
  border-radius: 20px; border: 1px solid var(--border);
  box-shadow: 0 10px 40px rgba(0,0,0,0.5);
  display: flex; flex-direction: column; overflow: hidden;
  animation: slideUp 0.3s ease-out;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

.chat-header {
  padding: 16px; display: flex; justify-content: space-between; align-items: center;
  color: white; font-weight: 700;
}
.support-header { background: linear-gradient(135deg, #3498db, #2980b9); }
.interview-header { background: linear-gradient(135deg, #9b59b6, #8e44ad); }
.chat-header button { background: transparent; border: none; color: white; font-size: 1.2rem; cursor: pointer; }

.chat-body {
  flex: 1; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px;
  background: var(--bg-root);
}
.chat-msg { max-width: 80%; padding: 12px 16px; border-radius: 18px; font-size: 0.9rem; line-height: 1.4; word-wrap: break-word; }
.chat-msg.bot { background: var(--bg-input); border: 1px solid var(--border-light); color: var(--text-heading); border-bottom-left-radius: 4px; align-self: flex-start; }
.chat-msg.user { background: var(--primary); color: var(--bg-dark); border-bottom-right-radius: 4px; align-self: flex-end; font-weight: 600; }

.chat-input-area {
  padding: 12px; border-top: 1px solid var(--border-light); display: flex; gap: 8px; background: var(--bg-card);
}
.chat-input-area input {
  flex: 1; padding: 10px 14px; border-radius: 20px; border: 1px solid var(--border-light);
  background: var(--bg-input); color: var(--text-heading); font-family: inherit;
}
.chat-input-area input:focus { outline: none; border-color: var(--primary); }
.chat-input-area button {
  background: var(--primary); color: var(--bg-dark); border: none;
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

</style>