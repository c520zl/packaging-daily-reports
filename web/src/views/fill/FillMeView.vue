<template>
  <AppShell :title="userStore.user?.realName ?? '我的'" :back-to="undefined">
    <!-- 头部资料卡 -->
    <div class="v-me-hero">
      <div class="v-me-avatar">
        <img v-if="userStore.user?.avatarUrl" :src="userStore.user.avatarUrl" alt="头像" />
        <span v-else>{{ userStore.user?.realName?.charAt(0) }}</span>
      </div>
      <div class="v-me-name">{{ userStore.user?.realName }}</div>
      <div class="v-me-sub">{{ deptLabel }} · {{ roleLabel }} · {{ userStore.user?.username }}</div>
    </div>

    <van-cell-group inset class="v-app-card">
      <div class="v-group-title">账号与安全</div>
      <van-cell title="个人资料" icon="user-o" is-link to="/fill/profile"
                :value="`${userStore.user?.realName} · ${userStore.user?.phone || '未绑定手机'}`" />
      <van-cell title="修改密码" icon="shield-o" is-link to="/fill/password" />
      <van-cell v-if="unread > 0" title="通知" icon="bell"
                :value="`${unread} 条未读，点击全部已读`" @click="readAll" />
    </van-cell-group>

    <van-cell-group inset class="v-app-card">
      <van-cell title="退出登录" icon="close" class="v-logout-cell" @click="logout" />
    </van-cell-group>

    <p class="v-copyright">软包装生产日报表系统 v1.0 · 填报端</p>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { DEPT, ROLE } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const unread = ref(0)

const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '全厂')
const roleLabel = computed(() => userStore.user?.role ? (ROLE as any)[userStore.user.role]?.label : '')

async function readAll() {
  await api.put('/notices/read-all')
  unread.value = 0
  showSuccessToast('已全部标记为已读')
}

async function logout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确认退出当前账号？' })
    try { await fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + userStore.token } }) } catch { /* 忽略 */ }
    userStore.logout()
    router.push('/login')
  } catch { /* cancel */ }
}

onMounted(async () => {
  try { unread.value = await api.get('/notices/unread-count') } catch { /* 忽略 */ }
})
</script>

<style scoped>
.v-me-hero {
  display: flex; flex-direction: column; align-items: center;
  padding: 20px 16px 16px;
  background: linear-gradient(160deg, #16337E 0%, #1D46C8 100%);
  color: #fff;
}
.v-me-avatar {
  width: 60px; height: 60px; border-radius: 50%;
  background: rgba(255,255,255,.18); border: 1px solid rgba(255,255,255,.3);
  display: grid; place-items: center;
  font-size: 22px; font-weight: 600; overflow: hidden;
}
.v-me-avatar img { width: 100%; height: 100%; object-fit: cover; font-size: 0; }
.v-me-name { font-size: var(--fs-lg); font-weight: 600; margin-top: 10px; }
.v-me-sub { font-size: var(--fs-sm); color: rgba(255,255,255,.75); margin-top: 4px; }
.v-logout-cell { color: var(--danger); }
.v-logout-cell :deep(.van-icon) { color: var(--danger); }
.v-copyright { text-align: center; font-size: var(--fs-xs); color: var(--muted-2); padding: 12px 0; }
</style>
