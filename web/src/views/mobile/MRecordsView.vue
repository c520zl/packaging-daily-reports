<template>
  <div class="app">
    <header class="m-head-plain">
      <router-link class="m-back-plain" to="/m/home">
        <AppIcon name="i-arrow-right" :size="18" style="transform:rotate(180deg)" />
      </router-link>
      <div class="m-title">我的填报记录</div>
    </header>

    <main class="m-body">
      <div class="m-filter-tabs">
        <span v-for="t in tabs" :key="t.value" class="m-ftab" :class="{ on: activeTab === t.value }" @click="activeTab = t.value; load()">{{ t.label }}</span>
      </div>

      <section class="m-card">
        <div class="m-card-body" style="padding-top:6px; padding-bottom:6px">
          <div v-for="r in records" :key="r.module + r.id" style="padding:10px 0" @click="goDetail(r)">
            <div class="m-record-top">
              <span class="tag" :class="deptCls(r.module)"><span class="dot"></span>{{ deptLabel(r.module) }}</span>
              <span class="m-record-no">{{ r.orderId }}</span>
              <span class="tag" :class="statusCls(r.status)" style="margin-left:auto">
                {{ statusLabel(r.status) }}<template v-if="r.status === 'REJECTED'"> · 去修改</template>
              </span>
            </div>
            <div class="m-record-meta" style="margin-top:6px">
              {{ r.date }} · {{ r.machine }} · {{ r.materialOrBagType }}
              <template v-if="r.status === 'REJECTED' && r.rejectReason"> · 退回原因：{{ r.rejectReason }}</template>
            </div>
            <div class="m-record-nums" style="margin-top:10px">
              <div class="m-rn"><div class="k">要求 / 来料</div><div class="v">{{ r.required ?? '—' }}</div></div>
              <div class="m-rn"><div class="k">实际 / 完成</div><div class="v">{{ r.actual ?? '—' }}</div></div>
              <div class="m-rn"><div class="k">废料</div><div class="v">{{ r.wasteKg ?? '—' }}</div></div>
            </div>
          </div>
          <p v-if="!records.length" class="m-card-sub" style="text-align:center; padding:16px 0">暂无记录</p>
        </div>
      </section>

      <p class="m-card-sub" style="text-align:center; padding:4px 0 8px">共 {{ total }} 条记录</p>
    </main>

    <MTabbar tab="home" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import MTabbar from '@/components/MTabbar.vue'
import { api } from '@/api/http'
import { STATUS, DEPT } from '@/constants'

const router = useRouter()
const records = ref<any[]>([])
const total = ref(0)
const activeTab = ref('')
const tabs = [
  { label: '全部', value: '' }, { label: '印刷部', value: 'PRINT' },
  { label: '复合部', value: 'LAMINATION' }, { label: '制袋部', value: 'BAGMAKING' },
  { label: '待审核', value: 'PENDING' }, { label: '已退回', value: 'REJECTED' }
]

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => (STATUS as any)[s]?.cls ?? 'tag-gray'
const deptLabel = (m: string) => (DEPT as any)[m]?.label ?? m
const deptCls = (m: string) => ({ PRINT: 'tag-blue', LAMINATION: 'tag-teal', BAGMAKING: 'tag-orange' }[m] ?? 'tag-gray')

async function load() {
  const data = await api.get('/records', {
    params: { mine: true, page: 1, size: 20, dept: ['PRINT', 'LAMINATION', 'BAGMAKING'].includes(activeTab.value) ? activeTab.value : undefined, status: ['PENDING', 'REJECTED'].includes(activeTab.value) ? activeTab.value : undefined }
  })
  records.value = data.records
  total.value = data.total
}

/** 已退回的进修改重报页；其他进详情（移动端简化：都进修改/查看页） */
function goDetail(r: any) {
  router.push(`/m/revise/${r.module}/${r.id}`)
}

onMounted(() => { load().catch(() => {}) })
</script>
