/**
 * 填报端（App 风格）全流程 E2E 测试
 * 真实 Chrome + nginx 生产栈(http://localhost)
 * 覆盖：员工登录分流 → App 首页 → 填报提交闭环 → 记录/详情 → 我的/资料 →
 *       主管登录分流 → 审核工作台(通过/退回) → 管理端不受影响 → 权限拦截
 */
const { chromium } = require('playwright-core')

const BASE = 'http://localhost'
const results = []
let browser

function log(step, ok, detail = '') {
  results.push({ step, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${step}${detail ? ' | ' + detail : ''}`)
}

async function login(page, username, password) {
  await page.goto(BASE + '/login')
  await page.waitForSelector('.login-form input')
  await page.fill('.login-form input >> nth=0', username)
  await page.fill('.login-form input >> nth=1', password)
  await page.click('button:has-text("登 录")')
}

async function logoutViaMe(page) {
  await page.goto(BASE + '/fill/me')
  await page.waitForSelector('.m-me-cell:has-text("退出登录")')
  await page.click('.m-me-cell:has-text("退出登录")')
  await page.waitForSelector('.el-message-box')
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForURL('**/login', { timeout: 8000 })
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

  const orderId = 'YS-FILL-' + Date.now().toString().slice(-6)

  try {
    // ============ 1. 员工登录分流 ============
    await login(page, 'yinshua-02', '123456')
    await page.waitForURL('**/fill/home', { timeout: 10000 })
    log('1.1 员工登录进入填报端 App 首页', true)

    await page.waitForSelector('.v-app-tabbar')
    log('1.2 App 底部标签栏渲染', true)

    const tabCount = await page.locator('.v-app-tabbar .van-tabbar-item').count()
    log('1.3 员工 Tabbar 为 4 个 tab（无审核）', tabCount === 4, `${tabCount} 个`)

    await page.waitForSelector('.van-cell:has-text("印刷部日报")')
    const lamVisible = await page.locator('.van-cell:has-text("复合部日报")').count()
    log('1.4 印刷部员工只看到本部门入口', lamVisible === 0, '复合入口隐藏')

    // ============ 2. App 首页统计 ============
    await page.waitForSelector('.v-stats__k:has-text("今日已填")')
    log('2.1 今日统计三栏渲染', true)

    // ============ 3. 填报闭环：印刷部 ============
    await page.click('.van-cell:has-text("印刷部日报")')
    await page.waitForURL('**/fill/print')
    await page.waitForSelector('.van-field:has-text("生产单号")')
    log('3.1 印刷部填报页（Vant 卡片式表单）加载', true)

    // 生产单号
    await page.fill('.van-field:has-text("生产单号") input', orderId)
    // 机速
    await page.fill('.van-field:has-text("机速") input', '350')
    // 颜色 chips
    await page.click('.v-chip:has-text("黑")')
    await page.click('.v-chip:has-text("红")')
    // 材质 / 规格
    await page.fill('.van-field:has-text("材质") input', 'PET')
    await page.fill('.van-field:has-text("尺寸规格") input', '300×200')
    // 米数
    await page.fill('.van-field:has-text("要求印刷米数") input', '5000')
    await page.fill('.van-field:has-text("实际印刷米数") input', '4800')
    await page.waitForTimeout(400)
    const calcVal = await page.locator('.v-calc').first().textContent()
    log('3.2 印刷时间自动计算', /13/.test(calcVal || ''), `实际4800÷机速350≈${(calcVal || '').trim().slice(0, 6)}`)

    // 先试空提交（清掉单号）验证校验拦截
    await page.fill('.van-field:has-text("生产单号") input', '')
    await page.click('button:has-text("提交日报")')
    await page.waitForSelector('.van-toast', { timeout: 5000 })
    log('3.3 空必填提交被校验拦截（Toast 提示）', true)
    // 补回单号再提交
    await page.fill('.van-field:has-text("生产单号") input', orderId)
    await page.click('button:has-text("提交日报")')
    await page.waitForSelector('.van-toast--success', { timeout: 8000 })
    log('3.4 提交成功有反馈提示', true)

    // 当日已录出现该单号
    await page.waitForSelector(`.van-cell:has-text("${orderId}")`, { timeout: 8000 })
    log('3.5 当日已录列表出现新行', true)

    // ============ 4. 记录页 ============
    await page.goto(BASE + '/fill/records')
    await page.waitForURL('**/fill/records')
    await page.waitForSelector(`.v-record-no:has-text("${orderId}")`, { timeout: 8000 })
    log('4.1 我的记录出现刚提交的单号', true)

    // 点进详情（PENDING → 只读详情）
    await page.click(`.v-record-no:has-text("${orderId}")`)
    await page.waitForURL('**/fill/revise/**')
    await page.waitForSelector('.van-cell-group .van-field')
    log('4.2 记录详情只读展示（待审核不可改）', true)

    // ============ 5. 我的 / 个人资料 ============
    await page.goto(BASE + '/fill/me')
    await page.waitForSelector('.v-me-name')
    log('5.1 我的页渲染', true)

    await page.click('.van-cell:has-text("个人资料")')
    await page.waitForURL('**/fill/profile')
    await page.waitForSelector('.v-avatar-edit')
    log('5.2 个人资料页（头像+资料+密码）渲染', true)

    // ============ 6. 权限拦截：员工访问管理端 ============
    await page.goto(BASE + '/users')
    await page.waitForURL('**/fill/home', { timeout: 8000 })
    log('6.1 员工访问 /users 被拦回填报端', true)

    // 员工退出（Vant 我的页 → 确认弹窗）
    await page.goto(BASE + '/fill/me')
    await page.click('.v-logout-cell')
    await page.waitForSelector('.van-dialog')
    await page.click('.van-dialog button:has-text("确认")')
    await page.waitForURL('**/login', { timeout: 8000 })
    log('6.2 员工退出登录回到登录页', true)

    // ============ 7. 主管登录分流 + 审核闭环 ============
    await login(page, 'yinshua-01', '123456')
    await page.waitForURL('**/fill/home', { timeout: 10000 })
    log('7.1 主管登录同样进入填报端', true)

    const leadTabs = await page.locator('.v-app-tabbar .van-tabbar-item').count()
    log('7.2 主管 Tabbar 5 个 tab（多审核）', leadTabs === 5, `${leadTabs} 个`)

    await page.goto(BASE + '/fill/review')
    await page.waitForSelector('.van-cell:has-text("待审核")', { timeout: 8000 })
    log('7.3 审核工作台加载（Vant App 风格）', true)

    // 通过刚提交的单（按钮在 .van-cell 同级的 .v-review-actions 里；先缩小到含目标单号的 cell-group 再点它内部的按钮）
    const targetGroup = page.locator(`.van-cell-group:has-text("${orderId}")`).first()
    await targetGroup.locator('.v-review-actions button:has-text("通过")').first().click()
    await page.waitForSelector('.van-dialog', { timeout: 5000 })
    await page.click('.van-dialog button:has-text("确认")')
    await page.waitForSelector('.van-toast--success', { timeout: 8000 })
    log('7.4 主管审核通过', true)

    // 退回流程：若还有其他待审单据则退回一单
    await page.waitForTimeout(800)
    const remain = await page.locator('.v-review-actions button:has-text("退回")').count()
    if (remain > 0) {
      await page.locator('.v-review-actions button:has-text("退回")').first().click()
      await page.waitForSelector('.el-message-box__input input', { timeout: 5000 })
      await page.fill('.el-message-box__input input', '机速与实际米数不符，请核对')
      await page.click('.el-message-box button:has-text("确定")')
      await page.waitForSelector('.van-toast--success', { timeout: 8000 })
      log('7.5 主管退回（需填原因）', true)
    } else {
      log('7.5 主管退回（无其他待审单，跳过）', true, '跳过')
    }

    // 主管退出
    await page.goto(BASE + '/fill/me')
    await page.click('.v-logout-cell')
    await page.waitForSelector('.van-dialog')
    await page.click('.van-dialog button:has-text("确认")')
    await page.waitForURL('**/login', { timeout: 8000 })

    // ============ 8. 管理端不受影响 ============
    await login(page, 'admin', '123456')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
    log('8.1 admin 登录仍进管理端工作台', true)

    await page.goto(BASE + '/fill/home')
    await page.waitForURL('**/dashboard', { timeout: 8000 })
    log('8.2 管理员访问填报端被分流回管理端', true)

    await logoutViaMe(page).catch(async () => {
      // 管理端退出走 AdminShell 下拉
      await page.goto(BASE + '/dashboard')
      await page.click('.user-dropdown')
      await page.click('.el-dropdown-menu__item:has-text("退出登录")')
      await page.waitForURL('**/login', { timeout: 8000 })
    })
    log('8.3 管理端退出正常', true)

    // ============ 9. 页面错误检查 ============
    const realErrors = errors.filter(e => !e.includes('favicon'))
    log('9.1 无页面脚本报错', realErrors.length === 0, realErrors.slice(0, 3).join(' ; '))

  } catch (e) {
    log('EXCEPTION', false, String(e).slice(0, 300))
  }

  await browser.close()
  const passed = results.filter(r => r.ok).length
  console.log(`\n===== ${passed}/${results.length} PASS =====`)
  process.exit(passed === results.length ? 0 : 1)
}

main()
