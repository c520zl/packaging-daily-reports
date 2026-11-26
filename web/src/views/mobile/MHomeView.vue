<template>
  <div class="app">
    <header class="m-head">
      <div class="m-head-row">
        <div>
          <div class="m-title">你好，{{ userStore.user?.realName }}</div>
          <div class="sub">{{ deptLabel }} · {{ roleLabel }}</div>
        </div>
        <div class="m-head-date">{{ today }}</div>
      </div>
    </header>

    <main class="m-body">
      <section class="m-card">
        <div class="m-card-body" style="display:grid; grid-template-columns:repeat(3,1fr); gap:8px; padding:14px 12px">
          <div class="m-rn"><div class="k">今日已填</div><div class="v">{{ mineToday }} 行</div></div>
          <div class="m-rn" style="border-left:1px solid var(--line); border-right:1px solid var(--line)">
            <div class="k">待我处理</div><div class="v" style="color:var(--warning)">{{ pending }}</div>
          </div>
          <div class="m-rn"><div class="k">今日废料</div><div class="v">{{ stats.wasteKg ?? 0 }} kg</div></div>
        </div>
      </section>

      <!-- 主管审核入口（API.md §8 pending-count） -->
      <router-link v-if="userStore.isLead && pending > 0" class="m-card"
                   to="/m/review" style="display:flex; align-items:center; gap:12px; padding:14px 16px; border-left:3px solid var(--warning)">
        <div class="m-quick-icon" style="background:var(--warning-weak); color:var(--warning)">
          <AppIcon name="i-check" :size="20" />
        </div>
        <div class="m-quick-info">
          <div class="m-quick-name">待我审核 <span style="color:var(--warning)">{{ pending }} 单</span></div>
          <div class="m-quick-desc">点击进入审核工作台</div>
        </div>
        <AppIcon name="i-arrow-right" :size="16" class="m-quick-arrow" />
      </router-link>

      <section class="m-card">
        <div class="m-card-head">
          <div>
            <div class="m-card-title">填报日报</div>
            <div class="m-card-sub">选择您所在的部门进入填写</div>
          </div>
        </div>
        <div class="m-card-body" style="padding-top:4px; padding-bottom:4px">
          <router-link class="m-quick" to="/m/print">
            <div class="m-quick-icon" style="background:var(--primary-weak); color:var(--primary)"><AppIcon name="i-print" :size="20" /></div>
            <div class="m-quick-info">
              <div class="m-quick-name">印刷部日报</div>
              <div class="m-quick-desc">上机时间 · 机速 · 实际印刷米数</div>
            </div>
            <AppIcon name="i-arrow-right" :size="16" class="m-quick-arrow" />
          </router-link>
          <router-link class="m-quick" to="/m/lamination">
            <div class="m-quick-icon" style="background:var(--accent-weak); color:var(--accent)"><AppIcon name="i-layers" :size="20" /></div>
            <div class="m-quick-info">
              <div class="m-quick-name">复合部日报</div>
              <div class="m-quick-desc">干式 / 无溶剂 · 卷数 · 调机米数</div>
            </div>
            <AppIcon name="i-arrow-right" :size="16" class="m-quick-arrow" />
          </router-link>
          <router-link class="m-quick" to="/m/bagmaking">
            <div class="m-quick-icon" style="background:var(--warning-weak); color:var(--warning)"><AppIcon name="i-bag" :size="20" /></div>
            <div class="m-quick-info">
              <div class="m-quick-name">制袋部日报</div>
              <div class="m-quick-desc">班次 · 机台 · 袋型 · 完成数量</div>
            </div>
            <AppIcon name="i-arrow-right" :size="16" class="m-quick-arrow" />
          </router-link>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head">
          <div class="m-card-title">最近填报</div>
          <router-link class="m-more" to="/m/records">全部记录</router-link>
        </div>
        <div class="m-card-body" style="display:flex; flex-direction:column; gap:12px">
          <div class="m-record" v-for="r in recent" :key="r.module + r.id">
            <div class="m-record-top">
              <span class="tag" :class="deptCls(r.module)">{{ moduleLabel(r.module) }}</span>
              <span class="m-record-no">{{ r.orderId }}</span>
              <span class="tag" :class="statusCls(r.status)" style="margin-left:auto">{{ statusLabel(r.status) }}</span>
            </div>
            <div class="m-record-meta">{{ r.date }} · {{ r.materialOrBagType }} · {{ r.machine }}</div>
            <div class="m-record-nums">
              <div class="m-rn"><div class="k">要求 / 来料</div><div class="v">{{ r.required ?? '—' }}</div></div>
              <div class="m-rn"><div class="k">实际 / 完成</div><div class="v">{{ r.actual ?? '—' }}</div></div>
              <div class="m-rn"><div class="k">废料</div><div class="v">{{ r.wasteKg ?? '—' }}</div></div>
            </div>
          </div>
          <p v-if="!recent.length" class="m-card-sub" style="text-align:center">今天还没有填报记录</p>
        </div>
      </section>
    </main>

    <MTabbar tab="home" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import MTabbar from '@/components/MTabbar.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, DEPT, ROLE, todayStr } from '@/constants'

const userStore = useUserStore()
const stats = ref<any>({})
const recent = ref<any[]>([])

const pending = computed(() => (userStore.user as any)?._pending ?? 0)
/** 「今日已填」按本人口径统计（/dashboard/today 的 submittedCount 是全厂数据，对员工是错的） */
const mineToday = computed(() => recent.value.filter(r => r.date === todayStr()).length)
const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '全厂')
const roleLabel = computed(() => userStore.user?.role ? (ROLE as any)[userStore.user.role]?.label : '')
const today = new Date().toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', weekday: 'long' })

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => (STATUS as any)[s]?.cls ?? 'tag-gray'
const deptCls = (m: string) => ({ PRINT: 'tag-blue', LAMINATION: 'tag-teal', BAGMAKING: 'tag-orange' }[m] ?? 'tag-gray')
const moduleLabel = (m: string) => (DEPT as any)[m]?.label ?? m

onMounted(async () => {
  stats.value = await api.get('/dashboard/today')
  const data = await api.get('/records', { params: { page: 1, size: 5, mine: true } })
  recent.value = data.records
  userStore.refreshPending()
})
</script>
