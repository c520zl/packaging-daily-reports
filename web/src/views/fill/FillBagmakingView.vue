<template>
  <AppShell title="制袋部日报" back-to="/fill/home">
    <van-form ref="formRef" @failed="onValidateFailed">
      <van-cell-group inset class="v-app-card">
        <van-field name="date" label="日报日期" required>
          <template #input><span style="flex:1">{{ form.date }}</span></template>
        </van-field>
        <van-field name="shift" label="班次" required :rules="requiredRule('班次')">
          <template #input>
            <van-radio-group v-model="form.shift" direction="horizontal">
              <van-radio name="DAY">白班</van-radio>
              <van-radio name="NIGHT">夜班</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field v-model="form.machine" name="machine" label="机台" required :rules="machineRule">
          <template #input>
            <div class="v-grid-select">
              <span v-for="m in MACHINES" :key="m" class="v-grid-item" :class="{ on: form.machine === m }"
                    @click="form.machine = m">{{ m }}</span>
            </div>
          </template>
        </van-field>
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">当班人员</div>
        <van-field v-model="form.leader" name="leader" label="机长" placeholder="当班机长"
                   required :rules="requiredRule('机长')" clearable />
        <van-field v-model="form.picker" name="picker" label="捡袋员" placeholder="当班捡袋员"
                   required :rules="requiredRule('捡袋员')" clearable />
        <van-field v-model="form.workStart" name="workStart" label="工时起" readonly is-link
                   required :rules="workStartRule" placeholder="选择时间" @click="openTime('workStart')" />
        <van-field v-model="form.workEnd" name="workEnd" label="工时止" readonly is-link
                   required :rules="workEndRule" placeholder="选择时间" @click="openTime('workEnd')" />
        <van-notice-bar wrapable :scrollable="false" color="var(--muted)" background="var(--surface-2)"
                        left-icon="info-o" :text="shiftHint" />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">生产信息 <span v-if="editingId" class="v-tag v-tag--orange">修改中</span></div>
        <van-field v-model="form.orderId" name="orderId" label="生产单号" placeholder="如 ZD-20261124-01"
                   required :rules="orderIdRules" clearable />
        <van-field v-model="form.bagType" name="bagType" label="袋型" readonly is-link
                   required :rules="requiredRule('袋型')" placeholder="请选择袋型"
                   @click="showBagPicker = true" />
        <van-popup v-model:show="showBagPicker" position="bottom" round>
          <van-picker v-if="showBagPicker" :columns="bagColumns" title="选择袋型"
                      @confirm="(v: any) => { form.bagType = v.selectedValues[0]; showBagPicker = false }"
                      @cancel="showBagPicker = false" />
        </van-popup>
        <van-field v-model="form.spec" name="spec" label="成品规格" placeholder="长×宽 mm"
                   required :rules="requiredRule('成品规格')" clearable />
        <van-field v-model="form.speed" name="speed" label="机速（个/分）" type="number"
                   required :rules="numRule('机速', true)" placeholder="如 30" />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">米数记录</div>
        <van-field v-model="form.incomingMeter" name="incomingMeter" label="来料米数" type="number"
                   required :rules="numRule('来料米数', true)" placeholder="如 12000" />
        <van-field v-model="form.adjustMeter" name="adjustMeter" label="调机米数" type="number" placeholder="选填" />
        <van-field v-model="form.endMeter" name="endMeter" label="结束米数" type="number"
                   required :rules="[...numRule('结束米数', true), endMeterRule]" placeholder="不能大于来料米数" />
        <van-notice-bar wrapable :scrollable="false" color="var(--muted)" background="var(--surface-2)"
                        left-icon="info-o" text="结束米数不能大于来料米数，否则提交会被退回" />
      </van-cell-group>

      <van-cell-group inset class="v-app-card">
        <div class="v-group-title">数量与损耗</div>
        <van-field v-model="form.completedQty" name="completedQty" label="完成数量（个）" type="digit"
                   required :rules="numRule('完成数量', true)" placeholder="如 86000" />
        <van-field v-model="form.wasteKg" name="wasteKg" label="废料 kg" type="number" placeholder="选填" />
        <van-field v-model="form.remainKg" name="remainKg" label="余料 kg" type="number" placeholder="选填" />
        <van-field v-model="form.defectQty" name="defectQty" label="不良品数量" type="digit" placeholder="选填" />
        <van-field v-model="form.abnormalReason" name="abnormalReason" label="异常原因" type="textarea" rows="1"
                   autosize maxlength="200" show-word-limit placeholder="停机、换版、质量问题等异常说明" />
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
          <div class="v-record-meta">{{ shiftLabel(r.shift) }} · {{ r.machine }} · {{ r.bagType }} · {{ r.leader }}</div>
        </template>
        <template #label>
          <div class="v-record-nums">
            <span>完成 <b>{{ fmt(r.completedQty) }}</b></span>
            <span>废料 <b>{{ r.wasteKg ?? '—' }} kg</b></span>
            <span>不良 <b>{{ r.defectQty ?? '—' }}</b></span>
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

    <!-- 工时选择 -->
    <van-popup v-model:show="showTimePopup" position="bottom" round>
      <van-time-picker v-if="showTimePopup" v-model="pickTime" :columns-type="['hour','minute']"
                       title="选择时间" @confirm="onTimeConfirm" @cancel="showTimePopup = false" />
    </van-popup>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, BAG_TYPES, MACHINES, todayStr } from '@/constants'

const userStore = useUserStore()
const rows = ref<any[]>([])
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()
const showTimePopup = ref(false)
const showBagPicker = ref(false)
/** Vant picker 需要 { text } 结构的选项 */
const bagColumns = BAG_TYPES.map(b => ({ text: b, value: b }))
const pickTime = ref<string[]>(['08', '00'])
const timeTarget = ref<'workStart' | 'workEnd'>('workStart')

function blankForm(): any {
  return {
    date: todayStr(), shift: 'DAY', machine: '1号机', picker: '', leader: '',
    speed: '', orderId: '', bagType: '三边封袋', spec: '',
    incomingMeter: '', adjustMeter: '', endMeter: '', completedQty: '',
    wasteKg: '', remainKg: '', workStart: '08:00', workEnd: '20:30',
    abnormalReason: '', defectQty: '', signer: userStore.user?.realName ?? ''
  }
}
const form = reactive<any>(blankForm())

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => ({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' }[s] ?? 'v-tag--gray')
const shiftLabel = (s: string) => s === 'NIGHT' ? '夜班' : '白班'
const canEdit = (r: any) => r.status === 'DRAFT' || r.status === 'REJECTED'
const fmt = (n: any) => (n === null || n === undefined || n === '' ? '—' : Number(n).toLocaleString('zh-CN'))

const orderIdRules = [
  { required: true, message: '请填写生产单号' },
  { pattern: /^[\w\u4e00-\u9fa5-]{2,50}$/, message: '单号仅限字母数字中文与连字符' }
]
const requiredRule = (label: string) => [{ required: true, message: `请填写${label}` }]
/** 机台是九宫格点选（初始有默认值），用 validator 校验 modelValue */
const machineRule = [{ validator: (v: string) => !!v, message: '请选择机台' }]
const numRule = (label: string, required: boolean) => {
  const rules: any[] = []
  if (required) rules.push({ required: true, message: `请填写${label}` })
  rules.push({ validator: (v: string) => !v || (Number(v) > 0 && Number(v) < 100000000), message: '须为正数' })
  return rules
}
/** 结束米数 ≤ 来料米数（生产安全规范，前端拦截 + 后端 2003 双保险） */
const endMeterRule = {
  validator: (v: string) => !v || !form.incomingMeter || Number(v) <= Number(form.incomingMeter),
  message: '结束米数不能大于来料米数'
}

// ===== 工时按班次校验（P4：白班 8:00-20:30，夜班 20:00-次日 8:00）=====
const toMin = (t: string) => { const [h, m] = String(t || '0:0').split(':').map(Number); return h * 60 + (m || 0) }
/** 白班窗口：8:00-20:30（同日）；夜班窗口：20:00-次日 8:00（跨零点） */
function inShiftWindow(shift: string, t: string): boolean {
  const v = toMin(t)
  if (shift === 'NIGHT') return v >= toMin('20:00') || v <= toMin('8:00')   // 跨零点：≥20:00 或 ≤8:00
  return v >= toMin('8:00') && v <= toMin('20:30')                          // 白班：8:00-20:30
}
const workTimeMsg = () => form.shift === 'NIGHT' ? '夜班工时须在 20:00 - 次日 8:00 内' : '白班工时须在 8:00 - 20:30 内'
const workStartRule = [
  { required: true, message: '请选择工时起' },
  // message 用函数：规则对象创建时求值会固化班次文案，函数式每次校验动态取当前班次
  { validator: (v: string) => !v || inShiftWindow(form.shift, v), message: () => workTimeMsg() }
]
const workEndRule = [
  { required: true, message: '请选择工时止' },
  { validator: (v: string) => !v || inShiftWindow(form.shift, v), message: () => workTimeMsg() },
  { validator: (v: string) => {
      if (!v || !form.workStart) return true
      const s = toMin(form.workStart), e = toMin(v)
      if (form.shift === 'NIGHT') return e >= s || s >= toMin('20:00')  // 夜班止点允许跨零点（e < s 合法）
      return e > s                                                       // 白班止点必须晚于起点
    }, message: '工时止必须晚于工时起（夜班跨零点除外）' }
]
const shiftHint = computed(() =>
  form.shift === 'NIGHT' ? '夜班上班时间 20:00 - 次日 8:00，工时须在此区间内' : '白班上班时间 8:00 - 20:30，工时须在此区间内')

/** 切班次时自动带入该班次的默认工时（并触发工时字段重新校验） */
watch(() => form.shift, (s) => {
  if (editingId.value) return
  if (s === 'NIGHT') { form.workStart = '20:00'; form.workEnd = '08:00' }
  else { form.workStart = '08:00'; form.workEnd = '20:30' }
  formRef.value?.validate(['workStart', 'workEnd']).catch(() => {})
})

function onValidateFailed() {
  showToast({ type: 'fail', message: '请检查标红的必填项' })
}

function openTime(target: 'workStart' | 'workEnd') {
  timeTarget.value = target
  pickTime.value = String(form[target] || '08:00').split(':')
  showTimePopup.value = true
}

function onTimeConfirm() {
  form[timeTarget.value] = `${pickTime.value[0]}:${pickTime.value[1]}`
  showTimePopup.value = false
  // 确认时间后立即重新校验该字段，让错误信息即时刷新（而非等提交才暴露）
  formRef.value?.validate(timeTarget.value).catch(() => {})
}

function toReq() {
  return {
    date: form.date, shift: form.shift, machine: form.machine,
    picker: form.picker, leader: form.leader, speed: form.speed || null, orderId: form.orderId,
    bagType: form.bagType, spec: form.spec, incomingMeter: form.incomingMeter || null,
    adjustMeter: form.adjustMeter || null, endMeter: form.endMeter || null, completedQty: form.completedQty || null,
    wasteKg: form.wasteKg || null, remainKg: form.remainKg || null, workStart: form.workStart,
    workEnd: form.workEnd, abnormalReason: form.abnormalReason, defectQty: form.defectQty || null,
    signer: form.signer
  }
}

async function load() {
  try {
    const data = await api.get('/bag-daily', { params: { page: 1, size: 200, date: form.date } })
    rows.value = data.records
  } catch { /* 拦截器已提示 */ }
}

async function save(submit: boolean) {
  if (submit) {
    const valid = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!valid) { onValidateFailed(); return }
    if (Number(form.endMeter) > Number(form.incomingMeter)) {
      showToast({ type: 'fail', message: '结束米数不能大于来料米数' }); return
    }
    // 工时按班次校验兜底（表单校验已含，此处防御性双保险）
    if (!inShiftWindow(form.shift, form.workStart) || !inShiftWindow(form.shift, form.workEnd)) {
      showToast({ type: 'fail', message: workTimeMsg() }); return
    }
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/bag-daily/${editingId.value}`, { ...toReq(), submit: false })
      if (submit) await api.put(`/bag-daily/${editingId.value}/submit`)
    } else {
      await api.post('/bag-daily', { ...toReq(), submit })
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
    date: r.date ?? form.date, shift: r.shift ?? 'DAY', machine: r.machine ?? '1号机',
    picker: r.picker ?? '', leader: r.leader ?? '', speed: r.speed ?? '', orderId: r.orderId,
    bagType: r.bagType ?? '三边封袋', spec: r.spec, incomingMeter: r.incomingMeter ?? '',
    adjustMeter: r.adjustMeter ?? '', endMeter: r.endMeter ?? '', completedQty: r.completedQty ?? '',
    wasteKg: r.wasteKg ?? '', remainKg: r.remainKg ?? '', workStart: r.workStart ?? '08:00',
    workEnd: r.workEnd ?? '20:30', abnormalReason: r.abnormalReason ?? '', defectQty: r.defectQty ?? '',
    signer: r.signer
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
  showToast('已载入该行，修改后请提交')
}

async function removeRow(r: any) {
  try {
    await showConfirmDialog({ title: '删除确认', message: `确认删除 ${r.orderId} 这一行？` })
    await api.delete(`/bag-daily/${r.id}`)
    if (editingId.value === r.id) { editingId.value = null; Object.assign(form, blankForm()) }
    showSuccessToast('已删除')
    await load()
  } catch { /* cancel */ }
}

watch(() => form.date, () => { load() })
load()
</script>

<style scoped>
.v-grid-select { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; width: 100%; }
.v-grid-item {
  height: 40px; display: grid; place-items: center;
  border: 1px solid var(--line); border-radius: var(--radius-md);
  font-size: var(--fs-md); color: var(--muted); background: var(--surface);
  cursor: pointer; user-select: none; font-variant-numeric: tabular-nums;
}
.v-grid-item.on { background: var(--primary-weak); border-color: var(--primary); color: var(--primary); font-weight: 600; }
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; }
.v-record-nums { display: flex; gap: 14px; font-size: var(--fs-xs); color: var(--muted); margin-top: 4px; }
.v-record-nums b { color: var(--text); font-variant-numeric: tabular-nums; }
.v-row-actions { margin-left: 8px; }  /* 布局交给全局 .v-row-actions（44px 点击区） */
</style>
