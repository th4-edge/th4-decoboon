# EXPENSE-D2 P1-002 研发修复证据

| 项 | 结果 |
| --- | --- |
| 缺陷 | P1-002：门店范围访问 `finance/project/report` 时 `Customer.store` 懒加载，返回 `hibernateErrorCode` |
| 根因 | `FinanceAccessContext.resolveProject` 在无事务边界下读取项目客户门店 |
| 修复 | 财务访问专用 JPQL `JOIN FETCH customer + customer.store`；`resolveProject` 使用 `@Transactional(readOnly=true)` |
| D2 HTTP | 18/18 PASS（原 17 项 + 本店合法范围矩阵） |
| 合法访问 | 本店店长、本店门店财务、BOSS、总部财务均 200 |
| 越权访问 | 跨店、跨企业均 404，无数据库异常码 |
| Java 22 编译 | 2086 主源码 PASS |
| 全量回归 | 261 suites / 1069 tests / 0 failures / 0 errors / 0 skipped |
| 状态 | `DEV_VERIFIED_CANDIDATE / REACCEPTANCE_PENDING` |
| 独立验收 | 历史三轮 FAIL 保留；等待独立复验 |
| 边界 | 未修改金额、成本、权限范围、状态机、DDL 或迁移；未发布；未进入 D3/D5 |

`p1-002-fix.diff` 只包含本次财务访问查询、只读事务和调用替换；工作区中既有的来源门店继承改动未被本次切片修改或回退。

关键文件：

- `p1-002-fix.diff`
- `access-matrix-summary.txt`
- `d2-http-18-summary.txt`
- `full-repository-test-count.txt`
- `java22-compile.log`
- `d2-full-http-18-final.log.gz`
- `full-repository-clean-test.log.gz`
- `sha256sums.txt`
