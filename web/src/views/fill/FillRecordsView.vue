<template>
  <AppShell title="我的填报记录">
    <van-tabs v-model:active="activeTab" sticky shrink>
      <van-tab v-for="t in tabs" :key="t.value" :title="t.label" />
    </van-tabs>

    <van-cell-group inset class="v-app-card">
      <van-cell v-for="r in records" :key="r.module + r.id" center is-link
                @click="goDetail(r)">
        <template #title>
          <div class="v-record-top">
            <span class="v-tag" :class="deptCls(r.module)">{{ deptLabel(r.module) }}</span>
            <span class="v-record-no">{{ r.orderId }}</span>
            <span class="v-tag" :class="statusCls(r.status)" style="margin-left:auto">
              {{ statusLabel(r.status) }}<template v-if="r.status === 'REJECTED'"> · 去修改</template>
            </span>
          </div>
          <div class="v-record-meta">
            {{ r.date }} · {{ r.machine }} · {{ r.materialOrBagType }}
            <template v-if="r.status === 'REJECTED' && r.rejectReason"> · 退回：{{ r.rejectReason }}</template>
          </div>
        </template>
        <template #label>
          <div class="v-record-nums">
            <span>要求/来料 <b>{{ r.required ?? '—' }}</b></span>
            <span>实际/完成 <b>{{ r.actual ?? '—' }}</b></span>
            <span>废料 <b>{{ r.wasteKg ?? '—' }}</b></span>
          </div>
        </template>
      </van-cell>
      <van-empty v-if="!records.length" description="暂无记录" image-size="80" />
    </van-cell-group>

    <div class="v-pager">
      <van-button size="small" :disabled="page <= 1" @click="page--; load()">上一页</van-button>
      <span class="v-pager-no">{{ page }} / {{ totalPages }}</span>
      <van-button size="small" :disabled="page >= totalPages" @click="page++; load()">下一页</van-button>
    </div>
    <p class="v-total">共 {{ total }} 条记录</p>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, DEPT } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const records = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 15
const activeTab = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

/**
 * Tab 按角色过滤（评审后新增）：
 * 员工（WORKER）→ 只显示本部门 + 全部/状态类 Tab（其他部门 tab 点了也查不到本人数据，属噪音）
 * 主管（DEPT_LEAD）/ 管理员 → 本部门或全部部门 + 状态类 Tab
 */
const ownDept = (userStore.user as any)?.dept
const deptTabs = ownDept
  ? [{ label: (DEPT as any)[ownDept]?.label ?? ownDept, value: ownDept }]
  : [{ label: '印刷部', value: 'PRINT' }, { label: '复合部', value: 'LAMINATION' }, { label: '制袋部', value: 'BAGMAKING' }]
const tabs = [
  { label: '全部', value: '' },
  ...deptTabs,
  { label: '待审核', value: 'PENDING' }, { label: '已退回', value: 'REJECTED' }
]
const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => ({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' }[s] ?? 'v-tag--gray')
const deptLabel = (m: string) => (DEPT as any)[m]?.label ?? m
const deptCls = (m: string) => ({ PRINT: 'v-tag--blue', LAMINATION: 'v-tag--teal', BAGMAKING: 'v-tag--orange' }[m] ?? 'v-tag--gray')

async function load() {
  const tab = tabs[activeTab.value]?.value ?? ''
  const data = await api.get('/records', {
    params: {
      mine: true, page: page.value, size,
      dept: ['PRINT', 'LAMINATION', 'BAGMAKING'].includes(tab) ? tab : undefined,
      status: ['PENDING', 'REJECTED'].includes(tab) ? tab : undefined
    }
  })
  records.value = data.records
  total.value = data.total
}

/** 被退回 / 草稿 → 修改重报页；其他 → 只读详情 */
function goDetail(r: any) {
  router.push(`/fill/revise/${r.module}/${r.id}`)
}

watch(activeTab, () => { page.value = 1; load() })
onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 4px; }
.v-record-nums { display: flex; gap: 14px; font-size: var(--fs-xs); color: var(--muted); margin-top: 4px; }
.v-record-nums b { color: var(--text); font-variant-numeric: tabular-nums; }
.v-pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 8px 0; }
.v-pager-no { font-size: var(--fs-sm); color: var(--muted); font-variant-numeric: tabular-nums; }
.v-total { text-align: center; font-size: var(--fs-sm); color: var(--muted-2); padding-bottom: 8px; }
</style>
