# B3 App 业务体检与页面上下文证据

日期：2026-10-17

环境：

- App：uni-app dev build，微信开发者工具自动化（`dist/dev/mp-weixin`，automator `ws://127.0.0.1:9420`）；
- H5：uni-app dev server，390×844，用于补充失败态与横向溢出检查；
- 后端：本地 dev 8090，真实 Provider 与造数企业 M 数据；
- 角色：企业负责人（Boss，Company 数据范围）。

## 真实数据

- 项目：`235520576473530368`（王芳（造数1100） · 望江府1栋102）。
- 材料体检来自 `blueprint/check/report`：2 条材料计划、1 条已选材、1 条待下单、2 条待到货/部分到场、1 条未完成选材、1 条采购未定价、成本未完整。
- 主材作业单：本地创建临时 Tile 草稿 `TILE-20261017-001` 做页面走查；证据采集后已删除。
- 帮助主题：`material-sheet-time`，来自 `src/content/helpCenter.ts`。

## 截图

| 场景 | 文件 | 说明 |
| --- | --- | --- |
| 项目材料体检 | `b3-app-project-material-health.png` | 微信开发者工具真机尺寸 375×599；阶段、指标、成本不完整、校核项和聚焦行 |
| 项目材料体检空态 | `b3-app-project-material-health-empty.png` | 真实无材料计划项目；明确“无法确认材料履约是否已开始” |
| 项目材料体检失败态 | `b3-app-project-material-health-error-h5.png` | H5 网络拦截 500；显示“材料业务暂不可确认”和重试 |
| 作业单上下文 | `b3-app-material-sheet-context.png` | 测量/配送/安装/到场口径、未定价、模板必填、取消边界 |
| 建单页来源说明 | `b3-app-material-sheet-create-guide.png` | 材料行来源、合同外走增减项、作业单不承载价格 |
| 帮助主题 | `b3-app-help-time-semantics.png` | 作业单时间语义的帮助解释 |

OCR 文本：`ocr-text.txt`。

## 交互与布局

- 微信开发者工具：项目详情、作业单详情、建单页均成功加载并截图；
- 项目材料体检卡“材料计划”按钮可从 H5 实际导航到 `/subpackages/project/pages/material-plan?projectId=...`；
- 作业单详情显示真实模板必填提示“当前品类模板把「测量时间」设为必填”；
- 作业单详情显示真实未定价提示“当前采购侧存在未定价或部分结算，成本不完整”；
- 建单页显示行来源、模板字段与价格边界说明；
- H5 390×844：项目详情、作业单详情、建单页 `scrollWidth = innerWidth = 390`，无横向溢出；
- 正常页面 Console Errors = 0（H5 登录页/第三方 uni-h5 运行时有历史噪音，不计入本批业务页面）。

## 数据与账号边界

- 临时作业单仅用于本地 dev 页面走查，采集后已删除；
- 本地造数账号密码哈希临时重置后已恢复原值；
- 不涉及生产、权限、金额/成本语义、DDL 或正式小程序发布。
