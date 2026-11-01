# CHG-20261030-035 · 本地库清空与装企业务链重建

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-035 |
| 类型 | 运维 / 本地测试数据重建 |
| 风险级别 | L3 |
| 状态 | 已关闭（本地数据重建与核心链验证完成） |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | 用户明确要求“清除本地数据，重新构建装企，进行业务驱动测试”；`BR-001/002/003/055` |

## 2. 目标与范围

- 目标：备份后重置本机 `127.0.0.1:3306/decoboon`，创建全新测试装企、组织与七类角色账号，并通过正式 Provider 重建一条可由页面验收的业务链。
- 范围内：本地 `decoboon`、本地 Redis DB0、后端 dev、Web/App 测试环境、合成客户与合成财务数据。
- 非范围：生产库、远端测试库、其他本地 schema、正式小程序提审与发布。

## 3. 业务与决策

- 业务事实来源：装得清业务智能体 `BR-001`、`BR-002`、`BR-003`、`BR-055`、`MET-002/005/006`。
- 企业初始化优先 Provider；dev 无安全可用的微信建企入口时，允许受控 SQL 创建租户骨架，随后所有业务对象必须走 Provider。
- 金额与成本不得用 0 冒充；成本不完整时利润为空。
- 测试数据统一使用 `AUDIT-20261030-*` 或明确审计前缀。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 备份 | gzip 可读、SHA-256、恢复命令明确 | 备份文件与账本 |
| 2 | 清库 | `decoboon` 空库，其他 schema 不受影响 | information_schema |
| 3 | 后端重建 | dev 启动、schema 初始化、套餐初始化 | 进程/API |
| 4 | 组织 | 1 企业、2 门店、七类角色、各账号可登录 | SQL/API |
| 5 | 业务链 | 客户→房屋→量房→设计→报价→合同→项目→应收/收款→应付/付款/台账 | Provider 响应与 DB |
| 6 | 页面 | Web/App 同企业同数据、角色可操作、失败不伪装 | 截图/文本 |
| 7 | 账务 | 来源唯一、收付台账一致、成本不完整利润空值 | 不变量脚本 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 不提交测试数据 | 本地重置记录、Provider 造数脚本 |
| th4-decoboon-web | dev | 当前工作区 | 页面验收；店长台账文案，财务关注卡文案 |
| th4-decoboon-app | dev | 当前工作区 | 页面验收；项目付款进度文案 |

## 6. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 备份与恢复演练 | PASS；备份 67,788B；恢复临时库 202 表成功 | 本记录第 8 节 |
| 清库与 schema 初始化 | PASS；本地 `decoboon` 202 表 | `raw/sql/business-baseline-final.json` |
| 组织与账号 | PASS；1 企业、2 门店、7 类角色、8 员工 | `2026-10-30-fresh-business-audit-manifest.json` |
| 业务链 | PASS；1 客户/房屋/量房/设计/报价/合同/项目，8 阶段、2 任务 | `raw/sql/business-baseline-final.json` |
| 财务链 | PASS；应收 5 / 收款 1 / 应付 1 / 付款 1 / 台账 4 | `raw/sql/accounting-invariants-final.json` |
| 账务不变量 | PASS；`coreBalanced=true`，各项差额 0 | 同上 |
| 成本利润 | PASS；预计成本 ¥1,760、实际成本 ¥800、`costComplete=false`、利润与毛利率为 `null` | `raw/api/app-finance-project-report-fixed.json` |
| Web/App 真实页面 | PASS（财务、Boss、店长核心页面） | `raw/screenshots/*-final.png`、`raw/notes/*-final.txt` |
| 后端定向回归 | PASS；83 tests / 0 failures / 0 errors | Surefire reports |
| Web 构建 | PASS | `npm run build` |
| App 类型检查与开发构建 | PASS | `npm run type-check`、`npm run build:mp-weixin` |

- 数据口径修正：客户面积草稿字段从 112㎡对齐为与房屋主档一致的 100㎡；同步修正造数脚本公式，后续新企业不再产生该不一致。
- 未完成项：材料采购/到场/结算、阶段验收/整改、七态和并发页面证据仍未补齐；最终审计只能为 `PARTIAL`。

## 7. 发布

不发布。所有验证仅限本地 `decoboon`、本地后端 8090、Web 8080 与微信开发者工具开发版。

## 8. 回滚

- 备份路径：`/Users/zhangwei/.decoboon-local/business-rebuild-20261030/decoboon-before-rebuild.sql.gz`
- SHA-256：`99338b2bad6c6d0cab7e6d6960702d8c5e406b010a0232167ce0e01b717aeb5b`
- 恢复命令：`gunzip -c /Users/zhangwei/.decoboon-local/business-rebuild-20261030/decoboon-before-rebuild.sql.gz | mysql -h127.0.0.1 -uroot -p decoboon`
- 回滚验证：临时库恢复成功，202 表、1 企业、3 员工；演练库已删除。
- 实际是否回滚：否；当前保留全新审计企业供继续检查。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集。
- 人工耗时：未采集。
- 一次通过：否；财务与角色报告各发现一次关联序列化截断并修复。
- 最终结论：本地清库、全新装企重建及核心业务/财务链验证完成；跨端核心页面一致性通过，但材料、验收与独立验收未完成，整体业务审计仍为 `PARTIAL`。
