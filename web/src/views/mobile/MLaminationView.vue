<template>
  <div class="app with-submitbar">
    <header class="m-head">
      <div class="m-head-row">
        <router-link class="m-back" to="/m/home">
          <AppIcon name="i-arrow-right" :size="18" style="color:#fff; transform:rotate(180deg)" />
        </router-link>
        <div>
          <div class="m-title">复合部日报填报</div>
          <div class="sub">新增记录</div>
        </div>
        <div class="m-head-date">{{ today }}</div>
      </div>
    </header>

    <main class="m-body">
      <section class="m-card">
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">生产单号<span class="req">*</span></label>
            <el-input v-model="form.orderId" placeholder="如 FH-20261123-01" />
          </div>
          <div class="m-field">
            <label class="m-label">机型<span class="req">*</span></label>
            <div class="m-seg">
              <div class="m-seg-item" :class="{ on: form.machineType === 'DRY' }" @click="form.machineType = 'DRY'">干式</div>
              <div class="m-seg-item" :class="{ on: form.machineType === 'SOLVENT_FREE' }" @click="form.machineType = 'SOLVENT_FREE'">无溶剂</div>
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">工艺参数</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">次数（复合层数）<span class="req">*</span></label>
            <div class="m-stepper">
              <button class="m-step-btn" @click="form.times = Math.max(1, (form.times || 1) - 1)">−</button>
              <div class="m-step-val">{{ form.times }}</div>
              <button class="m-step-btn" @click="form.times = (form.times || 1) + 1">＋</button>
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">机速<span class="req">*</span></label>
            <el-input-number v-model="form.speed" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">材质<span class="req">*</span></label>
            <el-input v-model="form.material" />
          </div>
          <div class="m-field">
            <label class="m-label">规格<span class="req">*</span></label>
            <el-input v-model="form.spec" placeholder="长×宽 mm" />
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">生产数量</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">工单要求米数<span class="req">*</span></label>
            <el-input-number v-model="form.requiredMeter" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">实际复合米数<span class="req">*</span></label>
            <el-input-number v-model="form.actualMeter" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">卷数<span class="req">*</span></label>
              <div class="m-stepper">
                <button class="m-step-btn" @click="form.rolls = Math.max(0, (form.rolls || 0) - 1)">−</button>
                <div class="m-step-val">{{ form.rolls ?? 0 }}</div>
                <button class="m-step-btn" @click="form.rolls = (form.rolls || 0) + 1">＋</button>
              </div>
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">调机米数</label>
              <el-input-number v-model="form.adjustMeter" :min="0" controls-position="right" style="width:100%" />
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">印刷剩余</label>
            <el-input-number v-model="form.printRemain" :min="0" controls-position="right" style="width:100%" />
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">签名</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">签名<span class="req">*</span></label>
            <el-input v-model="form.signer" />
          </div>
        </div>
      </section>
    </main>

    <div class="m-submitbar">
      <div class="sum">本行未保存</div>
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
import { todayStr } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)
const today = todayStr()

const form = reactive<any>({
  orderId: '', machineType: 'DRY', times: 2, speed: null, material: '', spec: '',
  requiredMeter: null, actualMeter: null, rolls: 0, printRemain: null,
  adjustMeter: null, signer: userStore.user?.realName ?? ''
})

async function save(submit: boolean) {
  saving.value = true
  try {
    await api.post('/lamination-daily', { date: today, ...form, submit })
    router.push({ path: '/m/success', query: { orderId: form.orderId } })
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}
</script>
