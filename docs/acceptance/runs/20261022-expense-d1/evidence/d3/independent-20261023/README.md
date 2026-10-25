# EXPENSE-D3 独立验收证据索引（2026-10-23）

| 项 | 内容 |
| --- | --- |
| 结论 | `FAIL`；P0=0 / P1=1 / P2=0 |
| 稳定产品失败 | `P1-003` 非法 Expense 来源仍成功发行普通付款凭证 |
| 最终 HTTP | 11 tests / 10 PASS / 1 FAIL / 0 ERROR / 0 SKIP |
| 后端全量 | 264 suites / 1,075 tests / 0 failures / 0 errors / 0 skipped |
| 迁移 130 | isolated MySQL 8.4 apply/rollback PASS |
| App | type-check/build PASS |
| Web | type-check PASS；production build FAIL（未定义 `$shadow-xs`） |
| 真实 Web/App/OSS | `BLOCKED` |
| D3 可关闭 | 否 |

## 目录

- `backend/`：完整快照环境、源码清单摘要、Java 22 主源码编译、完整测试和 Surefire 报告包。
- `http-round1/`、`http-round2/`：独立夹具与断言修正历史；不把测试代码问题冒充产品缺陷。
- `http-round3/`：最终稳定结果，唯一产品失败为 P1-003。
- `migration-130/`：隔离 MySQL baseline、apply、schema、source data、rollback、backup table 和原历史保留证据。
- `harness/`：独立 HTTP 验收源码；未进入产品 `src/test`。
- `app/`、`web/`：临时前端快照环境、类型检查和构建日志。
- `backend/source-sha256.txt`：独立快照源码 SHA-256 清单。
- `EVIDENCE-SHA256.txt`：本目录逐文件校验清单。
- `EVIDENCE-MANIFEST-SHA256.txt`：逐文件校验清单自身的 SHA-256。

## 快照边界

- 首轮结论只对应 `/private/tmp/expense-d3-independent.BeBugm/full-backend`。
- 首轮结束后原工作区 `VoucherContext.java` 于 18:48:41 出现外部候选修改；未纳入本目录的最终 HTTP 结果，必须新快照独立复验。
- `backend/d3-relevant-file-hashes.txt` 明确记录快照与原工作区的逐文件差异。
