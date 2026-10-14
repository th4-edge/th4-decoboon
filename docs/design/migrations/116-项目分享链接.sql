-- 116-项目分享链接
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 背景：journey 页原强制登录，家人/好友点开分享链接即被踢到登录页，分享功能实际不可用。
-- 本对象提供**带时效的只读令牌**，使未登录访客可查看脱敏内容（亲友观看通道）。
--
-- 安全：
--   · token 唯一 + 带时效 + 可撤销；撤销/过期立即失效（读取端不缓存）
--   · 不保存任何登录凭证、不代管账号
--   · 脱敏在服务端完成：Friend 范围不返回金额，房号一律不返回

CREATE TABLE IF NOT EXISTS cust_project_share_link (
  share_id      BIGINT       NOT NULL,
  com_id        BIGINT       NOT NULL,
  project_id    BIGINT       NOT NULL,
  created_by    BIGINT       NULL,
  token         VARCHAR(32)  NOT NULL,
  scope         VARCHAR(16)  NOT NULL,
  show_amount   BIT          NOT NULL DEFAULT 0,
  status        VARCHAR(16)  NOT NULL,
  expire_at     DATETIME(6)  NULL,
  create_date_time BIGINT    NULL,
  create_time   INTEGER      NULL,
  create_date   INTEGER      NULL,
  create_year   INTEGER      NULL,
  create_month  INTEGER      NULL,
  create_day    INTEGER      NULL,
  create_hour   INTEGER      NULL,
  create_minute INTEGER      NULL,
  create_second INTEGER      NULL,
  create_week   TINYINT      NULL,
  update_date_time BIGINT    NULL,
  update_time   INTEGER      NULL,
  update_date   INTEGER      NULL,
  update_year   INTEGER      NULL,
  update_month  INTEGER      NULL,
  update_day    INTEGER      NULL,
  update_hour   INTEGER      NULL,
  update_minute INTEGER      NULL,
  update_second INTEGER      NULL,
  update_week   TINYINT      NULL,
  PRIMARY KEY (share_id),
  UNIQUE KEY uk_share_link_token (token),
  KEY idx_share_link_company_project (com_id, project_id),
  KEY idx_share_link_company_status_expire (com_id, status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-10-15 事故修复：本表实体继承 AuditModel，**必须同时具备 create_* 与 update_* 两组审计列**。
-- 初版漏建 10 个 update_* 列，导致生产 ddl-auto=validate 报
-- 「Schema-validation: missing column [update_date] in table [cust_project_share_link]」而启动失败（重启循环）。
-- 已在生产用 ALTER TABLE 补齐；本文件同步修正，避免新环境重演。

-- 回滚：
-- DROP TABLE IF EXISTS cust_project_share_link;
