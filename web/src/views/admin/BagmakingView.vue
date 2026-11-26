<template>
  <AdminShell title="制袋部日报表" :badge="`待审核 ${pendingCount} 行`">
    <section class="card">
      <div class="card-head">
        <div>
          <div class="card-title">公共信息</div>
          <div class="card-sub">按班次 / 机台筛选查看，一行记录对应一个机台一个班次</div>
        </div>
      </div>
      <div class="card-body filter-bar">
        <div class="field" style="min-width:180px">
          <label class="field-label">日期</label>
          <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="field" style="min-width:130px">
          <label class="field-label">班次</label>
          <el-select v-model="queryShift" clearable style="width:100%">
            <el-option label="全部班次" value="" /><el-option label="白班" value="DAY" /><el-option label="夜班" value="NIGHT" />
          </el-select>
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
              <th style="width:70px">班次 *</th>
              <th style="width:76px">机台 *</th>
              <th style="width:90px">捡袋员</th>
              <th style="width:80px">机长</th>
              <th style="width:78px">机速 m/min</th>
              <th style="width:118px">生产单号 *</th>
              <th style="width:140px">袋型 *</th>
              <th style="width:96px">成品规格</th>
              <th style="width:82px">来料米数</th>
              <th style="width:78px">调机米数</th>
              <th style="width:82px">结束米数</th>
              <th style="width:82px">完成数量</th>
              <th style="width:68px">废料 kg</th>
              <th style="width:68px">余料 kg</th>
              <th style="width:80px">工时起</th>
              <th style="width:80px">工时止</th>
              <th style="min-width:200px">异常原因</th>
              <th style="width:68px">不良品</th>
              <th style="width:48px">状态</th>
              <th style="width:44px"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in rows" :key="row._key">
              <template v-if="canEdit(row)">
                <td><el-select v-model="row.shift" size="small" style="width:100%">
                  <el-option label="白班" value="DAY" /><el-option label="夜班" value="NIGHT" />
                </el-select></td>
                <td><el-select v-model="row.machine" size="small" style="width:100%">
                  <el-option v-for="m in MACHINES" :key="m" :label="m" :value="m" />
                </el-select></td>
                <td><el-input v-model="row.picker" size="small" /></td>
                <td><el-input v-model="row.leader" size="small" /></td>
                <td><el-input-number v-model="row.speed" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input v-model="row.orderId" size="small" /></td>
                <td><el-select v-model="row.bagType" size="small" style="width:100%" filterable>
                  <el-option v-for="b in BAG_TYPES" :key="b" :label="b" :value="b" />
                </el-select></td>
                <td><el-input v-model="row.spec" size="small" /></td>
                <td><el-input-number v-model="row.incomingMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.adjustMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.endMeter" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.completedQty" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.wasteKg" :min="0" :precision="2" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-input-number v-model="row.remainKg" :min="0" :precision="2" size="small" controls-position="right" style="width:100%" /></td>
                <td><el-time-select v-model="row.workStart" start="00:00" step="00:30" end="23:30" size="small" style="width:100%" /></td>
                <td><el-time-select v-model="row.workEnd" start="00:00" step="00:30" end="23:55" size="small" style="width:100%" /></td>
                <td><el-input v-model="row.abnormalReason" size="small" /></td>
                <td><el-input-number v-model="row.defectQty" :min="0" size="small" controls-position="right" style="width:100%" /></td>
                <td><span class="tag" :class="statusCls(row.status)">{{ statusLabel(row.status) }}</span></td>
                <td><button class="row-del" @click="removeRow(i, row)"><AppIcon name="i-trash" :size="15" /></button></td>
              </template>
              <!-- 只读行：纯文本完整展示 -->
              <template v-else>
                <td class="ro-cell">{{ row.shift === 'DAY' ? '白班' : '夜班' }}</td>
                <td class="ro-cell">{{ row.machine }}</td>
                <td class="ro-cell">{{ row.picker }}</td>
                <td class="ro-cell">{{ row.leader }}</td>
                <td class="ro-cell num">{{ fmt(row.speed) }}</td>
                <td class="ro-cell" :title="row.orderId">{{ row.orderId }}</td>
                <td class="ro-cell">{{ row.bagType }}</td>
                <td class="ro-cell">{{ row.spec }}</td>
                <td class="ro-cell num">{{ fmt(row.incomingMeter) }}</td>
                <td class="ro-cell num">{{ fmt(row.adjustMeter) }}</td>
                <td class="ro-cell num">{{ fmt(row.endMeter) }}</td>
                <td class="ro-cell num">{{ fmt(row.completedQty) }}</td>
                <td class="ro-cell num">{{ row.wasteKg ?? '—' }}</td>
                <td class="ro-cell num">{{ row.remainKg ?? '—' }}</td>
                <td class="ro-cell">{{ row.workStart }}</td>
                <td class="ro-cell">{{ row.workEnd }}</td>
                <td class="ro-cell ro-wide" :title="row.abnormalReason">{{ row.abnormalReason || '—' }}</td>
                <td class="ro-cell num">{{ row.defectQty ?? '—' }}</td>
                <td><span class="tag" :class="statusCls(row.status)">{{ statusLabel(row.status) }}</span></td>
                <td></td>
              </template>
            </tr>
          </tbody>
          <tbody v-if="!rows.length">
            <tr><td :colspan="20" style="text-align:center; padding:32px 0; color:var(--muted)">
              今日（{{ queryDate }}）暂无填报记录，点击右上角「添加一行」开始填报
            </td></tr>
          </tbody>
        </table>
      </div>
      <div class="form-foot">
        <span class="form-foot-sum">
          合计：完成 <b>{{ fmt(totalQty) }}</b> 个 · 废料 <b>{{ totalWaste }}</b> kg · 余料 <b>{{ totalRemain }}</b> kg · 不良品 <b>{{ totalDefect }}</b> 个
        </span>
        <span class="toolbar" style="margin-left:auto">
          <el-button @click="saveAll(false)" :loading="saving">存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="saveAll(true)">
            <AppIcon name="i-check" :size="15" />&nbsp;提交日报
          </el-button>
        </span>
      </div>
    </section>

    <p class="card-sub">提示：结束米数 = 来料米数 − 调机米数 − 当班使用米数；同一机台同班次同一生产单号重复录入时系统将提示去重。</p>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, BAG_TYPES, MACHINES, todayStr } from '@/constants'

const userStore = useUserStore()
const rows = ref<any[]>([])
const queryDate = ref(todayStr())
const queryShift = ref('')
const saving = ref(false)
const pendingCount = ref(0)
let keySeq = 1

const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const statusCls = (s: string) => (STATUS as any)[s]?.cls ?? 'tag-gray'
const canEdit = (row: any) => row.status === 'DRAFT' || row.status === 'REJECTED' || !row.id
const fmt = (n: any) => Number(n ?? 0).toLocaleString('zh-CN')

const totalQty = computed(() => rows.value.reduce((s, r) => s + (Number(r.completedQty) || 0), 0))
const totalWaste = computed(() => Math.round(rows.value.reduce((s, r) => s + (Number(r.wasteKg) || 0), 0) * 10) / 10)
const totalRemain = computed(() => Math.round(rows.value.reduce((s, r) => s + (Number(r.remainKg) || 0), 0) * 10) / 10)
const totalDefect = computed(() => rows.value.reduce((s, r) => s + (Number(r.defectQty) || 0), 0))

function blankRow(): any {
  return {
    _key: keySeq++, id: null, date: queryDate.value, shift: 'DAY', machine: '1号机',
    picker: '', leader: '', speed: null, orderId: '', bagType: '三边封袋', spec: '',
    incomingMeter: null, adjustMeter: null, endMeter: null, completedQty: null,
    wasteKg: null, remainKg: null, workStart: '08:00', workEnd: '16:00',
    abnormalReason: '', defectQty: null, signer: userStore.user?.realName ?? '', status: 'DRAFT'
  }
}

function addRow() { rows.value.unshift(blankRow()) }

async function removeRow(i: number, row: any) {
  try {
    if (row.id) await ElMessageBox.confirm('确认删除该行？', '删除', { type: 'warning' })
    if (row.id) await api.delete(`/bag-daily/${row.id}`)
    rows.value.splice(i, 1)
  } catch { /* cancel */ }
}

function toReq(row: any) {
  return {
    date: row.date ?? queryDate.value, shift: row.shift, machine: row.machine,
    picker: row.picker, leader: row.leader, speed: row.speed, orderId: row.orderId,
    bagType: row.bagType, spec: row.spec, incomingMeter: row.incomingMeter,
    adjustMeter: row.adjustMeter, endMeter: row.endMeter, completedQty: row.completedQty,
    wasteKg: row.wasteKg, remainKg: row.remainKg, workStart: row.workStart,
    workEnd: row.workEnd, abnormalReason: row.abnormalReason, defectQty: row.defectQty,
    signer: row.signer
  }
}

async function load() {
  const data = await api.get('/bag-daily', { params: { page: 1, size: 200, date: queryDate.value, shift: queryShift.value || undefined } })
  rows.value = data.records.map((r: any) => ({ _key: keySeq++, ...r, date: r.date ?? queryDate.value }))
  const pend = await api.get('/bag-daily', { params: { page: 1, size: 1, status: 'PENDING' } })
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
        await api.put(`/bag-daily/${row.id}`, { ...toReq(row), submit })
        if (submit) await api.put(`/bag-daily/${row.id}/submit`)
      } else {
        await api.post('/bag-daily', { ...toReq(row), submit })
      }
    }
    ElMessage.success((submit ? '日报已提交，等待主管审核：' : '草稿已保存：') + editable.length + ' 行')
    await load()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
/* 异常原因长文本列优先吸收剩余宽度，其余列按内容收紧 */
.form-table :deep(th[style*="200px"]), .form-table :deep(td:nth-child(18)) { white-space: normal; }
</style>
