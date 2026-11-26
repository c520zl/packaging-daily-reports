<template>
  <AdminShell :title="`报表详情 · ${orderId}`" :badge="statusLabel(status)">
    <div class="detail-cols">
      <div style="display:flex; flex-direction:column; gap:var(--space-4); min-width:0">
        <section class="card">
          <div class="card-head">
            <div>
              <div class="card-title">{{ moduleLabel }}日报 · 明细信息</div>
              <div class="card-sub">单号 {{ orderId }} · 录入人 {{ record.createdBy }}</div>
            </div>
            <div class="card-actions">
              <el-button size="small" v-if="canEdit" @click="editing = !editing">
                <AppIcon name="i-edit" :size="14" />&nbsp;{{ editing ? '取消' : '编辑' }}
              </el-button>
              <el-button size="small" type="danger" plain v-if="canEdit" @click="del">
                <AppIcon name="i-trash" :size="14" />&nbsp;删除
              </el-button>
            </div>
          </div>
          <div class="card-body">
            <template v-for="group in fieldGroups" :key="group.title">
              <div class="group-title">{{ group.title }}</div>
              <div class="kv-grid">
                <div class="kv" v-for="f in group.fields" :key="f.key">
                  <div class="k">{{ f.label }}<span v-if="f.required" style="color:var(--danger)">*</span></div>
                  <el-input v-if="editing && f.type !== 'select'" v-model="record[f.key]" size="small" />
                  <el-select v-else-if="editing" v-model="record[f.key]" size="small" style="width:100%">
                    <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
                  </el-select>
                  <div v-else class="v">{{ f.format ? f.format(record[f.key]) : (record[f.key] ?? '—') }}</div>
                </div>
              </div>
            </template>
            <div v-if="record.rejectReason" class="reject-banner" style="margin-top:16px">
              <AppIcon name="i-bell" :size="15" />
              <span>退回原因：{{ record.rejectReason }}</span>
            </div>
            <el-button v-if="editing" type="primary" style="margin-top:16px" @click="save">保存修改</el-button>
          </div>
        </section>

        <section class="card">
          <div class="card-head"><div class="card-title">修改留痕</div></div>
          <div class="card-body">
            <div class="kv-grid" style="grid-template-columns:repeat(2,1fr)">
              <div class="kv"><div class="k">创建时间</div><div class="v sub-val">{{ fmtTime(record.createdAt) }}</div></div>
              <div class="kv"><div class="k">录入人</div><div class="v sub-val">{{ record.createdBy }}</div></div>
            </div>
          </div>
        </section>
      </div>

      <section class="card">
        <div class="card-head">
          <div>
            <div class="card-title">审核流转</div>
            <div class="card-sub">员工提交 → 主管审核 → 归档</div>
          </div>
        </div>
        <div class="card-body" style="padding-top:var(--space-4)">
          <div class="audit-step" v-for="(t, i) in timeline" :key="i">
            <div class="audit-dot" :style="dotStyle(t.action)">
              <AppIcon :name="t.action === 'APPROVE' ? 'i-check' : 'i-edit'" :size="12" />
            </div>
            <div>
              <div class="audit-title">{{ actionLabel(t.action) }}</div>
              <div class="audit-desc">{{ t.actor ?? t.actorName }} · {{ t.detail }}</div>
              <div class="audit-time">{{ fmtTime(t.time ?? t.createdAt) }}</div>
            </div>
          </div>
        </div>
        <div class="form-foot" style="position:static;border-radius:0" v-if="status === 'PENDING' && canAudit">
          <el-button style="flex:1" @click="doReject">退回</el-button>
          <el-button type="primary" style="flex:1" @click="doApprove">
            <AppIcon name="i-check" :size="15" />&nbsp;审核通过
          </el-button>
        </div>
      </section>
    </div>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { STATUS, MACHINE_TYPE, SHIFT } from '@/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const moduleKey = computed(() => route.params.module as string)
const recordId = route.params.id as string
const orderId = computed(() => record.value.orderId ?? '')

const record = ref<any>({})
const timeline = ref<any[]>([])
const editing = ref(false)

const path = computed(() => ({ PRINT: 'print-daily', LAMINATION: 'lamination-daily', BAGMAKING: 'bag-daily' }[moduleKey.value]))
const moduleLabel = computed(() => ({ PRINT: '印刷部', LAMINATION: '复合部', BAGMAKING: '制袋部' }[moduleKey.value]))
const status = computed(() => record.value.status)
const statusLabel = (s: string) => (STATUS as any)[s]?.label ?? s
const actionLabel = (a: string) => ({ CREATE: '创建', SUBMIT: '提交审核', WITHDRAW: '撤回', EDIT: '修改', APPROVE: '审核通过', REJECT: '退回', DELETE: '删除' }[a] ?? a)
const fmtTime = (t: string) => t?.replace('T', ' ').slice(0, 16)
const canEdit = computed(() => userStore.isAdmin || record.value.createdBy === userStore.user?.realName)
/** 审核按钮：管理员/管理层全部可审；主管仅本部门（与后端 requireLeadScope 一致） */
const canAudit = computed(() =>
  userStore.isAdmin || userStore.isManagement
  || (userStore.isLead && userStore.user?.dept === moduleKey.value))

const fieldGroups = computed<any[]>(() => {
  const m = moduleKey.value
  if (m === 'PRINT') return [
    { title: '生产信息', fields: [
      { key: 'orderId', label: '生产单号', required: true },
      { key: 'date', label: '日期' },
      { key: 'onboardTime', label: '上机时间' },
      { key: 'speed', label: '机速 m/min' },
      { key: 'colorsText', label: '印刷颜色' },
      { key: 'printMinutes', label: '印刷时间(分)' },
      { key: 'signer', label: '签名', required: true },
      { key: 'remark', label: '备注' }
    ]},
    { title: '材料信息', fields: [
      { key: 'material', label: '材质', required: true },
      { key: 'thickness', label: '厚度 μm' },
      { key: 'spec', label: '尺寸规格', required: true }
    ]},
    { title: '数量统计', fields: [
      { key: 'requiredMeter', label: '要求印刷米数', required: true },
      { key: 'actualMeter', label: '实际印刷米数', required: true }
    ]}
  ]
  if (m === 'LAMINATION') return [
    { title: '生产信息', fields: [
      { key: 'orderId', label: '生产单号', required: true },
      { key: 'date', label: '日期' },
      { key: 'times', label: '次数', required: true },
      { key: 'speed', label: '机速 m/min' },
      { key: 'machineType', label: '机型', required: true, type: 'select',
        options: Object.entries(MACHINE_TYPE).map(([value, label]) => ({ value, label })),
        format: (v: string) => MACHINE_TYPE[v as keyof typeof MACHINE_TYPE] ?? v },
      { key: 'signer', label: '签名', required: true }
    ]},
    { title: '材料信息', fields: [
      { key: 'material', label: '材质', required: true },
      { key: 'spec', label: '规格', required: true }
    ]},
    { title: '数量统计', fields: [
      { key: 'requiredMeter', label: '工单要求米数', required: true },
      { key: 'actualMeter', label: '实际复合米数', required: true },
      { key: 'rolls', label: '卷数' },
      { key: 'printRemain', label: '印刷剩余' },
      { key: 'adjustMeter', label: '调机米数' }
    ]}
  ]
  return [
    { title: '生产信息', fields: [
      { key: 'orderId', label: '生产单号', required: true },
      { key: 'date', label: '日期' },
      { key: 'shift', label: '班次', required: true, type: 'select',
        options: Object.entries(SHIFT).map(([value, label]) => ({ value, label })),
        format: (v: string) => SHIFT[v as keyof typeof SHIFT] ?? v },
      { key: 'machine', label: '机台', required: true },
      { key: 'picker', label: '捡袋员', required: true },
      { key: 'leader', label: '机长', required: true },
      { key: 'bagType', label: '袋型', required: true },
      { key: 'spec', label: '成品规格', required: true },
      { key: 'signer', label: '签名', required: true }
    ]},
    { title: '数量统计', fields: [
      { key: 'incomingMeter', label: '来料米数', required: true },
      { key: 'adjustMeter', label: '调机米数' },
      { key: 'endMeter', label: '结束米数', required: true },
      { key: 'completedQty', label: '完成数量', required: true },
      { key: 'wasteKg', label: '废料 kg' },
      { key: 'remainKg', label: '余料 kg' }
    ]},
    { title: '工时与质量', fields: [
      { key: 'workStart', label: '工时起', required: true },
      { key: 'workEnd', label: '工时止', required: true },
      { key: 'abnormalReason', label: '异常原因' },
      { key: 'defectQty', label: '不良品数量' }
    ]}
  ]
})

function dotStyle(action: string) {
  const map: Record<string, string> = {
    CREATE: 'background:var(--surface-2);color:var(--muted)',
    SUBMIT: 'background:var(--primary-weak);color:var(--primary)',
    APPROVE: 'background:var(--success-weak);color:var(--success)',
    REJECT: 'background:var(--danger-weak);color:var(--danger)',
    WITHDRAW: 'background:var(--warning-weak);color:var(--warning)',
    EDIT: 'background:var(--primary-weak);color:var(--primary)'
  }
  return map[action] ?? map.CREATE
}

async function load() {
  record.value = await api.get(`/${path.value}/${recordId}`)
  record.value.colorsText = (record.value.colors || []).join(',')
  timeline.value = await api.get(`/${path.value}/${recordId}/timeline`)
}

async function save() {
  const r: any = { ...record.value }
  if (moduleKey.value === 'PRINT') r.colors = (r.colorsText || '').split(/[,，]/).filter(Boolean)
  delete r.colorsText
  await api.put(`/${path.value}/${recordId}`, r)
  ElMessage.success('已保存')
  editing.value = false
  load()
}

async function del() {
  await ElMessageBox.confirm('确认删除该记录？', '删除', { type: 'warning' })
  await api.delete(`/${path.value}/${recordId}`)
  ElMessage.success('已删除')
  router.push('/reports')
}

async function doApprove() {
  await api.put(`/${path.value}/${recordId}/approve`)
  ElMessage.success(orderId.value + ' 审核通过，已计入产量汇总')
  load()
}

async function doReject() {
  const { value } = await ElMessageBox.prompt('请填写退回原因', '退回', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
  await api.put(`/${path.value}/${recordId}/reject`, { reason: value })
  ElMessage.success(orderId.value + ' 已退回，已通知填报人修改')
  load()
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
.detail-cols { display: grid; grid-template-columns: 1.6fr 1fr; gap: var(--space-4); align-items: start; }
.kv-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-3) var(--space-5); }
.group-title {
  font-size: var(--fs-sm); font-weight: 600; color: var(--primary);
  padding: 6px 10px; margin: var(--space-2) 0 var(--space-3);
  background: var(--primary-weak); border-radius: var(--radius-sm);
  display: inline-block;
}
.group-title + .kv-grid { margin-bottom: var(--space-4); }
.kv { min-width: 0; }
.kv .k { font-size: var(--fs-xs); color: var(--muted); }
.kv .v { font-size: var(--fs-md); font-weight: 500; margin-top: 4px; font-variant-numeric: tabular-nums; }
.kv .v.sub-val { font-weight: 400; font-size: var(--fs-sm); }
.audit-step { display: flex; gap: 12px; position: relative; padding-bottom: 20px; min-height: 44px; }
.audit-step:last-child { padding-bottom: 0; }
.audit-step::before { content: ""; position: absolute; left: 11px; top: 30px; bottom: 4px; width: 2px; background: var(--line); }
.audit-step:last-child::before { display: none; }
.audit-dot { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0; z-index: 1; margin-top: 10px; }
.audit-title { font-size: var(--fs-sm); font-weight: 600; }
.audit-desc { font-size: var(--fs-xs); color: var(--muted); margin-top: 3px; line-height: 1.6; }
.audit-time { font-size: var(--fs-xs); color: var(--muted); margin-top: 3px; font-variant-numeric: tabular-nums; }
.reject-banner {
  background: var(--danger-weak); border: 1px solid var(--danger); border-radius: var(--radius-md);
  padding: 10px 14px; font-size: var(--fs-sm); color: #B91C1C; display: flex; gap: 8px; align-items: flex-start; line-height: 1.6;
}
@media (max-width: 768px) {
  .kv-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .detail-cols { grid-template-columns: 1fr; }
}
</style>
