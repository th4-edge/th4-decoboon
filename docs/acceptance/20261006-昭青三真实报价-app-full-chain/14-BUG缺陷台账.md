# BUG / 缺陷台账（三真实报价轮次）

| ID | 类别 | 严重度 | 状态 | 标题 | 证据 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| XA-20261006-001 | SEC | P1 | FIXED_PENDING_VERIFY | dev 构建产物 apiBase 指向生产 | 全角色轮次 `raw/preflight.json` | 已重建为本地 127.0.0.1:8090；建议纳入构建门禁 |
| XA-20261006-002 | BUG | P2 | FIXED_PENDING_VERIFY | iOS 日期解析致跟进/到期判断失真 | 全角色轮次 console（435 条） | 已修复（commit `1afbb7a`），复测 435→0 |
| XA-20261006-005 | DATA/BIZ | P2 | OPEN | 客户来源静默落「其他」 | API 探针 `source.key=Other` | 疑前端 sourceMap 与后端枚举名不匹配 |
| XA-20261006-006 | UX | P3 | OPEN | 客户保存 loading 与 hideLoading 配对告警 | `raw/case1-customer.json` console | 保存成功但控制台告警「showLoading 与 hideLoading 必须配对使用」 |
| XA-20261006-007 | UX | P3 | OPEN | 保存成功后停留在编辑页（无返回栈时） | `raw/case1-customer.json` | `setTimeout(navigateBack)` 在无上页时失败；建议 fallback 到列表 |
| XA-20261006-008 | BIZ/规范 | P2 | 记录（正向） | 未指定设计师时设计师不可见客户 | `raw/probe-design.json` | 数据权限按预期拒绝，属正确行为；同时说明全链路必须先完成「店长指定设计师」交接 |

## 阻断（BLOCKED / NOT_TESTED）

| ID | 范围 | 状态 | 原因 | 解除条件 |
| --- | --- | --- | --- | --- |
| BLOCK-Q-001 | 真实报价 Excel 逐行导入 | `LINE_IMPORT = NOT_TESTED / GAP` | `quote/workbook/import` 只解析 App 自有工作簿（设计师范式 + 草稿态），非装企原始 `.xlsx` | 产品确认；否则按约定用正式页面录入代表明细 |
| BLOCK-Q-002 | 报价按分类多行录入 | 未完成 | 自定义报价需先经「设计区域 + 设计分项」；设计分项价格由材料库主数据决定（不接受手改价），需按 `价格×数量` 逼近分类小计 | 继续执行：指派设计师→量房→方案→报价 |
| BLOCK-Q-003 | 业主确认/验收 | BLOCKED_NEEDS_DEVICE（待验证 devToken 可行性） | 需客户身份；计划用本地客户 devToken 标 `DEVTOKEN_SIMULATED` | 若构建无法切换客户身份则需真机/独立业主凭证 |
| BLOCK-Q-004 | 店长指定设计师 | NOT_TESTED | 本轮未执行（本轮止于设计师负向权限验证） | 以店长王洪文在客户详情执行「指定设计师」 |

## 说明

本轮**未推翻**任何此前结论；三案例四层金额与验收链均未实测，故不得写 PASS。

## 本轮新增

| ID | 类别 | 严重度 | 状态 | 标题 | 证据 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| XA-20261006-009 | BUG/权限 | P1 | OPEN | 店长无法使用「报价项目库维护」页 | `raw/probe-catalog2.json`(店长空白) vs `raw/probe-catalog3.json`(Boss 正常) | 权限矩阵 `storeManager` 含 `edit:catalog`，后端 `requireMaintainer` 也允许店长，但 App 既无入口、深链又空白；导致店长无法按既定路径维护固定总价条目 |
| XA-20261006-010 | DOC/BIZ | P2 | 记录 | 报价项目库维护无店长入口 | `raw/probe-catalog4.json` | 报价工作台无「项目库」入口，仅 Boss 工具条内含批量导入 |

> 影响：本轮要求的「店长/设计师创建固定总价主数据项」在 **店长身份下不可行**；Boss 可打开该页。
> 金额四层对账因此未完成，属 `FAIL/GAP`，未伪造。

## 本轮补充（正向/规则）

| ID | 类别 | 严重度 | 状态 | 结论 |
| --- | --- | --- | --- | --- |
| XA-20261006-011 | BIZ/规范 | — | 记录（正向） | `customer/customer/handoff` 强制要求客户**已跟进**（未跟进报「客户尚未跟进或已流失，不能指定设计师」）。判定为合理的行业规范约束：先建立跟进事实再分配设计师。 |
