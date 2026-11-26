<template>
  <AppShell :title="isRejected ? '修改重报' : '记录详情'" back-to="/fill/records">
    <!-- 退回原因横幅 -->
    <van-notice-bar v-if="record.rejectReason" wrapable :scrollable="false"
                    left-icon="warning-o" color="var(--danger)" background="var(--danger-weak)">
      <b>{{ record.createdBy }}</b> 退回：{{ record.rejectReason }}
    </van-notice-bar>

    <van-cell-group inset class="v-app-card" style="margin-top:12px">
      <template #title>
        <div class="v-group-title" style="padding-bottom:8px">
          单据内容
          <span class="v-tag" :class="statusCls" style="margin-left:auto">{{ statusLabel }}</span>
        </div>
      </template>
      <template v-for="f in fields" :key="f.key">
        <!-- 可编辑（退回状态）：数字走 number 键盘，文本走输入框 -->
        <van-field v-if="isRejected && !f.num" v-model="record[f.key]" :label="f.label" clearable />
        <van-field v-else-if="isRejected && f.num" v-model="record[f.key]" :label="f.label" type="number" />
        <!-- 只读 -->
        <van-field v-else :model-value="format(f, record[f.key])" :label="f.label" readonly />
      </template>
      <van-field v-model="note" label="补充说明" type="textarea" rows="1" autosize maxlength="200"
                 placeholder="可说明修改原因（选填）" />
    </van-cell-group>

    <p class="v-tip">重新提交后将再次进入主管审核列表；本单的退回与修改记录全程留痕。</p>

    <!-- 提交区（随流，避免与底部 Tabbar 重叠） -->
    <div v-if="isRejected" class="v-submit-flow">
      <div class="sum">修改后重新提交</div>
      <van-button size="small" :loading="saving" @click="save(false)">存草稿</van-button>
      <van-button size="small" type="primary" :loading="saving" @click="save(true)">重新提交</van-button>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { STATUS, MACHINE_TYPE, SHIFT } from '@/constants'

const route = useRoute()
const router = useRouter()
const moduleKey = route.params.module as string
const recordId = route.params.id as string

const record = ref<any>({})
const note = ref('')
const saving = ref(false)

const path = computed(() => ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' }[moduleKey] ?? 'print-daily'))
const status = computed(() => record.value.status)
const statusLabel = computed(() => (STATUS as any)[status.value]?.label ?? '')
const statusCls = computed(() => (({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' } as Record<string, string>)[status.value] ?? 'v-tag--gray'))
const isRejected = computed(() => status.value === 'REJECTED')

const fields = computed(() => {
  const m = moduleKey
  if (m === 'PRINT') return [
    { key: 'orderId', label: '生产单号', num: false },
    { key: 'onboardTime', label: '上机时间', num: false },
    { key: 'speed', label: '机速 m/min', num: true },
    { key: 'material', label: '材质', num: false },
    { key: 'spec', label: '尺寸规格', num: false },
    { key: 'requiredMeter', label: '要求印刷米数', num: true },
    { key: 'actualMeter', label: '实际印刷米数', num: true },
    { key: 'printMinutes', label: '印刷时间(分)', num: true },
    { key: 'signer', label: '签名', num: false }
  ]
  if (m === 'LAMINATION') return [
    { key: 'orderId', label: '生产单号', num: false },
    { key: 'times', label: '次数', num: true },
    { key: 'speed', label: '机速', num: true },
    { key: 'material', label: '材质', num: false },
    { key: 'spec', label: '规格', num: false },
    { key: 'requiredMeter', label: '工单要求米数', num: true },
    { key: 'actualMeter', label: '实际复合米数', num: true },
    { key: 'rolls', label: '卷数', num: true },
    { key: 'machineTypeLabel', label: '机型', num: false },
    { key: 'adjustMeter', label: '调机米数', num: true },
    { key: 'signer', label: '签名', num: false }
  ]
  return [
    { key: 'orderId', label: '生产单号', num: false },
    { key: 'shiftLabel', label: '班次', num: false },
    { key: 'machine', label: '机台', num: false },
    { key: 'picker', label: '捡袋员', num: false },
    { key: 'leader', label: '机长', num: false },
    { key: 'bagType', label: '袋型', num: false },
    { key: 'spec', label: '成品规格', num: false },
    { key: 'incomingMeter', label: '来料米数', num: true },
    { key: 'endMeter', label: '结束米数', num: true },
    { key: 'completedQty', label: '完成数量', num: true },
    { key: 'wasteKg', label: '废料 kg', num: true },
    { key: 'signer', label: '签名', num: false }
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
    // colors 数组原样提交（PRINT 必填字段；删除会被后端 2004 拒绝或清空颜色）
    await api.put(`/${path.value}/${recordId}`, r)
    if (submit) await api.put(`/${path.value}/${recordId}/submit`)
    showSuccessToast(submit ? '已重新提交审核' : '草稿已保存')
    router.push('/fill/records')
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

onMounted(async () => {
  try {
    // 详情加载失败（mock 兜底返回 null / 无权限）时兜成空对象，避免模板读 null 白屏
    record.value = (await api.get(`/${path.value}/${recordId}`)) ?? {}
  } catch {
    showToast({ type: 'fail', message: '记录加载失败' })
    router.back()
  }
})
</script>

<style scoped>
.v-tip { text-align: center; font-size: var(--fs-sm); color: var(--muted); padding: 8px 16px; }
</style>
