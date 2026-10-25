# EXPENSE-D2 第三轮独立复验证据

| 项 | 结果 |
| --- | --- |
| 结论 | **D2_PASS / EXPENSE-D2 ACCEPTED / PI-018 overall PENDING** |
| 完整快照 | `/private/tmp/expense-d2-reacceptance-round3.H50jW4/full-backend` |
| 快照提交 / Tree | `c77532c1ec18abffbc999951a3ca6d3c9ea5ed63` / `c035c3fd1f1535990a142c9723e9b91f8f26d26b` |
| D2 HTTP | **18 tests / 18 PASS / 0 FAIL / 0 ERROR / 0 SKIP** |
| P1-002 | **CLOSED**：本店店长/门店财务与 BOSS/总部财务成功；跨店/跨企业业务码 404；无 `hibernateErrorCode` |
| Java 22 全量 | **261 suites / 1069 tests / 0 failures / 0 errors / 0 skipped**，RC=0 |
| 测试隔离 | 18 项验收源码运行后移出；全量 262 个测试 Java 文件不含 D2 验收源码 |
| 真实 UI/OSS | **BLOCKED**：未执行真实 Web/App、真机或真实 OSS |
| D3 | 未启动 |
| D5 | `GAP-070` 仍 OPEN，保持 BLOCKED |

关键证据：

- `01-environment.txt`：Java 22、H2、Undertow、`open-in-view=false`、隔离 Redis。
- `03-source-hashes.txt`：P1-002 与 D2 关键源码哈希。
- `04-d2-http-18.log`：真实 TCP HTTP 原始日志。
- `05-d2-suite-summary.txt`：18 项方法逐项 PASS。
- `06-p1-002-access-matrix.txt`：P1-002 门店/企业访问矩阵。
- `07-full-repository-clean-test.log.gz`：Java 22 全量 `mvn -o clean test` 日志。
- `08-full-test-count.txt`：精确计数。
- `11-snapshot-archive.txt`：快照身份、源码全量哈希与测试树隔离状态。
- `12-test-harness-isolation.txt`：Redisson 测试配置污染及隔离处理说明。
