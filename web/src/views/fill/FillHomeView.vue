<template>
  <AppShell title="日报填报">
    <!-- 被退回通知条 -->
    <template v-if="rejectedRows.length">
      <van-notice-bar left-icon="warning-o" wrapable :scrollable="false"
                      color="var(--danger)" background="var(--danger-weak)"
                      :text="`${rejectedRows.length} 条记录被退回，请修改后重新提交`" />
      <div v-for="r in rejectedRows" :key="r.module + r.id" class="v-reject-item"
           @click="router.push(`/fill/revise/${r.module}/${r.id}`)">
        <van-icon name="edit" color="var(--danger)" size="18" />
        <div class="v-reject-info">
          <div class="v-reject-no">{{ r.orderId }}</div>
          <div class="v-reject-reason">{{ r.rejectReason || '请查看详情并修改重报' }}</div>
        </div>
        <van-icon name="arrow" color="var(--muted-2)" size="14" />
      </div>
    </template>

    <!-- 今日统计 -->
    <div class="v-app-card v-stats-wrap">
      <div class="v-stats">
        <div class="v-stats__item"><div class="v-stats__k">今日已填</div><div class="v-stats__v">{{ mineToday }}</div></div>
        <div class="v-stats__item"><div class="v-stats__k">待审核</div><div class="v-stats__v" style="color:var(--warning)">{{ pending }}</div></div>
        <div class="v-stats__item"><div class="v-stats__k">被退回</div><div class="v-stats__v" style="color:var(--danger)">{{ rejected }}</div></div>
      </div>
    </div>

    <!-- 部门填报入口 -->
    <van-cell-group inset class="v-app-card">
      <div class="v-group-title">填报日报</div>
      <van-cell v-for="d in deptCards" :key="d.path" :title="d.name" :label="d.desc"
                :icon="d.icon" is-link center :to="d.path" class="v-quick-cell" />
    </van-cell-group>

    <!-- 最近填报 -->
    <van-cell-group inset class="v-app-card">
      <template #title>
        <div class="v-group-title" style="padding-bottom:8px">最近填报
          <router-link to="/fill/records" class="v-more">全部记录</router-link>
        </div>
      </template>
      <van-cell v-for="r in recent" :key="r.module + r.id" center @click="router.push(`/fill/revise/${r.module}/${r.id}`)">
        <template #title>
          <div class="v-record-top">
            <span class="v-tag" :class="deptCls(r.module)">{{ deptLabel(r.module) }}</span>
            <span class="v-record-no">{{ r.orderId }}</span>
            <span class="v-tag" :class="statusCls(r.status)" style="margin-left:auto">{{ statusLabel(r.status) }}</span>
          </div>
          <div class="v-record-meta">{{ r.date }} · {{ r.machine }}</div>
        </template>
        <template #label>
          <div class="v-record-nums">
            <span>要求/来料 <b>{{ r.required ?? '—' }}</b></span>
            <span>实际/完成 <b>{{ r.actual ?? '—' }}</b></span>
            <span>废料 <b>{{ r.wasteKg ?? '—' }}</b></span>
          </div>
        </template>
      </van-cell>
      <van-empty v-if="!recent.length" description="今天还没有填报记录，点下方「填报」开始" image-size="64" />
    </van-cell-group>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, DEPT, todayStr } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const mine = ref<any[]>([])

const pending = computed(() => mine.value.filter(r => r.status === 'PENDING').length)
const rejected = computed(() => mine.value.filter(r => r.status === 'REJECTED').length)
const rejectedRows = computed(() => mine.value.filter(r => r.status === 'REJECTED').slice(0, 3))
const recent = computed(() => mine.value.slice(0, 5))
const mineToday = computed(() => mine.value.filter(r => r.date === todayStr()).length)

const deptLabel = (m: string) => (DEPT as any)[m]?.label ?? m
const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => ({ DRAFT: 'v-tag--gray', PENDING: 'v-tag--orange', APPROVED: 'v-tag--green', REJECTED: 'v-tag--red' }[s] ?? 'v-tag--gray')
const deptCls = (m: string) => ({ PRINT: 'v-tag--blue', LAMINATION: 'v-tag--teal', BAGMAKING: 'v-tag--orange' }[m] ?? 'v-tag--gray')

const ALL = [
  { path: '/fill/print', name: '印刷部日报', desc: '上机时间 · 机速 · 实际印刷米数', icon: 'printer-o' },
  { path: '/fill/lamination', name: '复合部日报', desc: '干式 / 无溶剂 · 卷数 · 调机米数', icon: 'layers-o' },
  { path: '/fill/bagmaking', name: '制袋部日报', desc: '班次 · 机台 · 袋型 · 完成数量', icon: 'gift-o' }
]
const deptCards = computed(() => {
  if (userStore.isLead || userStore.user?.role === 'ADMIN') return ALL
  const dept = (userStore.user as any)?.dept
  const own = ({ PRINT: '/fill/print', LAMINATION: '/fill/lamination', BAGMAKING: '/fill/bagmaking' } as any)[dept]
  return ALL.filter(d => d.path === own)
})

onMounted(async () => {
  try {
    const data: any = await api.get('/records', { params: { page: 1, size: 50, mine: true } })
    mine.value = data.records
  } catch { /* 拦截器已提示 */ }
})
</script>

<style scoped>
.v-reject-item {
  display: flex; align-items: center; gap: 10px;
  margin: 10px 16px 0; padding: 12px 14px;
  background: var(--surface); border-radius: var(--radius-lg);
  border-left: 3px solid var(--danger);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
}
.v-reject-info { flex: 1; min-width: 0; }
.v-reject-no { font-size: var(--fs-md); font-weight: 600; }
.v-reject-reason { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.v-stats-wrap { background: var(--surface); margin: 12px 16px 0; }
.v-quick-cell :deep(.van-icon) { color: var(--primary); font-size: 20px; }
.v-more { margin-left: auto; font-size: var(--fs-sm); color: var(--primary); font-weight: 400; }
.v-record-top { display: flex; align-items: center; gap: 8px; }
.v-record-no { font-size: var(--fs-md); font-weight: 600; font-variant-numeric: tabular-nums; }
.v-record-meta { font-size: var(--fs-sm); color: var(--muted); margin-top: 2px; }
.v-record-nums { display: flex; gap: 16px; font-size: var(--fs-xs); color: var(--muted); }
.v-record-nums b { color: var(--text); font-variant-numeric: tabular-nums; }
/* Vant cell-group inset 自带边距，这里统一 */
:deep(.van-cell-group--inset) { margin: 12px 16px; }
</style>
