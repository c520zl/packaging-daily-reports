/**
 * 演示模式 mock（仅后端不可用时兜底）
 * 数据形状与 API.md 完全一致；连上真实后端后本文件不再被触发。
 */
interface MockUser {
  id: number; username: string; realName: string; role: string; dept: string | null
  gender: string; phone: string; email: string; address: string; enabled: boolean
  password: string; createdAt: string
}

import { todayStr } from '@/constants'

const USERS: MockUser[] = [
  { id: 1, username: 'admin', realName: '系统管理员', role: 'ADMIN', dept: null, gender: 'M', phone: '13800000001', email: 'admin@example.com', address: '厂区办公楼 2F', enabled: true, password: '123456', createdAt: '2026-01-05' },
  { id: 2, username: 'boss', realName: '厂长', role: 'MANAGEMENT', dept: null, gender: 'M', phone: '13800000008', email: 'boss@example.com', address: '厂区办公楼 3F', enabled: true, password: '123456', createdAt: '2026-01-05' },
  { id: 3, username: 'yinshua-01', realName: '陈建国', role: 'DEPT_LEAD', dept: 'PRINT', gender: 'M', phone: '13800000002', email: 'chenjg@example.com', address: '印刷车间', enabled: true, password: '123456', createdAt: '2026-01-08' },
  { id: 4, username: 'yinshua-02', realName: '李明', role: 'WORKER', dept: 'PRINT', gender: 'M', phone: '13800000003', email: 'liming@example.com', address: '印刷车间', enabled: true, password: '123456', createdAt: '2026-01-08' },
  { id: 5, username: 'fuhe-01', realName: '王建军', role: 'DEPT_LEAD', dept: 'LAMINATION', gender: 'M', phone: '13800000004', email: 'wangjj@example.com', address: '复合车间', enabled: true, password: '123456', createdAt: '2026-01-09' },
  { id: 6, username: 'zhidai-01', realName: '赵德海', role: 'DEPT_LEAD', dept: 'BAGMAKING', gender: 'M', phone: '13800000005', email: 'zhaodh@example.com', address: '制袋车间', enabled: true, password: '123456', createdAt: '2026-01-10' },
  { id: 7, username: 'zhidai-02', realName: '马建军', role: 'WORKER', dept: 'BAGMAKING', gender: 'M', phone: '13800000006', email: 'majj@example.com', address: '制袋车间', enabled: true, password: '123456', createdAt: '2026-02-14' },
  { id: 8, username: 'zhidai-03', realName: '孙晓燕', role: 'WORKER', dept: 'BAGMAKING', gender: 'F', phone: '13800000007', email: 'sunxy@example.com', address: '制袋车间', enabled: false, password: '123456', createdAt: '2026-03-02' }
]

const today = todayStr()

const PRINT_ROWS = [
  { id: 1, date: today, orderId: 'YS-20261123-01', onboardTime: '08:05', speed: 175, material: 'PET/PE', thickness: 12, spec: '350×220', requiredMeter: 3000, actualMeter: 2980, colors: ['黑', '红', '蓝', '黄'], printMinutes: 17.0, signer: '陈建国', remark: '白膜套印正常', status: 'APPROVED', rejectReason: null, createdBy: '陈建国', createdAt: `${today}T08:10:00` },
  { id: 2, date: today, orderId: 'YS-20261123-02', onboardTime: '09:40', speed: 160, material: 'NY/PE', thickness: 15, spec: '420×300', requiredMeter: 4500, actualMeter: 4420, colors: ['黑', '白', '红'], printMinutes: 27.6, signer: '陈建国', remark: '版辊 2 号换色多', status: 'APPROVED', rejectReason: null, createdBy: '陈建国', createdAt: `${today}T09:45:00` },
  { id: 3, date: today, orderId: 'YS-20261123-03', onboardTime: '10:42', speed: 180, material: 'PET/CPP', thickness: 12, spec: '280×180', requiredMeter: 2200, actualMeter: 2150, colors: ['四色+白'], printMinutes: 12.0, signer: '李明', remark: '', status: 'PENDING', rejectReason: null, createdBy: '李明', createdAt: `${today}T10:42:00` },
  { id: 4, date: today, orderId: 'YS-20261123-04', onboardTime: '13:20', speed: 165, material: 'OPP/CPP', thickness: 20, spec: '500×320', requiredMeter: 5200, actualMeter: 5100, colors: ['六色+白'], printMinutes: 30.9, signer: '李明', remark: '下午套色微调 2 次', status: 'PENDING', rejectReason: null, createdBy: '李明', createdAt: `${today}T13:58:00` },
  { id: 5, date: today, orderId: 'YS-20261122-06', onboardTime: '08:30', speed: 150, material: 'NY/PE', thickness: 15, spec: '420×300', requiredMeter: 4500, actualMeter: 4420, colors: ['黑', '白'], printMinutes: 27.6, signer: '李明', remark: '', status: 'REJECTED', rejectReason: '实际印刷米数漏填，请补填后重新提交', createdBy: '李明', createdAt: '2026-11-22T08:30:00' }
]

const LAM_ROWS = [
  { id: 1, date: today, orderId: 'FH-20261123-01', times: 2, speed: 130, material: 'PET/AL/PE', spec: '360×240', requiredMeter: 3600, actualMeter: 3560, rolls: 12, printRemain: 240, machineType: 'SOLVENT_FREE', adjustMeter: 120, signer: '王建军', status: 'APPROVED', rejectReason: null, createdBy: '王建军', createdAt: `${today}T09:00:00` },
  { id: 2, date: today, orderId: 'FH-20261123-02', times: 1, speed: 95, material: 'PET/VMPET/CPP', spec: '420×300', requiredMeter: 4400, actualMeter: 4350, rolls: 15, printRemain: 0, machineType: 'DRY', adjustMeter: 210, signer: '王建军', status: 'APPROVED', rejectReason: null, createdBy: '王建军', createdAt: `${today}T10:15:00` },
  { id: 3, date: today, orderId: 'FH-20261123-03', times: 3, speed: 120, material: 'PET/PE', spec: '350×220', requiredMeter: 2900, actualMeter: 2860, rolls: 10, printRemain: 40, machineType: 'SOLVENT_FREE', adjustMeter: 90, signer: '王建军', status: 'PENDING', rejectReason: null, createdBy: '王建军', createdAt: `${today}T11:20:00` }
]

const BAG_ROWS = [
  { id: 1, date: today, shift: 'DAY', machine: '3号机', picker: '孙晓燕', leader: '赵德海', speed: 110, orderId: 'DB-20261123-01', bagType: '三边封袋', spec: '150×230', incomingMeter: 1850, adjustMeter: 45, endMeter: 1805, completedQty: 6200, wasteKg: 3.2, remainKg: 18.0, workStart: '08:00', workEnd: '16:00', abnormalReason: '', defectQty: 35, signer: '赵德海', status: 'APPROVED', rejectReason: null, createdBy: '赵德海', createdAt: `${today}T09:58:00` },
  { id: 2, date: today, shift: 'DAY', machine: '5号机', picker: '周丽娟', leader: '郑国强', speed: 85, orderId: 'DB-20261123-02', bagType: '八边封袋', spec: '180×260', incomingMeter: 2400, adjustMeter: 120, endMeter: 2280, completedQty: 4800, wasteKg: 6.8, remainKg: 25.5, workStart: '08:30', workEnd: '16:30', abnormalReason: '封口温度偏高，调机 40 分钟', defectQty: 72, signer: '郑国强', status: 'PENDING', rejectReason: null, createdBy: '郑国强', createdAt: `${today}T10:40:00` }
]

let printSeq = 100, lamSeq = 100, bagSeq = 100
const DB = { PRINT: PRINT_ROWS, LAMINATION: LAM_ROWS, BAG: BAG_ROWS }

function ok(data: any) {
  return Promise.resolve({ data, config: {}, headers: {}, status: 200, statusText: 'OK' })
}

function pageOf(rows: any[], params: any) {
  const page = Number(params?.page ?? 1), size = Number(params?.size ?? 20)
  return { total: rows.length, records: rows.slice((page - 1) * size, page * size) }
}

function toRecordRow(module: string, x: any) {
  const isBag = module === 'BAGMAKING'
  return {
    id: x.id, module, dept: module, date: x.date, orderId: x.orderId,
    machine: isBag ? x.machine : module === 'LAMINATION' ? (x.machineType === 'SOLVENT_FREE' ? '无溶剂机' : '干式复合机') : '印刷机',
    materialOrBagType: isBag ? x.bagType : x.material,
    spec: x.spec,
    required: isBag ? x.incomingMeter : x.requiredMeter,
    actual: isBag ? x.completedQty : x.actualMeter,
    rolls: isBag ? null : x.rolls,
    wasteKg: isBag ? x.wasteKg : null,
    shift: isBag ? x.shift : undefined,
    signer: x.signer, status: x.status, createdAt: x.createdAt, createdBy: x.createdBy
  }
}

/** 处理演示请求；返回 axios response 形状以复用拦截器 */
export async function mockRequest(config: any): Promise<any> {
  const url: string = (config.url || '').split('?')[0]
  const params: any = config.params || {}
  const body = typeof config.data === 'string' ? JSON.parse(config.data || '{}') : (config.data || {})
  const method = (config.method || 'get').toLowerCase()

  // 认证
  if (url === '/auth/login') {
    const u = USERS.find(x => x.username === body.username && x.password === body.password)
    if (!u) throw new Error('用户名或密码错误')
    if (!u.enabled) throw new Error('账号已停用，请联系管理员')
    return ok({ token: 'mock-token-' + u.id, expiresIn: 86400, user: userVO(u) })
  }
  if (url === '/auth/me') {
    const u = USERS.find(x => 'mock-token-' + x.id === sessionStorage.getItem('dsh_token'))
    return u ? ok(userVO(u)) : Promise.reject(new Error('401'))
  }
  if (url === '/dashboard/pending-count') {
    const u = current()
    if (u?.role === 'WORKER') return ok(rowsOf(u).filter(r => r.status === 'REJECTED').length)
    if (u?.role === 'DEPT_LEAD') return ok(allRows().filter(r => deptUsers(u.dept!).includes(r.createdBy) && r.status === 'PENDING').length)
    return ok(allRows().filter(r => r.status === 'PENDING').length)
  }
  if (url === '/dashboard/today') {
    const approved = allRows().filter(r => r.status === 'APPROVED')
    const printMeter = PRINT_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.actualMeter || 0), 0)
    const lamMeter = LAM_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.actualMeter || 0), 0)
    const bagQty = BAG_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.completedQty || 0), 0)
    const waste = BAG_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.wasteKg || 0), 0)
    return ok({ submittedCount: allRows().length, pendingCount: allRows().filter(r => r.status === 'PENDING').length, printMeter, laminationMeter: lamMeter, bagQty, wasteKg: Math.round(waste * 10) / 10 })
  }
  if (url === '/dashboard/weekly') {
    const days: any[] = []
    for (let i = 6; i >= 0; i--) {
      const d = localDateStr(new Date(Date.now() - i * 86400000))
      const factor = [0.72, 0.78, 0.67, 0.42, 0.83, 0.75, 1][6 - i]
      days.push({ date: d, printMeter: Math.round(18640 * factor), laminationMeter: Math.round(10770 * factor), bagQty: Math.round(86400 * factor) })
    }
    return ok(days)
  }

  // 列表 / 详情
  if (url === '/print-daily' && method === 'get') {
    let rows = [...PRINT_ROWS]
    if (params.status) rows = rows.filter(r => r.status === params.status)
    if (params.date) rows = rows.filter(r => r.date === params.date)
    return ok(pageOf(rows, params))
  }
  if (url === '/lamination-daily' && method === 'get') {
    let rows = [...LAM_ROWS]
    if (params.status) rows = rows.filter(r => r.status === params.status)
    return ok(pageOf(rows, params))
  }
  if (url === '/bag-daily' && method === 'get') {
    let rows = [...BAG_ROWS]
    if (params.status) rows = rows.filter(r => r.status === params.status)
    if (params.shift) rows = rows.filter(r => r.shift === params.shift)
    return ok(pageOf(rows, params))
  }
  if (url === '/records') {
    let rows = [...PRINT_ROWS.map(x => toRecordRow('PRINT', x)),
                ...LAM_ROWS.map(x => toRecordRow('LAMINATION', x)),
                ...BAG_ROWS.map(x => toRecordRow('BAGMAKING', x))]
    if (params.dept) rows = rows.filter(r => r.dept === params.dept)
    if (params.status) rows = rows.filter(r => r.status === params.status)
    if (params.orderId) rows = rows.filter(r => r.orderId.includes(params.orderId))
    rows.sort((a, b) => (b.date + (b.createdAt || '')).localeCompare(a.date + (a.createdAt || '')))
    return ok(pageOf(rows, params))
  }
  if (url === '/reports/summary') {
    const printMeter = PRINT_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.actualMeter || 0), 0)
    const lamMeter = LAM_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.actualMeter || 0), 0)
    const bagQty = BAG_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.completedQty || 0), 0)
    const waste = BAG_ROWS.filter(x => x.status === 'APPROVED').reduce((s, x) => s + (x.wasteKg || 0), 0)
    return ok({ printMeter, laminationMeter: lamMeter, bagQty, wasteKg: Math.round(waste * 10) / 10 })
  }

  // 新增 / 修改 / 状态流转
  if (url === '/print-daily' && method === 'post') {
    const row: any = { id: ++printSeq, date: body.date, orderId: body.orderId, onboardTime: body.onboardTime, speed: body.speed, material: body.material, thickness: body.thickness, spec: body.spec, requiredMeter: body.requiredMeter, actualMeter: body.actualMeter, colors: body.colors, printMinutes: body.printMinutes, signer: body.signer, remark: body.remark, status: body.submit ? 'PENDING' : 'DRAFT', rejectReason: null, createdBy: current()?.realName, createdAt: new Date().toISOString() }
    PRINT_ROWS.unshift(row)
    return ok(row)
  }
  if (url === '/lamination-daily' && method === 'post') {
    const row: any = { id: ++lamSeq, date: body.date, ...body, status: body.submit ? 'PENDING' : 'DRAFT', rejectReason: null, createdBy: current()?.realName, createdAt: new Date().toISOString() }
    LAM_ROWS.unshift(row)
    return ok(row)
  }
  if (url === '/bag-daily' && method === 'post') {
    const row: any = { id: ++bagSeq, date: body.date, ...body, status: body.submit ? 'PENDING' : 'DRAFT', rejectReason: null, createdBy: current()?.realName, createdAt: new Date().toISOString() }
    BAG_ROWS.unshift(row)
    return ok(row)
  }
  const m = url.match(/^\/(print-daily|lamination-daily|bag-daily)\/(\d+)$/)
  if (m && method === 'put') {
    const db = m[1] === 'print-daily' ? PRINT_ROWS : m[1] === 'lamination-daily' ? LAM_ROWS : BAG_ROWS
    const row = db.find(x => x.id === Number(m[2]))
    if (row) Object.assign(row, body, { status: row.status === 'REJECTED' ? 'DRAFT' : row.status })
    return ok(null)
  }
  if (m && method === 'delete') {
    const db = m[1] === 'print-daily' ? PRINT_ROWS : m[1] === 'lamination-daily' ? LAM_ROWS : BAG_ROWS
    const i = db.findIndex(x => x.id === Number(m[2]))
    if (i >= 0) db.splice(i, 1)
    return ok(null)
  }
  const act = url.match(/^\/(print-daily|lamination-daily|bag-daily)\/(\d+)\/(submit|withdraw|approve|reject)$/)
  if (act) {
    const db = act[1] === 'print-daily' ? PRINT_ROWS : act[1] === 'lamination-daily' ? LAM_ROWS : BAG_ROWS
    const row: any = db.find(x => x.id === Number(act[2]))
    if (row) {
      if (act[3] === 'submit') row.status = 'PENDING'
      if (act[3] === 'withdraw') row.status = 'DRAFT'
      if (act[3] === 'approve') row.status = 'APPROVED'
      if (act[3] === 'reject') { row.status = 'REJECTED'; row.rejectReason = body.reason }
    }
    return ok(null)
  }

  // 用户管理
  if (url === '/users' && method === 'get') return ok(pageOf(USERS.map(userVO), params))
  if (url === '/users' && method === 'post') {
    USERS.push({ id: USERS.length + 1, ...body, password: body.password, createdAt: today } as MockUser)
    return ok(null)
  }
  const um = url.match(/^\/users\/(\d+)\/status$/)
  if (um) {
    const u = USERS.find(x => x.id === Number(um[1]))
    if (u) u.enabled = body.enabled
    return ok(null)
  }
  if (url === '/notices/unread-count') return ok(0)
  if (url === '/me/profile' && method === 'put') {
    const u = current()
    if (u) Object.assign(u, body)
    return ok(null)
  }

  // 兜底：空成功
  return ok(null)
}

function userVO(u: MockUser) {
  return { id: u.id, username: u.username, realName: u.realName, gender: u.gender, phone: u.phone, email: u.email, address: u.address, role: u.role, dept: u.dept, avatarUrl: null, enabled: u.enabled, createdAt: u.createdAt }
}

function current(): MockUser | null {
  const token = sessionStorage.getItem('dsh_token') || ''
  return USERS.find(x => 'mock-token-' + x.id === token) ?? null
}

function deptUsers(dept: string) {
  return USERS.filter(u => u.dept === dept).map(u => u.realName)
}

function rowsOf(u: MockUser) {
  return allRows().filter(r => r.createdBy === u.realName)
}

function allRows(): any[] {
  return [...PRINT_ROWS, ...LAM_ROWS, ...BAG_ROWS]
}

function localDateStr(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

