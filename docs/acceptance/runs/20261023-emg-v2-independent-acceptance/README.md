# ACC-PI-20261022-005-EMG-V2 · 独立黑盒验收

- 验收批次：`ACC-PI-20261022-005-EMG-V2`
- 关联问题：`PI-20261022-005`
- 切片：`EMG-V2 员工生命周期`
- 验收日期：2026-10-23（Asia/Shanghai）
- 验收方式：独立黑盒；未修改后端、App、Web 源码或测试
- 结论：`PASS`
- 18 个 P0 用例：`18 PASS / 0 FAIL / 0 BLOCKED`
- Follow-up：`EMG-V2-DEF-01`、`EMG-V2-DEF-02` 均已于 2026-10-23 独立复验关闭
- 问题整体：仍为 `DEV_IN_PROGRESS`，不得 `CLOSED`

## 1. 判定口径

本结论覆盖 `EMG-V2` 的 18 个独立验收用例及 2 个后续缺陷复验。18/18 个 P0 用例 PASS，2/2 follow-up defects CLOSED，因此本切片独立验收为 `PASS`。问题 `PI-20261022-005` 整体仍为 `DEV_IN_PROGRESS`，仅代表 EMG-V2 切片通过。

Team 与 TeamLeader 仍冻结：

- Team 列表为空，只读接口可用；
- `user/team/save` 不存在；
- 新邀请携带 `teamId` 返回 400；
- 店长邀请 TeamLeader 返回 403；
- 临时 TeamLeader 登录探针返回 403，测试后已恢复合成员工原状态。

## 2. 环境与数据

- MySQL：`127.0.0.1:3307 / decoboon_accept`；未停止、未删库。
- 后端：`127.0.0.1:8090`，使用当前工作树主源码编译产物启动。
- Web：`127.0.0.1:8085` 真实 Vite 页面。
- App：微信开发者工具 `dist/dev` 项目根目录，`urlCheck=false`；真实店长令牌由接口签发后注入既有会话存储。
- 测试身份：BOSS `13990000001`、店长 `13990000002`，密码不落盘。
- 合成企业：`1001`，总店 `2001`，二店 `2002`。
- 跨企业基线：企业 `9501`、门店 `9502`、员工 `9503`。
- 合成责任客户：`custId=88001`，离职后仍归属员工 `3004 / 门店 2002`，用于验证强制离职与历史归属不回溯。

## 3. 逐用例统计

| 结果 | 数量 |
| --- | --- |
| PASS | 18 |
| FAIL | 0 |
| BLOCKED | 0 |
| 合计 | 18 |

逐用例详情：`http/case-results.md`、`http/case-results-full.json`。

关键覆盖：

1. 财务邀请缺少门店 400；店长本店财务固定 `2001`；跨店财务 403。
2. 店长邀请店长/TeamLeader/携带 Team 参数均拒绝。
3. BOSS 跨店调岗调店成功，成员日志记录前后值；店长本店调岗成功、跨店 403、提升店长 403。
4. 店长操作自己/BOSS/其他店长均拒绝，Web/App 对无权对象无操作按钮。
5. 停用后登录 403；恢复成功；停用门店上的恢复被明确拒绝；重复停用/恢复/离职均为 400。
6. 有在手客户时普通离职 409；强制离职后 `Resigned`，原因和时间落库，客户仍归属原员工。
7. 管理员代办离职的成员日志 `operator=3001`、目标 `employee=3003`。
8. 跨企业员工详情 404，跨企业门店调整拒绝。
9. Team/TeamLeader 冻结回归通过。
10. App/Web 成员状态、岗位、门店与接口事实同源。

## 4. 页面证据

- Web BOSS 在职页：`screenshots/web-boss-organization-final.png`
- Web BOSS 离职筛选页：`screenshots/web-boss-resigned-final.png`
- Web 店长成员页：`screenshots/web-manager-members-final.png`
- Web 店长邀请角色：`screenshots/web-manager-invite-roles.png`
- App 店长成员页：`screenshots/app-manager-members-final.png`
- App 店长邀请页：`screenshots/app-manager-invite-roles.png`
- Web DOM 快照：`web/*-snapshot.yml`
- App 页面运行态与 OCR：`app/app-page-review.json`、`app/*-ocr.txt`

透明说明：

- Web 店长邀请岗位下拉实际显示"设计师、业务员、项目经理、财务"，不存在店长/TeamLeader。
- App 邀请页运行态确认目标门店固定为"验收总店"、默认岗位为"业务员"；受微信原生 picker 自动化限制，本轮未展开完整选项列表。App 成员页与接口同源、无自己/BOSS 操作入口已通过页面运行态确认；完整岗位边界由 Web 页面和真实接口拒绝共同证明。
- 原始 `app/app-page-review.json` 的自动 `pass` 条件要求"财务"在当前折叠态文本中出现，导致其布尔值偏严；最终用例判定采用页面截图、OCR、运行态行数据与 Web/API 交叉证据，不把该自动布尔值冒充独立结论。
- App 首次误开 `dist/dev/mp-weixin` 子目录导致域名校验失败；重开带 `project.private.config.json` 的 `dist/dev` 项目根目录后完成有效页面取证。
- App `devToken` 快速登录在一次自动化流程中未生效，不是本切片产品结论；页面验收使用真实接口签发令牌注入既有会话存储。

## 5. 构建与门禁

- App 共享工作区 `npm run build:mp-weixin`：PASS，原 `expense.scss` 阻断已不存在。
- Web `npm run check:permissions`：PASS。
- Web `npm run check:workbenches`：PASS。
- 后端 `mvn spring-boot:run` 默认 testCompile 被无关 `CompanyPlanTemplateContextTest` 构造器错误阻断；主源码编译成功后通过 direct Java 启动，不影响本次 API 黑盒执行。

## 6. 缺陷关闭

- `EMG-V2-DEF-01` P1：CLOSED。店长取消自己邀请、BOSS 取消店长邀请均成功；DB 为 `Cancelled`，席位 `3 -> 2`，待邀请 `1 -> 0`。
- `EMG-V2-DEF-02` P2：CLOSED。店长页面不再请求 `statistic/organization/report`、无权限告警；BOSS 同一页面该接口 200 且组织报告正常。

详见 `defects.md` 与 `followup/README.md`。

## 7. 边界声明

- 未修改任何实现代码或测试。
- 未恢复 Team/TeamLeader。
- 未操作生产，未发布正式版。
- 未删除 `decoboon_accept`，未停止共享 MySQL `3307`。
- 验收库中的合成企业、员工和客户保留用于复核；临时恢复门店校验员工 `3201` 已清理。
