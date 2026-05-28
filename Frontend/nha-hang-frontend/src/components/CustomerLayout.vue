<template>
  <div class="customer-layout">
    <AppNavbar :transparent="transparentNav" />
    <main class="customer-main" :class="{ 'has-hero': transparentNav }">
      <slot />
    </main>
    <AppFooter />

    <!-- Global Toast -->
    <ToastGlobal />
  </div>
</template>

<script setup>
import AppNavbar from '@/components/AppNavbar.vue'
import AppFooter from '@/components/AppFooter.vue'
import ToastGlobal from '@/components/ToastGlobal.vue'

defineProps({
  transparentNav: { type: Boolean, default: false }
})
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-root);
}

.customer-main {
  flex: 1;
  padding-top: 72px; /* Space for fixed navbar */
}

.customer-main.has-hero {
  padding-top: 0; /* Hero overlaps under transparent navbar */
}
</style>
