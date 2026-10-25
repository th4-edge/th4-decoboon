# EMG-V2 员工生命周期 · 研发验证证据包

- 说明：本包保留研发阶段证据；最终独立验收与 follow-up 缺陷关闭见 `docs/acceptance/runs/20261023-emg-v2-independent-acceptance/`。
- 问题：`PI-20261022-005`
- 切片：`EMG-V2 员工生命周期`
- 业务裁决：`DEC-085` / `EV-353`、`DEC-081`、`DEC-082`、`DEC-070`
- 日期：2026-10-23
- 结论：`DEV_VERIFIED_PENDING_ACCEPTANCE`（不是独立验收 PASS，不代表问题 CLOSED）

## 1. 验证摘要

1. 后端定向 25 tests / 0 failures / 0 errors，编译随 Maven 测试执行通过。
2. BOSS 可调岗并跨店调整普通员工；店长只能本店调岗，跨店和提升店长均 403。
3. 停用员工登录 403，恢复后登录成功；离职状态、原因、时间和审计快照落库。
4. 管理员代办离职的 `operator_id=3001`，目标员工 `emp_id=3106`，修复了原先把目标员工记录为操作人的缺陷。
5. App/Web 真实运行态确认店长邀请页无店长/TeamLeader、店长成员页无权操作 BOSS/店长/自己、员工详情无门店选择器。
6. App 类型检查、导航契约和隔离小程序构建 PASS。
7. Web 共享工作区类型检查、权限与工作台门禁 PASS；隔离 `vite build` PASS。

## 2. 环境

- 本地隔离 MySQL `127.0.0.1:3307/decoboon_accept`
- 后端 `8090`
- Web Vite `8085`
- 微信开发者工具自动化 `9420`
- MySQL `3307` 为共享进程，未停止。

## 3. 文件说明

- `backend-targeted-tests.log`：后端 5 个测试类定向日志。
- `app-type-check.log`、`app-navigation.log`：App 静态门禁。
- `app-build-mp-weixin-isolated.log`：仅应用本切片差异的隔离 worktree 构建成功。
- `app-build-mp-weixin-shared-blocked.log`：共享 App 工作区被并行费用样式阻断的证据，非本切片文件。
- `app-lifecycle-acceptance.mjs/json`：App 生命周期页面运行态断言；账号密码通过 `EMG_ACCEPT_PHONE` / `EMG_ACCEPT_PASSWORD` 注入，不落盘。
- `web-type-check-shared.log`、`web-type-check-isolated.log`、`web-permissions*.log`、`web-workbenches*.log`、`web-build-isolated.log`：Web 门禁与构建证据。
- `boss-role-store-change.json`、`storemanager-*.json`、`suspend.json`、`resume.json`、`resign*.json`：真实 HTTP 响应。
- `db-after-lifecycle.txt`、`db-audit-after-lifecycle.txt`、`db-resign-audit-operator-fixed.txt`：最终数据库和审计事实。
- `backend-emg-v2.diff`、`app-emg-v2.diff`、`web-emg-v2.diff`：本切片源码差异快照。
- `screenshots/`：App/Web 运行态截图。

## 4. 透明说明

- App 自动化脚本登录后会清空首页其他域已知 403 console error，只统计生命周期页面；`consoleErrors: []` 不代表全应用零错误。
- 登录 token 已脱敏；验收账号密码不写入仓库。
- 共享 App 全量构建中的费用样式错误来自并行未提交文件，不归因于 `EMG-V2`；独立验收需在稳定工作区复跑。
- 本地 `decoboon_accept` 数据库尚未删除，删除属于数据清理门禁，需用户确认后执行。

## 5. 下一门禁

由独立验收责任方执行 `ACCEPT-EMG-V2-员工生命周期.md` 的 18 个用例。PASS 前不得启动 `EMG-V3`，问题整体保持 `DEV_IN_PROGRESS`。
