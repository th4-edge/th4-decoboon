# 修复验证 · D-002 / D-003 / D-004

日期：2026-10-21
状态：修复与测试环境独立复验 PASS
发布结论：允许进入发布准备；生产迁移和正式发布仍需单独执行

## 1. D-002 迁移脚本失败路径

- 修复：`docs/design/migrations/run-migrations.sh.template:346` 使用 `${description}`。
- 验证：在隔离 smoke 库执行故意失败的 124 迁移。
- 结果：
  - 原始 SQL 错误可见
  - 账本标记 `FAILED`
  - 输出 `FAILED 124 deliberate-failure`
  - 退出码 4
  - 无 `unbound variable`
- 证据：`migration-failure-path.log`
- 结论：PASS

## 2. D-003 固定水印

- 修复：后端预览投影生成完整固定文案：
  `业主视角预览 · 仅内部使用 · 员工姓名/工号/时间`
- API：`final4-project_owner-preview_info.json`
- App：OSIV=false 下实际打开预览页，页面文本包含完整前缀
- 截图：`app-final4.png`
- 结论：PASS

## 3. D-004 OSIV=false 项目详情

修复范围：

- 项目详情：只读事务 + 有界详情预取 + 二级关联事务内初始化
- 阶段列表：独立详情预取查询，避免改变所有阶段列表查询
- 成员列表：只读事务 + 同一企业范围详情预取 + Property 最小白名单
- 任务分页：只读事务 + 阶段/工艺/人员预取 + evidenceUrls 安全复制
- 合同详情：只读事务 + 合同/项目/客户/房屋/报价预取 + 二级关联事务内初始化

实测 `spring.jpa.open-in-view=false`：

| 接口 | 结果 |
| --- | --- |
| `project/project/info` | PASS，3935 B 有效 JSON |
| `project/stage/page` | PASS，444 B 有效 JSON |
| `project/member/list` | PASS，2719 B 有效 JSON |
| `project/owner-preview/info` | PASS，7664 B 有效 JSON |

App：项目详情正常渲染“业主视角”入口，并可进入预览页。

证据：

- `backend-osiv-false-verified-final.log`
- `summary-final4.txt`
- `app-final4.json`
- `app-final4.png`

结论：PASS

## 4. 测试

- 定向测试：`OwnerPreviewInfoAcceptanceTest` + `CustomerJourneyOwnerPreviewModeTest`，6/6 PASS
- 后端全量：`894/894`，`BUILD SUCCESS`（最终代码）
- 新增回归使用 `spring.jpa.open-in-view=false`，未加 `@Transactional`
- App 代码本轮未改；此前 `npm run verify:mp-weixin` 已通过

## 5. 修改文件

- `docs/design/migrations/run-migrations.sh.template`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectStageRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/service/projects/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/project/service/stages/page/support/Page.java`
- `src/main/java/com/th4/edge/decoboon/project/service/members/list/support/List.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectTaskRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/service/tasks/page/support/Page.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectContractRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contract/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/project/service/ownerpreview/info/support/Info.java`
- `src/test/java/com/th4/edge/decoboon/project/OwnerPreviewInfoAcceptanceTest.java`

## 6. 剩余阻断

D-001 已在 `decoboon-test` 目标测试环境修复：

- 全量备份后，审计发现仅 111 账本缺失、112-122 未应用
- 111 审计后补账本
- 112→123 正式脚本全部 SUCCESS
- 123 重复执行 SKIP
- 123 回滚后重放恢复
- `ddl-auto=validate` 启动 PASS
- 测试企业 API 59/59、App 15/15 PASS
- 原始 dev 仍保留历史漂移；生产未连接、未迁移

详见 `../migration/14-d001-decision.md`、`../migration/15-chain-pre-repair.txt` 至 `../migration/22-chain-validate-startup.log`。

## 6.1 员工与业主 UI 复用

- 用户最新要求：员工看到的 UI 与业主端完全一致
- 实现：`owner-preview` 只做权限校验和跳转，最终复用 `journey.vue?preview=1`
- 验证：15/15 PASS，最终页面路径为 `subpackages/project/pages/journey`
- 预览专属只读横幅、水印、返回入口和动作/分享关闭保持不变
- 证据：`../EXACT-UI-DECISION.md`、`../migration/exact-ui-app-results.json`、`../migration/55-exact-ui-owner-page-in-preview.png`

## 7. 残余风险

D-005（P2）：`finance/project/report` 和 `project/report/page` 在 OSIV=false 下仍有懒加载错误，App 已容错，不阻断预览主链。建议后续按同一模式修复。

## 8. 发布判断

- P0 功能测试环境独立验收：PASS
- 可进入体验版/发布准备
- 生产迁移、生产发布与正式版审核仍需单独授权和执行
