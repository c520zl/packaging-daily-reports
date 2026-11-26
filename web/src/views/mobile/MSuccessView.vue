<template>
  <div class="app">
    <main class="success-wrap">
      <div class="success-icon"><AppIcon name="i-check" :size="34" /></div>
      <div class="success-title">提交成功</div>
      <div class="success-desc">{{ orderId || '记录' }} 已提交给部门主管审核<br>审核结果将在「通知」中提醒您</div>

      <section class="m-card success-card">
        <div class="m-card-body" style="padding-top:2px; padding-bottom:2px">
          <div class="m-field">
            <label class="m-label">生产单号</label>
            <div style="font-size:15px; font-weight:600; font-variant-numeric:tabular-nums">{{ orderId }}</div>
          </div>
          <div class="m-field">
            <label class="m-label">当前状态</label>
            <div><span class="tag tag-orange"><span class="dot"></span>待主管审核</span></div>
          </div>
        </div>
      </section>

      <div class="success-actions">
        <router-link class="m-btn m-btn-outline" to="/m/records">我的记录</router-link>
        <router-link class="m-btn m-btn-primary" :to="backPath">继续填报</router-link>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const orderId = computed(() => route.query.orderId as string)
// 从哪个填报页来就回哪个（m-success 由三部门填报页共用）
const backPath = computed(() => (route.query.from as string) || '/m/print')
</script>

<style scoped>
.success-wrap { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 48px 24px 24px; }
.success-icon { width: 72px; height: 72px; border-radius: 50%; background: var(--success-weak); color: var(--success); display: grid; place-items: center; margin-bottom: 20px; }
.success-title { font-size: 20px; font-weight: 700; }
.success-desc { font-size: 14px; color: var(--muted); margin-top: 8px; line-height: 1.7; }
.success-card { width: 100%; margin-top: 24px; text-align: left; }
.success-actions { display: flex; gap: 10px; width: 100%; margin-top: 20px; max-width: 100%; }
.success-actions .m-btn { flex: 1; min-width: 0; justify-content: center; display: inline-flex; align-items: center; }
</style>
