<template>
  <CustomerLayout transparent-nav>
    <div class="home-wrapper">

    <section class="hero-banner">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-tag">✦ {{ $t('home.title') }}</div>
        <h1>{{ $t('home.restaurantName') }}</h1>
        <p>{{ $t('home.subtitle') }}</p>
        <div class="hero-actions">
          <button @click="$router.push('/dine-in')" class="g-btn-primary home-hero-btn">
             {{ $t('home.dineIn') }}
          </button>
          <button @click="$router.push('/reservation')" class="g-btn-outline home-hero-btn home-hero-outline">
             {{ $t('home.reserve') }}
          </button>
        </div>

        <!-- Stats bar -->
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">100+</span>
            <span class="stat-lbl">{{ $t('home.dishes') }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">5</span>
            <span class="stat-lbl">{{ $t('home.reviews') }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">10+</span>
            <span class="stat-lbl">{{ $t('home.yearsExperience') }}</span>
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
            <span class="meta-tag">{{ $t('home.featured') }}</span>
            <span class="meta-date"> {{ currentDate }}</span>
          </div>
          <h2 class="article-title">{{ $t('home.storyTitle') }}</h2>
          <p class="article-excerpt">{{ $t('home.story') }}</p>
          <div class="article-actions">
            <button @click="$router.push('/menu')" class="g-btn-primary home-action-btn">
               {{ $t('home.view_menu') }}
            </button>
            <button @click="$router.push('/reservation')" class="g-btn-outline home-action-btn">
               {{ $t('home.book_now') }}
            </button>
          </div>

          <!-- Feature Cards -->
          <div class="feature-grid">
            <div class="feature-card">
              <div class="feature-icon"><UiIcon name="location" /></div>
              <h4>{{ $t('home.delivery') }}</h4>
              <p>{{ $t('home.deliveryDesc') }}</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon"><UiIcon name="calendar" /></div>
              <h4>{{ $t('home.events') }}</h4>
              <p>{{ $t('home.eventsDesc') }}</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon"><UiIcon name="private" /></div>
              <h4>{{ $t('home.vipSpace') }}</h4>
              <p>{{ $t('home.vipSpaceDesc') }}</p>
            </div>
          </div>
        </article>

        <!-- Right col - Sidebar -->
        <aside class="right-col">
          <div class="sidebar-card">
            <h3>{{ $t('home.services') }}</h3>
            <ul class="service-list">
              <li>
                <router-link to="/menu">
                  <span class="svc-icon"><UiIcon name="location" /></span>
                  <span>{{ $t('home.delivery') }}</span>
                  <span class="svc-badge new">{{ $t('home.new') }}</span>
                </router-link>
              </li>
              <li>
                <router-link to="/reservation">
                  <span class="svc-icon"><UiIcon name="calendar" /></span>
                  <span>{{ $t('home.events') }}</span>
                  <span class="svc-badge hot">Hot</span>
                </router-link>
              </li>
              <li>
                <a href="#">
                  <span class="svc-icon"><UiIcon name="private" /></span>
                  <span>{{ $t('home.vipSpace') }}</span>
                </a>
              </li>
              <li>
                <a href="#">
                  <span class="svc-icon"><UiIcon name="garden" /></span>
                  <span>{{ $t('home.vegetarianMenu') }}</span>
                </a>
              </li>
            </ul>
          </div>

          <div class="sidebar-card hours-card">
            <h3>{{ $t('home.openingHours') }}</h3>
            <div class="hours-list">
              <div class="hours-item">
                <span>{{ $t('home.weekdays') }}</span>
                <span class="hours-time">10:00 - 22:00</span>
              </div>
              <div class="hours-item">
                <span>{{ $t('home.weekend') }}</span>
                <span class="hours-time">09:00 - 23:00</span>
              </div>
            </div>
            <div class="open-status">
              <span class="open-dot"></span>
              {{ $t('home.openNow') }}
            </div>
          </div>
        </aside>
      </div>
    </main>

    <!-- Tin Tức Nhà Hàng -->
    <section v-if="newsPosts.length > 0" class="posts-section">
      <div class="section-container">
        <div class="section-header-block">
          <span class="section-tag">{{ $t('home.news') }}</span>
          <h2>{{ $t('home.newsTitle') }}</h2>
          <p>{{ $t('home.newsHint') }}</p>
        </div>
        <div class="news-grid">
          <div v-for="post in newsPosts" :key="post.id" class="news-card">
            <div class="news-img-wrap">
              <img v-if="post.image" :src="post.image" :alt="localizedPostTitle(post)" loading="lazy" />
              <div v-else class="news-img-placeholder"><UiIcon name="note" /></div>
              <span class="news-date-badge">{{ formatPostDate(post.createDate) }}</span>
            </div>
            <div class="news-body">
              <h3>{{ localizedPostTitle(post) }}</h3>
              <p>{{ truncateText(localizedPostContent(post), 150) }}</p>
              <div class="news-footer">
                <button 
                  class="btn-like" 
                  :class="{'liked': isLiked(post.id)}" 
                  @click="likePost(post)"
                >
                   {{ post.likes || 0 }}
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
          <span class="section-tag recruit-tag">{{ $t('home.recruitment') }}</span>
          <h2>{{ $t('home.careers') }}</h2>
          <p>{{ $t('home.careersHint') }}</p>
        </div>
        <div class="recruit-list">
          <div v-for="post in recruitPosts" :key="post.id" class="recruit-card">
            <div class="recruit-icon"><UiIcon name="users" /></div>
            <div class="recruit-info">
              <h4>{{ localizedPostTitle(post) }}</h4>
              <p>{{ truncateText(localizedPostContent(post), 200) }}</p>
              <div class="recruit-meta">
                <span class="recruit-date">{{ $t('home.postedOn', { date: formatPostDate(post.createDate) }) }}</span>
                <button 
                  class="btn-like btn-like-small" 
                  :class="{'liked': isLiked(post.id)}" 
                  @click.stop="likePost(post)"
                >
                   {{ post.likes || 0 }}
                </button>
              </div>
            </div>
            <button class="btn-apply" @click="showRecruitDetail(post)">{{ $t('home.details') }}</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Recruit Detail Modal -->
    <div v-if="selectedRecruit" class="modal-overlay" @click.self="selectedRecruit = null">
      <div class="recruit-modal">
        <div class="modal-top">
          <h2>{{ localizedPostTitle(selectedRecruit) }}</h2>
          <button @click="selectedRecruit = null" class="btn-close-modal" :aria-label="$t('home.closeRecruitment')"><UiIcon name="x" /></button>
        </div>
        <img v-if="selectedRecruit.image" :src="selectedRecruit.image" :alt="localizedPostTitle(selectedRecruit)" class="modal-img" loading="lazy" />
        <div class="modal-content">
          <pre class="modal-text">{{ localizedPostContent(selectedRecruit) }}</pre>
          <div class="modal-footer">
            <p class="modal-date">{{ $t('home.postedOn', { date: formatPostDate(selectedRecruit.createDate) }) }}</p>
            <div class="modal-actions">
              <button class="btn-interview" @click="startInterview">{{ $t('home.tryInterview') }}</button>
              <button class="btn-submit-app" @click="openApplicationForm">{{ $t('home.applyNow') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Application Form Modal -->
    <div v-if="showAppForm" class="modal-overlay" @click.self="showAppForm = false">
      <div class="app-modal">
        <h3>{{ $t('home.application') }}</h3>
        <p class="app-subtitle">{{ $t('home.position', { position: localizedPostTitle(selectedRecruit) }) }}</p>

        <div class="form-group">
          <label>{{ $t('home.fullName') }}</label>
          <input v-model="appForm.fullname" type="text" class="g-input" :placeholder="$t('home.fullNamePlaceholder')" />
        </div>
        <div class="form-group">
          <label>{{ $t('home.phone') }}</label>
          <input v-model="appForm.phone" type="text" class="g-input" placeholder="VD: 0987654321" />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input v-model="appForm.email" type="email" class="g-input" placeholder="nguyenvana@gmail.com" />
        </div>
        <div class="form-group">
          <label>{{ $t('home.message') }}</label>
          <textarea v-model="appForm.message" class="g-input" rows="3" :placeholder="$t('home.experiencePlaceholder')"></textarea>
        </div>
        <div class="form-group">
          <label>{{ $t('home.cv') }}</label>
          <input type="file" @change="handleCvUpload" class="g-input" style="padding: 10px;" />
        </div>
        <div class="app-actions">
          <button class="btn-submit-app w-100" @click="submitApplication">{{ $t('home.submitApplication') }}</button>
          <button class="btn-close" @click="showAppForm = false">{{ $t('home.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- Chatbots -->
    <div class="chatbots-container">
      <!-- Chat Widget: Interview -->
      <div v-if="showInterviewChat" class="chat-widget interview-chat">
        <div class="chat-header interview-header">
          <span>{{ $t('home.interviewBot') }}</span>
          <button @click="showInterviewChat = false" :aria-label="$t('home.closeInterview')"><UiIcon name="x" /></button>
        </div>
        <div class="chat-body" ref="interviewChatBody">
          <div v-for="(msg, i) in interviewMessages" :key="i" :class="['chat-msg', msg.type]">
            {{ msg.text }}
          </div>
        </div>
        <div class="chat-input-area" v-if="interviewStep < interviewQuestions.length">
          <input v-model="interviewInput" @keyup.enter="sendInterviewMessage" :placeholder="$t('home.answerPlaceholder')" />
          <button @click="sendInterviewMessage">{{ $t('home.send') }}</button>
        </div>
        <div class="chat-input-area chat-done-area" v-else>
          <button class="btn-submit-app w-100" @click="openApplicationFormFromInterview">{{ $t('home.applyNow') }}</button>
        </div>
      </div>

      <!-- Chat Widget: Customer Support -->
      <div v-if="showSupportChat" class="chat-widget support-chat">
        <div class="chat-header support-header">
          <span>{{ $t('home.supportBot') }}</span>
          <button @click="showSupportChat = false" :aria-label="$t('home.closeSupport')"><UiIcon name="x" /></button>
        </div>
        <div class="chat-body" ref="supportChatBody">
          <div v-for="(msg, i) in supportMessages" :key="i" :class="['chat-msg', msg.type, { 'msg-booking': msg.isTableBooking, 'msg-menu': msg.isMenuLink }]">
            <template v-if="!msg.isTableBooking && !msg.isMenuLink">
              {{ msg.text }}
              <div v-if="msg.type === 'bot' && msg.interactionId && !msg.feedback" class="ai-feedback"><button @click="sendAiFeedback(msg, true)"><UiIcon name="thumbs-up" /></button><button @click="sendAiFeedback(msg, false)"><UiIcon name="thumbs-down" /></button></div>
              <small v-else-if="msg.feedback">{{ $t('home.feedbackThanks') }}</small>
            </template>
            <template v-else-if="msg.isMenuLink">
              <div class="menu-link-widget">
                <p>{{ $t('home.menuLinkHint') }}</p>
                <button @click="$router.push('/menu')" class="b-btn">{{ $t('home.view_menu') }}</button>
              </div>
            </template>
            <template v-else>
              <div class="booking-widget">
                <p>{{ $t('home.bookingSummary', msg.bookingState) }}</p>
                <p>{{ $t('home.bookingAuto') }}</p>
                <button @click="continueSecureReservation" class="b-btn">{{ $t('home.continueBooking') }}</button>
              </div>
            </template>
          </div>
          <div v-if="isSupportTyping" class="chat-msg bot typing-indicator">
            {{ $t('home.thinking') }}
          </div>
        </div>
        <div class="chat-input-area">
          <input v-model="supportInput" @keyup.enter="sendSupportMessage" :placeholder="$t('home.supportPlaceholder')" />
          <button @click="sendSupportMessage">{{ $t('home.send') }}</button>
        </div>
      </div>
      
      <!-- FAB Buttons -->
      <button
        class="fab-mobile-toggle"
        type="button"
        :aria-expanded="showMobileFabActions"
        :aria-label="$t('home.quickTools')"
        @click="showMobileFabActions = !showMobileFabActions"
      ><UiIcon name="sparkles" /></button>
      <div class="fab-group" :class="{ 'is-open': showMobileFabActions }">
        <button class="fab-chat fab-wheel" type="button" :aria-label="$t('home.openWheel')" @click="openLuckyWheel"><UiIcon name="sparkles" /></button>
        <button class="fab-chat" type="button" :aria-label="$t('home.openSupport')" @click="toggleSupportChat"><UiIcon name="note" /></button>
        <a href="tel:+84347944028" class="fab-phone" :aria-label="$t('home.callRestaurant')" style="text-decoration: none;"><UiIcon name="phone" /></a>
      </div>
    </div>

    <!-- Lucky Wheel Modal -->
    <div v-if="showLuckyWheel" class="modal-overlay" @click.self="showLuckyWheel = false">
      <div class="wheel-modal">
        <div class="wheel-header">
          <h2>{{ $t('home.luckyWheel') }}</h2>
          <button class="btn-close-modal" :aria-label="$t('home.closeWheel')" @click="showLuckyWheel = false"><UiIcon name="x" /></button>
        </div>
        <p class="wheel-desc">{{ $t('home.wheelHint') }}</p>
        <div class="wheel-container">
          <div class="wheel-pointer">▼</div>
          <div class="wheel-board" :style="{ transform: `rotate(${wheelRotation}deg)`, background: 'conic-gradient(var(--color-tertiary) 0 60deg, var(--secondary) 60deg 120deg, var(--color-tertiary) 120deg 180deg, var(--text-muted) 180deg 240deg, var(--secondary) 240deg 300deg, var(--color-on-secondary-container) 300deg 360deg)' }">
            <div v-for="(p, i) in prizes" :key="i" class="wheel-text" :style="{ transform: `rotate(${p.deg + 30}deg)` }">
              <span class="slice-label" :style="{ color: i === 0 ? 'var(--text-primary)' : '#FFFFFF' }">{{ p.label }}</span>
            </div>
          </div>
        </div>
        <button class="btn-spin" @click="spinWheel" :disabled="isSpinning">
          {{ isSpinning ? $t('home.spinning') : $t('home.spinNow') }}
        </button>
        <div v-if="spinResult" class="spin-result" :class="{ 'win': spinResult.type !== 'miss' }">
          <p v-if="spinResult.type !== 'miss'">{{ $t('home.congratulations', { prize: spinResult.label }) }}</p>
          <p v-else>{{ $t('home.tryAgain') }}</p>
        </div>
      </div>
    </div>

  </div>
  </CustomerLayout>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/services/api';
import CustomerLayout from '@/components/CustomerLayout.vue';
import UiIcon from '@/components/UiIcon.vue';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const { locale, t } = useI18n()
const toast = useToast();

const isLoggedIn = ref(false);
const user = ref(null);
const newsPosts = ref([]);
const recruitPosts = ref([]);
const selectedRecruit = ref(null);

const today = new Date();
const currentDate = ref(`${today.getDate()}-${today.getMonth() + 1}-${today.getFullYear()}`);

const formatPostDate = (d) => d ? new Date(d).toLocaleDateString(locale.value === 'vi' ? 'vi-VN' : 'en-US') : '---';
const truncateText = (str, len) => str && str.length > len ? str.substring(0, len) + '...' : str;
const localizedPostTitle = post => locale.value === 'vi' ? post?.title : (post?.titleEn || t('home.contentUnavailable'));
const localizedPostContent = post => locale.value === 'vi' ? post?.content : (post?.contentEn || t('home.contentUnavailable'));
const showRecruitDetail = (post) => { selectedRecruit.value = post; };

// === LUCKY WHEEL ===
const showLuckyWheel = ref(false);
const showMobileFabActions = ref(false);
const isSpinning = ref(false);
const wheelRotation = ref(0);
const spinResult = ref(null);

const prizes = computed(() => [
  { label: t('home.prizes.fivePoints'), value: 5, type: 'points', deg: 0, color: 'var(--color-tertiary)' },
  { label: t('home.prizes.tenPoints'), value: 10, type: 'points', deg: 60, color: 'var(--secondary)' },
  { label: t('home.prizes.discount'), value: 5, type: 'discount', deg: 120, color: 'var(--color-tertiary)' },
  { label: t('home.prizes.miss'), value: 0, type: 'miss', deg: 180, color: 'var(--text-muted)' },
  { label: t('home.prizes.gift'), value: 0, type: 'gift', deg: 240, color: 'var(--secondary)' },
  { label: t('home.prizes.fiftyPoints'), value: 50, type: 'points', deg: 300, color: 'var(--color-on-secondary-container)' }
]);

const openLuckyWheel = async () => {
  if (!isLoggedIn.value) {
    toast.warning(t('home.loginToSpin'));
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
    const token = sessionStorage.getItem('token');
    const response = await api.post('/api/vouchers/spin', null, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    reward = response.data;
  } catch (error) {
    isSpinning.value = false;
    if (error.response?.status === 409) {
      toast.info(t('home.alreadySpun'));
    } else if (error.response?.status === 403) {
      toast.warning(t('home.spinEligibility'));
    } else {
      toast.error(t('home.spinFailed'));
      console.error('Lỗi vòng quay may mắn', error);
    }
    return;
  }

  const prizeIndex = prizes.value.findIndex((prize) =>
    prize.type === reward.type && prize.value === reward.value
  );
  if (prizeIndex < 0) {
    isSpinning.value = false;
    console.error('Phần thưởng từ máy chủ không khớp cấu hình giao diện', reward);
    toast.error(t('home.invalidReward'));
    return;
  }

  const targetDeg = (360 - prizes.value[prizeIndex].deg) + 360 * 5;

  wheelRotation.value += targetDeg;

  setTimeout(() => {
    isSpinning.value = false;
    const won = { ...reward, ...prizes.value[prizeIndex] };
    spinResult.value = won;

    if (won.type === 'points') {
      toast.success(t('home.pointsWon', { prize: won.label, points: won.currentPoints, tier: won.membershipTier }));
    } else if (won.type === 'discount') {
      toast.success(t('home.discountWon', { value: won.value, code: won.voucherCode }));
    } else if (won.type === 'gift') {
      toast.success(t('home.giftWon', { prize: won.label }));
    }
  }, 7000); // 7 seconds
};

// === LIKE POSTS ===
const likedPosts = ref(JSON.parse(localStorage.getItem('likedPosts') || '[]'));
const isLiked = (id) => likedPosts.value.includes(id);
const likePost = async (post) => {
  if (!isLoggedIn.value) {
    router.push('/login');
    return;
  }
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
  if (!appForm.value.fullname || !appForm.value.phone) return toast.warning(t('home.applicationRequired'));
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
    toast.success(t('home.applicationSent'));
    showAppForm.value = false;
  } catch (err) { toast.error(t('home.applicationFailed')); }
};

// === SUPPORT CHATBOT ===
const showSupportChat = ref(false);
const supportInput = ref('');
const supportMessages = ref([
  { type: 'bot', text: t('home.supportGreeting') }
]);
const supportChatBody = ref(null);

const isSupportTyping = ref(false);
const supportSessionId = ref(sessionStorage.getItem('support_ai_session') || '');

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
      locale: locale.value,
      sessionId: supportSessionId.value || null
    });
    if (res.data.sessionId) {
      supportSessionId.value = res.data.sessionId;
      sessionStorage.setItem('support_ai_session', res.data.sessionId);
    }
    
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

      supportMessages.value.push({ 
        type: 'bot', 
        isTableBooking: true,
        bookingState: {
          time, pax, view,
          tableConfirmed: false
        }
      });
    } else if (menuMatch) {
      // Remove the tag from the text
      const cleanReply = reply.replace(menuRegex, '').trim();
      supportMessages.value.push({ type: 'bot', text: cleanReply, interactionId: res.data.interactionId });
      supportMessages.value.push({ type: 'bot', isMenuLink: true });
    } else {
      supportMessages.value.push({ type: 'bot', text: reply, interactionId: res.data.interactionId });
    }
  } catch (err) {
    supportMessages.value.push({ type: 'bot', text: t('home.supportOffline') });
  } finally {
    isSupportTyping.value = false;
    scrollToBottomSupport();
  }
};

const continueSecureReservation = () => {
  router.push('/reservation');
};
const sendAiFeedback = async (msg, helpful) => {
  await api.post('/api/ai/feedback', { interactionId: msg.interactionId, sessionId: supportSessionId.value, helpful, comment: '' });
  msg.feedback = true;
};

// === INTERVIEW CHATBOT ===
const showInterviewChat = ref(false);
const interviewInput = ref('');
const interviewStep = ref(0);
const interviewMessages = ref([]);
const interviewChatBody = ref(null);

const interviewQuestions = [true];

const startInterview = () => {
  selectedRecruit.value = null;
  showInterviewChat.value = true;
  interviewMessages.value = [];
  setTimeout(() => {
    interviewMessages.value.push({ type: 'bot', text: t('home.interviewGreeting') });
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

  interviewMessages.value.push({ type: 'bot', text: t('home.thinking'), isTyping: true });
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
    interviewMessages.value.push({ type: 'bot', text: t('home.interviewOffline') });
    scrollInterview();
  }
};

onMounted(async () => {
  const token = sessionStorage.getItem('token');
  const storedUser = sessionStorage.getItem('user');
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
  color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent);
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
.btn-admin { border-color: color-mix(in srgb, var(--color-tertiary) 45%, transparent); color: var(--secondary); }
.btn-admin:hover { background: color-mix(in srgb, var(--color-tertiary) 14%, transparent); }
.btn-kitchen { border-color: color-mix(in srgb, var(--secondary) 40%, transparent); color: var(--primary); }
.btn-waiter { border-color: color-mix(in srgb, var(--secondary) 45%, transparent); color: var(--secondary); }
.btn-logout-nav { border-color: color-mix(in srgb, var(--primary) 40%, transparent); color: var(--primary); }
.btn-logout-nav:hover { background: color-mix(in srgb, var(--primary) 15%, transparent); }

/* ===== HERO ===== */
.hero-banner {
  position: relative;
  background: linear-gradient(120deg, #271717 0%, #3e2c2b 58%, #92001c 160%);
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
    radial-gradient(circle at 20% 15%, rgba(255, 179, 177, 0.22) 0%, transparent 42%),
    radial-gradient(circle at 82% 80%, rgba(183, 16, 42, 0.28) 0%, transparent 48%),
    linear-gradient(180deg, rgba(39, 23, 23, 0.16), rgba(39, 23, 23, 0.66));
}
.hero-content {
  position: relative; z-index: 1;
  padding: 0 20px; max-width: 700px;
}
.hero-tag {
  display: inline-block;
  background: rgba(255, 218, 216, 0.14);
  border: 1px solid rgba(255, 218, 216, 0.44);
  color: #ffdad8;
  padding: 6px 16px; border-radius: 999px;
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
  color: var(--color-primary-fixed-dim);
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
  border-radius: var(--radius-lg);
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
  background: rgba(255, 248, 247, 0.12);
  border: 1px solid rgba(255, 218, 216, 0.28);
  border-radius: var(--radius-lg); padding: 16px 32px;
  backdrop-filter: blur(15px);
}
.stat-item { text-align: center; padding: 0 20px; }
.stat-num { display: block; font-size: 1.6rem; font-weight: 900; color: var(--color-primary-fixed-dim); }
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
.svc-badge.new { background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--primary); }
.svc-badge.hot { background: color-mix(in srgb, var(--primary) 15%, transparent); color: var(--primary); }

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
.b-success { background: color-mix(in srgb, var(--secondary) 10%, transparent); border: 1px solid var(--primary); padding: 12px; border-radius: 6px; color: var(--primary); font-weight: bold; text-align: center; }

.fab-phone {
  width: 60px; height: 60px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.6rem; cursor: pointer;
  box-shadow: 0 6px 25px color-mix(in srgb, var(--secondary) 40%, transparent);
  animation: fab-pulse 2.5s ease-in-out infinite;
  transition: var(--transition);
}
.fab-phone:hover { transform: scale(1.1); box-shadow: 0 8px 30px color-mix(in srgb, var(--secondary) 60%, transparent); }
@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 6px 25px color-mix(in srgb, var(--secondary) 40%, transparent); }
  50% { box-shadow: 0 6px 25px color-mix(in srgb, var(--secondary) 40%, transparent), 0 0 0 15px transparent; }
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
.recruit-tag { background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); color: var(--color-tertiary); border-color: color-mix(in srgb, var(--color-tertiary) 30%, transparent); }
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
.recruit-card:hover { border-color: color-mix(in srgb, var(--color-tertiary) 40%, transparent); transform: translateX(6px); box-shadow: var(--shadow-md); }
.recruit-icon {
  font-size: 2rem; width: 60px; height: 60px; flex-shrink: 0;
  background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
}
.recruit-info { flex: 1; min-width: 0; }
.recruit-info h4 { margin: 0 0 6px 0; font-size: 1.1rem; font-weight: 800; color: var(--text-heading); }
.recruit-info p { margin: 0 0 8px 0; font-size: 0.88rem; color: var(--text-muted); line-height: 1.5; }
.recruit-date { font-size: 0.78rem; color: var(--text-muted); }
.btn-apply {
  background: color-mix(in srgb, var(--color-tertiary) 15%, transparent); border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  color: var(--color-tertiary); padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.88rem; font-family: inherit;
  white-space: nowrap; transition: var(--transition); flex-shrink: 0;
}
.btn-apply:hover { background: var(--color-tertiary); color: #FFFFFF; }

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
.g-input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px color-mix(in srgb, var(--secondary) 10%, transparent); }
.app-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.btn-submit-app {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF; border: none; padding: 14px; border-radius: var(--radius-md);
  font-weight: 800; font-size: 1rem; cursor: pointer; transition: var(--transition);
}
.btn-submit-app:hover { transform: translateY(-2px); box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent); }
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
  background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); border: 1px solid color-mix(in srgb, var(--color-tertiary) 24%, transparent);
  color: var(--secondary); padding: 6px 14px; border-radius: 20px;
  cursor: pointer; font-weight: 700; font-size: 0.85rem;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.btn-like:hover { background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); transform: scale(1.05); }
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
  background: color-mix(in srgb, var(--color-tertiary) 12%, transparent); border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  color: var(--secondary); padding: 10px 20px; border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.9rem; transition: var(--transition);
}
.btn-interview:hover { background: var(--secondary); color: #FFFFFF; }

/* ===== CHATBOTS ===== */
.chatbots-container { position: fixed; bottom: 30px; right: 30px; z-index: 999; display: flex; flex-direction: column; align-items: flex-end; gap: 16px; }
.fab-mobile-toggle { display: none; }
.fab-group { display: flex; gap: 12px; }
.fab-chat {
  width: 60px; height: 60px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF; font-size: 1.8rem; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent);
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
.interview-header { background: linear-gradient(135deg, var(--color-tertiary), var(--warning)); }
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

.fab-wheel { background: linear-gradient(45deg, var(--color-tertiary), var(--color-tertiary)); font-size: 1.5rem; display:flex; align-items:center; justify-content:center; box-shadow: 0 5px 20px color-mix(in srgb, var(--color-tertiary) 50%, transparent); }
.fab-wheel:hover { transform: scale(1.15) rotate(10deg); }

/* Lucky Wheel */
.wheel-modal { background: var(--bg-card); padding: 30px; border-radius: 20px; width: 400px; max-width: 90%; text-align: center; border: 1px solid var(--border); box-shadow: var(--shadow-lg); }
.wheel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.wheel-header h2 { color: var(--color-tertiary); margin: 0; font-size: 1.5rem; text-transform: uppercase; font-weight: 900; }
.wheel-desc { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 20px; }
.wheel-container { position: relative; width: 300px; height: 300px; margin: 0 auto 30px; border-radius: 50%; box-shadow: 0 0 20px color-mix(in srgb, var(--color-tertiary) 20%, transparent); border: 8px solid var(--bg-root); overflow: hidden; }
.wheel-pointer { position: absolute; top: -10px; left: 50%; transform: translateX(-50%); font-size: 2rem; color: var(--secondary); z-index: 10; text-shadow: 0 2px 5px rgba(0,0,0,0.35); }
.wheel-board { width: 100%; height: 100%; border-radius: 50%; position: relative; transition: transform 7s cubic-bezier(0.1, 0.8, 0.1, 1); }
.wheel-text { position: absolute; width: 100%; height: 100%; top: 0; left: 0; display: flex; justify-content: center; align-items: flex-start; padding-top: 15px; transform-origin: 50% 50%; }
.slice-label { font-weight: 900; font-size: 1.1rem; text-shadow: 1px 1px 3px rgba(0,0,0,0.8); }

.btn-spin { background: linear-gradient(45deg, var(--color-tertiary), var(--color-tertiary)); color: #FFFFFF; font-size: 1.2rem; font-weight: 900; border: none; padding: 15px 40px; border-radius: 30px; cursor: pointer; transition: 0.3s; box-shadow: 0 5px 15px color-mix(in srgb, var(--color-tertiary) 40%, transparent); }
.btn-spin:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 8px 25px color-mix(in srgb, var(--color-tertiary) 60%, transparent); }
.btn-spin:disabled { opacity: 0.7; cursor: not-allowed; background: var(--text-muted); box-shadow: none; }
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
  .chatbots-container { right: 12px; bottom: 12px; left: auto; max-width: calc(100vw - 24px); gap: 8px; }
  .chat-widget { width: 100%; height: min(500px, 72vh); }
  .fab-mobile-toggle {
    display: flex;
    width: 52px;
    height: 52px;
    align-items: center;
    justify-content: center;
    border: 0;
    border-radius: 50%;
    background: var(--primary);
    color: #FFFFFF;
    box-shadow: var(--shadow-md);
    cursor: pointer;
    font-size: 1.35rem;
  }
  .fab-group {
    display: none;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
    max-width: 100%;
  }
  .fab-group.is-open { display: flex; }
  .fab-chat,
  .fab-phone { width: 52px; height: 52px; }
  .wheel-modal { padding: 22px 14px; }
  .wheel-container { width: 260px; height: 260px; }
}

/* GustoPro home composition: dark culinary hero, warm editorial sections, compact cards. */
.home-wrapper { background: var(--color-surface); }
.hero-banner { min-height: 560px; justify-content: flex-start; text-align: left; background: linear-gradient(110deg, #211312 0%, #3e2c2b 65%, #65101d 145%); }
.hero-overlay { background: linear-gradient(90deg, rgba(20, 10, 10, 0.72), rgba(20, 10, 10, 0.34) 60%, rgba(20, 10, 10, 0.18)); }
.hero-content { width: min(1240px, 100%); max-width: none; margin: 0 auto; padding: 104px 24px 56px; }
.hero-tag { margin-bottom: 14px; padding: 5px 10px; border-radius: var(--radius-sm); font-size: 0.68rem; }
.hero-content h1 { max-width: 560px; font-family: var(--font-display); font-size: 3.25rem; letter-spacing: 0; text-shadow: none; }
.hero-content h1 span { color: #ffb3b1; text-shadow: none; }
.hero-content p { max-width: 520px; font-size: 1rem; letter-spacing: 0; text-shadow: none; }
.hero-actions { justify-content: flex-start; margin-bottom: 28px; }
.home-hero-btn { min-height: 42px; padding: 10px 20px; border-radius: var(--radius-md); font-size: 0.86rem; }
.home-hero-btn.g-btn-primary { background: var(--primary); border-color: var(--primary); color: var(--color-on-primary); }
.home-hero-outline { border-color: rgba(255, 255, 255, 0.65); }
.hero-stats { background: rgba(255, 248, 247, 0.11); border-color: rgba(255, 218, 216, 0.32); border-radius: var(--radius-md); padding: 12px 20px; }
.stat-item { padding: 0 14px; }.stat-num { font-size: 1.25rem; }.stat-lbl { font-size: 0.7rem; }
.main-content { padding: 64px 24px; background: var(--color-surface-container-low); }
.content-container { max-width: 1040px; grid-template-columns: minmax(0, 1.25fr) minmax(260px, 0.75fr); gap: 56px; align-items: start; }
.meta-tag { border-color: var(--color-outline-variant); background: var(--color-primary-fixed); color: var(--primary); border-radius: 999px; }
.article-title { font-family: var(--font-display); font-size: 2rem; color: var(--text-primary); }
.article-excerpt { color: var(--text-secondary); font-size: 1rem; line-height: 1.7; }
.home-action-btn { border-radius: var(--radius-md); }.home-action-btn.g-btn-primary { background: var(--primary); color: var(--color-on-primary); }
.feature-grid { gap: 12px; }.feature-card { background: var(--bg-card); border-color: var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.feature-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); transform: translateY(-2px); }.feature-card h4 { font-family: var(--font-display); color: var(--text-primary); }
.sidebar-card { background: var(--bg-card); border-color: var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }.sidebar-card h3 { font-family: var(--font-display); color: var(--text-primary); border-color: var(--color-outline-variant); }
.service-list a:hover { background: var(--color-surface-container); color: var(--primary); }.svc-badge.new, .svc-badge.hot { background: var(--color-primary-fixed); color: var(--primary); }.open-status { color: #197a45; }.open-dot { background: #197a45; }
.posts-section { padding: 64px 24px; border-color: var(--color-outline-variant); background: var(--color-surface); }.recruit-section { background: var(--color-surface-container-low); }.section-container { max-width: 1040px; }
.section-header-block h2 { font-family: var(--font-display); color: var(--text-primary); font-size: 2rem; }.section-tag { border-color: var(--color-outline-variant); background: var(--color-primary-fixed); color: var(--primary); border-radius: 999px; }
.news-grid { gap: 16px; }.news-card { border-color: var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }.news-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); transform: translateY(-2px); }.news-img-wrap { height: 170px; }.news-date-badge { background: var(--primary); }.news-body h3 { font-family: var(--font-display); color: var(--text-primary); }
.recruit-card { background: var(--bg-card); border-color: var(--color-outline-variant); border-radius: var(--radius-md); }.recruit-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); transform: none; }.recruit-icon { background: var(--color-primary-fixed); }.recruit-info h4 { color: var(--text-primary); }
.btn-apply, .btn-interview { background: var(--color-surface-container); border-color: var(--color-outline-variant); color: var(--primary); }.btn-apply:hover, .btn-interview:hover { background: var(--primary); color: var(--color-on-primary); }.fab-phone, .fab-chat { background: var(--primary); box-shadow: var(--shadow-md); }
@media (max-width: 1024px) { .hero-content { padding-right: 32px; padding-left: 32px; }.content-container { grid-template-columns: 1fr; gap: 32px; } }
@media (max-width: 640px) { .hero-banner { min-height: 520px; }.hero-content { padding: 98px 16px 36px; }.hero-content h1 { font-size: 2.45rem; }.hero-stats { width: 100%; justify-content: space-between; }.stat-item { padding: 0 6px; }.main-content, .posts-section { padding: 44px 16px; }.feature-grid { grid-template-columns: 1fr; }.article-title, .section-header-block h2 { font-size: 1.65rem; } }
</style>
