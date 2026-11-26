<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo">包</div>
        <div class="brand-name">生产日报表</div>
      </div>
      <nav class="nav">
        <router-link class="nav-item" :class="{ active: $route.path === '/dashboard' }" to="/dashboard">
          <AppIcon name="i-dashboard" :size="18" /><span class="nav-label">工作台</span>
        </router-link>
        <div class="nav-group">日报填报</div>
        <router-link class="nav-item" :class="{ active: $route.path === '/print' }" to="/print">
          <AppIcon name="i-print" :size="18" /><span class="nav-label">印刷部日报</span>
        </router-link>
        <router-link class="nav-item" :class="{ active: $route.path === '/lamination' }" to="/lamination">
          <AppIcon name="i-layers" :size="18" /><span class="nav-label">复合部日报</span>
        </router-link>
        <router-link class="nav-item" :class="{ active: $route.path === '/bagmaking' }" to="/bagmaking">
          <AppIcon name="i-bag" :size="18" /><span class="nav-label">制袋部日报</span>
        </router-link>
        <div class="nav-group">数据管理</div>
        <router-link class="nav-item" :class="{ active: $route.path === '/reports' || $route.path.startsWith('/detail') }" to="/reports">
          <AppIcon name="i-report" :size="18" /><span class="nav-label">报表查询</span>
        </router-link>
        <router-link v-if="userStore.isAdmin" class="nav-item" :class="{ active: $route.path === '/users' }" to="/users">
          <AppIcon name="i-users" :size="18" /><span class="nav-label">用户管理</span>
        </router-link>
      </nav>
    </aside>

    <div class="main-wrap">
      <header class="topbar">
        <div class="topbar-title">{{ title }}</div>
        <div class="topbar-right">
          <span v-if="badge" class="tag tag-blue"><span class="dot"></span>{{ badge }}</span>
          <span class="topbar-date">{{ today }}</span>
          <button class="theme-toggle" :title="themeState.isDark ? '切换到浅色模式' : '切换到深色模式'" @click="toggleTheme">
            <!-- 太阳（暗色时显示，点击回浅色）/ 月亮（浅色时显示，点击去暗色） -->
            <svg v-if="themeState.isDark" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round">
              <circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>
            </svg>
            <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>
            </svg>
          </button>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="topbar-user user-dropdown">
              <img v-if="userStore.user?.avatarUrl" class="avatar avatar-img" :src="userStore.user.avatarUrl" alt="头像" />
              <div v-else class="avatar">{{ userStore.user?.realName?.charAt(0) || '?' }}</div>
              <div class="user-meta">
                <span class="user-name">{{ userStore.user?.realName }}</span>
                <span class="user-role">{{ userStore.user?.username }} · {{ roleLabel }}</span>
              </div>
              <AppIcon name="i-arrow-right" :size="12" style="color:var(--muted-2); transform:rotate(90deg)" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="logout-item">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="page">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from './AppIcon.vue'
import { useUserStore } from '@/stores/user'
import { ROLE } from '@/constants'
import { themeState, toggleTheme } from '@/theme'

const props = defineProps<{ title: string; badge?: string }>()
const userStore = useUserStore()
const router = useRouter()
const roleLabel = computed(() => {
  const r = userStore.user?.role as keyof typeof ROLE
  return ROLE[r]?.label ?? ''
})
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'long' }).replace(/\//g, '-')

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    try { await fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + userStore.token } }) } catch { /* 忽略 */ }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
  if (cmd === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.user-dropdown { display: flex; align-items: center; gap: 10px; padding: 6px 10px; cursor: pointer; border-radius: var(--radius-md); outline: none; }
.user-dropdown:hover { background: var(--surface-2); }
.avatar-img { object-fit: cover; }
.user-meta { display: flex; flex-direction: column; line-height: 1.3; }
.user-name { font-size: var(--fs-sm); font-weight: 600; }
.user-role { font-size: var(--fs-xs); color: var(--muted); }
.theme-toggle {
  width: 34px; height: 34px;
  border-radius: var(--radius-md);
  border: none; background: none; cursor: pointer;
  display: grid; place-items: center;
  color: var(--muted);
  transition: background .15s, color .15s;
}
.theme-toggle:hover { background: var(--surface-2); color: var(--primary); }
:deep(.logout-item) { color: var(--danger); }
</style>
