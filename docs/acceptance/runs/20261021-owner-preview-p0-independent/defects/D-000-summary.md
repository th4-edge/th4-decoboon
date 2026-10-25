# 缺陷汇总

| ID | 级别 | 结论 | 发布影响 |
| --- | --- | --- | --- |
| D-001 | P0 | 迁移链漂移，121 失败，123 未能正式执行 | BLOCK |
| D-002 | P1 | 迁移脚本失败分支异常退出 | 需修复 |
| D-003 | P1 | 固定水印前缀缺失 | 需修复 |
| D-004 | P1 | OSIV=false 时项目详情懒加载失败，入口不可达 | 需修复 |

未发现：跨租户读取、CustomerAccess 创建、业主 token 生成、预览写状态或内部成本泄漏。

## 后续修复状态

| ID | 后续状态 | 证据 |
| --- | --- | --- |
| D-001 | RESOLVED_IN_TEST_ENV_PROD_PENDING | `../migration/24-test-repair-preflight.txt` 至 `../migration/48-final-app-verify.log` |
| D-002 | FIX_VERIFIED | `../fix-verification/migration-failure-path.log` |
| D-003 | FIX_VERIFIED | `../fix-verification/final4-project_owner-preview_info.json`、`../fix-verification/app-final4.json` |
| D-004 | FIX_VERIFIED | `../fix-verification/summary-final4.txt`、`../fix-verification/app-final4.json` |

| D-005 | P2 · OPEN | 可选 report/finance 投影在 OSIV=false 下仍有懒加载错误，预览入口不受阻 |
