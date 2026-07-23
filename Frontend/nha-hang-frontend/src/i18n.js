import { createI18n } from 'vue-i18n';
import { watch } from 'vue';
import en from './locales/en.json';
import vi from './locales/vi.json';
import reservationMessages from './locales/reservationMessages';

// Cấu hình vue-i18n
const i18n = createI18n({
  legacy: false, // Dùng Composition API
  locale: localStorage.getItem('lang') || 'vi', // Mặc định là Tiếng Việt
  fallbackLocale: 'en', // Fallback là Tiếng Anh
  messages: {
    en: { ...en, reservation: reservationMessages.en },
    vi: { ...vi, reservation: reservationMessages.vi }
  }
});

watch(i18n.global.locale, value => {
  localStorage.setItem('lang', value);
  document.documentElement.lang = value;
}, { immediate: true });

export default i18n;
