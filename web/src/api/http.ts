import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * axios 实例（API.md §11 前端对接约束）
 * - baseURL /api，Vite 代理到 8080
 * - 请求带 Bearer token（仅 sessionStorage，避免残留旧 token 覆盖新登录态）
 * - 响应 code!==0 时 ElMessage 报错；401 清内存态+存储后跳登录
 * - 演示模式：后端未启动时（网络错误）自动降级到内置模拟数据，
 *   界面可完整走通；连上真实后端后行为不变。
 */
export const api = axios.create({ baseURL: '/api', timeout: 4000 }) as any

api.interceptors.request.use((config: any) => {
  const token = sessionStorage.getItem('dsh_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function fail(resp: any, message: string) {
  const body = resp?.data
  const code = body?.code
  if (code === 401) {
    // 同时清存储与 Pinia 内存态：只清存储会让守卫里 store.isLoggedIn 仍为 true，
    // 进入 /login 时被弹回门户页 → 门户页请求又 401 → 登录页死循环
    localStorage.removeItem('token')
    sessionStorage.removeItem('dsh_token')
    sessionStorage.removeItem('dsh_user')
    import('@/stores/user').then(({ useUserStore }) => useUserStore().logout())
    router.push('/login')
  }
  ElMessage.error(message || '操作失败')
  return Promise.reject(new Error(message))
}

api.interceptors.response.use(
  (resp: any) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      return fail(resp, body.message)
    }
    return body
  },
  async (err: any) => {
    // 网络层失败（后端没起）：演示模式兜底
    if (!err.response) {
      const { mockRequest } = await import('./mock')
      return mockRequest(err.config)
    }
    // 403 无权限走统一失败通道：静默返回 null 会让调用方把无权限当成成功
    return fail(err.response, err.response.data?.message)
  }
)
