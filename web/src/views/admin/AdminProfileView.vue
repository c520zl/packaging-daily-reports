<template>
  <AdminShell title="个人资料">
    <div class="profile-cols">
      <!-- 左：头像与账号信息 -->
      <section class="card">
        <div class="card-head"><div class="card-title">头像</div></div>
        <div class="card-body avatar-area">
          <div class="avatar-big" :class="{ uploading }">
            <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
            <span v-else>{{ userStore.user?.realName?.charAt(0) || '?' }}</span>
            <div class="avatar-mask" v-if="uploading"><el-icon class="is-loading"><Loading /></el-icon></div>
          </div>
          <label class="avatar-pick">
            <input type="file" accept=".jpg,.jpeg,.png" hidden @change="onAvatarChange" :disabled="uploading" />
            <el-button size="small" tag="span" :loading="uploading">更换头像</el-button>
          </label>
          <p class="avatar-hint">JPG / PNG，不超过 2MB</p>
        </div>

        <div class="card-head"><div class="card-title">账号信息</div><div class="card-sub">由管理员分配，不可修改</div></div>
        <div class="card-body">
          <div class="kv-grid">
            <div class="kv"><div class="k">用户名</div><div class="v sub-val">{{ userStore.user?.username }}</div></div>
            <div class="kv"><div class="k">角色</div><div class="v sub-val">{{ roleLabel }}</div></div>
            <div class="kv"><div class="k">部门</div><div class="v sub-val">{{ deptLabel }}</div></div>
          </div>
        </div>
      </section>

      <!-- 中：资料编辑 -->
      <section class="card">
        <div class="card-head"><div class="card-title">基本资料</div></div>
        <div class="card-body profile-form">
          <div class="field"><label class="field-label">姓名（日报签名用）<span class="req">*</span></label>
            <el-input v-model="form.realName" maxlength="20" show-word-limit /></div>
          <div class="field"><label class="field-label">性别</label>
            <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="M" /><el-option label="女" value="F" /></el-select></div>
          <div class="field"><label class="field-label">手机号</label>
            <el-input v-model="form.phone" inputmode="numeric" maxlength="13" placeholder="11 位手机号" /></div>
          <div class="field"><label class="field-label">电子邮箱</label>
            <el-input v-model="form.email" placeholder="name@example.com" /></div>
          <div class="field"><label class="field-label">住址</label>
            <el-input v-model="form.address" placeholder="如：印刷车间 / 宿舍 3 号楼" /></div>
          <el-button type="primary" :loading="saving" @click="saveProfile">
            <AppIcon name="i-check" :size="15" />&nbsp;保存资料
          </el-button>
        </div>
      </section>

      <!-- 右：修改密码 -->
      <section class="card">
        <div class="card-head"><div class="card-title">修改密码</div></div>
        <div class="card-body profile-form">
          <div class="field"><label class="field-label">原密码<span class="req">*</span></label>
            <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="当前密码" /></div>
          <div class="field"><label class="field-label">新密码<span class="req">*</span></label>
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6 位以上" /></div>
          <div class="field"><label class="field-label">确认新密码<span class="req">*</span></label>
            <el-input v-model="pwd.confirm" type="password" show-password placeholder="再次输入" /></div>
          <el-button :loading="savingPwd" @click="changePwd">修改密码</el-button>
          <p class="pwd-hint">修改成功后需重新登录</p>
        </div>
      </section>
    </div>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { ROLE, DEPT } from '@/constants'

const userStore = useUserStore()
const router = useRouter()
const saving = ref(false)
const savingPwd = ref(false)
const uploading = ref(false)
const avatarUrl = ref(userStore.user?.avatarUrl || '')

const form = reactive({
  realName: userStore.user?.realName ?? '',
  gender: userStore.user?.gender ?? 'M',
  phone: userStore.user?.phone ?? '',
  email: userStore.user?.email ?? '',
  address: userStore.user?.address ?? ''
})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const roleLabel = computed(() => userStore.user?.role ? (ROLE as any)[userStore.user.role]?.label : '')
const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '—')

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片不能超过 2MB'); input.value = ''; return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const data: any = await api.post('/me/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    avatarUrl.value = data.avatarUrl + '?t=' + Date.now()
    // 刷新 store 里的头像
    await userStore.fetchMe()
    ElMessage.success('头像已更新')
  } catch { /* 拦截器已提示 */ }
  uploading.value = false
  input.value = ''
}

async function saveProfile() {
  if (!form.realName?.trim()) { ElMessage.warning('姓名不能为空'); return }
  if (form.phone && !/^1\d{10}$/.test(form.phone.replace(/\s/g, ''))) { ElMessage.warning('手机号格式不正确'); return }
  saving.value = true
  try {
    await api.put('/me/profile', { ...form })
    await userStore.fetchMe()
    ElMessage.success(`个人资料已更新${form.realName !== userStore.user?.realName ? '，签名将使用新姓名' : ''}`)
    await userStore.fetchMe()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { ElMessage.warning('请填写原密码和新密码'); return }
  if (pwd.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  savingPwd.value = true
  try {
    await api.put('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    setTimeout(() => router.push('/login'), 800)
  } catch { /* 拦截器已提示 */ }
  savingPwd.value = false
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
.profile-cols { display: grid; grid-template-columns: 300px 1fr 320px; gap: var(--space-4); align-items: start; }
.avatar-area { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.avatar-big {
  width: 96px; height: 96px; border-radius: 50%;
  background: var(--accent-weak); color: #0A5E5D;
  display: grid; place-items: center;
  font-size: 34px; font-weight: 600;
  overflow: hidden; position: relative;
}
.avatar-big img { width: 100%; height: 100%; object-fit: cover; }
.avatar-big.uploading { opacity: .6; }
.avatar-mask { position: absolute; inset: 0; display: grid; place-items: center; color: var(--text); }
.avatar-pick { cursor: pointer; }
.avatar-hint { font-size: var(--fs-xs); color: var(--muted-2); }
.profile-form { display: flex; flex-direction: column; gap: 12px; max-width: 420px; }
.pwd-hint { font-size: var(--fs-xs); color: var(--muted-2); }
.kv-grid { display: grid; grid-template-columns: 1fr; gap: var(--space-2); }
@media (max-width: 1100px) {
  .profile-cols { grid-template-columns: 1fr; }
}
</style>
