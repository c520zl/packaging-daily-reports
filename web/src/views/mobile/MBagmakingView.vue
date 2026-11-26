<template>
  <div class="app with-submitbar">
    <header class="m-head">
      <div class="m-head-row">
        <router-link class="m-back" to="/m/home">
          <AppIcon name="i-arrow-right" :size="18" style="color:#fff; transform:rotate(180deg)" />
        </router-link>
        <div>
          <div class="m-title">制袋部日报填报</div>
          <div class="sub">新增记录</div>
        </div>
        <div class="m-head-date">{{ today }}</div>
      </div>
    </header>

    <main class="m-body">
      <section class="m-card">
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">班次<span class="req">*</span></label>
            <div class="m-seg">
              <div class="m-seg-item" :class="{ on: form.shift === 'DAY' }" @click="form.shift = 'DAY'">白班</div>
              <div class="m-seg-item" :class="{ on: form.shift === 'NIGHT' }" @click="form.shift = 'NIGHT'">夜班</div>
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">机台<span class="req">*</span></label>
            <div class="m-grid-select">
              <div v-for="m in MACHINES" :key="m" class="m-grid-item" :class="{ on: form.machine === m }" @click="form.machine = m">{{ m }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">人员信息</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">机长<span class="req">*</span></label>
              <el-input v-model="form.leader" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">捡袋员<span class="req">*</span></label>
              <el-input v-model="form.picker" />
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">生产信息</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">生产单号<span class="req">*</span></label>
            <el-input v-model="form.orderId" placeholder="如 DB-20261123-01" />
          </div>
          <div class="m-field">
            <label class="m-label">袋型<span class="req">*</span></label>
            <el-select v-model="form.bagType" style="width:100%" filterable>
              <el-option v-for="b in BAG_TYPES" :key="b" :label="b" :value="b" />
            </el-select>
          </div>
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">成品规格<span class="req">*</span></label>
              <el-input v-model="form.spec" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">机速<span class="req">*</span></label>
              <el-input-number v-model="form.speed" :min="0" controls-position="right" style="width:100%" />
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">米数统计</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">来料米数<span class="req">*</span></label>
              <el-input-number v-model="form.incomingMeter" :min="0" controls-position="right" style="width:100%" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">调机米数</label>
              <el-input-number v-model="form.adjustMeter" :min="0" controls-position="right" style="width:100%" />
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">结束米数<span class="req">*</span></label>
            <el-input-number v-model="form.endMeter" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">完成数量<span class="req">*</span></label>
            <el-input-number v-model="form.completedQty" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">废料</label>
              <el-input-number v-model="form.wasteKg" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">余料</label>
              <el-input-number v-model="form.remainKg" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">工时与质量</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">工时起<span class="req">*</span></label>
              <el-time-select v-model="form.workStart" start="00:00" step="00:30" end="23:30" style="width:100%" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">工时止<span class="req">*</span></label>
              <el-time-select v-model="form.workEnd" start="00:00" step="00:30" end="23:55" style="width:100%" />
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">不良品数量</label>
            <el-input-number v-model="form.defectQty" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">异常原因</label>
            <el-input v-model="form.abnormalReason" type="textarea" :rows="2" placeholder="如：封口温度偏高，调机 40 分钟" />
          </div>
        </div>
      </section>
    </main>

    <div class="m-submitbar">
      <div class="sum">完成 {{ form.completedQty ?? 0 }} 个</div>
      <el-button @click="save(false)" :loading="saving">存草稿</el-button>
      <el-button type="primary" @click="save(true)" :loading="saving">保存本行</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { BAG_TYPES, MACHINES, todayStr } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)
const today = todayStr()

const form = reactive<any>({
  shift: 'DAY', machine: '1号机', picker: '', leader: userStore.user?.realName ?? '',
  speed: null, orderId: '', bagType: '三边封袋', spec: '',
  incomingMeter: null, adjustMeter: null, endMeter: null, completedQty: null,
  wasteKg: null, remainKg: null, workStart: '08:00', workEnd: '16:00',
  abnormalReason: '', defectQty: null, signer: userStore.user?.realName ?? ''
})

async function save(submit: boolean) {
  saving.value = true
  try {
    await api.post('/bag-daily', { date: today, ...form, submit })
    router.push({ path: '/m/success', query: { orderId: form.orderId } })
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}
</script>
