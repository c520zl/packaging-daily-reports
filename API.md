# 软包装生产日报表系统 · API 接口文档与规范

> 版本 v1.0 · 本文档是前后端唯一契约：后端按此实现，前端按此对接，字段语义以本文为准。
> 配套文档：`PRD.md`（业务规则）、`design/*.html`（UI 稿，字段与文案以此为准）。

---

## 1. 全局约定

| 项 | 约定 |
| --- | --- |
| Base URL | `/api`（开发环境前端 Vite 代理到 `http://localhost:8080`） |
| Content-Type | `application/json; charset=UTF-8`；文件上传 `multipart/form-data` |
| 认证 | 请求头 `Authorization: Bearer <JWT>`；除 §2.1 登录外全部接口需要 |
| 统一响应 | `{"code": 0, "message": "success", "data": ...}`；`code=0` 成功，非 0 见错误码表 |
| 分页请求 | `page`（从 1 开始）、`size`（默认 20，最大 200） |
| 分页响应 | `data: {"total": 123, "records": [...]}` |
| 日期时间 | 日期 `yyyy-MM-dd`；时刻 `HH:mm`；完整时间 `yyyy-MM-dd HH:mm:ss` |
| 字段命名 | camelCase；布尔用 `enabled`、`unread` 这类正向词 |
| 数值单位 | 米数/长度：整数 m；机速：m/min；厚度：μm；重量：kg（两位小数，number）；数量：整数个；印刷时间：分钟（一位小数，number） |
| 审计字段 | 所有业务记录返回 `id, createdAt, createdBy, updatedAt, updatedBy` |

### 1.1 枚举（前后端必须使用同一套英文值，文案由前端映射）

| 枚举 | 值 → 中文文案 |
| --- | --- |
| 单据状态 status | `DRAFT` 草稿 · `PENDING` 待审核 · `APPROVED` 已审核 · `REJECTED` 已退回 |
| 角色 role | `ADMIN` 系统管理员 · `DEPT_LEAD` 部门主管 · `WORKER` 普通员工 · `MANAGEMENT` 厂部管理层 |
| 部门 dept | `PRINT` 印刷部 · `LAMINATION` 复合部 · `BAGMAKING` 制袋部 |
| 性别 gender | `M` 男 · `F` 女 |
| 复合机型 machineType | `DRY` 干式 · `SOLVENT_FREE` 无溶剂 |
| 班次 shift | `DAY` 白班 · `NIGHT` 夜班 |
| 通知类型 noticeType | `SUBMITTED` 提交提醒 · `APPROVED` 审核通过 · `REJECTED` 退回提醒 |

袋型字典 bagType（key=文案，前端下拉直接用）：
`连体拉链直立袋`、`三边封袋`、`三边封拉链袋`、`三边封自粘袋`、`八边封袋`、`中封袋`、`中封等边风琴袋`、`中封侧边风琴袋`

### 1.2 错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 参数错误（message 带具体字段） |
| 401 | 未登录 / token 失效（前端清 token 跳登录） |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 业务冲突 |
| 500 | 服务器内部错误 |
| 1001 | 用户名或密码错误 |
| 1002 | 账号已停用，请联系管理员 |
| 1003 | 原密码错误 |
| 2001 | 该生产单号在当日已存在（同模块） |
| 2002 | 当前审核状态不允许该操作（状态机拒绝） |
| 2003 | 数据校验失败（如结束米数 > 来料米数） |
| 2004 | 字段缺失，不允许提交审核（提交前强校验） |

---

## 2. 认证 /auth

### 2.1 登录
`POST /auth/login`
```json
{ "username": "yinshua-02", "password": "123456" }
```
→ `data: { "token": "...", "expiresIn": 86400, "user": { "id": 3, "username": "yinshua-02", "realName": "李明", "role": "WORKER", "dept": "PRINT", "avatarUrl": null } }`
错误：1001 / 1002

### 2.2 当前用户
`GET /auth/me` → `data: user`（同上结构，含 `phone/email/address/gender`）

### 2.3 退出登录
`POST /auth/logout` → `data: null`（服务端吊销 token）

### 2.4 修改本人密码
`PUT /auth/password`
```json
{ "oldPassword": "...", "newPassword": "..." }
```
错误：1003。成功后旧 token 全部失效。

---

## 3. 用户管理 /users（仅 ADMIN）

### 3.1 列表
`GET /users?page&size&keyword&role&status`
- `keyword`：模糊匹配用户名 / 真实姓名 / 手机号
- `status`：`ENABLED` / `DISABLED`，不传=全部
→ 分页，记录字段：`{id, username, realName, gender, phone, email, address, role, dept, avatarUrl, enabled, createdAt}`

### 3.2 新增
`POST /users`
```json
{ "username": "yinshua-04", "password": "初始密码", "realName": "张三", "gender": "M",
  "phone": "13800000000", "email": "zs@example.com", "address": "印刷车间",
  "role": "WORKER", "dept": "PRINT", "enabled": true, "avatarUrl": null }
```
错误：409 用户名已存在

### 3.3 详情 / 编辑
`GET /users/{id}` · `PUT /users/{id}`（body 同 3.2，无 password；`PUT /users/{id}/password` `{newPassword}` 重置密码）

### 3.4 启用/停用
`PUT /users/{id}/status` `{ "enabled": false }` → 停用后该用户 token 立即失效

### 3.5 删除
`DELETE /users/{id}` 逻辑删除；不可删除自己

---

## 4. 个人资料 /me（登录用户本人）

- `GET /me/profile` → 同 user 结构
- `PUT /me/profile` `{ realName, gender, phone, email, address }`（username/role/dept 只读）
- `POST /me/avatar` multipart 字段 `file`（JPG/PNG ≤2MB）→ `data: { "avatarUrl": "..." }`
  - 存储后端固定为 RustFS 对象存储（S3 兼容），返回完整 URL（如 `http://localhost:9000/daily-report/avatar/xxx.png`）

---

## 5. 字典 /dicts

`GET /dicts/{type}`，type ∈ `bagType` `machine` `printMachine` `lamMachine` `material`
→ `data: [{ "key": "三边封袋", "label": "三边封袋" }, ...]`（machine 返回 `1号机`…`8号机`）

---

## 6. 日报表（三模块同构，路径不同，字段不同）

模块路径：`/print-daily`（印刷）、`/lamination-daily`（复合）、`/bag-daily`（制袋）。以下用 `{module}` 指代。

### 6.1 通用规则
- **列表**：`GET {module}?page&size&date&startDate&endDate&orderId&status&mine=true`
  - 员工（WORKER）只看本人创建；主管看本部门；管理层/管理员看全部
- **新增**：`POST {module}`，body 末尾带 `"submit": false|true`（false=存草稿，true=直接提交进审核）
- **编辑**：`PUT {module}/{id}`，仅 `DRAFT` / `REJECTED` 状态可改（`REJECTED` 改后状态回 `DRAFT`，需再提交）
- **删除**：`DELETE {module}/{id}`，本人可删 `DRAFT`；主管可删 `PENDING`（物理删，写审计日志）
- **状态机**（后端强制，违反返回 2002）：
  `DRAFT --submit--> PENDING --approve--> APPROVED（锁定）`
  `PENDING --reject{reason}--> REJECTED --edit--> DRAFT`
  `PENDING --withdraw--> DRAFT`（提交人撤回，留痕）

### 6.2 印刷部字段（POST/PUT body，GET 返回同结构 + 审计字段）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| date | 日期 | ✓ | 报表日期 |
| orderId | string | ✓ | 生产单号，如 YS-20261123-01；同日同模块唯一（409/2001） |
| onboardTime | 时刻 | ✓ | 上机时间 |
| speed | number | ✓ | 机速 m/min |
| material | string | ✓ | 如 PET/PE |
| thickness | number | | 厚度 μm |
| spec | string | ✓ | 尺寸规格，如 350×220 |
| requiredMeter | number | ✓ | 要求印刷米数 |
| actualMeter | number | ✓ | 实际印刷米数 |
| colors | string[] | ✓ | 印刷颜色，如 ["黑","红"] |
| printMinutes | number | ✓ | 印刷时间分钟；前端按 actualMeter/speed 预填，可改 |
| signer | string | ✓ | 签名（默认当前用户 realName） |
| remark | string | | 备注 |

### 6.3 复合部字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| date | 日期 | ✓ | |
| orderId | string | ✓ | 同日唯一 |
| times | int | ✓ | 复合次数 |
| speed | number | ✓ | 机速 |
| material | string | ✓ | |
| spec | string | ✓ | |
| requiredMeter | number | ✓ | 工单要求米数 |
| actualMeter | number | ✓ | 实际复合米数 |
| rolls | int | ✓ | 卷数 |
| printRemain | number | | 印刷剩余米数 |
| machineType | 枚举 | ✓ | `DRY` / `SOLVENT_FREE` |
| adjustMeter | number | | 调机米数 |
| signer | string | ✓ | |

### 6.4 制袋部字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| date | 日期 | ✓ | |
| shift | 枚举 | ✓ | `DAY` / `NIGHT` |
| machine | string | ✓ | `1号机`…`8号机` |
| picker | string | ✓ | 捡袋员 |
| leader | string | ✓ | 机长 |
| speed | number | ✓ | 机速 |
| orderId | string | ✓ | 同日+机台+班次唯一（2001） |
| bagType | string | ✓ | 袋型字典值 |
| spec | string | ✓ | 成品规格 |
| incomingMeter | number | ✓ | 来料米数 |
| adjustMeter | number | | 调机米数 |
| endMeter | number | ✓ | 结束米数；校验 ≤ incomingMeter（2003） |
| completedQty | int | ✓ | 完成数量 |
| wasteKg | number | | 废料 kg |
| remainKg | number | | 余料 kg |
| workStart / workEnd | 时刻 | ✓ | 工时起止 |
| abnormalReason | string | | 异常原因 |
| defectQty | int | | 不良品数量 |

### 6.5 状态流转接口

| 接口 | 权限 | 说明 |
| --- | --- | --- |
| `PUT {module}/{id}/submit` | 创建人 | DRAFT→PENDING；缺失必填字段返回 2004 |
| `PUT {module}/{id}/withdraw` | 创建人 | PENDING→DRAFT |
| `PUT {module}/{id}/approve` | 本部门主管 | PENDING→APPROVED |
| `PUT {module}/{id}/reject` body `{ "reason": "..." }` | 本部门主管 | PENDING→REJECTED，并给创建人发通知 |
| `GET {module}/{id}/timeline` | 可见该单的人 | `[{ time, actor, action, detail }]`，action ∈ `CREATE/SUBMIT/WITHDRAW/EDIT/APPROVE/REJECT/DELETE` |

---

## 7. 报表查询 /reports（主管/管理层/管理员）

### 7.1 汇总
`GET /reports/summary?date=yyyy-MM-dd`（或 `startDate&endDate`；`dept` 可选）
→ `data: { "printMeter": 14650, "laminationMeter": 10770, "bagQty": 11000, "wasteKg": 10.0, "counts": { "print": 4, "lamination": 3, "bagmaking": 2 } }`
（只统计 `APPROVED`；counts 为已提交单据行数）

### 7.2 跨部门明细（合并视图）
`GET /records?page&size&dept&startDate&endDate&orderId&machine&shift&status&mine`（`mine=true`：填报端只看本人；WORKER 角色强制生效）
记录：`{ id, dept, module("PRINT"|"LAMINATION"|"BAGMAKING"), orderId, machine, materialOrBagType, spec, required, actual, rolls, wasteKg, shift, signer, status, date, createdAt }`
（`required/actual`：印刷/复合为米数；制袋 actual 为完成数量个、required 为来料米数）

### 7.3 导出
`GET /records/export?`（筛选参数同 7.2）→ `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，文件名 `日报表明细_2026-11-23.xlsx`

---

## 8. 工作台 /dashboard

- `GET /dashboard/today` → `{ submittedCount, pendingCount, printMeter, laminationMeter, bagQty, wasteKg }`
- `GET /dashboard/weekly?days=7` → `[{ date, printMeter, laminationMeter, bagQty }]`
- `GET /dashboard/activities?size=10` → `[{ time, actor, action, detail, dept }]`（全公司最近动态）
- `GET /dashboard/pending-count` → `data: 3`（主管待审角标；员工返回自己被退回未处理数）

---

## 9. 通知 /notices

- `GET /notices?page&size&unread=true` → 分页：`{ id, type, title, content, relatedModule, relatedId, read, createdAt }`
- `PUT /notices/{id}/read` · `PUT /notices/read-all`

---

## 10. 后端实现约束

- 技术栈：Java 17 + Spring Boot 3.x + MyBatis-Plus + MySQL 8 + JJWT；端口 8080
- 表：`sys_user`、`print_daily`、`lamination_daily`、`bag_daily`、`daily_audit_log`、`sys_notice`（schema.sql 建表 + 种子数据，密码 BCrypt）
- JWT payload：`{ uid, username, role, dept }`，有效期 24h；用户被停用立即失效（查库校验 enabled）
- 统一响应体 `R<T>{code,message,data}` + 全局异常处理器（BusinessException→对应 code）
- 权限：Spring Security `@PreAuthorize`，角色即 §1.1 枚举；主管仅可审本部门
- CORS：允许 `http://localhost:5173`
- 所有写操作写 `daily_audit_log`（含字段级 diff）

## 11. 前端对接约束

- axios 实例：`baseURL: '/api'`，请求拦截带 token，响应拦截 `code!==0` 时 `ElMessage.error(message)`，`code===401` 清 token 跳 `/login`
- 路由守卫：无 token 访问业务页 → `/login`；登录后按 role 显示菜单（WORKER 默认进移动端首页）
- 枚举→文案映射集中在 `src/constants/index.ts`，值与 §1.1 完全一致
- 分页参数与 §1 分页约定一致；数字输入 `inputmode` 与单位后缀按设计稿
- 移动端（<768px）自动使用 `m-*` 系列页面（路由 `/m/*`），桌面使用后台页面；两套页面共用同一 api 层与 store，保证语义一致
