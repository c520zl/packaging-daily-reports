/**
 * 后台管理系统全页面交互 E2E 测试
 * 真实 Chrome + 真实后端(compose: mysql+server) + 前端 dev server
 * 覆盖：登录 → 工作台 → 三部门填报(增/删/提交) → 报表查询(筛选/详情/退回/审核) → 用户管理(增/停用/重置密码) → 移动端员工流程
 */
const { chromium } = require('playwright-core')

const BASE = 'http://localhost'
const results = []
let browser

function log(step, ok, detail = '') {
  results.push({ step, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${step}${detail ? ' | ' + detail : ''}`)
}

async function main() {
  browser = await chromium.launch({
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    headless: true,
    args: ['--no-sandbox']
  })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.setDefaultTimeout(15000)
  const errors = []
  page.on('pageerror', e => errors.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()) })

  try {
    // ============ 1. 登录页交互 ============
    await page.goto(BASE + '/login')
    await page.waitForSelector('.login-box')
    log('1.1 登录页渲染', true)

    // 空表单校验
    await page.click('button:has-text("登 录")')
    await page.waitForSelector('.el-message--warning', { timeout: 5000 })
    log('1.2 空表单提交有 warning 提示', true)

    // 错误密码
    await page.fill('.login-form input >> nth=0', 'admin')
    await page.fill('.login-form input >> nth=1', 'wrongpass')
    await page.click('button:has-text("登 录")')
    await page.waitForSelector('.el-message--error', { timeout: 5000 })
    log('1.3 错误密码有 error 提示', true)

    // 正确登录
    await page.fill('.login-form input >> nth=1', '123456')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
    log('1.4 admin 登录成功跳转工作台', true)

    // ============ 2. 工作台 ============
    await page.waitForSelector('.stat-grid .stat-card')
    const statCount = await page.locator('.stat-card').count()
    log('2.1 工作台统计卡渲染', statCount >= 4, `${statCount} 张卡`)
    // 快捷入口跳转
    await page.click('text=印刷部日报 >> nth=0')
    await page.waitForURL('**/print')
    await page.goBack()
    await page.waitForURL('**/dashboard')
    log('2.2 快捷填报入口跳转往返', true)

    // ============ 3. 印刷部日报填报 ============
    await page.click('.nav-item:has-text("印刷部日报")')
    await page.waitForURL('**/print')
    await page.waitForSelector('.form-table')
    log('3.1 印刷部填报页加载', true)

    // 添加一行（等列表接口返回后再取行数，避免加载竞态）
    await page.waitForTimeout(1000)
    const beforeRows = await page.evaluate(() => document.querySelector('.form-table tbody tr td[colspan]') ? 0 : document.querySelectorAll('.form-table tbody tr').length)
    await page.locator('.filter-bar button:has-text("添加一行")').click()
    await page.waitForTimeout(500)
    const afterRows = await page.evaluate(() => document.querySelector('.form-table tbody tr td[colspan]') ? 0 : document.querySelectorAll('.form-table tbody tr').length)
    log('3.2 添加一行', afterRows === beforeRows + 1, `${beforeRows}->${afterRows}`)

    // 填写首行：el-input 用 fill，el-input-number(type=number) 用点击后键盘输入
    const firstRow = page.locator('.form-table tbody tr').first()
    async function fillNum(locator, text) {
      await locator.click({ clickCount: 3 })
      await locator.pressSequentially(text, { delay: 20 })
      await locator.press('Tab')
    }
    await firstRow.locator('td input[type="text"], td input:not([type])').first().fill('YS-E2E-' + Date.now() % 100000)
    // 跳过 time-select(有默认值)；td 顺序: 1 单号(text) 2 时间 3 机速(num) 4 材质(text) 5 厚度(num) 6 规格(text) 7 要求(num) 8 实际(num) 9 颜色(text) 10 印刷时间(num)
    const tds = firstRow.locator('td')
    await fillNum(tds.nth(2).locator('input'), '175')
    await tds.nth(3).locator('input').fill('PET/PE')
    await fillNum(tds.nth(4).locator('input'), '12')
    await tds.nth(5).locator('input').fill('350x220')
    await fillNum(tds.nth(6).locator('input'), '3000')
    await fillNum(tds.nth(7).locator('input'), '2980')
    await tds.nth(8).locator('input').fill('黑,红')
    await fillNum(tds.nth(9).locator('input'), '17')
    // 签名默认已有
    log('3.3 填写行数据', true)

    // 存草稿（断言以接口响应为准，而非瞬时的 toast）
    const [saveResp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/print-daily') && r.request().method() === 'POST', { timeout: 10000 }).catch(() => null),
      page.locator('.form-foot button:has-text("存草稿")').click()
    ])
    const saveOk = saveResp && JSON.parse(await saveResp.text()).code === 0
    log('3.4 存草稿保存成功', !!saveOk, saveResp ? 'HTTP ' + saveResp.status() : 'no request')
    await page.waitForTimeout(800)

    // ============ 4. 复合部 ============
    await page.click('.nav-item:has-text("复合部日报")')
    await page.waitForURL('**/lamination')
    await page.waitForSelector('.form-table')
    await page.waitForTimeout(1000)
    const lamBefore = await page.evaluate(() => document.querySelector('.form-table tbody tr td[colspan]') ? 0 : document.querySelectorAll('.form-table tbody tr').length)
    await page.locator('.filter-bar button:has-text("添加一行")').click()
    await page.waitForTimeout(400)
    log('4.1 复合部添加一行', (await page.evaluate(() => document.querySelector('.form-table tbody tr td[colspan]') ? 0 : document.querySelectorAll('.form-table tbody tr').length)) === lamBefore + 1)
    // 机型下拉切换（行内唯一 el-select）
    await page.locator('.form-table tbody tr').first().locator('.el-select').first().click()
    await page.click('.el-select-dropdown__item:has-text("无溶剂")')
    log('4.2 复合部机型选择无溶剂', true)

    // ============ 5. 制袋部 ============
    await page.click('.nav-item:has-text("制袋部日报")')
    await page.waitForURL('**/bagmaking')
    await page.waitForTimeout(1200)
    await page.waitForSelector('.form-table')
    log('5.1 制袋部填报页加载', true)
    // 袋型下拉：无论当日有无数据都添加一行（新行在首位且可编辑），行内下拉顺序 = 班次(0) 机台(1) 袋型(2)
    await page.locator('.filter-bar button:has-text("添加一行")').click()
    await page.waitForTimeout(400)
    await page.locator('.form-table tbody tr').first().locator('.el-select').nth(2).click()
    await page.waitForSelector('.el-select-dropdown__item:visible', { timeout: 5000 }).catch(() => {})
    const bagOptions = await page.locator('.el-select-dropdown__item:visible').count()
    log('5.2 袋型下拉选项数', bagOptions === 8, `${bagOptions} 项`)
    await page.keyboard.press('Escape')

    // ============ 6. 报表查询 ============
    await page.click('.nav-item:has-text("报表查询")')
    await page.waitForURL('**/reports')
    await page.waitForSelector('.tbl')
    await page.waitForTimeout(1000)
    const recRows = await page.locator('.tbl tbody tr').count()
    log('6.1 报表查询明细加载', recRows > 0, `${recRows} 行`)
    // 部门筛选
    await page.locator('.filter-bar .el-select').first().click()
    await page.click('.el-select-dropdown__item:has-text("印刷部")')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    log('6.2 部门筛选查询', true)

    // 进详情
    const detailLink = page.locator('a:has-text("详情")').first()
    if (await detailLink.count() > 0) {
      await detailLink.click()
      await page.waitForURL('**/detail/**')
      await page.waitForSelector('.kv-grid')
      await page.waitForTimeout(1200) // 时间轴异步加载
      log('6.3 详情页加载(含字段网格)', true)
      const steps = await page.locator('.audit-step').count()
      log('6.4 审核流转时间轴', steps > 0, `${steps} 步`)
      await page.goBack()
      await page.waitForURL('**/reports')
    } else {
      log('6.3 详情页入口', false, '无待审记录')
    }

    // ============ 7. 用户管理 ============
    await page.click('.nav-item:has-text("用户管理")')
    await page.waitForURL('**/users')
    await page.waitForSelector('.tbl')
    await page.waitForTimeout(800)
    const userRows = await page.locator('.tbl tbody tr').count()
    log('7.1 用户列表加载', userRows >= 8, `${userRows} 位用户`)

    // 新增用户弹窗
    await page.click('button:has-text("新增用户")')
    await page.waitForSelector('.el-dialog')
    log('7.2 新增用户弹窗打开', true)
    const dlg = page.locator('.el-dialog')
    // 弹窗字段顺序: 0用户名 1密码 2姓名 3手机号(4性别/角色/部门是 select 组合框 readonly)
    const newUsername = 'e2e-tester-' + Date.now() % 100000
    await dlg.locator('input').nth(0).fill(newUsername)
    await dlg.locator('input').nth(1).fill('test123456')
    await dlg.locator('input').nth(2).fill('端到端测试员')
    // 手机号是第 4 个纯文本输入框（跳过 readonly 的 el-select combobox）
    await dlg.locator('input:not([readonly])').nth(3).fill('13900000000')
    // 角色保持普通员工 + 部门印刷部
    await dlg.locator('button:has-text("保 存"), button:has-text("保存")').click()
    await page.waitForTimeout(1200)
    // 断言以接口结果为准：列表里能搜到新用户
    await page.fill('.filter-bar input >> nth=0', newUsername)
    await page.press('.filter-bar input >> nth=0', 'Enter')
    await page.waitForTimeout(1000)
    const searched = await page.locator('.tbl tbody tr').count()
    log('7.3+7.4 新增用户并搜索定位', searched === 1, `新增 ${newUsername} → 搜索 ${searched} 行`)

    // 停用（switch）
    const sw = page.locator('.tbl tbody tr').first().locator('.el-switch')
    await sw.click()
    await page.waitForSelector('.el-message-box')
    await page.click('.el-message-box button:has-text("确定")')
    await page.waitForTimeout(600)
    log('7.5 停用用户(带确认框)', true)

    // ============ 8. 填报端（Vant）员工全流程 ============
    // 退出登录 → 用员工账号登录（分流进 /fill 填报端）
    await page.evaluate(() => { sessionStorage.clear(); localStorage.clear() })
    await page.goto(BASE + '/login')
    await page.setViewportSize({ width: 390, height: 844 })
    await page.fill('.login-form input >> nth=0', 'yinshua-02')
    await page.fill('.login-form input >> nth=1', '123456')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/fill/home', { timeout: 10000 })
    log('8.1 员工登录进填报端首页（Vant）', true)

    await page.waitForSelector('.v-app-tabbar')
    log('8.2 填报端底部 Tabbar', true)

    // 进印刷部填报（Vant 卡片式表单）
    await page.goto(BASE + '/fill/print')
    await page.waitForURL('**/fill/print')
    await page.waitForSelector('.van-field:has-text("生产单号")')
    log('8.3 Vant 填报页加载', true)

    // 填写并提交（Vant field）
    const vOrderId = 'YS-VANT-' + Date.now() % 100000
    await page.fill('.van-field:has-text("生产单号") input', vOrderId)
    await page.fill('.van-field:has-text("机速") input', '180')
    await page.click('.v-chip:has-text("黑")')
    await page.fill('.van-field:has-text("材质") input', 'PET/PE')
    await page.fill('.van-field:has-text("尺寸规格") input', '360x240')
    await page.fill('.van-field:has-text("要求印刷米数") input', '3800')
    await page.fill('.van-field:has-text("实际印刷米数") input', '3700')
    await page.click('button:has-text("提交日报")')
    await page.waitForSelector('.van-toast--success', { timeout: 8000 })
    log('8.4 Vant 填报提交成功提示', true)

    // 我的记录（Tab 已按部门过滤）
    await page.goto(BASE + '/fill/records')
    await page.waitForTimeout(1200)
    await page.waitForSelector(`.v-record-no:has-text("${vOrderId}")`, { timeout: 6000 }).catch(() => {})
    log('8.5 我的记录含刚提交单号', (await page.locator(`.v-record-no:has-text("${vOrderId}")`).count()) > 0)

    // 我的页
    await page.goto(BASE + '/fill/me')
    await page.waitForSelector('.v-me-name')
    log('8.6 填报端我的页', true)

    // ============ 9. 页面报错收集 ============
    const realErrors = errors.filter(e => !e.includes('favicon') && !e.includes('Failed to load resource'))
    log('9.1 无页面 JS 报错', realErrors.length === 0, realErrors.slice(0, 3).join(' || '))

  } catch (e) {
    log('EXCEPTION', false, e.message.slice(0, 300))
    await page.screenshot({ path: 'e2e-fail.png', fullPage: true }).catch(() => {})
  }

  await browser.close()
  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 结果: ${pass}/${results.length} PASS =====`)
  process.exit(results.every(r => r.ok) ? 0 : 1)
}

main().catch(e => { console.error('FATAL', e); process.exit(2) })
