# 2026-10-24 设计师五阶段 App/Web 统一

> 状态：`ACCEPTED / TRIAL_SINGLE_PERSON`  
> 性质：试运营单人模式下完成的只读验收门禁，不等同第三方独立验收，不代表生产发布。  
> 范围：待我跟进、待我量房、待我设计、待我谈单、待我签约的名称、顺序、筛选语义、关键词检索与分页交互。

## 1. 统一契约

| 阶段 | Web 入口/页面 | App 入口/页面 | Provider | App 分页 |
| --- | --- | --- | --- | --- |
| 待我跟进 | 待我跟进 | 待我跟进 | `customer/my-customer/overview` | 下拉刷新 + 触底加载 |
| 待我量房 | 待我量房 | 待我量房 | `statistic/designer/measure/page` | 下拉刷新 + 触底加载 |
| 待我设计 | 待我设计 | 待我设计 | `statistic/workbench/design/page` | 下拉刷新 + 触底加载 |
| 待我谈单 | 待我谈单 | 待我谈单 | `statistic/workbench/quote/page` | 下拉刷新 + 触底加载 |
| 待我签约 | 待我签约 | 待我签约 | `statistic/workbench/sign/page` | 下拉刷新 + 触底加载 |

App 五个入口按上述顺序出现在设计师首页。Web 继续使用分页组件；App 不展示上一页、下一页、页码或总页数。

## 2. 筛选与检索

| 阶段 | 统一筛选项 | 关键词字段 |
| --- | --- | --- |
| 待我跟进 | 全部待办 / 已逾期 / 今日跟进 / 3 天内 / 未安排 | `keyword` + `followFilter` |
| 待我量房 | 全部任务 / 逾期 / 今天 / 已预约 / 待预约 / 待交设计 | `keyword` + `state` |
| 待我设计 | 全部 / 待设计 / 设计中 / 方案完成 | `keyword` + `filter` |
| 待我谈单 | 全部任务 / 草稿 / 待客户确认 / 需调整 | `keyword` + `filter=all|draft|submitted|rejected` |
| 待我签约 | 全部 / 待发起合同 / 待企业确认 / 已生效 | `keyword` + `filter=all|toCreate|waitingConfirm|effective` |

App 待我谈单、待我签约在本轮补齐关键词搜索；首屏、下拉刷新和触底加载都携带同一 `keyword` 与筛选值，禁止只在已加载数组内过滤。

## 3. 本轮代码变更

- App 设计师入口：
  - `待我报价` → `待我谈单`
  - `合同签约` → `待我签约`
- App 页面标题：
  - 设计师设计队列：`设计工作台` → `待我设计`
  - 报价队列：`报价工作台` → `待我谈单`
  - 设计师签约队列：`我的合同签约` → `待我签约`
- App 待我量房：`全部` → `全部任务`。
- App 待我谈单：筛选值改为与 Web 相同的 `all/draft/submitted/rejected`，增加关键词搜索。
- App 待我签约：筛选值改为与 Web 相同的 `all/toCreate/waitingConfirm/effective`，增加关键词搜索。
- 谈单、签约筛选条支持横向滚动，避免窄屏折行或挤压。

## 4. 真实验证

环境：

- 后端：`127.0.0.1:8090`，dev profile。
- App：微信开发者工具原生模拟器，自动连接 `ws://127.0.0.1:9430`。
- Web：真实 Chromium，`1440×900`，`127.0.0.1:8080`。
- 身份：`18660219918 / 张伟 / Designer / 安徽昭青网络科技`，App/Web 同一用户。
- 仅执行重新进入页面、输入搜索、切换筛选和截图，无业务写操作。

结果：

- App 首页五个入口顺序正确。
- Web 侧栏五个入口顺序正确，五页页面标题分别为 `待我跟进 / 待我量房 / 待我设计 / 待我谈单 / 待我签约`，与控制台文档标题一致。
- App 与 Web 的五个页面标题、筛选文案统一。
- 五页均未发现 `待我报价`、`我的合同签约`、`报价工作台`、`设计工作台`、上一页、下一页、总页数或 `第 x / y 页`。
- 待我谈单搜索 `周先生` 返回空队列，证明不是只过滤当前已加载报价。
- 待我谈单筛选：草稿显示 `Q1790383677036`；待客户确认、需调整为空。
- 待我签约搜索 `王亮` 仅显示王亮。
- 待我签约筛选：待发起合同仅显示周先生；待企业确认为空；已生效仅显示王亮。
- App 自动检查 24/24 PASS，0 error。
- Web 自动检查 20/20 PASS，Console error 0。
- 两端样本一致：跟进、量房、设计、谈单、签约均命中同一批业务对象。

证据：

- App JSON：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/output/playwright/20261024-designer-five-stage-unified/app-unified-audit.json`
- App 脚本：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/scripts/audit-designer-five-stage-unified-20261024.cjs`
- App 截图：同目录 `followup.png`、`measure.png`、`design.png`、`quote.png`、`sign.png`、`quote-search.png`、`sign-search.png`。
- Web JSON/截图：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-web/output/playwright/20261024-designer-five-stage-unified/`，含 `web-unified-audit.json`、`web-followup.png`、`web-measure.png`、`web-design.png`、`web-quotes.png`、`web-contract.png`。
- 汇总结论：`acceptance-summary.json`。

## 5. 构建与门禁

- App：`npm run type-check` PASS。
- App：`npm run check:ui` PASS。
- App：`npm run check:client-copy` PASS。
- App：`npm run check:navigation` PASS。
- App：`npm run check:pagination` PASS，无新增大页固定值。
- App：`npm run build:mp-weixin:release` PASS；主包 1,034,871 B，最大分包 726,052 B，总包 4,032,308 B。
- Web：`npm run build` PASS。
- Web：`npm run check:workbenches`、`check:workspaces`、`check:permissions` PASS。

## 6. 边界与下一步

- 本轮没有后端 Provider、权限、状态机、金额或数据库变更。
- Web 首次登录遇到 dev 后端热重启窗口，重启完成后已用同一设计师账号重新登录并完成五页真实浏览器复核。
- 试运营单人模式验收已通过；第三方独立验收未执行，不冒充独立验收结论。
- 分页边界沿用 DQ-X1 的第 2 页/触底证据：Web 26/27 条、App 26/27 条；本轮未新增业务数据。
- 小程序未上传、未提审、未发布；正式版仍由用户本人操作。

## 7. 验收结论

| 用例 | App | Web | 结果 |
| --- | --- | --- | --- |
| 五阶段入口顺序 | PASS | PASS | PASS |
| 五页标题与文档标题 | PASS | PASS | PASS |
| 跟进筛选语义 | PASS | PASS | PASS |
| 量房筛选语义 | PASS | PASS | PASS |
| 设计筛选语义 | PASS | PASS | PASS |
| 谈单筛选与关键词 | PASS | PASS | PASS |
| 签约筛选与关键词 | PASS | PASS | PASS |
| App 无页码控件 | PASS | 不适用 | PASS |
| Web 分页组件 | 不适用 | PASS | PASS |
| 同一业务样本跨端一致 | PASS | PASS | PASS |
| Console error | 不适用 | 0 | PASS |

结论：`PASS`。本轮无业务写操作、无数据库变更、无权限/金额/状态语义变化。下一步可进入用户验收或发布准备，但生产发布和小程序正式版仍须用户明确确认。
