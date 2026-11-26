<template>
  <div class="app with-submitbar">
    <header class="m-head-plain">
      <router-link class="m-back-plain" to="/m/me">
        <AppIcon name="i-arrow-right" :size="18" style="transform:rotate(180deg)" />
      </router-link>
      <div class="m-title">个人资料</div>
    </header>

    <main class="m-body">
      <section class="m-card">
        <div class="m-card-body" style="padding-top:0; padding-bottom:0">
          <div class="avatar-edit">
            <div class="avatar-big">{{ userStore.user?.realName?.charAt(0) }}</div>
            <span style="font-size:12px; color:var(--muted)">头像请联系管理员上传（演示环境）</span>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">基本信息</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">用户名</label>
            <div class="m-calc">{{ userStore.user?.username }}<span class="hint">不可修改</span></div>
          </div>
          <div class="m-field">
            <label class="m-label">姓名（签名用）<span class="req">*</span></label>
            <el-input v-model="form.realName" />
          </div>
          <div class="m-field">
            <label class="m-label">性别</label>
            <div class="m-seg">
              <div class="m-seg-item" :class="{ on: form.gender === 'M' }" @click="form.gender = 'M'">男</div>
              <div class="m-seg-item" :class="{ on: form.gender === 'F' }" @click="form.gender = 'F'">女</div>
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">手机号<span class="req">*</span></label>
            <el-input v-model="form.phone" inputmode="numeric" />
          </div>
          <div class="m-field">
            <label class="m-label">电子邮箱</label>
            <el-input v-model="form.email" inputmode="email" />
          </div>
          <div class="m-field">
            <label class="m-label">住址</label>
            <el-input v-model="form.address" placeholder="如：制袋车间 / 宿舍 3 号楼" />
          </div>
          <div class="m-field">
            <label class="m-label">部门 / 角色</label>
            <div class="m-calc">{{ deptLabel }} · {{ roleLabel }}<span class="hint">由管理员分配</span></div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">修改密码</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">原密码<span class="req">*</span></label>
            <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </div>
          <div class="m-field">
            <label class="m-label">新密码<span class="req">*</span></label>
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6 位以上，含字母和数字" />
          </div>
          <div class="m-field">
            <label class="m-label">确认新密码<span class="req">*</span></label>
            <el-input v-model="pwd.confirm" type="password" show-password placeholder="再次输入新密码" />
          </div>
        </div>
      </section>
    </main>

    <div class="m-submitbar">
      <div class="sum">资料与密码修改后需重新验证</div>
      <el-button @click="changePwd" :loading="savingPwd">修改密码</el-button>
      <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { DEPT, ROLE } from '@/constants'

const userStore = useUserStore()
const saving = ref(false)
const savingPwd = ref(false)

const form = reactive({
  realName: userStore.user?.realName ?? '',
  gender: userStore.user?.gender ?? 'M',
  phone: userStore.user?.phone ?? '',
  email: userStore.user?.email ?? '',
  address: userStore.user?.address ?? ''
})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '全厂')
const roleLabel = computed(() => userStore.user?.role ? (ROLE as any)[userStore.user.role]?.label : '')

async function saveProfile() {
  saving.value = true
  try {
    await api.put('/me/profile', { ...form })
    await userStore.fetchMe()
    ElMessage.success('资料已保存')
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { ElMessage.warning('请填写原密码和新密码'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  savingPwd.value = true
  try {
    await api.put('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    location.href = '/login'
  } catch { /* 拦截器已提示 */ }
  savingPwd.value = false
}
</script>

<style scoped>
.avatar-edit { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px 0 6px; }
.avatar-big { width: 72px; height: 72px; border-radius: 50%; background: var(--accent-weak); color: #0A5E5D; display: grid; place-items: center; font-size: 26px; font-weight: 600; }
</style>
