<template>
  <AdminShell title="报表查询">
    <section class="card">
      <div class="card-body filter-bar">
        <div class="field" style="min-width:120px">
          <label class="field-label">部门</label>
          <el-select v-model="filters.dept" clearable style="width:100%">
            <el-option label="全部部门" value="" />
            <el-option label="印刷部" value="PRINT" /><el-option label="复合部" value="LAMINATION" /><el-option label="制袋部" value="BAGMAKING" />
          </el-select>
        </div>
        <div class="field" style="min-width:150px">
          <label class="field-label">开始日期</label>
          <el-date-picker v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="field" style="min-width:150px">
          <label class="field-label">结束日期</label>
          <el-date-picker v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="field" style="min-width:170px">
          <label class="field-label">生产单号</label>
          <el-input v-model="filters.orderId" placeholder="模糊搜索单号" clearable />
        </div>
        <div class="field" style="min-width:110px">
          <label class="field-label">状态</label>
          <el-select v-model="filters.status" clearable style="width:100%">
            <el-option v-for="(v, k) in STATUS" :key="k" :label="v.label" :value="k" />
          </el-select>
        </div>
        <div class="toolbar" style="margin-left:auto">
          <el-button @click="exportExcel"><AppIcon name="i-download" :size="15" />&nbsp;导出 Excel</el-button>
          <el-button type="primary" @click="load"><AppIcon name="i-search" :size="15" />&nbsp;查询</el-button>
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <div class="stat-card stat-accent stat-print">
        <div class="stat-label"><AppIcon name="i-print" :size="15" />印刷实际米数</div>
        <div class="stat-value">{{ fmt(summary.printMeter) }}<span class="stat-unit">m</span></div>
      </div>
      <div class="stat-card stat-accent stat-lam">
        <div class="stat-label"><AppIcon name="i-layers" :size="15" />复合实际米数</div>
        <div class="stat-value">{{ fmt(summary.laminationMeter) }}<span class="stat-unit">m</span></div>
      </div>
      <div class="stat-card stat-accent stat-bag">
        <div class="stat-label"><AppIcon name="i-bag" :size="15" />制袋完成数量</div>
        <div class="stat-value">{{ fmt(summary.bagQty) }}<span class="stat-unit">个</span></div>
      </div>
      <div class="stat-card stat-accent stat-waste">
        <div class="stat-label"><AppIcon name="i-trash" :size="15" />废料合计</div>
        <div class="stat-value">{{ summary.wasteKg ?? 0 }}<span class="stat-unit">kg</span></div>
      </div>
    </section>

    <section class="card">
      <div class="card-head">
        <div class="card-title">明细数据 <span class="card-sub">共 {{ total }} 行</span></div>
        <div class="card-actions legend">
          <span><i class="bar-seg-print"></i>印刷</span>
          <span><i class="bar-seg-lam"></i>复合</span>
          <span><i class="bar-seg-bag"></i>制袋</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="tbl">
          <thead>
            <tr>
              <th style="width:76px">部门</th>
              <th style="width:100px">日期</th>
              <th style="width:130px">生产单号</th>
              <th style="width:110px">机台 / 机型</th>
              <th style="width:130px">材质 / 袋型</th>
              <th style="width:100px">规格</th>
              <th class="num" style="width:96px">要求 / 来料</th>
              <th class="num" style="width:96px">实际 / 完成</th>
              <th class="num" style="width:64px">卷数</th>
              <th class="num" style="width:72px">废料 kg</th>
              <th style="width:84px">签名</th>
              <th style="width:92px">状态</th>
              <th style="width:100px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in records" :key="r.module + r.id">
              <td><span class="tag" :class="deptCls(r.module)"><span class="dot"></span>{{ deptLabel(r.module) }}</span></td>
              <td>{{ r.date }}</td>
              <td>{{ r.orderId }}</td>
              <td>{{ r.machine }}</td>
              <td>{{ r.materialOrBagType }}</td>
              <td>{{ r.spec }}</td>
              <td class="num">{{ fmt(r.required) }}</td>
              <td class="num">{{ fmt(r.actual) }}{{ r.module === 'BAGMAKING' ? ' 个' : '' }}</td>
              <td class="num">{{ r.rolls ?? '—' }}</td>
              <td class="num">{{ r.wasteKg ?? '—' }}</td>
              <td>{{ r.signer }}</td>
              <td><span class="tag" :class="statusCls(r.status)"><span class="dot"></span>{{ statusLabel(r.status) }}</span></td>
              <td>
                <router-link class="link-btn" :to="`/detail/${r.module}/${r.id}`">详情</router-link>
                <a v-if="r.status === 'PENDING' && canAudit(r)" class="link-btn danger" href="#"
                   @click.prevent="reject(r)">退回</a>
              </td>
            </tr>
            <tr v-if="!records.length"><td colspan="13" style="text-align:center;color:var(--muted)">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
      <div class="form-foot" style="position:static">
        <span class="form-foot-sum">共 {{ total }} 条记录 · 每页 {{ filters.size }} 条</span>
        <span class="toolbar" style="margin-left:auto">
          <el-button size="small" :disabled="filters.page <= 1" @click="filters.page--; load()">上一页</el-button>
          <el-button size="small" type="primary" style="min-width:44px">{{ filters.page }}</el-button>
          <el-button size="small" :disabled="filters.page * filters.size >= total" @click="filters.page++; load()">下一页</el-button>
        </span>
      </div>
    </section>
  </AdminShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, DEPT, todayStr } from '@/constants'

const userStore = useUserStore()
const filters = reactive({ dept: '', startDate: '', endDate: '', orderId: '', status: '', page: 1, size: 20 })
const records = ref<any[]>([])
const total = ref(0)
const summary = ref<any>({})

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => (STATUS as any)[s]?.cls ?? 'tag-gray'
const deptLabel = (m: string) => (DEPT as any)[m]?.label ?? m
const deptCls = (m: string) => (({ PRINT: 'tag-blue', LAMINATION: 'tag-teal', BAGMAKING: 'tag-orange' } as Record<string, string>)[m] ?? 'tag-gray')
const fmt = (n: any) => n == null ? '—' : Number(n).toLocaleString('zh-CN')
const canAudit = (r: any) => userStore.isLead ? userStore.user?.dept === r.dept : (userStore.isAdmin || userStore.isManagement)

async function load() {
  const data = await api.get('/records', {
    params: {
      page: filters.page, size: filters.size,
      dept: filters.dept || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      orderId: filters.orderId || undefined,
      status: filters.status || undefined
    }
  })
  records.value = data.records
  total.value = data.total
  summary.value = await api.get('/reports/summary', {
    params: { startDate: filters.startDate || undefined, endDate: filters.endDate || undefined }
  })
}

async function reject(r: any) {
  const { value } = await ElMessageBox.prompt('请填写退回原因', '退回 ' + r.orderId, { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
  const path = ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' } as Record<string, string>)[r.module]
  await api.put(`/${path}/${r.id}/reject`, { reason: value })
  ElMessage.success(r.orderId + ' 已退回，已通知填报人修改')
  load()
}

/** 导出 Excel：axios blob 下载（window.open 带不上 Authorization 头，必 401） */
async function exportExcel() {
  try {
    const blob: any = await api.get('/reports/export', {
      params: { dept: filters.dept || undefined, startDate: filters.startDate || undefined, endDate: filters.endDate || undefined },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `日报明细_${filters.startDate || '全部'}_${filters.endDate || todayStr()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { /* 拦截器已提示（blob 响应的 code 校验由 catch 兜底） */ }
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
/* 汇总卡主题色：左侧色条 + 同色图标，一眼区分部门维度 */
.stat-accent { border-left: 3px solid var(--line); }
.stat-accent .stat-label svg { flex-shrink: 0; }
.stat-print { border-left-color: var(--primary); }
.stat-print .stat-label { color: var(--primary); }
.stat-lam { border-left-color: var(--accent); }
.stat-lam .stat-label { color: var(--accent); }
.stat-bag { border-left-color: var(--warning); }
.stat-bag .stat-label { color: var(--warning); }
.stat-waste { border-left-color: var(--danger); }
.stat-waste .stat-label { color: var(--danger); }
</style>
