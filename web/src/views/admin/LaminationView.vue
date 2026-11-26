<template>
  <AdminShell title="复合部日报表" :badge="`待审核 ${pendingCount} 行`">
    <section class="card">
      <div class="card-head">
        <div>
          <div class="card-title">公共信息</div>
          <div class="card-sub">日报日期与当班信息，整表共用</div>
        </div>
      </div>
      <div class="card-body filter-bar">
        <div class="field" style="min-width:180px">
          <label class="field-label">日期</label>
          <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="toolbar" style="margin-left:auto">
          <el-button @click="addRow"><AppIcon name="i-plus" :size="15" />&nbsp;添加一行</el-button>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="card-head">
        <div class="card-title">明细记录 <span class="card-sub">共 {{ rows.length }} 行</span></div>
      </div>
      <div class="table-wrap">
        <table class="tbl form-table">
          <thead>
            <tr>
              <th style="width:130px">生产单号 *</th>
              <th style="width:70px">次数</th>
              <th style="width:84px">机速 m/min</th>
              <th style="width:110px">材质</th>
              <th style="width:120px">规格</th>
              <th style="width:104px">工单要求米数</th>
              <th style="width:104px">实际复合米数</th>
              <th style="width:64px">卷数</th>
              <th style="width:88px">印刷剩余米数</th>
              <th style="width:110px">机型 *</th>
              <th style="width:88px">调机米数</th>
              <th style="width:84px">签名</th>
              <th style="width:48px">状态</th>
              <th style="width:44px"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in rows" :key="row._key">
              <template v-if="canEdit(row)">
                <td><el-input v-model="row.orderId" size="small" /></td>
                <td><el-input-number v-model="row.times" :min="1" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.speed" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input v-model="row.material" size="small" /></td>
                <td><el-input v-model="row.spec" size="small" /></td>
                <td><el-input-number v-model="row.requiredMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.actualMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.rolls" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.printRemain" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-select v-model="row.machineType" size="small" style="width:100%">
                  <el-option label="干式" value="DRY" /><el-option label="无溶剂" value="SOLVENT_FREE" />
                </el-select></td>
                <td><el-input-number v-model="row.adjustMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input v-model="row.signer" size="small" /></td>
                <td><span class="tag" :class="statusCls(row.status)">{{ statusLabel(row.status) }}</span></td>
                <td><button class="row-del" @click="removeRow(i, row)"><AppIcon name="i-trash" :size="15" /></button></td>
              </template>
              <!-- 只读行：纯文本完整展示 -->
              <template v-else>
                <td class="ro-cell" :title="row.orderId">{{ row.orderId }}</td>
                <td class="ro-cell num">{{ row.times }}</td>
                <td class="ro-cell num">{{ fmt(row.speed) }}</td>
                <td class="ro-cell">{{ row.material }}</td>
                <td class="ro-cell">{{ row.spec }}</td>
                <td class="ro-cell num">{{ fmt(row.requiredMeter) }}</td>
                <td class="ro-cell num">{{ fmt(row.actualMeter) }}</td>
                <td class="ro-cell num">{{ row.rolls ?? '—' }}</td>
                <td class="ro-cell num">{{ fmt(row.printRemain) }}</td>
                <td class="ro-cell">{{ row.machineType === 'SOLVENT_FREE' ? '无溶剂' : '干式' }}</td>
                <td class="ro-cell num">{{ fmt(row.adjustMeter) }}</td>
                <td class="ro-cell">{{ row.signer }}</td>
                <td><span class="tag" :class="statusCls(row.status)">{{ statusLabel(row.status) }}</span></td>
                <td></td>
              </template>
            </tr>
          </tbody>
          <tbody v-if="!rows.length">
            <tr><td :colspan="14" style="text-align:center; padding:32px 0; color:var(--muted)">
              今日（{{ queryDate }}）暂无填报记录，点击右上角「添加一行」开始填报
            </td></tr>
          </tbody>
        </table>
      </div>
      <div class="form-foot">
        <span class="form-foot-sum">
          合计：实际复合 <b>{{ fmt(totalActual) }}</b> m · 卷数 <b>{{ totalRolls }}</b> 卷 · 调机损耗 <b>{{ fmt(totalAdjust) }}</b> m
        </span>
        <span class="toolbar" style="margin-left:auto">
          <el-button @click="saveAll(false)" :loading="saving">存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="saveAll(true)">
            <AppIcon name="i-check" :size="15" />&nbsp;提交日报
          </el-button>
        </span>
      </div>
    </section>

    <p class="card-sub">提示：调机米数为开机调试消耗的膜料，不计入实际复合产量；无溶剂机开工前需记录固化时间。</p>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, todayStr } from '@/constants'

const userStore = useUserStore()
const rows = ref<any[]>([])
const queryDate = ref(todayStr())
const saving = ref(false)
const pendingCount = ref(0)
let keySeq = 1

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => (STATUS as any)[s]?.cls ?? 'tag-gray'
const canEdit = (row: any) => row.status === 'DRAFT' || row.status === 'REJECTED' || !row.id
const fmt = (n: any) => Number(n ?? 0).toLocaleString('zh-CN')

const totalActual = computed(() => rows.value.reduce((s, r) => s + (Number(r.actualMeter) || 0), 0))
const totalRolls = computed(() => rows.value.reduce((s, r) => s + (Number(r.rolls) || 0), 0))
const totalAdjust = computed(() => rows.value.reduce((s, r) => s + (Number(r.adjustMeter) || 0), 0))

function blankRow(): any {
  return {
    _key: keySeq++, id: null, date: queryDate.value, orderId: '', times: 2, speed: null,
    material: '', spec: '', requiredMeter: null, actualMeter: null, rolls: null,
    printRemain: null, machineType: 'DRY', adjustMeter: null,
    signer: userStore.user?.realName ?? '', status: 'DRAFT'
  }
}

function addRow() { rows.value.unshift(blankRow()) }

async function removeRow(i: number, row: any) {
  try {
    if (row.id) await ElMessageBox.confirm('确认删除该行？', '删除', { type: 'warning' })
    if (row.id) await api.delete(`/lamination-daily/${row.id}`)
    rows.value.splice(i, 1)
  } catch { /* cancel */ }
}

function toReq(row: any) {
  return {
    date: row.date ?? queryDate.value, orderId: row.orderId, times: row.times,
    speed: row.speed, material: row.material, spec: row.spec,
    requiredMeter: row.requiredMeter, actualMeter: row.actualMeter, rolls: row.rolls,
    printRemain: row.printRemain, machineType: row.machineType, adjustMeter: row.adjustMeter,
    signer: row.signer
  }
}

async function load() {
  const data = await api.get('/lamination-daily', { params: { page: 1, size: 200, date: queryDate.value } })
  rows.value = data.records.map((r: any) => ({ _key: keySeq++, ...r, date: r.date ?? queryDate.value }))
  const pend = await api.get('/lamination-daily', { params: { page: 1, size: 1, status: 'PENDING' } })
  pendingCount.value = pend.total
}

async function saveAll(submit: boolean) {
  // 只保存可编辑行（DRAFT/REJECTED/新行）；只读行 PUT 会被 2002 拒绝并中断整批
  const editable = rows.value.filter(canEdit)
  if (!editable.length) { ElMessage.warning('没有可保存的行（待审核/已审核记录为只读）'); return }
  saving.value = true
  try {
    for (const row of editable) {
      if (row.id) {
        await api.put(`/lamination-daily/${row.id}`, { ...toReq(row), submit })
        if (submit) await api.put(`/lamination-daily/${row.id}/submit`)
      } else {
        await api.post('/lamination-daily', { ...toReq(row), submit })
      }
    }
    ElMessage.success((submit ? '日报已提交，等待主管审核：' : '草稿已保存：') + editable.length + ' 行')
    await load()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

onMounted(() => { load().catch(() => {}) })
</script>
