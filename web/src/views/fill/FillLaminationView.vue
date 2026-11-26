<template>
  <AppShell title="复合部日报" back-to="/fill/home">
    <van-form ref="formRef" @failed="onValidateFailed">
      <van-cell-group inset class="v-app-card">
        <van-field name="date" label="日报日期" required>
          <template #input><span style="flex:1">{{ form.date }}</span></template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">生产信息 <span v-if="editingId" class="v-tag v-tag--orange">修改中</span></div>
        <van-field v-model="form.orderId" name="orderId" label="生产单号" placeholder="如 FH-20261124-01"
                   required :rules="orderIdRules" clearable />
        <van-field v-model="form.times" name="times" label="复合次数" type="digit"
                   required :rules="numRule('复合次数', true)" placeholder="如 2" />
        <van-field v-model="form.speed" name="speed" label="机速 m/min" type="number"
                   required :rules="numRule('机速', true)" placeholder="如 180" />
        <van-field name="machineType" label="复合机型" required :rules="requiredRule('机型')">
          <template #input>
            <van-radio-group v-model="form.machineType" direction="horizontal">
              <van-radio name="DRY">干式</van-radio>
              <van-radio name="SOLVENT_FREE">无溶剂</van-radio>
            </van-radio-group>
          </template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">材料信息</div>
        <van-field v-model="form.material" name="material" label="材质" placeholder="如 PET+PE"
                   required :rules="requiredRule('材质')" clearable />
        <van-field v-model="form.spec" name="spec" label="规格" placeholder="如 300×200 mm"
                   required :rules="requiredRule('规格')" clearable />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">生产数量</div>
        <van-field v-model="form.requiredMeter" name="requiredMeter" label="工单要求米数" type="number"
                   required :rules="numRule('要求米数', true)" placeholder="如 6000" />
        <van-field v-model="form.actualMeter" name="actualMeter" label="实际复合米数" type="number"
                   required :rules="numRule('实际米数', true)" placeholder="如 5800" />
        <van-field v-model="form.rolls" name="rolls" label="卷数" type="digit" placeholder="选填" />
        <van-field v-model="form.adjustMeter" name="adjustMeter" label="调机米数" type="number" placeholder="选填" />
        <van-field v-model="form.printRemain" name="printRemain" label="印刷剩余米数" type="number" placeholder="选填" />
        <van-notice-bar wrapable :scrollable="false" color="var(--muted)" background="var(--surface-2)"
                        left-icon="info-o" text="调机米数为开机调试消耗的膜料，不计入实际复合产量" />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">签名</div>
        <van-field v-model="form.signer" name="signer" label="签名" required :rules="requiredRule('签名')" clearable />
      </van-cell-group>
    </van-form>

    <!-- 提交区：紧贴表单尾部（P1：主操作离填写字段最近） -->
    <div class="v-submit-flow">
      <div class="sum">{{ editingId ? '修改完成后请重新提交' : '可先存草稿，稍后继续' }}</div>
      <van-button size="small" plain :loading="saving" @click="save(false)">存草稿</van-button>
      <van-button size="normal" type="primary" :loading="saving" @click="save(true)">提交日报</van-button>
    </div>
    <!-- 当日已录（参考资料，置于提交区之后） -->
    <van-cell-group inset class="v-app-card">
      <template #title><div class="v-group-title" style="padding-bottom:8px">当日已录（{{ rows.length }}）</div></template>
      <van-cell v-for="r in rows" :key="r.id" center>
        <template #title>
          <div class="v-record-top">
            <span class="v-record-no">{{ r.orderId }}</span>
            <span class="v-tag" :class="statusCls(r.status)" style="margin-left:auto">{{ statusLabel(r.status) }}</span>
          </div>
          <div class="v-record-meta">{{ r.onboardTime }} 上机 · 机速 {{ fmt(r.speed) }} · {{ r.material }} {{ r.spec }}</div>
        </template>
        <template #label>
          <div class="v-record-nums">
            <span>要求 <b>{{ fmt(r.requiredMeter) }}</b></span>
            <span>实际 <b>{{ fmt(r.actualMeter) }}</b></span>
            <span>用时 <b>{{ fmt(r.printMinutes) }} 分</b></span>
          </div>
        </template>
        <template #right-icon>
          <div v-if="canEdit(r)" class="v-row-actions">
            <van-icon name="edit" size="18" color="var(--primary)" aria-label="编辑此行" @click.stop="editRow(r)" />
            <van-icon name="delete-o" size="18" color="var(--danger)" aria-label="删除此行" @click.stop="removeRow(r)" />
          </div>
        </template>
      </van-cell>
      <van-empty v-if="!rows.length" description="当日还没有记录，填写后点「提交日报」" image-size="64" />
    </van-cell-group>
  </AppShell>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, MACHINE_TYPE, todayStr } from '@/constants'

const userStore = useUserStore()
const rows = ref<any[]>([])
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()

function blankForm(): any {
  return {
    date: todayStr(), orderId: '', times: '2', speed: '', material: '', spec: '',
    requiredMeter: '', actualMeter: '', rolls: '', printRemain: '',
    machineType: 'DRY', adjustMeter: '', signer: userStore.user?.realName ?? ''
  }
}
const form = reactive<any>(blankForm())

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => ({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' }[s] ?? 'v-tag--gray')
const machineTypeLabel = (t: string) => (MACHINE_TYPE as any)[t] ?? t
const canEdit = (r: any) => r.status === 'DRAFT' || r.status === 'REJECTED'
const fmt = (n: any) => (n === null || n === undefined || n === '' ? '—' : Number(n).toLocaleString('zh-CN'))

const orderIdRules = [
  { required: true, message: '请填写生产单号' },
  { pattern: /^[\w\u4e00-\u9fa5-]{2,50}$/, message: '单号仅限字母数字中文与连字符' }
]
const requiredRule = (label: string) => [{ required: true, message: `请填写${label}` }]
const numRule = (label: string, required: boolean) => {
  const rules: any[] = []
  if (required) rules.push({ required: true, message: `请填写${label}` })
  rules.push({ validator: (v: string) => !v || (Number(v) > 0 && Number(v) < 100000000), message: '须为正数' })
  return rules
}

function onValidateFailed() {
  showToast({ type: 'fail', message: '请检查标红的必填项' })
}

function toReq() {
  return {
    date: form.date, orderId: form.orderId, times: form.times || null, speed: form.speed || null,
    material: form.material, spec: form.spec, requiredMeter: form.requiredMeter || null,
    actualMeter: form.actualMeter || null, rolls: form.rolls || null, printRemain: form.printRemain || null,
    machineType: form.machineType, adjustMeter: form.adjustMeter || null, signer: form.signer
  }
}

async function load() {
  try {
    const data = await api.get('/lamination-daily', { params: { page: 1, size: 200, date: form.date } })
    rows.value = data.records
  } catch { /* 拦截器已提示 */ }
}

async function save(submit: boolean) {
  if (submit) {
    const valid = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!valid) { onValidateFailed(); return }
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/lamination-daily/${editingId.value}`, { ...toReq(), submit: false })
      if (submit) await api.put(`/lamination-daily/${editingId.value}/submit`)
    } else {
      await api.post('/lamination-daily', { ...toReq(), submit })
    }
    showSuccessToast(submit ? '已提交，等待主管审核' : '草稿已保存')
    Object.assign(form, blankForm())
    editingId.value = null
    window.scrollTo({ top: 0, behavior: 'smooth' })
    await load()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

function editRow(r: any) {
  editingId.value = r.id
  Object.assign(form, {
    date: r.date ?? form.date, orderId: r.orderId, times: r.times ?? 2, speed: r.speed ?? '',
    material: r.material, spec: r.spec, requiredMeter: r.requiredMeter ?? '',
    actualMeter: r.actualMeter ?? '', rolls: r.rolls ?? '', printRemain: r.printRemain ?? '',
    machineType: r.machineType ?? 'DRY', adjustMeter: r.adjustMeter ?? '', signer: r.signer
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
  showToast('已载入该行，修改后请提交')
}

async function removeRow(r: any) {
  try {
    await showConfirmDialog({ title: '删除确认', message: `确认删除 ${r.orderId} 这一行？` })
    await api.delete(`/lamination-daily/${r.id}`)
    if (editingId.value === r.id) { editingId.value = null; Object.assign(form, blankForm()) }
    showSuccessToast('已删除')
    await load()
  } catch { /* cancel */ }
}

watch(() => form.date, () => { load() })
load()
</script>

<style scoped>
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; }
.v-record-nums { display: flex; gap: 14px; font-size: var(--fs-xs); color: var(--muted); margin-top: 4px; }
.v-record-nums b { color: var(--text); font-variant-numeric: tabular-nums; }
.v-row-actions { margin-left: 8px; }  /* 布局交给全局 .v-row-actions（44px 点击区） */
</style>
