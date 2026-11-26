<template>
  <svg :width="size" :height="size" class="icon"><use :href="`/icons.svg#${name}`" /></svg>
</template>

<script setup lang="ts">
/** 图标组件：引用 public/icons.svg 的 symbol（设计稿同源） */
withDefaults(defineProps<{ name: string; size?: number }>(), { size: 18 })
</script>

<style scoped>
.icon { display: inline-block; vertical-align: -3px; }
</style>
