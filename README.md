# 软包装生产日报表系统

纸质日报线上化：印刷 / 复合 / 制袋三部门填报 → 主管审核 → 管理层汇总导出。

| 文档 | 说明 |
| --- | --- |
| [PRD.md](PRD.md) | 产品需求：角色权限、字段定义、业务规则、状态机 |
| [API.md](API.md) | 前后端接口契约（字段、枚举、错误码以此为准） |
| [design/](design/) | UI 设计稿（20 屏） |

## 技术栈

- 后端 `server/`：Java 17 · Spring Boot 3.x · MyBatis-Plus · MySQL 8 · JWT
- 前端 `web/`：Vue 3 + TypeScript · Vite · Pinia · Element Plus · Axios
- 存储：RustFS 对象存储（S3 兼容，头像等上传文件的唯一后端）

## 双门户

| 门户 | 角色 | 界面 | 功能 |
| --- | --- | --- | --- |
| 管理端 `/dashboard` | ADMIN / MANAGEMENT | 桌面后台 | 数据管理、报表汇总、用户管理 |
| 填报端 `/fill/home` | WORKER / DEPT_LEAD（手机/桌面同一入口） | Vant App 风格，手机优先响应式 | 日报填报、我的记录、退回修改；主管多一个「审核」Tab |

- 按角色严格分流，误入对方门户自动踢回；员工只见本部门填报入口
- 填报记录接口 `mine=true` 只返回本人数据（后端强制）
- `/m/*` 为旧移动版路由，仅兼容保留，新入口统一走 `/fill`

## 快速启动（Docker，推荐）

```bash
# 1. 配置环境变量（JWT 密钥等；compose 自动读取）
cp .env.example .env      # 编辑 .env，填入 APP_JWT_SECRET（未配置时后端启动即失败）

# 2. 拉取 RustFS 镜像（国内用 DaoCloud 源，约 773MB）
docker pull docker.m.daocloud.io/rustfs/rustfs:latest-glibc
docker tag docker.m.daocloud.io/rustfs/rustfs:latest-glibc rustfs/rustfs:latest

# 3. 启动全套：nginx(80) + server(8080) + mysql(3306) + rustfs(9000/9001)
docker compose -f docker-compose.yml -p yicai-report up -d
```

- 首次启动 mysql 自动执行 `schema.sql` 建库建表 + 种子数据
- 后端自动创建 bucket `daily-report` 并设公开读；上传文件 URL 形如 `http://localhost:9000/daily-report/avatar/xxx.png`
- RustFS 控制台：http://localhost:9001（rustfsadmin / rustfsadmin）
- 本地 8080 被占用时，新建 `docker-compose.override.yml` 改映射端口：
  ```yaml
  services:
    server:
      ports: !override
        - "18080:8080"
    web:
      ports: !override
        - "18081:80"
  ```

### 本地开发（不走 Docker）

```bash
# 后端：先执行 schema.sql，改 application.yml 数据库密码
cd server && mvn spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:8080（dev profile：localhost 直连 + 开发默认密钥）

# 前端
cd web && pnpm i && pnpm dev              # http://localhost:5173（代理 /api → 8080）
```

## 演示账号（密码均为 123456）

| 账号 | 角色 | 部门 |
| --- | --- | --- |
| admin | 系统管理员 | — |
| boss | 厂部管理层 | — |
| yinshua-01 / 02 | 主管 / 员工 | 印刷部 |
| fuhe-01 | 主管 | 复合部 |
| zhidai-01 / 02 | 主管 / 员工 | 制袋部 |

## E2E 测试

后端 + 前端启动后，运行真实浏览器全流程测试：

```bash
cd e2e
pnpm add playwright-core      # executablePath 指向本机 Chrome
node e2e.cjs                  # 管理端 29 项
node e2e-fill.cjs             # 填报端 24 项
```
