-- 软包装生产日报表系统 · 建库建表 + 种子数据
-- 字段语义与 API.md 完全一致
-- 关键：docker-entrypoint 初始化客户端默认 latin1，必须先 SET NAMES 保证中文种子数据不乱码
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS daily_report DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE daily_report;

-- 用户表（API.md §3）
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  real_name VARCHAR(50) NOT NULL COMMENT '姓名（签名用）',
  gender CHAR(1) COMMENT 'M/F',
  phone VARCHAR(20),
  email VARCHAR(100),
  address VARCHAR(200),
  role VARCHAR(20) NOT NULL COMMENT 'ADMIN/DEPT_LEAD/WORKER/MANAGEMENT',
  dept VARCHAR(20) COMMENT 'PRINT/LAMINATION/BAGMAKING，管理类角色为 NULL',
  avatar_url VARCHAR(200),
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_role_dept (role, dept)
) COMMENT '用户';

-- 印刷部日报（API.md §6.2）
CREATE TABLE IF NOT EXISTS print_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '创建人',
  date DATE NOT NULL,
  order_id VARCHAR(50) NOT NULL COMMENT '生产单号',
  onboard_time TIME NOT NULL COMMENT '上机时间',
  speed DECIMAL(10,2) NOT NULL COMMENT '机速 m/min',
  material VARCHAR(100) NOT NULL,
  thickness DECIMAL(10,2) COMMENT 'μm',
  spec VARCHAR(100) NOT NULL COMMENT '尺寸规格',
  required_meter DECIMAL(12,2) NOT NULL COMMENT '要求印刷米数',
  actual_meter DECIMAL(12,2) COMMENT '实际印刷米数',
  colors VARCHAR(200) COMMENT '印刷颜色，逗号分隔',
  print_minutes DECIMAL(10,1) COMMENT '印刷时间分钟',
  signer VARCHAR(50) NOT NULL,
  remark VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED',
  reject_reason VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_date_order (date, order_id, deleted),
  INDEX idx_user_date (user_id, date),
  INDEX idx_status (status)
) COMMENT '印刷部日报';

-- 复合部日报（API.md §6.3）
CREATE TABLE IF NOT EXISTS lamination_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  date DATE NOT NULL,
  order_id VARCHAR(50) NOT NULL,
  times INT NOT NULL COMMENT '复合次数',
  speed DECIMAL(10,2) NOT NULL,
  material VARCHAR(100) NOT NULL,
  spec VARCHAR(100) NOT NULL,
  required_meter DECIMAL(12,2) NOT NULL COMMENT '工单要求米数',
  actual_meter DECIMAL(12,2) COMMENT '实际复合米数',
  rolls INT COMMENT '卷数',
  print_remain DECIMAL(12,2) COMMENT '印刷剩余米数',
  machine_type VARCHAR(20) NOT NULL COMMENT 'DRY/SOLVENT_FREE',
  adjust_meter DECIMAL(12,2) COMMENT '调机米数',
  signer VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  reject_reason VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_date_order (date, order_id, deleted),
  INDEX idx_user_date (user_id, date),
  INDEX idx_status (status)
) COMMENT '复合部日报';

-- 制袋部日报（API.md §6.4）
CREATE TABLE IF NOT EXISTS bag_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  date DATE NOT NULL,
  shift VARCHAR(10) NOT NULL COMMENT 'DAY/NIGHT',
  machine VARCHAR(20) NOT NULL COMMENT '1号机..8号机',
  picker VARCHAR(50) NOT NULL COMMENT '捡袋员',
  leader VARCHAR(50) NOT NULL COMMENT '机长',
  speed DECIMAL(10,2) NOT NULL,
  order_id VARCHAR(50) NOT NULL,
  bag_type VARCHAR(50) NOT NULL COMMENT '袋型字典',
  spec VARCHAR(100) NOT NULL COMMENT '成品规格',
  incoming_meter DECIMAL(12,2) NOT NULL COMMENT '来料米数',
  adjust_meter DECIMAL(12,2) COMMENT '调机米数',
  end_meter DECIMAL(12,2) COMMENT '结束米数',
  completed_qty INT COMMENT '完成数量',
  waste_kg DECIMAL(10,2) COMMENT '废料kg',
  remain_kg DECIMAL(10,2) COMMENT '余料kg',
  work_start TIME COMMENT '工时起',
  work_end TIME COMMENT '工时止',
  abnormal_reason VARCHAR(500) COMMENT '异常原因',
  defect_qty INT COMMENT '不良品数量',
  signer VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  reject_reason VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_date_machine_shift_order (date, machine, shift, order_id, deleted),
  INDEX idx_user_date (user_id, date),
  INDEX idx_status (status)
) COMMENT '制袋部日报';

-- 审计日志（字段级留痕，API.md §10）
CREATE TABLE IF NOT EXISTS daily_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(20) NOT NULL COMMENT 'PRINT/LAMINATION/BAGMAKING/USER',
  record_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_name VARCHAR(50) NOT NULL,
  action VARCHAR(20) NOT NULL COMMENT 'CREATE/SUBMIT/WITHDRAW/EDIT/APPROVE/REJECT/DELETE/STATUS',
  detail VARCHAR(1000) COMMENT '字段级 diff 或原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_record (module, record_id)
) COMMENT '审计日志';

-- 站内通知（API.md §9）
CREATE TABLE IF NOT EXISTS sys_notice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收人',
  type VARCHAR(20) NOT NULL COMMENT 'SUBMITTED/APPROVED/REJECTED',
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500),
  related_module VARCHAR(20),
  related_id BIGINT,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_read (user_id, read_flag)
) COMMENT '站内通知';

-- ===== 种子数据（密码均为 123456 的 BCrypt）=====
INSERT INTO sys_user (username, password, real_name, gender, phone, email, address, role, dept, enabled) VALUES
('admin',      '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '系统管理员', 'M', '13800000001', 'admin@example.com',    '厂区办公楼 2F', 'ADMIN',      NULL,        1),
('boss',       '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '厂长',       'M', '13800000008', 'boss@example.com',     '厂区办公楼 3F', 'MANAGEMENT', NULL,        1),
('yinshua-01', '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '陈建国',     'M', '13800000002', 'chenjg@example.com',   '印刷车间',      'DEPT_LEAD',  'PRINT',     1),
('yinshua-02', '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '李明',       'M', '13800000003', 'liming@example.com',   '印刷车间',      'WORKER',     'PRINT',     1),
('fuhe-01',    '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '王建军',     'M', '13800000004', 'wangjj@example.com',   '复合车间',      'DEPT_LEAD',  'LAMINATION',1),
('zhidai-01',  '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '赵德海',     'M', '13800000005', 'zhaodh@example.com',   '制袋车间',      'DEPT_LEAD',  'BAGMAKING', 1),
('zhidai-02',  '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '马建军',     'M', '13800000006', 'majj@example.com',     '制袋车间',      'WORKER',     'BAGMAKING', 1),
('zhidai-03',  '$2a$10$hYmePs6y.2VzYsQc1LvmEeplOVYnYTDmo5uJHV5rjXznv/YWVdo8e', '孙晓燕',     'F', '13800000007', 'sunxy@example.com',    '制袋车间',      'WORKER',     'BAGMAKING', 0);
