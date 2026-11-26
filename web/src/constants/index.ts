/**
 * 枚举与常量（API.md §1.1 的唯一前端映射处，值必须与服务端一致）
 */
export const STATUS = {
  DRAFT: { label: '草稿', cls: 'tag-gray' },
  PENDING: { label: '待审核', cls: 'tag-orange' },
  APPROVED: { label: '已审核', cls: 'tag-green' },
  REJECTED: { label: '已退回', cls: 'tag-red' }
} as const

export const ROLE = {
  ADMIN: { label: '系统管理员' },
  DEPT_LEAD: { label: '部门主管' },
  WORKER: { label: '普通员工' },
  MANAGEMENT: { label: '厂部管理层' }
} as const

export const DEPT = {
  PRINT: { label: '印刷部' },
  LAMINATION: { label: '复合部' },
  BAGMAKING: { label: '制袋部' }
} as const

export const GENDER = { M: '男', F: '女' } as const

export const MACHINE_TYPE = { DRY: '干式', SOLVENT_FREE: '无溶剂' } as const

export const SHIFT = { DAY: '白班', NIGHT: '夜班' } as const

export const BAG_TYPES = [
  '连体拉链直立袋', '三边封袋', '三边封拉链袋', '三边封自粘袋',
  '八边封袋', '中封袋', '中封等边风琴袋', '中封侧边风琴袋'
] as const

export const MACHINES = ['1号机', '2号机', '3号机', '4号机', '5号机', '6号机', '7号机', '8号机'] as const

/** 是否移动端（<768px 用移动员工端界面，API.md §11） */
export function isMobile(): boolean {
  return window.innerWidth < 768
}

/**
 * 本地时区的「今天」字符串（YYYY-MM-DD）。
 * 禁止用 new Date().toISOString()：那是 UTC，东八区 0-8 点会得到昨天，
 * 夜班/早班填报时日报归档日期错误。
 */
export function todayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
