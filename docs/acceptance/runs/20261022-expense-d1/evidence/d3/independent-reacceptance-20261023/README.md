# EXPENSE-D3 第二轮独立复验证据索引（2026-10-23）

| 项 | 内容 |
| --- | --- |
| 结论 | **PASS / ACCEPTED** |
| 缺陷计数 | P0=0 / P1=0 / P2=0 |
| P1-003 | **CLOSED** |
| 独立 HTTP | 13 tests / 13 PASS / 0 FAIL / 0 ERROR / 0 SKIP |
| Java 22 主源码编译 | PASS；2,087 Java / 2,298 class |
| 完整测试 | attempt1 FAIL（非 D3 时间敏感测试），定向复跑 PASS；attempt2 265 suites / 1,078 tests / 0/0/0 |
| 迁移 130 | 隔离 MySQL 8.4 apply/rollback PASS |
| App | type-check/build PASS |
| Web | type-check/build PASS |
| 真实 Web/App/OSS | `BLOCKED` |
| D4/D5 | 未进入 |
| GAP-070 | OPEN |

## P1-003 零副作用证据

- `sourceId` 指向不存在费用：业务 **404**；Voucher `0 → 0`；文件 `22 → 22`。
- `sourceId` 指向他企费用：业务 **404**；Voucher `0 → 0`；文件 `25 → 25`。
- Expense 与 Payment 所属 Payable 不一致：业务 **400**；Voucher `0 → 0`；文件 `27 → 27`。
- 三种非法来源均未降级为普通付款凭证。
- 合法 Expense 来源仍成功生成费用付款凭证。

## 目录

- `backend/`：新快照环境、源码稳定性、Java 22 编译、完整测试 attempt1/attempt2 和输出。
- `http-run1-harness-error/`：独立夹具构造触发唯一约束的测试侧错误；不计为产品缺陷。
- `http-run2-pass/`：修正夹具后的 13/13 PASS（未含分场景日志标签）。
- `http-run3-final/`：最终 13/13 PASS，含 P1-003 三种场景响应码和零副作用计数。
- `migration-130/`：隔离 MySQL apply、schema、数据、rollback 与备份表证据。
- `harness/`：最终独立 HTTP 源码；未进入产品 `src/test`。
- `app/`、`web/`：独立前端快照的类型检查和构建日志。
- `backend/d3-relevant-file-hashes.txt`：快照与验收结束时原工作区 D3 文件哈希对照。
- `EVIDENCE-SHA256.txt`：逐文件校验清单。
- `EVIDENCE-MANIFEST-SHA256.txt`：清单自身 SHA-256。

## 历史边界

- 第一轮 FAIL 记录和 `evidence/d3/independent-20261023/` 保持原样，不因本轮 PASS 改写。
- 本轮只对应新快照 `/private/tmp/expense-d3-reacceptance-round2.nUlJTV/full-backend`。
