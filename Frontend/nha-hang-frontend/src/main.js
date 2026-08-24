import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import UiIcon from './components/UiIcon.vue'
import { useToast } from './composables/useToast'
import './services/api'
import './assets/theme-tokens.css'
import './assets/global.css'

// Keep legacy call sites non-blocking while rendering every notification
// through the shared application toast instead of the browser alert dialog.
const toast = useToast()
window.alert = message => toast.info(String(message ?? ''))

const app = createApp(App)
app.component('UiIcon', UiIcon)

app.use(createPinia())
app.use(router)
app.use(i18n)

app.mount('#app')
