<template>
  <div class="app with-submitbar">
    <header class="m-head-plain">
      <router-link class="m-back-plain" to="/m/records">
        <AppIcon name="i-arrow-right" :size="18" style="transform:rotate(180deg)" />
      </router-link>
      <div class="m-title">{{ isRejected ? '修改重报' : '记录详情' }} · {{ record.orderId }}</div>
      <span class="tag" :class="statusCls" style="margin-left:auto">{{ statusLabel }}</span>
    </header>

    <main class="m-body">
      <!-- 退回原因横幅（API.md §6.5 REJECTED → 修改重报） -->
      <div v-if="record.rejectReason" class="reject-banner">
        <AppIcon name="i-bell" :size="15" />
        <span><b>{{ record.createdBy }}</b> 退回：<br>「{{ record.rejectReason }}」</span>
      </div>

      <section class="m-card">
        <div class="m-card-head">
          <div class="m-card-title">单据内容</div>
          <div class="m-card-sub" style="margin-left:auto">{{ record.date }} · {{ record.machine }}</div>
        </div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field" v-for="f in fields" :key="f.key">
            <label class="m-label">{{ f.label }}<span v-if="f.required" class="req">*</span></label>
            <!-- 可编辑（退回状态下关键字段）：数字字段走 el-input-number（顺序：先判断 num 再判断可编辑） -->
            <el-input-number v-if="canEditField(f) && f.num" v-model="record[f.key]" style="width:100%" />
            <el-input v-else-if="canEditField(f)" v-model="record[f.key]" />
            <!-- 只读 -->
            <div v-else class="m-calc">{{ format(f, record[f.key]) }}<span class="hint">不可修改</span></div>
          </div>
          <div class="m-field">
            <label class="m-label">补充说明</label>
            <el-input v-model="note" type="textarea" :rows="2" placeholder="可说明修改原因" />
          </div>
        </div>
      </section>

      <p class="m-card-sub" style="padding:0 4px">重新提交后将再次进入主管审核列表；本单的退回与修改记录全程留痕。</p>
    </main>

    <div class="m-submitbar">
      <div class="sum">{{ isRejected ? '修改后重新提交' : '当前状态：' + statusLabel }}</div>
      <el-button v-if="isRejected" @click="save(false)" :loading="saving">存草稿</el-button>
      <el-button v-if="isRejected" type="primary" @click="save(true)" :loading="saving">重新提交</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { STATUS, MACHINE_TYPE, SHIFT } from '@/constants'

const route = useRoute()
const router = useRouter()
const moduleKey = route.params.module as string
const recordId = route.params.id as string

const record = ref<any>({})
const timeline = ref<any[]>([])
const note = ref('')
const saving = ref(false)

const path = computed(() => ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' }[moduleKey] ?? 'print-daily'))
const status = computed(() => record.value.status)
const statusLabel = computed(() => (STATUS as any)[status.value]?.label ?? '')
const statusCls = computed(() => (STATUS as any)[status.value]?.cls ?? 'tag-gray')
const isRejected = computed(() => status.value === 'REJECTED')

/** 退回状态下可编辑的字段（简化：全部开放编辑，保存后回 DRAFT 再提交） */
function canEditField(_f: any) { return isRejected.value }

const fields = computed(() => {
  const m = moduleKey
  if (m === 'PRINT') return [
    { key: 'orderId', label: '生产单号', required: true },
    { key: 'onboardTime', label: '上机时间' },
    { key: 'speed', label: '机速 m/min', num: true, required: true },
    { key: 'material', label: '材质', required: true },
    { key: 'spec', label: '尺寸规格', required: true },
    { key: 'requiredMeter', label: '要求印刷米数', num: true, required: true },
    { key: 'actualMeter', label: '实际印刷米数', num: true, required: true },
    { key: 'printMinutes', label: '印刷时间(分)', num: true },
    { key: 'signer', label: '签名', required: true }
  ]
  if (m === 'LAMINATION') return [
    { key: 'orderId', label: '生产单号', required: true },
    { key: 'times', label: '次数', num: true, required: true },
    { key: 'speed', label: '机速', num: true, required: true },
    { key: 'material', label: '材质', required: true },
    { key: 'spec', label: '规格', required: true },
    { key: 'requiredMeter', label: '工单要求米数', num: true, required: true },
    { key: 'actualMeter', label: '实际复合米数', num: true, required: true },
    { key: 'rolls', label: '卷数', num: true },
    { key: 'machineTypeLabel', label: '机型' },
    { key: 'adjustMeter', label: '调机米数', num: true },
    { key: 'signer', label: '签名', required: true }
  ]
  return [
    { key: 'orderId', label: '生产单号', required: true },
    { key: 'shiftLabel', label: '班次' },
    { key: 'machine', label: '机台', required: true },
    { key: 'picker', label: '捡袋员', required: true },
    { key: 'leader', label: '机长', required: true },
    { key: 'bagType', label: '袋型', required: true },
    { key: 'spec', label: '成品规格', required: true },
    { key: 'incomingMeter', label: '来料米数', num: true, required: true },
    { key: 'endMeter', label: '结束米数', num: true, required: true },
    { key: 'completedQty', label: '完成数量', num: true, required: true },
    { key: 'wasteKg', label: '废料 kg', num: true },
    { key: 'signer', label: '签名', required: true }
  ]
})

function format(f: any, v: any) {
  if (f.key === 'machineTypeLabel') return MACHINE_TYPE[record.value.machineType as keyof typeof MACHINE_TYPE] ?? '—'
  if (f.key === 'shiftLabel') return SHIFT[record.value.shift as keyof typeof SHIFT] ?? '—'
  return v ?? '—'
}

async function save(submit: boolean) {
  saving.value = true
  try {
    const r: any = { ...record.value }
    delete r.colorsText; delete r.createdBy; delete r.machineTypeLabel; delete r.shiftLabel
    // colors 数组原样提交（PRINT 必填字段）
    if (moduleKey === 'PRINT' && typeof r.colors === 'object') delete r.colors
    await api.put(`/${path.value}/${recordId}`, r)
    if (submit) await api.put(`/${path.value}/${recordId}/submit`)
    ElMessage.success(submit ? '已重新提交审核' : '草稿已保存')
    router.push('/m/records')
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

onMounted(async () => {
  try {
    record.value = (await api.get(`/${path.value}/${recordId}`)) ?? {}
    timeline.value = (await api.get(`/${path.value}/${recordId}/timeline`)) ?? []
  } catch {
    ElMessage.error('记录加载失败')
    router.back()
  }
})
</script>

<style scoped>
.reject-banner {
  background: var(--danger-weak); border: 1px solid var(--danger); border-radius: var(--radius-md);
  padding: 12px 14px; font-size: 13.5px; color: #B91C1C; line-height: 1.7;
  display: flex; gap: 10px; align-items: flex-start;
}
.reject-banner svg { flex-shrink: 0; margin-top: 2px; }
</style>
