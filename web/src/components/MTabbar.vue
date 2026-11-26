<template>
  <nav class="m-tabbar">
    <router-link class="m-tab" :class="{ on: tab === 'home' }" to="/m/home">
      <AppIcon name="i-dashboard" :size="22" />首页
    </router-link>
    <router-link class="m-tab" to="/m/print" aria-label="快速填报">
      <span class="m-fab"><AppIcon name="i-plus" :size="22" /></span>
    </router-link>
    <router-link class="m-tab" :class="{ on: tab === 'me' }" to="/m/me">
      <AppIcon name="i-users" :size="22" />我的
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import AppIcon from './AppIcon.vue'
defineProps<{ tab: 'home' | 'me' }>()
</script>
