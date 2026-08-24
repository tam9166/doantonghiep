<template>
  <svg
    class="admin-nav-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="path in iconPaths" :key="path" :d="path" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true }
})

const icons = {
  analytics: ['M4 19V10h4v9H4Z', 'M10 19V5h4v14h-4Z', 'M16 19v-7h4v7h-4Z'],
  ai: ['M9 4a3 3 0 0 0-3 3v1a3 3 0 0 0-2 5 3 3 0 0 0 3 4h2', 'M15 4a3 3 0 0 1 3 3v1a3 3 0 0 1 2 5 3 3 0 0 1-3 4h-2', 'M9 4v16', 'M15 4v16', 'M9 12h6'],
  product: ['M4 7h16l-1 13H5L4 7Z', 'M8 7a4 4 0 0 1 8 0'],
  category: ['M3 6h7l2 2h9v11H3V6Z'],
  orders: ['M7 4h10v16H7V4Z', 'M9 8h6', 'M9 12h6', 'M9 16h4'],
  reservations: ['M4 6h16v14H4V6Z', 'M8 3v6', 'M16 3v6', 'M4 10h16'],
  cancellation: ['M9 7 4 12l5 5', 'M4 12h10a6 6 0 0 1 6 6'],
  reviews: ['m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2 7.5 14 3 9.6l6.2-.9L12 3Z'],
  customers: ['M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M22 20v-2a4 4 0 0 0-3-3.9', 'M16 2.1a4 4 0 0 1 0 7.8'],
  deposit: ['M3 6h18v12H3V6Z', 'M3 10h18', 'M7 15h3'],
  tables: ['M5 9h14', 'M7 9v10', 'M17 9v10', 'M4 5h16v4H4V5Z'],
  areas: ['M4 21V3h10v18', 'M14 9h6v12', 'M8 7h2', 'M8 11h2', 'M8 15h2'],
  ingredients: ['M12 21V9', 'M12 13C7 13 4 10 4 5c5 0 8 3 8 8Z', 'M12 16c4 0 7-2 8-6-4 0-7 2-8 6Z'],
  popular: ['M12 22c4 0 7-3 7-7 0-3-2-5-4-7 0 3-1 4-2 5 0-5-2-8-5-11 0 5-3 7-3 12 0 4 3 7 7 7Z'],
  purchase: ['M3 4h2l2 11h10l3-7H6', 'M9 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z', 'M17 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z'],
  staff: ['M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M8.5 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M20 8v6', 'M17 11h6'],
  posts: ['M4 4h16v16H4V4Z', 'M8 8h8', 'M8 12h8', 'M8 16h5'],
  vouchers: ['M3 8a2 2 0 0 0 0 4v4h18v-4a2 2 0 0 0 0-4V4H3v4Z', 'M12 6v2', 'M12 12v2'],
  activity: ['M4 5h16v15H4V5Z', 'M8 3v4', 'M16 3v4', 'M8 11h8', 'M8 15h5'],
  profile: ['M20 21a8 8 0 0 0-16 0', 'M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z'],
  home: ['M3 11 12 3l9 8', 'M5 10v10h14V10', 'M9 20v-6h6v6'],
  logout: ['M10 17l5-5-5-5', 'M15 12H3', 'M15 4h5v16h-5']
}

const iconPaths = computed(() => icons[props.name] || icons.activity)
</script>

<style scoped>
.admin-nav-icon {
  display: block;
  width: 20px;
  height: 20px;
}
</style>
