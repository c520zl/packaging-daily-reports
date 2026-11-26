<template>
  <AppShell title="审核工作台">
    <van-tabs v-model:active="filterIdx" sticky shrink>
      <van-tab v-for="t in filterTabs" :key="t.value" :title="t.label" />
    </van-tabs>

    <van-cell-group inset class="v-app-card">
      <template v-for="item in filtered" :key="item.module + item.id">
        <van-cell center>
          <template #title>
            <div class="v-record-top">
              <span class="v-record-no">{{ item.orderId }}</span>
              <span class="v-tag v-tag--orange" style="margin-left:auto">待审核</span>
            </div>
            <div class="v-record-meta">{{ deptLabelOf(item.module) }} · {{ item.createdBy }} · {{ fmtTime(item.createdAt) }}</div>
          </template>
          <template #label>
            <div class="v-diff">
              <div class="v-diff-row"><span>机台</span><b>{{ item.machine }}</b></div>
              <div class="v-diff-row"><span>材质 / 袋型</span><b>{{ item.materialOrBagType }}</b></div>
              <div class="v-diff-row"><span>要求 / 来料</span><b>{{ item.required ?? '—' }}</b></div>
              <div class="v-diff-row"><span>实际 / 完成</span><b>{{ item.actual ?? '—' }}{{ item.module === 'BAGMAKING' ? ' 个' : '' }}</b></div>
            </div>
          </template>
        </van-cell>
        <div class="v-review-actions">
          <van-button size="small" block plain @click="reject(item)">退回</van-button>
          <van-button size="small" block type="primary" @click="approve(item)">通过</van-button>
        </div>
      </template>
      <van-empty v-if="!filtered.length" description="没有待审核的单据" image="search" image-size="80" />
    </van-cell-group>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { DEPT } from '@/constants'

const userStore = useUserStore()
const pending = ref<any[]>([])
const filterIdx = ref(0)
const filterTabs = [
  { label: '全部', value: '' }, { label: '印刷部', value: 'PRINT' },
  { label: '复合部', value: 'LAMINATION' }, { label: '制袋部', value: 'BAGMAKING' }
]

const filtered = computed(() => {
  const f = filterTabs[filterIdx.value]?.value ?? ''
  return f ? pending.value.filter(x => x.module === f) : pending.value
})
const fmtTime = (t: string) => t?.replace('T', ' ').slice(5, 16)
const deptLabelOf = (m: string) => (DEPT as any)[m]?.label ?? m

const pathOf = (m: string) => ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' }[m])

async function load() {
  const results = await Promise.all([
    api.get('/print-daily', { params: { page: 1, size: 50, status: 'PENDING' } }),
    api.get('/lamination-daily', { params: { page: 1, size: 50, status: 'PENDING' } }),
    api.get('/bag-daily', { params: { page: 1, size: 50, status: 'PENDING' } })
  ])
  const rows: any[] = []
  const modules = ['PRINT', 'LAMINATION', 'BAGMAKING']
  results.forEach((r, i) => {
    r.records.forEach((x: any) => rows.push({ ...x, module: modules[i], machine: machineOf(modules[i], x) }))
  })
  pending.value = rows
}

function machineOf(module: string, x: any) {
  if (module === 'PRINT') return '印刷机'
  if (module === 'LAMINATION') return x.machineType === 'SOLVENT_FREE' ? '无溶剂机' : '干式复合机'
  return x.machine
}

async function reject(item: any) {
  try {
    // 退回必须填原因：复用 Element Plus 的 prompt（Vant Dialog 无输入框）
    const { ElMessageBox } = await import('element-plus')
    const { value: reason } = await ElMessageBox.prompt('请填写退回原因', `退回 ${item.orderId}`, {
      inputPlaceholder: '如：机速 / 实际米数不符',
      inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
    })
    await api.put(`/${pathOf(item.module)}/${item.id}/reject`, { reason })
    showSuccessToast('已退回，已通知填报人修改')
    load()
    userStore.refreshPending()
  } catch { /* cancel */ }
}

async function approve(item: any) {
  try {
    await showConfirmDialog({ title: '审核确认', message: `确认通过 ${item.orderId}？通过后将计入汇总` })
    await api.put(`/${pathOf(item.module)}/${item.id}/approve`)
    showSuccessToast(item.orderId + ' 已通过审核')
    load()
    userStore.refreshPending()
  } catch { /* cancel */ }
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; }
.v-diff { display: flex; flex-direction: column; gap: 4px; margin-top: 6px; }
.v-diff-row { display: flex; justify-content: space-between; font-size: var(--fs-sm); color: var(--muted); }
.v-diff-row b { color: var(--text); font-weight: 500; font-variant-numeric: tabular-nums; }
.v-review-actions { display: flex; gap: 10px; padding: 4px 16px 12px; }
</style>
