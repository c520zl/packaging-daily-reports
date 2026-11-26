<template>
  <AppShell title="提交成功">
    <div class="v-success">
      <van-icon name="checked" size="64" color="var(--success)" />
      <div class="v-success-title">提交成功</div>
      <div class="v-success-desc">{{ orderId || '记录' }} 已提交给部门主管审核<br>审核结果将在「通知」中提醒您</div>
    </div>

    <van-cell-group inset class="v-app-card">
      <van-cell title="生产单号" :value="orderId || '—'" />
      <van-cell title="当前状态">
        <template #value><span class="v-tag v-tag--orange">待主管审核</span></template>
      </van-cell>
    </van-cell-group>

    <div class="v-success-actions">
      <van-button block plain type="primary" replace to="/fill/records">我的记录</van-button>
      <van-button block type="primary" replace :to="backPath">继续填报</van-button>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const orderId = computed(() => route.query.orderId as string)
const backPath = computed(() => (route.query.from as string) || '/fill/print')
</script>

<style scoped>
.v-success {
  display: flex; flex-direction: column; align-items: center;
  text-align: center; padding: 40px 24px 20px;
}
.v-success-title { font-size: 20px; font-weight: 700; margin-top: 14px; }
.v-success-desc { font-size: var(--fs-md); color: var(--muted); margin-top: 8px; line-height: 1.7; }
.v-success-actions { display: flex; gap: 10px; padding: 16px; }
</style>
