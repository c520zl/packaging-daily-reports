<template>
  <AdminShell title="工作台">
    <section class="stat-grid">
      <div class="stat-card">
        <div class="stat-label"><AppIcon name="i-report" :size="15" />今日填报单据</div>
        <div class="stat-value">{{ stats.submittedCount ?? 0 }}<span class="stat-unit">张</span></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">印刷实际米数</div>
        <div class="stat-value">{{ fmt(stats.printMeter) }}<span class="stat-unit">m</span></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">复合实际米数</div>
        <div class="stat-value">{{ fmt(stats.laminationMeter) }}<span class="stat-unit">m</span></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">制袋完成数量</div>
        <div class="stat-value">{{ fmt(stats.bagQty) }}<span class="stat-unit">个</span></div>
        <div class="stat-delta delta-down">废料 {{ stats.wasteKg ?? 0 }} kg</div>
      </div>
    </section>

    <section class="card">
      <div class="card-head">
        <div>
          <div class="card-title">快捷填报</div>
          <div class="card-sub">选择部门进入今日日报表填写</div>
        </div>
      </div>
      <div class="card-body quick-grid">
        <router-link class="quick-card" to="/print">
          <div class="quick-icon qi-print"><AppIcon name="i-print" :size="21" /></div>
          <div class="quick-info">
            <div class="quick-name">印刷部日报</div>
            <div class="quick-desc">逐单记录上机与实际印刷米数</div>
          </div>
          <AppIcon name="i-arrow-right" :size="18" class="quick-arrow" />
        </router-link>
        <router-link class="quick-card" to="/lamination">
          <div class="quick-icon qi-lam"><AppIcon name="i-layers" :size="21" /></div>
          <div class="quick-info">
            <div class="quick-name">复合部日报</div>
            <div class="quick-desc">干式 / 无溶剂机型分类统计</div>
          </div>
          <AppIcon name="i-arrow-right" :size="18" class="quick-arrow" />
        </router-link>
        <router-link class="quick-card" to="/bagmaking">
          <div class="quick-icon qi-bag"><AppIcon name="i-bag" :size="21" /></div>
          <div class="quick-info">
            <div class="quick-name">制袋部日报</div>
            <div class="quick-desc">八种袋型、1–8 号机全覆盖</div>
          </div>
          <AppIcon name="i-arrow-right" :size="18" class="quick-arrow" />
        </router-link>
      </div>
    </section>

    <div class="dash-cols">
      <section class="card">
        <div class="card-head">
          <div>
            <div class="card-title">近 7 日各部门实际产量</div>
            <div class="card-sub">已审核数据汇总</div>
          </div>
          <div class="card-actions legend">
            <span><i class="bar-seg-print"></i>印刷</span>
            <span><i class="bar-seg-lam"></i>复合</span>
            <span><i class="bar-seg-bag"></i>制袋</span>
          </div>
        </div>
        <div class="card-body">
          <div v-if="hasData" ref="chartEl" class="trend-chart"></div>
          <div v-else class="chart-empty">
            <AppIcon name="i-report" :size="36" />
            <p>近 7 日暂无已审核的日报数据</p>
            <p class="chart-empty-sub">填报并审核通过后，这里会展示各部门产量趋势</p>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="card-title">我的待办</div>
        </div>
        <div class="card-body" style="padding-top:8px">
          <div class="feed-item">
            <span class="feed-dot" style="background:var(--warning)"></span>
            <div>
              <div class="feed-text" v-if="userStore.isLead"><b>{{ pending }}</b> 单待我审核</div>
              <div class="feed-text" v-else-if="userStore.isWorker"><b>{{ pending }}</b> 单被退回待修改</div>
              <div class="feed-text" v-else>全厂 <b>{{ pending }}</b> 单待审核</div>
              <div class="feed-time">数据实时更新</div>
            </div>
          </div>
          <div class="feed-item" v-if="userStore.isLead || userStore.isAdmin || userStore.isManagement">
            <span class="feed-dot" style="background:var(--primary)"></span>
            <div>
              <div class="feed-text">前往<router-link class="link-btn" style="padding:0 4px" to="/reports">报表查询</router-link>查看明细</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { themeState } from '@/theme'

const userStore = useUserStore()
const stats = ref<any>({})
const weekly = ref<any[]>([])
const chartEl = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
const hasData = computed(() => weekly.value.some(r => (r.printMeter || 0) + (r.laminationMeter || 0) + (r.bagQty || 0) > 0))

const pending = computed(() => (userStore.user as any)?._pending ?? 0)

function fmt(n: any) {
  return Number(n ?? 0).toLocaleString('zh-CN')
}

/** ECharts 折线（印刷/复合米数）+ 柱（制袋数量）混合图，颜色随明暗主题取 CSS 变量 */
function renderChart() {
  if (!chartEl.value || !hasData.value) return
  const css = getComputedStyle(document.documentElement)
  const v = (name: string) => css.getPropertyValue(name).trim()
  const textColor = v('--muted') || '#55657D'
  const lineColor = v('--line') || '#E2E8F0'
  chart = chart || echarts.init(chartEl.value)
  chart.setOption({
    color: [v('--primary') || '#2457F5', v('--accent') || '#0C7372', v('--warning') || '#D97706'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['印刷米数', '复合米数', '制袋数量'], textStyle: { color: textColor, fontSize: 12 }, top: 0 },
    grid: { left: 8, right: 8, top: 32, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      data: weekly.value.map(r => r.date.slice(5)),
      axisLine: { lineStyle: { color: lineColor } },
      axisLabel: { color: textColor, fontSize: 12 },
      axisTick: { show: false }
    },
    yAxis: [
      { type: 'value', name: '米数', nameTextStyle: { color: textColor }, axisLabel: { color: textColor, fontSize: 11 }, splitLine: { lineStyle: { color: lineColor, type: 'dashed' } } },
      { type: 'value', name: '个数', nameTextStyle: { color: textColor }, axisLabel: { color: textColor, fontSize: 11 }, splitLine: { show: false } }
    ],
    series: [
      { name: '印刷米数', type: 'line', smooth: true, symbolSize: 6, data: weekly.value.map(r => r.printMeter || 0) },
      { name: '复合米数', type: 'line', smooth: true, symbolSize: 6, data: weekly.value.map(r => r.laminationMeter || 0) },
      { name: '制袋数量', type: 'bar', yAxisIndex: 1, barWidth: 18, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: weekly.value.map(r => r.bagQty || 0) }
    ]
  })
}

onMounted(async () => {
  try {
    stats.value = await api.get('/dashboard/today')
    weekly.value = await api.get('/dashboard/weekly', { params: { days: 7 } })
    await userStore.refreshPending()
    await nextTick()
    renderChart()
  } catch { /* 路由踢转时请求中断，忽略 */ }
})

// 数据或明暗主题变化时重绘
watch([weekly, () => themeState.isDark], () => {
  nextTick(() => { chart?.dispose(); chart = null; renderChart() })
})

onBeforeUnmount(() => { chart?.dispose(); chart = null })
</script>

<style scoped>
.quick-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); }
.quick-card {
  border: 1px solid var(--line); border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5); display: flex; align-items: center; gap: var(--space-4);
  background: var(--surface); transition: border-color .15s, box-shadow .15s;
}
.quick-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }
.quick-icon { width: 42px; height: 42px; border-radius: var(--radius-md); display: grid; place-items: center; flex-shrink: 0; }
.qi-print { background: var(--primary-weak); color: var(--primary); }
.qi-lam { background: var(--accent-weak); color: var(--accent); }
.qi-bag { background: var(--warning-weak); color: var(--warning); }
.quick-info { flex: 1; min-width: 0; }
.quick-name { font-size: var(--fs-md); font-weight: 600; }
.quick-desc { font-size: var(--fs-sm); color: var(--muted); margin-top: 4px; }
.quick-arrow { color: var(--muted-2); }
.dash-cols { display: grid; grid-template-columns: 1.5fr 1fr; gap: var(--space-4); align-items: start; }
.feed-item { display: flex; gap: 12px; padding: 12px 0; align-items: flex-start; }
.feed-item + .feed-item { border-top: 1px solid var(--line); }
.feed-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 7px; flex-shrink: 0; }
.feed-text { font-size: var(--fs-sm); line-height: 1.6; color: var(--muted); }
.feed-text b { color: var(--text); font-weight: 600; }
.feed-time { font-size: var(--fs-xs); color: var(--muted); margin-top: 2px; }

/* ECharts 趋势图容器 */
.trend-chart { height: 260px; width: 100%; }

/* 图表空状态 */
.chart-empty {
  height: 200px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 8px;
  color: var(--muted-2);
}
.chart-empty p { font-size: var(--fs-sm); color: var(--muted); }
.chart-empty-sub { font-size: var(--fs-xs) !important; color: var(--muted-2) !important; }

/* 待办卡片与左图等高 */
.dash-cols > .card:last-child { display: flex; flex-direction: column; }
.dash-cols > .card:last-child .card-body { flex: 1; }

@media (max-width: 1100px) {
  .quick-grid, .dash-cols { grid-template-columns: 1fr; }
}
</style>
