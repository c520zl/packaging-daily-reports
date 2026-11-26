<template>
  <AppShell title="修改密码" back-to="/fill/me">
    <van-notice-bar wrapable :scrollable="false" left-icon="info-o"
                    color="var(--muted)" background="var(--surface-2)"
                    text="密码修改成功后需重新登录，请牢记新密码" />

    <van-form ref="formRef" style="margin-top:12px" @failed="showToast({ type: 'fail', message: '请检查标红的必填项' })">
      <van-cell-group inset class="v-app-card">
        <van-field v-model="pwd.oldPassword" name="oldPassword" label="原密码" :type="showOld ? 'text' : 'password'"
                   required :rules="requiredRule('原密码')" placeholder="请输入当前密码">
          <template #right-icon>
            <van-icon :name="showOld ? 'eye-o' : 'closed-eye'" aria-label="显示或隐藏密码" @click="showOld = !showOld" />
          </template>
        </van-field>
        <van-field v-model="pwd.newPassword" name="newPassword" label="新密码" :type="showNew ? 'text' : 'password'"
                   required :rules="pwdRule" placeholder="6 位以上，建议含字母和数字">
          <template #right-icon>
            <van-icon :name="showNew ? 'eye-o' : 'closed-eye'" aria-label="显示或隐藏密码" @click="showNew = !showNew" />
          </template>
        </van-field>
        <van-field v-model="pwd.confirm" name="confirm" label="确认密码" :type="showConfirm ? 'text' : 'password'"
                   required :rules="confirmRules" placeholder="再次输入新密码">
          <template #right-icon>
            <van-icon :name="showConfirm ? 'eye-o' : 'closed-eye'" aria-label="显示或隐藏密码" @click="showConfirm = !showConfirm" />
          </template>
        </van-field>
      </van-cell-group>
    </van-form>

    <div style="padding:16px">
      <van-button block type="primary" :loading="savingPwd" @click="changePwd">确认修改</van-button>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const savingPwd = ref(false)
const formRef = ref()
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const showOld = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

const requiredRule = (label: string) => [{ required: true, message: `请填写${label}` }]
const pwdRule = [
  { required: true, message: '请填写新密码' },
  { validator: (v: string) => !v || v.length >= 6, message: '新密码至少 6 位' }
]
const confirmRules = [
  { required: true, message: '请再次输入新密码' },
  { validator: (v: string) => !v || v === pwd.newPassword, message: '两次输入的新密码不一致' }
]

async function changePwd() {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await api.put('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    showSuccessToast('密码已修改，请重新登录')
    userStore.logout()
    setTimeout(() => { location.href = '/login' }, 800)
  } catch { /* 拦截器已提示 */ }
  savingPwd.value = false
}
</script>
