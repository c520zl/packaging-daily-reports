<template>
  <AppShell title="印刷部日报" back-to="/fill/home">
    <van-form ref="formRef" @failed="onValidateFailed">
      <van-cell-group inset class="v-app-card">
        <van-field name="date" label="日报日期" required>
          <template #input>
            <span style="flex:1">{{ form.date }}</span>
          </template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">生产信息 <span v-if="editingId" class="v-tag v-tag--orange">修改中</span></div>
        <van-field v-model="form.orderId" name="orderId" label="生产单号" placeholder="如 YS-20261124-01"
                   required :rules="orderIdRules" clearable />
        <van-field v-model="form.onboardTime" name="onboardTime" label="上机时间" readonly is-link
                   required :rules="requiredRule('上机时间')" placeholder="选择时间"
                   @click="showTimePicker = true" />
        <van-popup v-model:show="showTimePicker" position="bottom" round>
          <van-time-picker v-model="pickTime" :columns-type="['hour','minute']" title="选择上机时间"
                           @confirm="onTimeConfirm" @cancel="showTimePicker = false" />
        </van-popup>
        <van-field v-model="form.speed" name="speed" label="机速 m/min" type="number" required
                   :rules="numRule('机速', true)" placeholder="如 350" />
        <van-field name="colors" label="印刷颜色" required :rules="colorsRule">
          <template #input>
            <div class="v-chips">
              <span v-for="c in colorOptions" :key="c" class="v-chip" :class="{ on: colors.includes(c) }"
                    @click="toggleColor(c)">{{ c }}</span>
            </div>
          </template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">材料信息</div>
        <van-field v-model="form.material" name="material" label="材质" placeholder="如 PET / PA / CPP"
                   required :rules="requiredRule('材质')" clearable />
        <van-field v-model="form.thickness" name="thickness" label="厚度 μm" type="number" placeholder="选填" />
        <van-field v-model="form.spec" name="spec" label="尺寸规格" placeholder="长×宽 mm"
                   required :rules="requiredRule('尺寸规格')" clearable />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">生产数量</div>
        <van-field v-model="form.requiredMeter" name="requiredMeter" label="要求印刷米数" type="number"
                   required :rules="numRule('要求米数', true)" placeholder="如 5000" />
        <van-field v-model="form.actualMeter" name="actualMeter" label="实际印刷米数" type="number"
                   required :rules="numRule('实际米数', true)" placeholder="如 4800" />
        <div class="v-calc">
          {{ form.printMinutes ?? '待实际米数填入后自动算出' }}
          <span class="hint">实际米数 ÷ 机速</span>
        </div>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">签名与备注</div>
        <van-field v-model="form.signer" name="signer" label="签名" required :rules="requiredRule('签名')" clearable />
        <van-field v-model="form.remark" name="remark" label="备注" type="textarea" rows="1" autosize
                   maxlength="200" show-word-limit placeholder="异常、换版、停机等情况说明" />
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
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, todayStr } from '@/constants'

const userStore = useUserStore()
const rows = ref<any[]>([])
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()
const showTimePicker = ref(false)
const pickTime = ref<string[]>(['08', '00'])

const BASE_COLORS = ['黑', '红', '蓝', '黄', '白', '专色']
const colors = ref<string[]>([])
const colorOptions = computed(() => Array.from(new Set([...BASE_COLORS, ...colors.value])))

function blankForm(): any {
  return {
    date: todayStr(), orderId: '', onboardTime: '08:00', speed: '', material: '', thickness: '',
    spec: '', requiredMeter: '', actualMeter: '', printMinutes: null,
    signer: userStore.user?.realName ?? '', remark: ''
  }
}
const form = reactive<any>(blankForm())

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => ({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' }[s] ?? 'v-tag--gray')
const canEdit = (r: any) => r.status === 'DRAFT' || r.status === 'REJECTED'
const fmt = (n: any) => (n === null || n === undefined || n === '' ? '—' : Number(n).toLocaleString('zh-CN'))

// ===== 校验规则（生产数据规范：必填 + 正数 + 单号格式）=====
const orderIdRules = [
  { required: true, message: '请填写生产单号' },
  { pattern: /^[\w\u4e00-\u9fa5-]{2,50}$/, message: '单号仅限字母数字中文与连字符' }
]
const requiredRule = (label: string) => [{ required: true, message: `请填写${label}` }]
/** 颜色是多选 chips（无 modelValue），用自定义 validator 校验 */
const colorsRule = [{ validator: () => colors.value.length > 0, message: '请至少选择一个印刷颜色' }]
const numRule = (label: string, required: boolean) => {
  const rules: any[] = []
  if (required) rules.push({ required: true, message: `请填写${label}` })
  rules.push({ validator: (v: string) => !v || (Number(v) > 0 && Number(v) < 100000000), message: '须为正数' })
  return rules
}

function onValidateFailed(e: any) {
  showToast({ type: 'fail', message: '请检查标红的必填项' })
}

function toggleColor(c: string) {
  const i = colors.value.indexOf(c)
  i >= 0 ? colors.value.splice(i, 1) : colors.value.push(c)
}

function onTimeConfirm() {
  form.onboardTime = `${pickTime.value[0]}:${pickTime.value[1]}`
  showTimePicker.value = false
}

// 印刷时间 = 实际米数 ÷ 机速，实时自动计算
watch(() => [form.actualMeter, form.speed], ([actual, speed]) => {
  if (actual && speed) form.printMinutes = Math.round(actual / speed * 10) / 10
})

function toReq() {
  return {
    date: form.date, orderId: form.orderId, onboardTime: form.onboardTime,
    speed: form.speed || null, material: form.material, thickness: form.thickness || null, spec: form.spec,
    requiredMeter: form.requiredMeter || null, actualMeter: form.actualMeter || null,
    colors: colors.value, printMinutes: form.printMinutes,
    signer: form.signer, remark: form.remark
  }
}

async function load() {
  try {
    const data = await api.get('/print-daily', { params: { page: 1, size: 200, date: form.date } })
    rows.value = data.records
  } catch { /* 拦截器已提示 */ }
}

async function save(submit: boolean) {
  if (submit) {
    const valid = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!valid) { onValidateFailed(null); return }
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/print-daily/${editingId.value}`, { ...toReq(), submit: false })
      if (submit) await api.put(`/print-daily/${editingId.value}/submit`)
    } else {
      await api.post('/print-daily', { ...toReq(), submit })
    }
    showSuccessToast(submit ? '已提交，等待主管审核' : '草稿已保存')
    Object.assign(form, blankForm())
    pickTime.value = ['08', '00']
    colors.value = []
    editingId.value = null
    window.scrollTo({ top: 0, behavior: 'smooth' })
    await load()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

function editRow(r: any) {
  editingId.value = r.id
  Object.assign(form, {
    date: r.date ?? form.date, orderId: r.orderId, onboardTime: r.onboardTime ?? '08:00',
    speed: r.speed ?? '', material: r.material, thickness: r.thickness ?? '', spec: r.spec,
    requiredMeter: r.requiredMeter ?? '', actualMeter: r.actualMeter ?? '', printMinutes: r.printMinutes,
    signer: r.signer, remark: r.remark ?? ''
  })
  if (r.onboardTime) pickTime.value = String(r.onboardTime).split(':')
  colors.value = (r.colors || []).filter(Boolean)
  window.scrollTo({ top: 0, behavior: 'smooth' })
  showToast('已载入该行，修改后请提交')
}

async function removeRow(r: any) {
  try {
    await showConfirmDialog({ title: '删除确认', message: `确认删除 ${r.orderId} 这一行？` })
    await api.delete(`/print-daily/${r.id}`)
    if (editingId.value === r.id) { editingId.value = null; Object.assign(form, blankForm()) }
    showSuccessToast('已删除')
    await load()
  } catch { /* cancel */ }
}

watch(() => form.date, () => { load() })
load()
</script>

<style scoped>
.v-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.v-chip {
  height: 32px; padding: 0 14px;
  display: inline-flex; align-items: center;
  border: 1px solid var(--line); border-radius: 999px;
  font-size: 13px; color: var(--muted); background: var(--surface);
  cursor: pointer; user-select: none;
}
.v-chip.on { background: var(--primary-weak); border-color: var(--primary); color: var(--primary); font-weight: 600; }
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; }
.v-record-nums { display: flex; gap: 14px; font-size: var(--fs-xs); color: var(--muted); margin-top: 4px; }
.v-record-nums b { color: var(--text); font-variant-numeric: tabular-nums; }
.v-row-actions { margin-left: 8px; }  /* 布局交给全局 .v-row-actions（44px 点击区） */
:deep(.van-time-picker) { width: 100%; }
:deep(.van-picker__columns) { height: 120px; }
</style>
