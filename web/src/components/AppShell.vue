<template>
  <div class="v-app-frame">
    <van-nav-bar :title="title" class="v-app-navbar" fixed placeholder safe-area-inset-top>
      <template #left>
        <slot name="nav-left">
          <van-icon v-if="backTo" name="arrow-left" size="20" @click="goBack" />
        </slot>
      </template>
      <template #right>
        <slot name="nav-right">
          <router-link to="/fill/me" aria-label="个人中心">
            <div class="nav-avatar">
              <img v-if="userStore.user?.avatarUrl" :src="userStore.user.avatarUrl" alt="头像" />
              <span v-else>{{ userStore.user?.realName?.charAt(0) }}</span>
            </div>
          </router-link>
        </slot>
      </template>
    </van-nav-bar>

    <main class="v-app-body">
      <slot />
    </main>

    <van-tabbar route fixed placeholder safe-area-inset-bottom class="v-app-tabbar">
      <van-tabbar-item replace to="/fill/home" icon="apps-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/fill/records" icon="records">记录</van-tabbar-item>
      <van-tabbar-item replace :to="fillPath" icon="edit">填报</van-tabbar-item>
      <van-tabbar-item v-if="userStore.isLead" replace to="/fill/review" icon="checked"
                       :badge="userStore.pendingCount || ''">审核</van-tabbar-item>
      <van-tabbar-item replace to="/fill/me" icon="contact">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ title: string; backTo?: string }>()
const userStore = useUserStore()
const router = useRouter()

/** 员工只进本部门填报页；主管/管理员默认印刷部 */
const fillPath = computed(() => {
  const dept = (userStore.user as any)?.dept
  if (userStore.isLead || userStore.user?.role === 'ADMIN') return '/fill/print'
  return ({ PRINT: '/fill/print', LAMINATION: '/fill/lamination', BAGMAKING: '/fill/bagmaking' } as any)[dept] ?? '/fill/print'
})

function goBack() {
  if (props.backTo) router.replace(props.backTo)
}
</script>

<style scoped>
.nav-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(255,255,255,.2); border: 1px solid rgba(255,255,255,.4);
  display: grid; place-items: center;
  color: #fff; font-size: 14px; font-weight: 600;
  overflow: hidden;
}
.nav-avatar img { width: 100%; height: 100%; object-fit: cover; font-size: 0; }
.v-app-body { padding-bottom: calc(76px + env(safe-area-inset-bottom)); }
</style>
