<template>
  <div class="app">
    <header class="m-head">
      <div class="me-profile">
        <div class="me-avatar">{{ userStore.user?.realName?.charAt(0) }}</div>
        <div>
          <div class="me-name">{{ userStore.user?.realName }}</div>
          <div class="me-dept">{{ deptLabel }} · {{ roleLabel }} · {{ userStore.user?.username }}</div>
        </div>
      </div>
    </header>

    <main class="m-body" style="margin-top:12px">
      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">账号与安全</div></div>
        <div class="m-card-body" style="padding:0">
          <router-link class="m-me-cell" to="/m/profile">
            <AppIcon name="i-users" :size="18" />
            个人资料
            <span class="m-me-val">{{ userStore.user?.realName }} · {{ userStore.user?.phone || '未绑定手机' }}</span>
          </router-link>
          <router-link class="m-me-cell" to="/m/profile">
            <AppIcon name="i-shield" :size="18" />
            修改密码
            <AppIcon name="i-arrow-right" :size="14" style="color:var(--muted-2)" class="m-me-val" />
          </router-link>
          <div class="m-me-cell" @click="readAll" v-if="unread > 0">
            <AppIcon name="i-bell" :size="18" />
            通知
            <span class="m-me-val">{{ unread }} 条未读，点击全部已读</span>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">帮助</div></div>
        <div class="m-card-body" style="padding:0">
          <div class="m-me-cell" @click="logout">
            <AppIcon name="i-logout" :size="18" />
            退出登录
            <span class="m-me-val" style="color:var(--danger)">退出</span>
          </div>
        </div>
      </section>

      <p class="m-card-sub" style="text-align:center">软包装生产日报表系统 v1.0</p>
    </main>

    <MTabbar tab="me" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import MTabbar from '@/components/MTabbar.vue'
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
  ElMessage.success('已全部标记为已读')
}

async function logout() {
  await ElMessageBox.confirm('确认退出登录？', '退出', { type: 'warning' })
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  unread.value = await api.get('/notices/unread-count')
})
</script>

<style scoped>
.me-profile { display: flex; align-items: center; gap: 14px; padding: 20px 16px; }
.me-avatar { width: 56px; height: 56px; border-radius: 50%; background: rgba(255,255,255,.18); border: 1px solid rgba(255,255,255,.3); display: grid; place-items: center; font-size: 20px; font-weight: 600; flex-shrink: 0; }
.me-name { font-size: 18px; font-weight: 600; }
.me-dept { font-size: 13px; color: rgba(255,255,255,.75); margin-top: 4px; }
</style>
