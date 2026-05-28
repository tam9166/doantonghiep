<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <Transition name="page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </router-view>
    
    <!-- Global Toast (luôn hiển thị) -->
    <ToastGlobal />
  </div>
</template>

<script setup>
import ToastGlobal from '@/components/ToastGlobal.vue'
</script>

<style>
#app {
  min-height: 100vh;
}

/* Page Transitions */
.page-enter-active {
  animation: pageIn 0.35s ease-out;
}
.page-leave-active {
  animation: pageOut 0.2s ease-in;
}

@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes pageOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-8px);
  }
}
</style>