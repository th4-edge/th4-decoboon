# Acceptance Worklog

## Current

`ACC-MP-BOSS-20260916` 企业负责人小程序全链路闭环验收已完成。在本地后端（dev, 127.0.0.1:8090）+ 微信开发者工具（AppID `wxe5cc5fec9e50a9a9`）环境下，以 Boss 张伟真实登录，完成 57/57 路由巡检与 14 步业务闭环（客户 → 跟进 → 量房 → 设计 → 报价 → 合同 → 项目自动生成）。

最终状态：`PASS WITH DEFECTS`。发现 4 项缺陷（1 项后端规则粒度、2 项前端角色/状态分支、1 项前端健壮性）；后端未发现越权放行、数据错乱或 5xx。新增可复用资产：全路由巡检脚本、后端日志时间窗分析脚本，以及《小程序全链路回归规范》。

## Outputs

| title | path | audience | status |
| --- | --- | --- | --- |
| Web 第一阶段验收报告（最终复验） | `docs/acceptance/Web端第一阶段验收报告.md` | development / product-planning / user | pass_with_blocked_visual |
| 密码会话验收测试 | `src/test/java/com/th4/edge/tenant/user/WebPasswordSessionAcceptanceTest.java` | development / acceptance | 7 passed |
| Provider 安全验收测试 | `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java` | development / acceptance | 3 passed |
| 1440×900 登录页截图 | `docs/acceptance/evidence/web-phase1-login-1440x900.png` | acceptance / user | pass |
| 1280×800 登录页截图 | `docs/acceptance/evidence/web-phase1-login-1280x800.png` | acceptance / user | pass |
| 小程序企业负责人全链路闭环验收报告 | `docs/acceptance/小程序企业负责人全链路闭环验收报告.md` | development / product-planning / user | pass_with_defects |
| 小程序全链路回归规范（含根因反思） | `docs/acceptance/小程序全链路回归规范.md` | development / product-planning / acceptance | ready |
| 小程序全路由巡检脚本与结果 | `_系统/协作/lanes/acceptance/workspace/miniapp-boss-pilot/route-sweep.cjs`、`route-sweep-result.json` | development / acceptance | ready |
| 后端日志时间窗分析脚本 | `_系统/协作/lanes/acceptance/workspace/miniapp-boss-pilot/analyze-backend-log.sh` | development / acceptance | ready |

## Requests

| to | request | status | link |
| --- | --- | --- | --- |
| product-planning / 运维 | 提供真实测试后端、有效员工账号和驾驶舱/财务/报价/项目业务夹具，完成已登录页面视觉与接口联调。 | blocked_external | `BLOCKED-ENV-WEB-001` |
| 运维 / 产品 | 正式发布前替换 `https://api.invalid/`，验证 HTTPS 域名、证书、备案、CORS 和网络策略。 | blocked_external | `docs/acceptance/Web端第一阶段验收报告.md` §11 |
| development | 可选性能优化：处理主 JS chunk 约 1.1 MB 的构建警告。 | open_low | `DEF-WEB-006` |

## Notes

- 职责：负责测试用例设计、独立验证、缺陷结论和验收报告，不代替开发修改生产代码。
- 主动写入范围：`src/test/**`、`docs/acceptance/**`、`../../前端代码/th4-decoboon-web/**`、`_系统/协作/lanes/acceptance/**`。
- 未修改后端 `src/main/**`、迁移或 Web 业务页面。
- `password-login`、`password-setup` 为公开路由；`password-change`、`logout` 继承 `Security`。
- Provider 安全测试使用 196 个显式路由的当前清单，不再依赖旧的 192/9 固定预期。
- Web 前端菜单/守卫仅用于展示；服务端 `Security + DataPermission/DataScope` 仍是权限事实源。

## Verification

- `npm run type-check`：PASS。
- `npm run build`：PASS，1723 modules；主 JS chunk 约 1,096.13 kB，gzip 约 362.43 kB。
- `mvn -o -q -Dtest=WebPasswordSessionAcceptanceTest test`：7 tests，0 failures，0 errors。
- `mvn -o -q -Dtest=ProviderSecurityAcceptanceTest,SubscriptionAccessFilterAcceptanceTest,ProductionReleaseBaselineAcceptanceTest test`：PASS。
- `mvn -o -q test`：26 suites，79 tests，0 failures，0 errors，0 skipped。
- Provider 扫描：`total=196 protected=185 public=11 violations=0`。
- Web API 路由存在性：18/18。
- 页面字段契约：驾驶舱、财务总览、报价、项目筛选、任务、财务流水均已对齐；无真实接口联调，结论限定为静态契约 PASS。
- 1440×900 登录页：真实浏览器 PASS，`body.scrollWidth=1440`，无横向溢出。
- 1280×800 登录页：真实浏览器 PASS，`body.scrollWidth=1280`，无横向溢出。
- 已登录业务页真实浏览器冒烟：BLOCKED，缺少真实后端、测试账号和业务数据。

## Defects

| id | severity | status | summary |
| --- | --- | --- | --- |
| `DEF-WEB-001` | Blocker/P0 | CLOSED | 匿名密码首设/登录被统一 `Security` 拦截 |
| `DEF-WEB-002` | High/P1 | CLOSED | 驾驶舱字段与后端响应不一致 |
| `DEF-WEB-003` | High/P1 | CLOSED | 财务总览字段与后端响应不一致 |
| `DEF-WEB-004` | Medium/P2 | CLOSED | 报价和任务字段不一致 |
| `DEF-WEB-005` | Medium/P2 | CLOSED | 项目状态筛选无效且枚举错误 |
| `DEF-WEB-006` | Low/P3 | OPEN | 主包 JS 体积过大 |
| `BLOCKED-ENV-WEB-001` | Environment | BLOCKED | 缺少真实后端、有效员工账号和业务数据，无法完成已登录页面浏览器验收 |
| `DEF-MP-BOSS-001` | High/P1 | OPEN | Boss 进入设计师列表调用店长专属 `statistic/store/report` 得 403，数据全为 0 |
| `DEF-MP-BOSS-002` | High/P1 | OPEN | `company/member/info` 对本人返回"不能操作自己的员工记录"，成员详情页空白 |
| `DEF-MP-BOSS-003` | Medium/P2 | OPEN | 已锁定方案的洽谈页直接 `design/talk/build`，无法查看既有洽谈记录 |
| `DEF-MP-BOSS-004` | Low/P3 | OPEN | 参数异常时页面 `onLoad` 未捕获 Promise 异常，出现白屏与未处理异常 |

## Next

1. 准备真实测试后端、员工账号和业务夹具。
2. 执行已登录驾驶舱、财务、报价、项目页面 1440×900/1280×800 真实浏览器冒烟。
3. 验证真实接口字段、分页、筛选、401/403 和退出跨页面行为。
4. 正式发布前替换生产占位域名并完成 HTTPS/备案/CORS 检查。
5. 按发布优先级处理 `DEF-WEB-006`；在真实环境验证前保留 `BLOCKED-ENV-WEB-001`。

## 2026-09-16 小程序 Boss 全链路闭环验收

- 环境：本地 `th4-decoboon`（dev，8090）+ 本地 MySQL `decoboon` + 微信开发者工具 `th4-decoboon-app/dist/dev/mp-weixin`。
- 身份：BOSS 张伟 `18660219918`（合肥测试装饰工程有限公司）。
- 巡检：57/57 路由可达；未处理异常 1 次；3 个页面出现功能性失败。
- 闭环：小程序 UI 新建客户（`225745825844690944`）+ 记录跟进（`225745919914541056`）；其余节点沿用同一 Boss 会话完成量房、设计需求/方案/区域/分项、方案构建、报价（`225746268830302208`）、提交/接受、合同签约（`225746324421607424`）、合同生效 → 项目 `225746343807680512`（阶段 1、应收 3）自动生成，并在小程序项目详情页复核渲染正常。
- 后端业务校验（区域缺失、分项缺失、报价状态机、方案锁定）均按预期拒绝，判定为正确行为而非缺陷。
- 关键结论：既往"通过"来自构建门禁、合规静态检查、H2 集成测试与 Web 端闭环，均未覆盖"小程序 × Boss × 终态数据"组合，因此不构成对小程序真实运行时的验证；详见回归规范第二节。

## Next

1. 将 `DEF-MP-BOSS-001/002/003/004` 移交 development lane 修复，acceptance 按同一四元矩阵复验。
2. 扩展巡检脚本支持 `--role`，覆盖店长/团队长/顾问/设计师/项目经理/履约/财务 7 类角色。
3. 补充业主端小程序闭环与真机（非开发者工具）回归。
4. 在新建项目 `225746343807680512` 上继续项目执行段（任务开工 → 日报 → 阶段验收 → 收款）写闭环。

## 2026-09-16 测试基线同步（历史断言清零）

- `ProviderSecurityAcceptanceTest`：路由总数断言 196 → **197**，受保护路由 185 → **186**（public 11 保持不变），与 `project/contract-change/batch-save` 等新增 Provider 对齐。
- `ProductionReleaseBaselineAcceptanceTest`：
  1. 原 `doesNotContain("127.0.0.1")` 与 `server.address: ${SERVER_ADDRESS:127.0.0.1}` 的回环绑定策略冲突 → 改为精确禁止外部依赖硬编码回环（`jdbc:mysql://127.0.0.1`、`127.0.0.1:3306`、`127.0.0.1:6379`、`localhost`、`mysqladmin`、`password: 123456`）。
  2. Runbook 断言由已失效的"尚未在真实生产等价库执行迁移和恢复演练"改为当前事实文本"备份恢复和回滚演练尚未完成"+"存量库差异迁移、`adopt` 和恢复演练尚未执行"。
- 结果：`mvn -o -q test` → **83 tests, 0 failures, 0 errors, 0 skipped（全绿）**。
- 说明：本次只改测试断言与规范文档，未修改生产代码，未发版。
