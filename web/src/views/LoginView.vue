<template>
  <div class="login-page">
    <aside class="login-aside">
      <div class="aside-brand">
        <div class="aside-logo">包</div>
        <div class="aside-name">软包装生产日报表系统</div>
      </div>
      <div class="aside-hero">
        <h1>纸质日报表，<br>一上线。</h1>
        <p>印刷、复合、制袋三个部门统一在线填报，机速、米数、废料自动汇总，管理层随时掌握当天生产全貌。</p>
        <div class="aside-points">
          <div class="aside-point"><i><AppIcon name="i-print" :size="13" /></i>印刷部 · 逐单记录上机与实际印刷米数</div>
          <div class="aside-point"><i><AppIcon name="i-layers" :size="13" /></i>复合部 · 干式 / 无溶剂机型分类统计</div>
          <div class="aside-point"><i><AppIcon name="i-bag" :size="13" /></i>制袋部 · 八种袋型、1–8 号机全覆盖</div>
        </div>
      </div>
      <div class="aside-foot">© 2026 生产部 · 信息管理组</div>
    </aside>

    <main class="login-main">
      <div class="login-box">
        <h2>欢迎登录</h2>
        <p class="sub">请使用车间分配的账号登录系统</p>
        <el-form class="login-form" @submit.prevent="onSubmit">
          <div class="field">
            <label class="field-label">用户名</label>
            <el-input v-model="username" placeholder="请输入用户名" size="large" @keyup.enter="onSubmit">
              <template #prefix><AppIcon name="i-users" :size="17" style="color:var(--muted-2)" /></template>
            </el-input>
          </div>
          <div class="field">
            <label class="field-label">密码</label>
            <el-input v-model="password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="onSubmit">
              <template #prefix><AppIcon name="i-shield" :size="17" style="color:var(--muted-2)" /></template>
            </el-input>
          </div>
          <el-button class="btn-login" type="primary" size="large" :loading="loading" native-type="submit" @click="onSubmit">
            登 录
          </el-button>
        </el-form>
        <div class="login-tip">
          <b>演示账号：</b>印刷部 yinshua-01 / 员工 yinshua-02 / 复合部 fuhe-01 / 制袋部 zhidai-01 / 管理员 admin，密码均为 123456。
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await store.login(username.value, password.value)
    await store.fetchMe()
    ElMessage.success(`欢迎回来，${store.user?.realName ?? username.value}！登录成功`)
    // 分流：员工/主管进填报端（Vant 手机优先响应式，手机/桌面同一入口）；管理员/管理层进管理端
    const role = store.user?.role
    router.push(role === 'WORKER' || role === 'DEPT_LEAD' ? '/fill/home' : '/dashboard')
  } catch { /* 拦截器已提示 */ }
  loading.value = false
}
</script>

<style scoped>
@import '@/styles/login.css';
</style>
