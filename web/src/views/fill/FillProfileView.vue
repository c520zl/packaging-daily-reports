<template>
  <AppShell title="个人资料" back-to="/fill/me">
    <van-form ref="formRef" @failed="showToast({ type: 'fail', message: '请检查标红的必填项' })">
      <!-- 头像 -->
      <div class="v-avatar-edit">
        <div class="v-avatar-big">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <span v-else>{{ userStore.user?.realName?.charAt(0) || '?' }}</span>
          <van-loading v-if="uploading" class="v-avatar-loading" />
        </div>
        <!-- 显式触发：button 是 labelable 元素，放进 label 里会抢走转发导致点击无反应 -->
        <van-button size="small" plain type="primary" :loading="uploading" @click="pickAvatar">
          {{ uploading ? '上传中…' : '更换头像' }}
        </van-button>
        <input ref="fileInput" type="file" accept=".jpg,.jpeg,.png" hidden @change="onAvatarChange" />
        <p class="v-avatar-hint">JPG / PNG，不超过 2MB</p>
      </div>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">基本信息</div>
        <van-field label="用户名" :model-value="userStore.user?.username" readonly />
        <van-field v-model="form.realName" name="realName" label="姓名" placeholder="日报签名用"
                   required :rules="requiredRule('姓名')" clearable maxlength="20" />
        <van-field name="gender" label="性别">
          <template #input>
            <van-radio-group v-model="form.gender" direction="horizontal">
              <van-radio name="M">男</van-radio>
              <van-radio name="F">女</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field v-model="form.phone" name="phone" label="手机号" type="tel" maxlength="11"
                   :rules="phoneRule" placeholder="11 位手机号" clearable />
        <van-field v-model="form.email" name="email" label="邮箱" placeholder="name@example.com" clearable
                   :rules="[{ pattern: /^$|^[\w.%-]+@[\w.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确' }]" />
        <van-field v-model="form.address" name="address" label="住址" placeholder="如：制袋车间 / 宿舍 3 号楼" clearable />
        <van-field label="部门 / 角色" :model-value="`${deptLabel} · ${roleLabel}`" readonly />
      </van-cell-group>
    </van-form>

    <div class="v-submit-flow">
      <div class="sum">签名将使用此处保存的姓名</div>
      <van-button size="small" type="primary" :loading="saving" @click="saveProfile">保存资料</van-button>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { DEPT, ROLE } from '@/constants'

const userStore = useUserStore()
const saving = ref(false)
const uploading = ref(false)
const avatarUrl = ref(userStore.user?.avatarUrl || '')
const formRef = ref()
const fileInput = ref<HTMLInputElement>()

/** 显式打开文件选择器（用户手势上下文内调用，跨浏览器可靠） */
function pickAvatar() {
  fileInput.value?.click()
}

const form = reactive({
  realName: userStore.user?.realName ?? '',
  gender: userStore.user?.gender ?? 'M',
  phone: userStore.user?.phone ?? '',
  email: userStore.user?.email ?? '',
  address: userStore.user?.address ?? ''
})

const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '全厂')
const roleLabel = computed(() => userStore.user?.role ? (ROLE as any)[userStore.user.role]?.label : '')

const requiredRule = (label: string) => [{ required: true, message: `请填写${label}` }]
const phoneRule = [{ validator: (v: string) => !v || /^1\d{10}$/.test(v.replace(/\s/g, '')), message: '手机号格式不正确' }]

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { showToast({ type: 'fail', message: '图片不能超过 2MB' }); input.value = ''; return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const data: any = await api.post('/me/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    avatarUrl.value = data.avatarUrl + '?t=' + Date.now()
    await userStore.fetchMe()
    showSuccessToast('头像已更新')
  } catch { /* 拦截器已提示 */ }
  uploading.value = false
  input.value = ''
}

async function saveProfile() {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await api.put('/me/profile', { ...form })
    await userStore.fetchMe()
    showSuccessToast('资料已保存')
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

onMounted(async () => {
  try {
    const me: any = await api.get('/me/profile')
    Object.assign(form, { realName: me.realName, gender: me.gender ?? 'M', phone: me.phone ?? '', email: me.email ?? '', address: me.address ?? '' })
    avatarUrl.value = me.avatarUrl || ''
  } catch { /* 拦截器已提示 */ }
})
</script>

<style scoped>
.v-avatar-edit { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 16px 0 8px; }
.v-avatar-big {
  width: 72px; height: 72px; border-radius: 50%;
  background: var(--accent-weak); color: #0A5E5D;
  display: grid; place-items: center; font-size: 26px; font-weight: 600;
  overflow: hidden; position: relative;
}
.v-avatar-big img { width: 100%; height: 100%; object-fit: cover; font-size: 0; }
.v-avatar-loading { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(255,255,255,.6); }
.v-avatar-pick { cursor: pointer; }
.v-avatar-hint { font-size: var(--fs-xs); color: var(--muted-2); }
</style>
