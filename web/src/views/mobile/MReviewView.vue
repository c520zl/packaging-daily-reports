<template>
  <div class="app with-submitbar">
    <header class="m-head">
      <div class="m-head-row">
        <router-link class="m-back" to="/m/home">
          <AppIcon name="i-arrow-right" :size="18" style="color:#fff; transform:rotate(180deg)" />
        </router-link>
        <div>
          <div class="m-title">审核工作台</div>
          <div class="sub">{{ deptLabel }}主管 · {{ userStore.user?.realName }}</div>
        </div>
        <div class="m-head-date">{{ today }}</div>
      </div>
    </header>

    <main class="m-body">
      <section class="m-card" v-for="item in pending" :key="item.module + item.id">
        <div class="m-card-head">
          <div>
            <div class="m-card-title">{{ item.orderId }}</div>
            <div class="m-card-sub">{{ item.createdBy }} · 提交于 {{ fmtTime(item.createdAt) }}</div>
          </div>
          <span class="tag tag-orange" style="margin-left:auto"><span class="dot"></span>待审核</span>
        </div>
        <div class="review-diff">
          <div class="review-diff-row"><span class="lab">机台</span><span class="val">{{ item.machine }}</span></div>
          <div class="review-diff-row"><span class="lab">材质 / 袋型</span><span class="val">{{ item.materialOrBagType }}</span></div>
          <div class="review-diff-row"><span class="lab">要求 / 来料</span><span class="val">{{ item.required ?? '—' }}</span></div>
          <div class="review-diff-row"><span class="lab">实际 / 完成</span><span class="val">{{ item.actual ?? '—' }}{{ item.module === 'BAGMAKING' ? ' 个' : '' }}</span></div>
        </div>
        <div class="m-card-body review-actions" style="padding-top:12px">
          <el-button style="flex:1" @click="reject(item)">退回</el-button>
          <el-button type="primary" style="flex:1" @click="approve(item)">通过</el-button>
        </div>
      </section>
      <p v-if="!pending.length" class="m-card-sub" style="text-align:center; padding:32px 0">没有待审核的单据 🎉</p>
    </main>

    <div class="m-submitbar">
      <div class="sum">还有 <b>{{ pending.length }}</b> 单待审</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { DEPT } from '@/constants'

const userStore = useUserStore()
const pending = ref<any[]>([])
const today = new Date().toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
const deptLabel = computed(() => userStore.user?.dept ? (DEPT as any)[userStore.user.dept]?.label : '')
const fmtTime = (t: string) => t?.replace('T', ' ').slice(5, 16)

const pathOf = (m: string) => ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' }[m])

async function load() {
  // 三个模块分别拉 PENDING 列表合并展示
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

async function approve(item: any) {
  await api.put(`/${pathOf(item.module)}/${item.id}/approve`)
  ElMessage.success('已通过')
  load()
  userStore.refreshPending()
}

async function reject(item: any) {
  const { value } = await ElMessageBox.prompt('请填写退回原因', `退回 ${item.orderId}`, {
    inputPlaceholder: '如：机速 / 实际米数漏填',
    inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
  })
  await api.put(`/${pathOf(item.module)}/${item.id}/reject`, { reason: value })
  ElMessage.success('已退回')
  load()
  userStore.refreshPending()
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
.review-diff-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 16px; font-size: 14px; }
.review-diff-row + .review-diff-row { border-top: 1px solid var(--line); }
.review-diff-row .lab { color: var(--muted); flex-shrink: 0; }
.review-diff-row .val { font-variant-numeric: tabular-nums; font-weight: 500; text-align: right; }
.review-actions { display: flex; gap: 10px; }
.review-actions .el-button { flex: 1; min-width: 0; }
</style>
