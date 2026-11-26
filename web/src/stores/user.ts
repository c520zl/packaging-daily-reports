import { defineStore } from 'pinia'
import { api } from '@/api/http'

interface UserInfo {
  id: number
  username: string
  realName: string
  gender?: string
  phone?: string
  email?: string
  address?: string
  role: string
  dept?: string | null
  avatarUrl?: string | null
}

// 登录态存 sessionStorage：无头浏览器/新标签各自独立，避免串号（API.md §2）
const saved = sessionStorage.getItem('dsh_user')
const savedToken = sessionStorage.getItem('dsh_token') || ''

/** 登录态与当前用户（API.md §2） */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: savedToken,
    user: (saved ? JSON.parse(saved) : null) as UserInfo | null
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isWorker: (s) => s.user?.role === 'WORKER',
    isLead: (s) => s.user?.role === 'DEPT_LEAD',
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isManagement: (s) => s.user?.role === 'MANAGEMENT',
    /** 主管待审数 / 员工被退回数（角标） */
    pendingCount: (s) => (s.user ? (s.user as any)._pending ?? 0 : 0)
  },
  actions: {
    async login(username: string, password: string) {
      const data: any = await api.post('/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      sessionStorage.setItem('dsh_token', data.token)
      sessionStorage.setItem('dsh_user', JSON.stringify(data.user))
    },
    async fetchMe() {
      const data: any = await api.get('/auth/me')
      this.user = data
      sessionStorage.setItem('dsh_user', JSON.stringify(data))
    },
    async refreshPending() {
      const data: any = await api.get('/dashboard/pending-count')
      if (this.user) {
        (this.user as any)._pending = data
        sessionStorage.setItem('dsh_user', JSON.stringify(this.user))
      }
    },
    logout() {
      this.token = ''
      this.user = null
      sessionStorage.removeItem('dsh_token')
      sessionStorage.removeItem('dsh_user')
    }
  }
})
