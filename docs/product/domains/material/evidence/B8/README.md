# 材料域 B8 · 助手 UI 聚焦独立验收

- 日期：2026-10-17
- 性质：独立黑盒验收；未修改任何实现代码。
- 结论：**PASS**（110 / 110，0 FAIL，0 BLOCKED）。
- 最终正式 `ACCEPTED` / 发布：仍由用户决定；本证据只证明已满足进入用户决策的证据条件。

## 1. 精确提交与环境

| 端 | 提交 | 干净 worktree |
| --- | --- | --- |
| 后端 | `302193eef30bbe2341148394d3679c23ec2c84bd` | `/tmp/th4-b8-backend-302193e` |
| App | `67e6da0f310d5f209ad0f739d7826b314a87d990` | `/tmp/th4-b8-app-67e6da0` |
| Web | `d15d088074d28c3c1eb1530fd0f9a58abe8cbedb` | `/tmp/th4-b8-web-d15d088` |

环境：本地 dev，MySQL `127.0.0.1:3306/decoboon`、后端 `http://127.0.0.1:8091/api`、Web `http://127.0.0.1:4175`、App H5 `http://127.0.0.1:8080`；不连接生产。

测试账号：Boss `13910000100`、店长 `13910000110`、Designer `13910000111`、PM `13910000113`、Finance `13910000114`。本地口令失效后临时重置哈希，验收结束后已恢复原哈希；原哈希只保存在临时目录，未写入仓库。

## 2. 结果总览

| 类别 | 总数 | PASS | FAIL | BLOCKED |
| --- | ---: | ---: | ---: | ---: |
| API / 只读边界 | 29 | 29 | 0 | 0 |
| Web 真实 UI | 34 | 34 | 0 | 0 |
| App H5 真实 UI | 26 | 26 | 0 | 0 |
| 跨端一致性 | 5 | 5 | 0 | 0 |
| 微信开发者工具 automator | 11 | 11 | 0 | 0 |
| 只读业务表回归 | 1 | 1 | 0 | 0 |
| 清理 | 4 | 4 | 0 | 0 |
| **合计** | **110** | **110** | **0** | **0** |

- `summary.json`：总结、环境、提交、类别、观察项；
- `verification-results.json`：110 项逐项结果；
- `defects.json`：空数组，无新缺陷；
- `golden-question-results.json`：B4 黄金问题 50/50；
- `api-samples.json`：项目/作业单/上下文/权限/幂等/清理响应样本；
- `ui-metrics.json`：视口、横向溢出、Console、样式和键盘代理指标；
- `devtools-results.json`：微信开发者工具 automator 真实结果；
- `screenshots/`：34 张真实渲染截图。

## 3. API 与只读边界

- `suggestions` 的 general/project/purchase/settlement/sheet 五类上下文均来自同一 `material-golden-questions.json`；50 个黄金问题 50/50 意图匹配。
- `ask` 返回 answer/citations/toolCalls/nextActions/readOnly/auditStatus；项目与作业单回答均 `readOnly=true`、`auditStatus=RECORDED`。
- Boss/店长/Finance 可见材料应付、实际成本和成本不完整时的 `利润：null`；Designer/PM 只看履约状态，回答不含成本金额、供应商价格或 `利润：null`。
- Boss 跨企业项目、Designer 跨项目、伪造作业单均返回 404；伪造 `companyId/empId/ownerId` 不改变登录身份，审计仍记录 Designer 与其所属企业。
- REFUSAL、NO_EVIDENCE、PERMISSION_DENIED、空白/超长/未登录请求均按服务端原样处理。
- 同一 `requestId + idempotencyKey` 两次返回同一 `auditId`，数据库仅 1 条；审计文本字段不保存问题或回答正文。
- 助手问答前后及 Web/App/DevTools 问答后，材料计划、作业单、采购、到场、结算、应付和供应商表计数不变。

## 4. Web 真实 UI

覆盖 `/finance/projects/:projectId`、`/finance/material-ordering`、`/finance/material-settlement`：

- 1440×900 与 1024×768 均无横向溢出；
- 显示项目/采购/结算上下文、服务端建议问题、回答、证据、只读工具、下一步、审计状态；
- 建议问题可点击发送；加载态、REFUSAL、NO_EVIDENCE、清除/恢复上下文、请求失败/重试均实测；
- Esc 关闭、焦点进入抽屉并在关闭后回到「问材料」按钮；
- 长回答 + 18 条注入压力证据无破版、无横向溢出（注入数据只用于渲染压力，不代表业务事实）；
- 助手抽屉无审批/付款/删除/改权限按钮；
- Designer 访问财务项目路由时无助手入口，也不显示实际成本/材料应付；
- 业务 Console Errors 0；故意注入 500 的网络错误单独记录，不计产品缺陷。

## 5. App H5 真实 UI

- 390×844：项目详情、真实作业单详情、主材结算；
- 375×667：项目详情；
- Designer 权限态：入口可见但只返回履约口径，不显示成本金额或 `利润：null`；
- 上下文、服务端建议、回答、证据、只读工具、下一步、审计、拒答、错误重试、清除/恢复均实测；
- 无横向溢出；面板和输入区位于视口内；
- 375×430 缩小视口模拟键盘后 composer 仍完整可见（**这是布局代理证据，不是物理键盘/真机键盘证据**）；
- 长回答 + 18 条注入压力证据无破版、无横向溢出；
- 业务 Console Errors 0，故意注入 500 单独记录。

## 6. 微信开发者工具

- 使用干净 App worktree 的 `dist/dev/mp-weixin`；
- `miniprogram-automator@0.12.1` 启动成功，真实系统尺寸 **375×667**，SDK `3.17.4`；
- 真实进入项目详情，点击「问材料」，选择服务端建议问题并发送；
- 真实进入作业单详情，验证上下文含真实 `TILE-*` 单号；点击建议问题并额外发送自由输入；
- 项目与作业单回答均显示证据、只读工具、下一步、只读、审计和成本 null 口径；
- 4 张模拟器截图；业务 Console Errors / exception 0；
- DevTools 仅有 4 条非业务 warning：`wx.getSystemInfoSync` 弃用提示、`app.json` 不识别 `versionName/versionCode`。

## 7. 跨端一致性

同一项目与同一临时作业单分别通过 API、Web、App、微信开发者工具提问：

- 三端 answer 文本归一化后完全一致；
- 项目状态/计数/阶段/下一步一致：材料计划 2 条、待下单 1 条、待到货 1 条、选材/现场确认、下一步“设计师完成选材并交付财务”；
- 作业单均引用同一 `TILE-20261017-001`；
- 项目 citations 一致包含 `CHK-UNSELECTED`、`CHK-UNPRICED`、`CHK-COST-INCOMPLETE`；作业单包含 `R-SHEET-NO-PRICE`；
- 成本可见性随角色一致，App/Web 均未另写一套文案事实。

## 8. 观察项与未闭环风险

- `OBS-B8-001`（P3，非阻塞）：`suggestions` 对任意 `projectId/sheetId` 只作为问题分组依据，不读取项目、成本或供应商数据；越权对象访问由 `ask` 统一 404/403。未发现数据泄漏，但建议接口没有对象身份校验。
- Web/App 的 UI 重试会生成新的 `requestId/idempotencyKey`。API 审计幂等已通过；若“服务端已处理但响应丢失”后用户点重试，可能新增一条审计调用，不产生业务写操作。当前不作为 B8 失败项。
- 物理键盘/真机软键盘遮挡未在实体手机上验证；已用 H5 缩小视口和 DevTools 375×667 做布局代理，不能替代真机证据。
- 首轮 Vite 出现过 `504 Outdated Optimize Dep` 瞬态，重试后通过；不属产品缺陷。

## 9. 清理

- 临时作业单已通过 `cancel → delete` 删除，材料计划恢复 `ReadyToOrder`，项目经理恢复 `NULL`；
- 业务表最终计数与预期基线一致（作业单/作业单行回到 0，采购 18、采购行 18、到场 18、应付 37、供应商 2）；
- 临时密码哈希已恢复；
- 验收后停止后端、Web、App、MySQL，移除临时 worktree 与 node_modules 软链接；
- 未执行 DDL/迁移、生产发布或正式小程序提审/发布；未修改实现代码。

## 10. 复跑

```bash
B8_PASSWORD='<local-test-password>' \
B8_DB_PASSWORD='<local-db-password>' \
B8_API='http://127.0.0.1:8091/api' \
B8_WEB='http://127.0.0.1:4175' \
B8_APP='http://127.0.0.1:8080' \
node docs/product/domains/material/evidence/B8/run-b8-verification.mjs
```

DevTools 单独脚本：

```bash
B8_PASSWORD='<local-test-password>' \
B8_SHEET_ID='<temporary-sheet-id>' \
B8_DEVTOOLS_PROJECT='/tmp/th4-b8-app-67e6da0/dist/dev/mp-weixin' \
node docs/product/domains/material/evidence/B8/run-b8-devtools.mjs
```
