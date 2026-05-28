<template>
  <div class="skeleton-wrapper" :class="[`skeleton-${variant}`]">
    <div v-if="variant === 'card'" class="sk-card">
      <div class="sk-img shimmer"></div>
      <div class="sk-body">
        <div class="sk-line sk-line-lg shimmer"></div>
        <div class="sk-line sk-line-md shimmer"></div>
        <div class="sk-line sk-line-sm shimmer"></div>
      </div>
    </div>

    <div v-else-if="variant === 'table-row'" class="sk-table-row">
      <div class="sk-cell shimmer" v-for="n in columns" :key="n"></div>
    </div>

    <div v-else-if="variant === 'text'" class="sk-text">
      <div class="sk-line shimmer" v-for="n in lines" :key="n"
        :style="{ width: n === lines ? '60%' : '100%' }"
      ></div>
    </div>

    <div v-else-if="variant === 'avatar'" class="sk-avatar shimmer"></div>

    <div v-else class="sk-block shimmer" :style="{ width, height }"></div>
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'block' }, // card, table-row, text, avatar, block
  width: { type: String, default: '100%' },
  height: { type: String, default: '20px' },
  lines: { type: Number, default: 3 },
  columns: { type: Number, default: 5 }
})
</script>

<style scoped>
/* Shimmer Animation */
.shimmer {
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
}
.shimmer::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.06) 40%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.06) 60%,
    transparent 100%
  );
  animation: shimmerSlide 1.8s ease-in-out infinite;
}

@keyframes shimmerSlide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* Card Skeleton */
.sk-card {
  border-radius: var(--radius-lg, 20px);
  border: 1px solid rgba(255, 255, 255, 0.04);
  overflow: hidden;
  background: rgba(13, 27, 42, 0.4);
}
.sk-img {
  height: 200px;
  width: 100%;
}
.sk-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Lines */
.sk-line {
  height: 14px;
}
.sk-line-lg { height: 20px; width: 70%; }
.sk-line-md { height: 14px; width: 100%; }
.sk-line-sm { height: 14px; width: 50%; }

/* Text block */
.sk-text {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Table row */
.sk-table-row {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}
.sk-cell {
  flex: 1;
  height: 16px;
}

/* Avatar */
.sk-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

/* Block */
.sk-block {
  border-radius: var(--radius-md, 14px);
}
</style>
