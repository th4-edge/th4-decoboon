# EXPENSE-D2 第二轮独立复验证据

| 项 | 结果 |
| --- | --- |
| 结论 | **FAIL** |
| D2 HTTP | **17 tests / 16 PASS / 1 FAIL / 0 ERROR / 0 SKIP** |
| 失败项 | `P1-002`：店长门店范围访问 `finance/project/report` 时懒加载 `Customer.store`，返回 `hibernateErrorCode`，不是 403/404 |
| P1-001 | PASS：合法 JSON、Property 白名单、`payables[].payeeEmployee`、`finance/project/page` 同结构均通过 |
| 完整快照 | commit `206bc86bc4e2d816c353dd6576925e795a78eacd`；tree `999a36e064b7a08d1ddba5814a14e103018a14dc`；源码归档 SHA-256 `b14cda9c8d8cc8c112ea6925748fa8684dd6cc92233fbf977c4dff15bbfdf04d` |
| 全量测试 | Java 22 `mvn -o clean test` = **1069 / 0 / 0 / 0**，261 suites，RC=0 |
| 真实 UI/OSS | BLOCKED |
| D3 | 未进入 |
| GAP-070 | 仍 OPEN；D5 BLOCKED |

完整运行环境、快照、源码哈希、HTTP 日志、Surefire、JSON/缺陷证据、全量测试和 Redis 命令统计见同目录文件。
