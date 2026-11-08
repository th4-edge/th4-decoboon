# 工资确认门禁与付款归属月份热修发布记录

> 日期：2026-11-08  
> 变更：`CHG-20261108-003`  
> 状态：后端/Web 已发布，App 1.0.90 开发版已上传；正式提审与发布由用户本人完成

## 1. 事故与根因

### 事故一：草稿工资单可被负责人确认

- 麦鸿装饰 2026-10 工资单在 `Draft` 状态被企业负责人直接确认。
- 后端确认逻辑允许 `Draft → Confirmed`，且只校验工资单头缓存 `attendanceComplete`。
- 确认后生成工资项正式状态、员工应付、工资条并锁定考勤，财务无法继续核算。
- 已通过 `CHG-20261108-002` 完成生产数据回退。

### 事故二：跨月工资付款看起来重复

- 生产数据核对：8 月工资于 10 月 9 日支付，9 月工资于 11 月 8 日支付；每名员工在 10 月时间线上看到两笔正常付款。
- 同一工资应付未被重复支付；8 月付款合计 `¥9,333.32`，9 月付款合计 `¥20,812.50`。
- 根因是付款记录接口未返回 `payable.period`，Web/App 没有独立展示工资归属月份，导致视觉上难以区分。

## 2. 修复内容

- 后端只允许 `Pending → Confirmed`；`Draft` 直接确认拒绝。
- 提交复核与确认时实时加载同月考勤，逐工资项校验 `InPayroll` 和天数完整性。
- Web/App 仅对 `Pending` 展示确认工资单入口；`Draft` 明确提示先提交财务复核。
- `finance/payment/page` 增加 `payable.period` 返回；Web 增加“归属月份”，App 工资/提成付款显示对应月份。

## 3. 代码与制品

| 仓库 | 分支 | 代码提交 | 发布内容 |
| --- | --- | --- | --- |
| th4-decoboon | `dev/test/prod` | `f0118f479532a9dc0af287facfbd65144f9e8eee` | 后端确认门禁、实时考勤校验、付款期间字段 |
| th4-decoboon-web | `dev/test/prod` | `3b5515a407a2c9f0da62dc36c316cfc4234915f2` | 草稿确认门禁、付款归属月份 |
| th4-decoboon-app | `dev/test/prod` | `151ece6b0929fd58aa46637aa0bf47ae02d4e4aa` | 草稿确认门禁、付款月份；版本 1.0.90/190 |

### 后端

- Release：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon-hotfix-payroll-confirm-20261108/dist/releases/20261108-164104`
- Archive SHA-256：`ece8a7f64ecc2680dc61736f2c2f3f43ce85ed7e684c108c87a0ab4188757799`
- SOURCE_COMMIT：`f0118f479532a9dc0af287facfbd65144f9e8eee`
- 生产 release：`/opt/decoboon/releases/20261108-164146`
- 上一回滚点：`/opt/decoboon/releases/20261104-223704`

### Web

- 生产 release：`/opt/decoboon-web/releases/20261108-164413`
- 上一回滚点：`/opt/decoboon-web/releases/20261104-223851`

### App

- 版本：`1.0.90 / 190`
- 微信开发者工具：`✔ upload`
- 上传信息：`dist/release/upload-info-1.0.90.json`
- 上传信息 SHA-256：`32079bef1d16f463ceaa33f7671670280e82ed23ad21f2e290253c8dd2122b69`
- 总包：`4,968,409 B`
- 状态：开发版已上传；未提交审核、未发布正式版

## 4. 验证

| 检查 | 结果 |
| --- | --- |
| 后端全量测试 | PASS：318 个报告 / 1259 个用例，0 失败、0 错误、0 跳过 |
| 后端定向测试 | PASS：工资确认 9 个用例、付款投影契约 1 个用例 |
| Provider 响应契约 | PASS：providers=550 properties=544 findings=0 |
| Web 类型检查/生产构建 | PASS |
| App 发布构建/分包 | PASS |
| 分支基线 | 后端/Web/App 三支分别一致 |
| 后端健康 | HTTP 200，systemd active |
| 登录态真实页面 | PASS：麦鸿财务账号可进入员工工资和付款记录 |
| Draft 页面 | PASS：2026-10 工资单无“确认工资单”按钮，显示“财务提交复核后才能确认” |
| 付款月份 | PASS：谢玲玲 2026-08 / 2026-09 两笔员工工资分别显示归属月份 |
| App 真实页面 | 待体验版/真机验证 |

## 5. 回滚

- 后端：执行 `scripts/rollback-prod.sh /opt/decoboon/releases/20261104-223704`，或使用默认上一回滚点。
- Web：将 `/opt/decoboon-web/current` 切回 `/opt/decoboon-web/releases/20261104-223851` 并 reload nginx。
- App：不发布正式版；开发版按版本管理。
- 数据：本次热修无 DML；`CHG-20261108-002` 的回退备份仍保留。

## 6. 残留与边界

- App 正式版提审、审核和发布由用户本人完成。
- App 体验版/真机尚未验证。
- 员工提成的历史应付部分 `period` 为空，页面会显示“暂不可确认”；本次只修复工资归属月份。
- 员工付款后端并发锁属于独立风险，未纳入本次热修。
- 本次不等同于独立验收、业务验收或客户签字。
