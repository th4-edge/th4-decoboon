# 材料域 B7 · 材料业务助手上下文入口

- 日期：2026-10-17
- 性质：开发实现 + 定向测试 + 真实浏览器 UI 走查；不是 B8 独立验收，不自批 `ACCEPTED`。
- 结论：**PASS**（48 / 48，0 FAIL，0 BLOCKED）。
- 范围：App/Web 统一单轮「问材料」抽屉；项目、作业单、采购/结算上下文；复用 `material/assistant/ask`；新增只读建议问题入口。
- 环境：后端 `8091`、Web `4175`、App H5 `8080`、MySQL `127.0.0.1:3306/decoboon`；Web 1440×900 / 1024×768；App H5 390×844 / 375×667。

## 1. 实现内容

### 后端

- 新增只读 `material/assistant/suggestions`：
  - 只从 `MaterialAssistantKnowledge` 的 50 个黄金问题中按上下文挑选；
  - 支持 `project/sheet/purchase/settlement/general`；
  - 不读取金额、成本或业务单据，不产生写操作；
  - 不新增 DDL、实体、表或权限语义。
- 回答链路继续只复用 `material/assistant/ask`；`suggestions` 只是建议问题门面，不形成第二套助手规则。

### Web

- 新增 `MaterialAssistant` 浮动入口与右侧抽屉：
  - 项目核算/材料体检：项目上下文；
  - 主材下单：采购上下文；
  - 主材采购结算：结算上下文；
  - 上下文摘要、清除/恢复上下文、服务端建议问题；
  - 展示回答类型、置信度、证据、只读工具、下一步、建议追问、审计状态；
  - `NO_EVIDENCE`/`REFUSAL`/`PERMISSION_DENIED` 原样展示，不兜底；
  - Esc 关闭、请求失败可重试、无写操作按钮。
- 未改金额/成本公式、权限范围或业务状态机。

### App

- 新增 `MaterialAssistant` 底部弹层与浮动「问材料」入口：
  - 项目详情：项目上下文；
  - 作业单详情：真实 `sheetId` 上下文；
  - 主材结算：结算上下文；
  - 与 Web 相同的证据/工具/下一步/拒答展示；
  - 处理底部安全区、长内容滚动与 H5 键盘输入。
- 未改 App 现有下单/结算动作。

## 2. 验证结果

| 类别 | PASS | FAIL | 说明 |
| --- | ---: | ---: | --- |
| 后端 API | 14 | 0 | 建议问题同源、项目/作业单上下文、权限、REFUSAL、NO_EVIDENCE、空/超长问题、审计 |
| Web UI | 22 | 0 | 三页入口、上下文、建议、回答、证据、工具、下一步、拒答、清除/恢复、错误重试、1024/1440、Console |
| App UI | 12 | 0 | 项目/作业单/结算上下文、Designer 权限态、390/375、Console |
| **合计** | **48** | **0** | |

补充：

- 后端定向测试：`MaterialAssistantGoldenQuestionsTest`、`MaterialAssistantSuggestionsTest`、`MaterialAssistantAuditContextTest` PASS；
- App：`type-check`、`build:mp-weixin`、`check:ui`、`check:numeric-inputs`、`check:subpackages` PASS；
- Web：`type-check`、`build` PASS；
- Web 1440×900 / 1024×768 与 App H5 390×844 / 375×667 均无横向溢出；
- Console Errors 0（错误重试用例中故意中断的 `net::ERR_FAILED` 已单独标注，不计为产品缺陷）；
- 14 张真实渲染截图，见 `screenshots/`。

## 3. 关键 UI 证据

- Web 项目抽屉：`screenshots/b7-web-project-drawer-1440x900.png`
- Web 项目回答：`screenshots/b7-web-project-answer-1440x900.png`
- Web 拒答：`screenshots/b7-web-project-refusal-1440x900.png`
- Web 未绑定上下文：`screenshots/b7-web-project-unbound-1440x900.png`
- Web 采购上下文：`screenshots/b7-web-ordering-drawer-1440x900.png`
- Web 结算上下文：`screenshots/b7-web-settlement-drawer-1440x900.png`
- Web 错误重试：`screenshots/b7-web-project-error-1440x900.png`
- Web 1024×768：`screenshots/b7-web-project-1024x768.png`
- App 项目回答：`screenshots/b7-app-project-answer-390x844.png`
- App 作业单回答：`screenshots/b7-app-sheet-answer-390x844.png`
- App 结算回答：`screenshots/b7-app-settlement-answer-390x844.png`
- App Designer 权限态：`screenshots/b7-app-designer-project-390x844.png`
- App 375×667：`screenshots/b7-app-project-375x667.png`

## 4. 边界与未闭环

- 本轮为开发自测/UI 走查，不是 B8 独立验收；不得据此宣称 B8 `ACCEPTED`。
- 微信开发者工具本轮未完成 automator 自动化；MP 构建通过，H5 390×844/375×667 真实渲染已完成。DevTools 逐页走查留 B8 独立验收。
- 助手保持单轮问答，不保存多轮记忆；审计只记录调用元数据，不保存问题/回答正文。
- 临时作业单 `1792246721730889455` 已删除，材料计划恢复 `ReadyToOrder`；测试账号密码未变更；未执行 DDL、迁移、生产部署或正式小程序提审/发布。

## 5. 复跑

```bash
B7_SHEET_ID=<temporary-sheet-id> node docs/product/domains/material/evidence/B7/run-b7-verification.mjs
```

脚本从 `B7_PASSWORD` 读取登录口令；未设置则拒绝运行，不写入证据。
