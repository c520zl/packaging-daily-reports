<template>
  <div class="app with-submitbar">
    <header class="m-head">
      <div class="m-head-row">
        <router-link class="m-back" to="/m/home">
          <AppIcon name="i-arrow-right" :size="18" style="color:#fff; transform:rotate(180deg)" />
        </router-link>
        <div>
          <div class="m-title">印刷部日报填报</div>
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
            <el-input v-model="form.orderId" placeholder="如 YS-20261123-01" />
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">上机信息</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">上机时间<span class="req">*</span></label>
            <el-time-select v-model="form.onboardTime" start="00:00" step="00:05" end="23:55" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">机速<span class="req">*</span></label>
            <div class="m-unit-wrap">
              <el-input-number v-model="form.speed" :min="0" controls-position="right" style="width:100%" class="m-input-num" />
              <span class="m-unit">m/min</span>
            </div>
          </div>
          <div class="m-field">
            <label class="m-label">印刷颜色<span class="req">*</span></label>
            <div class="m-chips">
              <span v-for="c in COLOR_OPTIONS" :key="c" class="m-chip" :class="{ on: colors.includes(c) }" @click="toggleColor(c)">{{ c }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">材料信息</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">材质<span class="req">*</span></label>
            <el-input v-model="form.material" />
          </div>
          <div class="m-row2">
            <div class="m-field m-field-inline">
              <label class="m-label">材质厚度</label>
              <el-input-number v-model="form.thickness" :min="0" controls-position="right" style="width:100%" />
            </div>
            <div class="m-field m-field-inline">
              <label class="m-label">尺寸规格<span class="req">*</span></label>
              <el-input v-model="form.spec" placeholder="长×宽 mm" />
            </div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">生产数量</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">要求印刷米数<span class="req">*</span></label>
            <el-input-number v-model="form.requiredMeter" :min="0" controls-position="right" style="width:100%" />
          </div>
          <div class="m-field">
            <label class="m-label">实际印刷米数<span class="req">*</span></label>
            <el-input-number v-model="form.actualMeter" :min="0" controls-position="right" style="width:100%" @change="autoMinutes" />
          </div>
          <div class="m-field">
            <label class="m-label">印刷时间<span class="m-auto">自动计算</span></label>
            <div class="m-calc">{{ form.printMinutes ?? '待实际米数填入后自动算出' }}<span class="hint">实际米数 ÷ 机速</span></div>
          </div>
        </div>
      </section>

      <section class="m-card">
        <div class="m-card-head"><div class="m-card-title">签名与备注</div></div>
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">签名<span class="req">*</span></label>
            <el-input v-model="form.signer" />
          </div>
          <div class="m-field">
            <label class="m-label">备注</label>
            <el-input v-model="form.remark" type="textarea" :rows="2" />
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
const COLOR_OPTIONS = ['黑', '红', '蓝', '黄', '白', '专色']
const colors = ref<string[]>([])

const today = todayStr()
const form = reactive<any>({
  orderId: '', onboardTime: '08:00', speed: null, material: '', thickness: null,
  spec: '', requiredMeter: null, actualMeter: null, printMinutes: null,
  signer: userStore.user?.realName ?? '', remark: ''
})

function toggleColor(c: string) {
  const i = colors.value.indexOf(c)
  i >= 0 ? colors.value.splice(i, 1) : colors.value.push(c)
}

function autoMinutes() {
  if (form.actualMeter && form.speed) {
    form.printMinutes = Math.round(form.actualMeter / form.speed * 10) / 10
  }
}

async function save(submit: boolean) {
  saving.value = true
  try {
    await api.post('/print-daily', {
      date: today, ...form, colors: colors.value, submit
    })
    router.push({ path: '/m/success', query: { orderId: form.orderId } })
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}
</script>
