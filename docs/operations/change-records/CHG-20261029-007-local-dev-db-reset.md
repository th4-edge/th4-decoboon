# CHG-20261029-007 · 本地开发库重置与小程序联调环境启动

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-007 |
| 类型 | 运维 / 本地开发环境 |
| 风险级别 | L3 |
| 状态 | 待用户验证 |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-004`、Company 信息梳理、用户明确要求清空本地开发数据库 |

## 2. 目标与范围

- 目标：备份后重置本机 `127.0.0.1:3306/decoboon`，启动后端、小程序开发构建和 App H5，供用户从小程序全新入驻开始验证。
- 范围内：本地 MySQL `decoboon`、当前本地 Redis DB0、后端 dev、App mp-weixin 开发构建、App H5 开发环境。
- 非范围：生产数据库、远端环境、代码业务逻辑修改、正式小程序提审和发布。
- 受影响端：本地后端、小程序、App H5。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：数据删除涉及全部本地开发数据。
- 业务事实来源与裁决编号：用户 2026-10-29 明确要求清空本地开发数据库。
- 尚存 GAP 或假设置：企业 Logo/品牌色的产品概念已确认；本轮不做生产代码实现。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 本地数据库备份 | 重置前 | 生成完整压缩备份并校验可读 | 备份文件、gzip/sha256 |
| 2 | 重置 | 备份成功 | 本地 `decoboon` 为空库 | information_schema |
| 3 | 后端启动 | 空库 | dev 后端启动并自动建表 | 健康/登录/表数量 |
| 4 | 小程序构建 | 后端可用 | mp-weixin dev 构建成功并打开开发者工具 | dist/dev/mp-weixin |
| 5 | App H5 | 后端可用 | H5 开发服务可访问 | URL/健康 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 不提交环境数据 | 本地数据库重置记录 |
| th4-decoboon-app | `dev` | 不提交本地 `.env.local` | mp-weixin/H5 启动 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 本地身份 | MySQL hostname/port/database | PASS：MacBook-Pro-3.local:3306/decoboon，MySQL 8.4.0 | 本记录 |
| 备份 | `mysqldump` + `gzip -t` + SHA-256 | PASS；315,332 bytes；SHA-256 `a524a4a20b781bb45a72d49661e25bc25f34da2a246555520ff332426ffbea8f` | `output/local-env/backups/decoboon-before-reset-20261029-141300.sql.gz` |
| 重置回读 | `information_schema` | PASS；重置后 0 表、0 企业、0 员工、0 门店；后端自动建表后 202 表 | 本记录 |
| 后端 | `mvn spring-boot:run` | PASS；JDK22，dev profile，8090 监听；未登录 session 返回 `tokenIsUndefined` | 后台日志 |
| 小程序 | `npm run dev:mp-weixin` | PASS；构建完成，API `http://172.20.10.2:8090/api/`，DevTools 已登录并打开项目 | `dist/dev/mp-weixin` |
| App H5 | `npm run dev:h5` | PASS；http://localhost:8080/ 返回 200 | 本地 URL |

## 7. 发布

- 不发布，无生产制品、无生产迁移。

## 8. 回滚

- 回滚条件：重置失败、后端无法自动建表、用户需要恢复原数据。
- 回滚命令：`gunzip -c output/local-env/backups/decoboon-before-reset-20261029-141300.sql.gz | mysql -h127.0.0.1 -P3306 -uroot -pmysqladmin decoboon`；Redis 按需重建。
- 回滚验证：表数量、业务数据抽样和登录。
- 实际是否回滚：否

## 9. 成本与结论

- 最终结论：本地库已重置，后端、小程序开发构建、App H5 和微信开发者工具均已启动，等待用户从全新企业入驻流程验证

## 10. 复盘与沉淀

- Redis DB0 重置前后均为 0，无会话缓存需要恢复。
- DevTools storage/compile cache 已清理，微信开发者工具登录态保留。
- 后续：从小程序新鲜企业入驻路径验证 Company 信息快速创建；Logo/品牌色概念待进入 BRAND-D1。
