# 2026-10-23 设计师 Web 队列真实页面复核

> 状态：`UI_REVIEWED_PARTIAL / ACCEPTANCE_PENDING`  
> 性质：研发侧真实浏览器复核，不是独立验收，不代表生产可用或已发布。  
> 范围：DAW-FIX-02（待我谈单、报价历史、待我签约）及复核中发现的“下一步”同源偏差。  
> 不包含：设计模板 App、阶段验收全模式、201+ 压力、Dormant UI 排除、其他角色能力改造。

## 1. 环境

| 项 | 值 |
| --- | --- |
| 后端 | 当前工作树 `target/classes`，dev profile，`127.0.0.1:8090` |
| Web | Vite dev，`127.0.0.1:8080` |
| DB | 本机 MySQL 8.4 用户级快照，`127.0.0.1:3306/decoboon` |
| 浏览器 | Playwright CLI，真实 Chromium |
| Designer | 董夏鹏（本地快照临时测试凭证，复核后已恢复原哈希） |
| StoreManager | 王宏文（本地快照临时测试凭证，复核后已恢复原哈希） |
| 生产边界 | 未连接生产、未写生产数据、未部署、未提交 |

## 2. 验收结果

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| `/designer/quotes` 标题与拆分 | PASS：标题「待我谈单」，说明明确历史版本进入报价历史 | `designer-quotes-mixed-1440.png` |
| 待我谈单 Provider | PASS：只请求 `statistic/workbench/quote/page` | 浏览器网络记录 |
| Draft/Submitted 集合与计数 | PASS：草稿 1、待客户确认 1；无 Accepted/Converted | `raw/` 与页面快照 |
| 服务端风险与下一步 | PASS：草稿 17 天 Danger/继续编辑；提交 4 天 Danger/跟进客户确认 | 页面快照 |
| 筛选/搜索/选中联动 | PASS：草稿筛选仅保留草稿；搜索命中/空态正确；切换任务后详情同步 | Playwright 交互记录 |
| 进入报价工作区 | PASS：路由到 `/designer/quotes/990000000000000001` | 浏览器 URL/网络记录 |
| `/designer/quote-history` Provider | PASS：只请求 `quote/designer/page` | 浏览器网络记录 |
| 服务端分页 | PASS：请求 `{current:1,size:20}`；23 条返回 20+3，两页；第 2 页请求 `current:2` | `raw/designer-quote-history-page*.json` |
| 状态筛选 | PASS：请求携带 `status=Draft`，总数 5，返回状态全部为 Draft | `raw/designer-quote-history-draft.json` |
| 搜索 | PASS：搜索条件回源服务端，命中项与空态正确 | Playwright 交互记录 |
| `/designer/contract` Provider | PASS：只请求 `statistic/workbench/sign/page`，不再批量请求通用报价页 | 浏览器网络记录 |
| 待发起合同 | PASS：无合同报价显示「发起合同」/`createContract` | `designer-contract-pending-1440.png` |
| 选中单合同 | PASS：选中后仅读取当前 `project/contract/info` 与合同工作台摘要，无 200 条 N+1 | 浏览器网络记录 |
| PendingSign | PASS：显示「待企业确认签约」/`waitEnterpriseSign` | 页面快照 |
| Signed | PASS：显示「待企业确认生效」/`waitEnterpriseEffective` | 页面快照 |
| Effective 有项目 | PASS：Provider 返回「查看项目」/`viewProject` | `raw/storemanager-sign-page.json` |
| Effective 无项目 | PASS（修复后）：Provider 返回「合同已生效待项目建立」/`waitProjectCreated`，页面同文案 | `raw/designer-sign-effective-after-fix.json` |
| StoreManager 本店队列 | PASS：本店 2 条；跨店临时报价未返回 | `raw/storemanager-quote-queue.json` |
| StoreManager 报价历史 | PASS：本店 8 条，跨店搜索为空 | `raw/storemanager-quote-history.json` |
| StoreManager 签约 | PASS：本店 6 条；跨店 Accepted 未返回 | `raw/storemanager-sign-page.json` |
| 1440/1024 布局 | Designer 队列、历史、签约 1024 无横向溢出；1440 截图已采集 | `ui-checks.json` |
| Console | 0 error；仅 1 条既有 warning | Playwright console |

## 3. 本轮发现并修复

### DAW-FIX-02-UI-01：Effective 无项目时下一步前后端不一致

- 修复前：Provider 返回「查看合同」/`viewContract`，页面自行推导「合同已生效待项目建立」。
- 风险：违反“下一步由服务端给，前端只展示”的跨端同源约束；后续接口调整时页面会漂移。
- 修复：
  - 后端 `DesignerWorkbenchRisk.sign` 对“合同已生效但项目缺失”返回 `waitProjectCreated`，文案「合同已生效待项目建立」；
  - Designer 的只读主行动改为消费 `SignWorkbenchItem.nextActionKey/nextActionLabel`；
  - 增加单元断言。
- 复验：Provider 与页面文案一致；1024/1440 无溢出；console 0 error。

## 4. 验证命令

```text
mvn -Dtest=DesignerWorkbenchRiskTest test                  PASS（2/2）
npm run type-check                                         PASS
npm run build                                              PASS
npm run check:actions                                      PASS
npm run check:permissions                                  PASS
npm run check:workbenches                                  PASS
npm run check:workspaces                                   PASS
```

App 阶段验收最小切片沿用既有证据：

```text
mvn -Dtest=ProjectMemberSwitchAndStageOpenTest test         PASS（17/17）
App npm run type-check                                     PASS
App npm run build:mp-weixin                                PASS
App check:action / check:contract-sign / check:navigation  PASS
```

## 5. 临时夹具与清理

为覆盖分页、状态流转和跨店隔离，仅在本地 MySQL 快照中创建临时报价、客户、门店和合同状态。复核后已完成：

- 临时报价剩余：0；
- 临时客户剩余：0；
- 临时门店剩余：0；
- 原始报价 `233178252326207488`、`235809576165834752` 已恢复；
- 原始合同 `233179575582982144` 已恢复（含原 quote/project/owner/status/时间）；
- Designer 与 StoreManager 凭证哈希、失败计数、锁定和更新时间已按本地备份恢复；
- 浏览器 cookie/storage 已清空。

## 6. 仍未覆盖

1. 201+ 条的真实压力与截断验收；本轮 23 条已证明服务端分页链路，但未达到 201+ 门槛。
2. Dormant 客户在队列中的 UI 排除证据。
3. 独立验收；研发侧页面复核不能替代独立责任方验收。
4. DAW-FIX-01 的 `None / ConstructionDone / 空 acceptanceMode` 全模式完成路径。

## 7. 截图

截图目录：`output/playwright/20261023-designer-web-acceptance/`

关键文件：

- `designer-quotes-mixed-1440.png`
- `designer-quotes-1024.png`
- `designer-quote-history-paged-1440.png`
- `designer-quote-history-1024.png`
- `designer-contract-pending-1440.png`
- `designer-contract-effective-1440-after-fix.png`
- `designer-contract-effective-1024-after-fix.png`
- `storemanager-quotes-1440.png`
- `storemanager-quote-history-1440.png`
- `storemanager-contract-1440.png`
