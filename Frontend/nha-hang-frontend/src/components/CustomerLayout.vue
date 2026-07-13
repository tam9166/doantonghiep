<template>
  <div class="customer-layout">
    <AppNavbar :transparent="transparentNav" />
    <main class="customer-main" :class="{ 'has-hero': transparentNav }">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import AppNavbar from '@/components/AppNavbar.vue'
import AppFooter from '@/components/AppFooter.vue'

defineProps({
  transparentNav: { type: Boolean, default: false }
})
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-root);
  overflow-x: clip;
}

.customer-main {
  flex: 1;
  min-width: 0;
  max-width: 100%;
  padding-top: 72px; /* Space for fixed navbar */
}

.customer-main.has-hero {
  padding-top: 0; /* Hero overlaps under transparent navbar */
}

@media (max-width: 640px) {
  .customer-main:not(.has-hero) { padding-top: 68px; }
}
</style>
