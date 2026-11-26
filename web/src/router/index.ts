import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由表
 * /login 独立页；管理端 /dashboard 等（仅 ADMIN/MANAGEMENT）；
 * 填报端 /fill/*：Vant App 风格，WORKER 与 DEPT_LEAD 共用（主管多一个审核 tab），
 *                手机优先响应式设计，任何屏宽都是同一入口；
 * 移动端 /m/*：旧移动版，仅作兼容保留（API.md §11）
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    // 管理端（系统管理员 / 厂部管理层）
    { path: '/dashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { admin: true } },
    { path: '/print', component: () => import('@/views/admin/PrintView.vue'), meta: { admin: true } },
    { path: '/lamination', component: () => import('@/views/admin/LaminationView.vue'), meta: { admin: true } },
    { path: '/bagmaking', component: () => import('@/views/admin/BagmakingView.vue'), meta: { admin: true } },
    { path: '/reports', component: () => import('@/views/admin/ReportsView.vue'), meta: { admin: true } },
    { path: '/detail/:module/:id', component: () => import('@/views/admin/DetailView.vue'), meta: { admin: true } },
    { path: '/users', component: () => import('@/views/admin/UsersView.vue'), meta: { admin: true } },
    { path: '/profile', component: () => import('@/views/admin/AdminProfileView.vue'), meta: { admin: true } },
    // 填报端（App 风格，员工 + 主管共用；主管在 Tabbar 多一个「审核」入口）
    { path: '/fill/home', component: () => import('@/views/fill/FillHomeView.vue'), meta: { fill: true } },
    { path: '/fill/print', component: () => import('@/views/fill/FillPrintView.vue'), meta: { fill: true } },
    { path: '/fill/lamination', component: () => import('@/views/fill/FillLaminationView.vue'), meta: { fill: true } },
    { path: '/fill/bagmaking', component: () => import('@/views/fill/FillBagmakingView.vue'), meta: { fill: true } },
    { path: '/fill/records', component: () => import('@/views/fill/FillRecordsView.vue'), meta: { fill: true } },
    { path: '/fill/me', component: () => import('@/views/fill/FillMeView.vue'), meta: { fill: true } },
    { path: '/fill/profile', component: () => import('@/views/fill/FillProfileView.vue'), meta: { fill: true } },
    { path: '/fill/password', component: () => import('@/views/fill/FillPasswordView.vue'), meta: { fill: true } },
    { path: '/fill/success', component: () => import('@/views/fill/FillSuccessView.vue'), meta: { fill: true } },
    { path: '/fill/revise/:module/:id', component: () => import('@/views/fill/FillReviseView.vue'), meta: { fill: true } },
    { path: '/fill/review', component: () => import('@/views/fill/FillReviewView.vue'), meta: { fill: true, lead: true } },
    // 移动员工端（<768px 自动分流）
    { path: '/m/home', component: () => import('@/views/mobile/MHomeView.vue') },
    { path: '/m/review', component: () => import('@/views/mobile/MReviewView.vue') },
    { path: '/m/print', component: () => import('@/views/mobile/MPrintView.vue') },
    { path: '/m/lamination', component: () => import('@/views/mobile/MLaminationView.vue') },
    { path: '/m/bagmaking', component: () => import('@/views/mobile/MBagmakingView.vue') },
    { path: '/m/success', component: () => import('@/views/mobile/MSuccessView.vue') },
    { path: '/m/records', component: () => import('@/views/mobile/MRecordsView.vue') },
    { path: '/m/revise/:module/:id', component: () => import('@/views/mobile/MReviseView.vue') },
    { path: '/m/me', component: () => import('@/views/mobile/MMeView.vue') },
    { path: '/m/profile', component: () => import('@/views/mobile/MProfileView.vue') },
    { path: '/', redirect: () => {
        // 填报端角色（员工/主管）一律进 Vant 填报端；管理角色进管理端；未登录去登录页
        const store = useUserStore()
        if (!store.isLoggedIn) return '/login'
        const role = store.user?.role
        return (role === 'WORKER' || role === 'DEPT_LEAD') ? '/fill/home' : '/dashboard'
      } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.meta.public) {
    // 已登录访问登录页 → 按角色分流（与根路径一致）
    if (store.isLoggedIn) {
      const role = store.user?.role
      return (role === 'WORKER' || role === 'DEPT_LEAD') ? '/fill/home' : '/dashboard'
    }
    return true
  }
  if (!store.isLoggedIn) return '/login'
  if (!store.user) {
    try { await store.fetchMe() } catch { return '/login' }
  }
  const role = store.user?.role
  const isWorker = role === 'WORKER'
  const isFillSide = isWorker || role === 'DEPT_LEAD'
  // 管理端仅 ADMIN / MANAGEMENT 可进；员工与主管 → 填报端
  if (to.meta.admin && isFillSide) return '/fill/home'
  // 填报端仅员工 / 主管可进；管理员与管理层 → 管理端（双门户严格分流）
  if (to.meta.fill && !isFillSide) return '/dashboard'
  // 审核页仅主管（员工点审核入口时拦回首页）
  if (to.meta.lead && !store.isLead) return '/fill/home'
  return true
})

export default router
