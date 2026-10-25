# 装得清产品体系基线 · 设计—蓝图—实现—页面—验收对账

状态：`IN_PROGRESS`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B0-01 | 五维角色模型 | `BR-028`、`OBJ-001` | 待 B1 冻结 | 不涉及 | 角色矩阵 v0.1 | 文档核对 | 待业务确认 | PENDING |
| REQ-B0-02 | 七类固定员工角色 | 后端 `EmployeeRole`、项目 `AGENTS.md` | 待 B1 冻结 | `EmployeeRole` | App/Web 类型 | 键值人工对账 | App/Web/后端 7 键一致 | PASS |
| REQ-B0-03 | 角色 × 16 段业务责任 | `CAP-006`、`BR-001/002/028`、`DEC-074` | 待 B1 冻结 | 不涉及 | 01a v0.2 责任矩阵 | 16 行覆盖检查 | 材料/履约已归 Finance；其余待确认格有来源或标记 | PENDING |
| REQ-B0-04 | App/Web 角色落位 | App/Web 角色代码 | 待 B3/B5 | 不涉及 | 01a §5 | 静态代码盘点 | 6 目标角色 + TeamLeader 历史兼容 + 业主已覆盖 | PASS |
| REQ-B0-05 | 未决角色与权限问题 | `GAP-026/058/060` | 待 B1 | 不涉及 | 01a v0.2 §5.3、02 | T05 形成 | 材料、跨端、多岗已确认；高风险动作和外部身份待确认 | PENDING |
| REQ-B0-06 | 不越权修改 | 用户指令、`AGENTS.md` | 不涉及 | 无代码/无迁移 | 无代码变更 | Git diff 审核 | 仅产品文档变化 | PASS |
| UD-01 | 材料/履约归财务 | `DEC-074`、迁移 50 | 角色节点 | `EmployeeRole` 不变 | App/Web 材料与财务页 | 静态核对 | 无第八角色 | PASS |
| UD-02 | App/Web 强制能力一致完整 | 用户 2026-10-22 裁决 | 待 B3 能力矩阵 | 后续对账 | 01a v0.2 §5.3/5.4 | 差异盘点 | 所有强制能力必须双端 complete | IN_PROGRESS |
| UD-03 | 普通员工不支持通用多岗 | 用户 2026-10-22 裁决 | 角色模型冻结 | `EmployeeRole` 单值不变 | 角色矩阵 v0.2 | 底层代码核对 | 单主岗 + BOSS 超集 + 项目责任关系 | PASS |
| UD-04 | TeamLeader 角色冻结 | 用户 2026-10-22 裁决 | 目标角色排除 | 枚举暂保留 | 角色矩阵/跨端范围 | 静态核对 | 不新增应用能力；存量转 PI-002 | PASS |

| REQ-EOP-01 | 五类资产生命周期分层 | `DEC-083`、`BR-051` | `EOP-V1` | 待实现 | 现有五类资产页 | 待 EOP-V1 | 待独立验收 | BIZ_ALIGNED |
| REQ-EOP-02 | 套餐不可变版本与报价来源版本 | `DEC-083` §3、`BR-051` §2–3 | `EOP-V2` | `pack/*`、`quote/*` | App 套餐管理；Web 缺失 | 待研发 | 待独立验收 | BIZ_ALIGNED |
| REQ-EOP-03 | 付款/施工模板不可变启用版本 | `DEC-083` §4、`DEC-048` | `EOP-V3` | `plan/*`、`contract/*` | App 有管理；Web 缺失 | 待研发 | 待独立验收 | BIZ_ALIGNED |
| REQ-EOP-04 | 报价项目库 revision 与来源修订 | `DEC-083` §2、`DEC-079` | `EOP-V4` | `catalog/*`、`quote/*` | App/Web 报价项目库 | 待研发 | 待独立验收 | BIZ_ALIGNED |
| REQ-EOP-05 | 单据模板状态分层与历史回读 | `DEC-020`、`DEC-083` §5 | `EOP-V5` | `document/*` | App/Web 凭证模板 | 待研发 | 待独立验收 | BIZ_ALIGNED |
| REQ-EOP-06 | Web 三类模板管理闭环 | 用户 2026-10-22 双端裁决 | `EOP-V6` | 待实现 | 套餐/付款/施工 Web 缺失 | 待研发 | 待独立验收 | BIZ_ALIGNED |

## 未映射

- 高风险业务动作完整主体矩阵：待业务确认。
- 业主、工人、供应商自助身份与业主 Web 入口：待业务确认。
- App/Web 强制能力一致完整已确认，具体差距转入 B3 对账。

## 已过期

- 业务审查文档中的 `ProjectSupport` 未在现行 `EmployeeRole` 中出现；暂标记为过期/待修订，不能据此新增角色。

## 未决业务问题

- T05：`REQ-BIZ-20261022-001-角色体系与跨端职责边界.md`
- `GAP-058` G15 全角色 × 全领域数据权限矩阵统一卡
- `GAP-026` TeamLeader 历史兼容（不再阻塞目标应用）
- `GAP-060` 店长工资单确认/作废边界
- `GAP-WEB-019` PM 主材选材 Web 入口

## 本批结论

- 已对齐：七类持久化 `EmployeeRole` 兼容、六类目标角色、TeamLeader 冻结、材料/履约归 Finance、BOSS 能力超集、店长本店业务能力、财务门店/总部范围、合同签约与生效主体、工资单确认主体；App/Web 强制能力一致完整已由用户裁决。
- 待对齐：高风险动作完整主体矩阵、外部身份与业主 Web 边界、跨端能力缺口逐项补齐。
- 下一批：继续收敛高风险动作主体和外部身份边界；同时形成 B3 跨端能力差集清单。

## B2C-1 统一对象视图一期合并裁决（PI-20261022-010）

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-UOV-01 | REGION/TYPE 同源双投影 | `BR-052` §1、`DEC-086 §2` | `UnifiedQuoteView` / `QuoteRegionProjection` / `QuoteTypeProjection` | U2 服务端双投影 PASS；U3 App/Web/业主消费同一服务端投影 | App/Web/业主 | U2 65/65；U3 第二次复验 18/18 + 三端构建 PASS | U2/U3 PASS | ACCEPTED |
| REQ-UOV-02 | 报价场景字段/动作裁剪 | `BR-052` §2、`DEC-086 §3` | `QuoteSceneContract` / `QuoteLineSourceRef` | U3 金额缺失、分组失败恢复与合同错误态已复验关闭 | App/Web/业主/PDF | 18/18 + App/Web type-check/build + 独立探针 PASS | U3/U4 PASS | ACCEPTED |
| REQ-UOV-03 | 合同上游事实与直接签约/承接 | `BR-052` §3、`DEC-086` §4 | `ContractFactChain` / `MeasureSummaryNode` | 上游/下游事实链、成本角色、承接标签和无项目契约已验证 | 合同页/App 合同摘要 | 26/26 + 独立探针 3/3 + App/Web 构建 PASS | U4 PASS | ACCEPTED |
| REQ-UOV-04 | 项目 PHASE 事实追踪 | `BR-052` §4、`DEC-086 §5` | `ProjectFactTrace` / `ProjectFactNode` | App PHASE ACCEPTED；Web PHASE 第二轮独立验收 PASS（历史锚点、材料摘要/路由闭合） | App/Web/业主项目页 | 后端 22/22 + App 9/9 + Web independent probes 35/35 + build/source guard PASS | U5C/U5D ACCEPTED | ACCEPTED |
| REQ-UOV-05 | PDF 分组与归档 | `BR-052` §5、`DEC-086` §6 | `QuotePdfArchiveContract` | `quoteId + version + groupMode`、文件名/页眉 | App/Web/业主 PDF | 版本/分组/旧归档待建 | 待独立验收 | PLAN_READY |

### 本切片边界

- 已对齐：5 个业务问题均有 T06/T12、`DEC-086`、`BR-052` 和证据链。
- 已冻结：`03-blueprint.json` 已建立对象/投影节点和场景绑定；`04-requirements.md` 已冻结 U1 验收条件；`05-plan.md` 已冻结 UOV-V1~V6。
- 已完成：UOV-V2 服务端双投影、UOV-V3 场景接线、UOV-V4 合同事实追踪、U5-C App PHASE、U5-D Web PHASE 均独立验收 PASS / ACCEPTED。
- 未完成：UOV-V5 的 U5-E 业主 journey 与 U5-F 全链验证、UOV-V6 PDF 全链与最终独立验收。
- 门禁：U1/U2/U3/U4/U5-C/U5-D PASS；U5-A/U5-B1/U5-B2 DEV_VERIFIED；U5-E 仅允许重新对账后准备，U5-F 未启动；不得宣称 U5 完成。
- 独立证据：U2 `docs/acceptance/runs/20261022-unified-object-view-u2-independent/README.md`；U3 `docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md`；U4 `docs/acceptance/runs/20261023-unified-object-view-u4-final-reacceptance/README.md`。
- `GAP-065` 保持打开；不因业务对齐而关闭实现缺口。
- 当前 `run-envelope.md` 仅授权只读盘点和文档沉淀；U1 编码需用户明确进入研发。

## B2C-1 U3 第二次独立复验 PASS（2026-10-23）

| ID | 消费面 | 规则/对象 | 当前实现 | 独立验收结果 | 证据/剩余风险 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U3-APP-01 | App 员工报价模型/API | `DEC-086 §2/§3` | 消费 `regionGroups/typeGroups/groupMode/source*`，保留原始 `itemId` | PASS | 无字段缺口 | ACCEPTED |
| U3-APP-02 | App 员工报价详情 | REGION/TYPE 同源、无前端合并/重算 | 服务端分组直出；行金额 null 显示“暂不可确认” | PASS | `QuoteItemRow.vue:53` 独立静态探针 | ACCEPTED |
| U3-APP-03 | 项目来源报价 | `PROJECT` 不返回原始 `items` | manager Provider 保留分组；入口跳统一报价详情 | PASS（静态） | 真实入口权限证据留 U6 | ACCEPTED |
| U3-OWNER-01 | 业主报价 journey | `BR-052 §2/§4`、`DEC-086 §3` | `applyForPrint`；小计/税额/总额判空；`hasSourceLines` 区分空清单与失败 | PASS | `JOURNEY_MONEY_NULL=null`、`MISSING_GROUPS` 探针 | ACCEPTED |
| U3-WEB-01 | Web 报价模型/API | `DEC-086 §2/§3` | 分组保持 undefined 语义；行金额 null 保持 null | PASS | 无字段缺口 | ACCEPTED |
| U3-WEB-02 | Web 报价工作表 | 服务端唯一分组、失败不伪空 | `toSheetGroups` 缺失时返回 `null`；页面显示暂不可确认并可重新读取 | PASS | REGION/TYPE 缺失探针均为 true | ACCEPTED |
| U3-CONTRACT-01 | 合同报价摘要/跳转 | 绑定 `quoteId + version`，不取最新报价 | 使用绑定 `item.quote.id/version`；错误/读取中状态统一 | PASS（静态） | 真实合同权限/UI 留 U6 | ACCEPTED |
| U3-PDF-01 | PDF 入口与版本参数 | `quoteId + version + groupMode`，默认 REGION | App/Web 调用与后端参数/版本校验贯通 | PASS（入口） | 真实渲染、归档回读留 U6 | ACCEPTED |

### 首轮问题关闭

| 原问题 | 复验结果 | 关键证据 |
| --- | --- | --- |
| `P1-01` 金额缺失补 0/重算 | CLOSED | App 行金额 null 保持；业主金额判空；journey `money(null)=null` |
| `P1-02` 分组缺失伪空态 | CLOSED | Web `toSheetGroups` 返回 null；App `hasSourceLines` + 分组失败态 |
| `P2-01` 合同读取失败伪待发起 | CLOSED | 队列/Hero/阶段/金额/签约进度错误态；`toCreate` 排除失败项 |

### 命令与证据

- 证据：`docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md`
- 复验快照：`/tmp/u3-reaccept.VBA8s3/{backend,app,web}`
- 快照摘要：`eaf5f3cd9b1258e9a171d6d66c49e2b49dfcb4caeaf0ea2babd1abb7a68737da`
- 后端 7 类定向 18/18 PASS；JDK22 package PASS（7.5 MB）
- App type-check + `build:mp-weixin` PASS；Web type-check + build + client-copy PASS
- 三端 diff、JSON、registry ID、领域账本 validate PASS
- 无 U3-owned DDL、权限放宽、锁、状态机或金额/成本/利润公式变化

### U3 最终门禁

- U3：**PASS / ACCEPTED**；P0/P1/P2=`0/0/0`
- 后续：U4 已完成最终独立复验 PASS；U5-A/U5-B1/U5-B2 `DEV_VERIFIED`；U5-C ACCEPTED；U5-D 首轮 FAIL 后第二轮独立验收 PASS / ACCEPTED
- 整体 `PI-20261022-010`：`DEV_IN_PROGRESS`
- `GAP-065`：OPEN；真实 UI、越权、PDF 归档与 `GAP-053` 仍留 U6

## B2C-1 U4 合同事实追踪独立验收 FAIL（2026-10-23）

| ID | 消费面 | 规则/对象 | 当前实现 | 独立验收结果 | 缺口/退回 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U4-UPSTREAM-01 | 合同上游事实 | 客户/房屋/量房/设计/报价/合同固定顺序 | `ContractWorkbenchContext.facts` 已实现上游节点和稳定顺序 | PASS（scoped） | 无 Project 时 `projectId` 边界待明确 | PASS / P2_OPEN |
| U4-MEASURE-01 | 最近已完成量房 | `completedAt desc, measureId desc`；区域/实测/待补 | 已过滤 Completed 并排序/计数；无记录进入 sourceGaps | PASS（scoped） | 真实非空区域证据留 U6 | PASS |
| U4-VERSION-01 | 绑定设计/报价版本 | `ProjectContract.quote` / `quote.design` | 沿绑定对象读取版本，未取最新 | PASS（scoped） | 无 | PASS |
| U4-ORIGIN-01 | 直接签约/承接 | 无设计显示“无设计方案来源”；承接标签冻结 | 直接签约缺口逻辑通过；`BusinessOrigin.getName()` 输出“接管基线”而非冻结标签 | **FAIL（P2-01）** | 使用冻结承接文案并补承接场景测试 | REJECTED_P2_01 |
| U4-DOWNSTREAM-01 | 下游摘要 | 项目/变更/材料/验收/应收/成本，只摘要 | 摘要节点已覆盖；Cost 节点无角色裁剪，Web 设计师交接直接渲染 amount | **FAIL（P1-02）** | 按 BOSS/店长/财务裁剪 Cost 及前端入口 | REJECTED_P1_02 |
| U4-UI-01 | App/Web 合同事实链 | 展示上游/下游，报价跳统一视图 | App/Web 事实卡接线，Web 报价跳转 | PASS（scoped） | 真实页面/权限证据留 U6 | MAPPED |
| U4-PREREQ-01 | 前置门禁 | U1/U2/U3 PASS 后 U4 | U3 第二次独立复验 PASS / ACCEPTED；U4 尚未重新对账 | PASS（前置） | U4 仅允许重新对账后准备，不得启动或宣称完成 | ALLOWED_PREPARE |

### U4 证据与结论

- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u4-independent/README.md`。
- 后端 scoped 测试 14/14 PASS、compile PASS；App type-check/check:document/build PASS；Web type-check/build PASS；U4-owned diff/DDL 审计 PASS。
- U4 技术上下文已有开发验证，但历史独立验收 **FAIL**；U3 第二次复验已 PASS/ACCEPTED，U4 仍仅允许重新对账后准备。历史 Cost 下游角色裁剪、承接标签、无项目 `projectId` 问题仍不得因 U3 PASS 而视为闭环。
- U5：**BLOCKED**；U4 修复并重新验收前不得启动；`GAP-065` 保持打开。

## B2C-1 U4 修复后第二轮独立验收 FAIL（2026-10-23）

| ID | 消费面 | 规则/对象 | 当前实现 | 独立验收结果 | 缺口/退回 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U4-PREREQ-02 | 前置门禁 | U3 必须先有独立 PASS 报告 | U3 PASS 报告已落盘：`docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md` | PASS（前置） | U4 下一步只做重新对账和准备 | ALLOWED_PREPARE |
| U4-COST-02 | 成本角色裁剪 | 仅 BOSS/店长/财务生成成本事实和成本摘要 | 7 角色探针 PASS；设计师/PM 无 Cost 节点且 `workbench.cost=null`；合同响应白名单已去除成本字段 | PASS | 无 | CLOSED_P1_02 |
| U4-ORIGIN-02 | 承接标签 | 固定“项目承接（接管基线）” | 承接场景断言精确通过 | PASS | 无 | CLOSED_P2_01 |
| U4-PREPROJECT-02 | 无项目事实 | `projectId` 可为空但必须显式豁免且不伪造对象 | 实际节点带固定说明、缺量房/设计只进 sourceGaps；但 `isPreProjectFact=true` 时 `validateNode` 仍拒绝空 projectId | **FAIL（P2-01）** | 将豁免接入统一校验并补正向/负向契约测试 | PARTIAL_P2_02 |
| U4-UPSTREAM-02 | 上游顺序/量房/版本 | 固定顺序、Completed 最新量房、绑定版本 | 20/20 后端回归及静态审计 PASS；真实页面/跨租户留 U6 | PASS（scoped） | 空 completedAt 边界待补测试 | MAPPED |
| U4-DOWNSTREAM-02 | 下游摘要与 App/Web | 项目/变更/材料/验收/应收/成本摘要和页面展示 | App 合同页、Web 设计师合同交接展示事实链；App/Web 构建 PASS | PASS（静态/构建） | 真实 UI/权限证据留 U6 | MAPPED |

- 复验证据：`docs/acceptance/runs/20261023-unified-object-view-u4-reacceptance/README.md`。
- 验证：后端相关回归 20/20、compile PASS；7 角色探针 2/2；App type-check/check:document/build PASS；Web type-check/build/client-copy/actions PASS；无 U4-owned DDL/权限/金额公式/状态机变更。
- 结论：U4 **FAIL / BLOCKED**；U5 仍 **BLOCKED**；`GAP-065` 保持 OPEN。

## B2C-1 U4 最终独立复验 PASS（2026-10-23）

| ID | 消费面 | 规则/对象 | 当前实现 | 独立验收结果 | 证据/剩余风险 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U4-PREREQ-03 | 前置门禁 | U1/U2/U3 ACCEPTED 后 U4 | U3 独立复验 PASS；registry U3 PASS | PASS | U4 可最终验收；U5 仅准备 | ACCEPTED |
| U4-VALIDATE-03 | 无项目节点契约 | 仅无项目合同上游事实且带明确 uncertainty 允许 projectId=null | `validateNode` 调用 `isPreProjectFact`；无 uncertainty 和下游空项目仍拒绝 | PASS | 独立探针 1/1 | ACCEPTED |
| U4-COST-03 | 成本角色裁剪 | 仅 BOSS/店长/财务 Cost | 7 角色矩阵 PASS；设计师/PM 无 Cost；`workbench.cost=null`；合同根无 signedCost/costComplete | PASS | `ContractFactPropertiesTest` | ACCEPTED |
| U4-ORIGIN-03 | 承接标签 | “项目承接（接管基线）” | 场景断言精确通过 | PASS | 无 | ACCEPTED |
| U4-FACT-03 | 上游/量房/版本/下游 | 固定顺序、Completed 最新、绑定版本、下游摘要 | 上游顺序 PASS；同时间 measureId desc 探针 PASS；绑定设计/报价 PASS；下游摘要 PASS | PASS | 真实页面留 U6 | ACCEPTED |
| U4-UI-03 | App/Web 展示 | 上下游事实卡与报价跳转 | App/Web 事实卡存在；type-check/build PASS | PASS（静态/构建） | 真实 UI/权限证据留 U6 | ACCEPTED |

- 最新快照：`/tmp/u4-final.NiDrKN/{backend,app,web}`。
- U4-owned 23 文件；清单 SHA-256：`d7ccde5fbe2afa61191b408d08d547f8cce99463fabeb339e486f319a8dd33d4`；与当前工作区 23 文件逐一比较 0 mismatch。
- 验证：后端 9 类 26/26 PASS、独立探针 3/3、JDK22 package PASS；App type-check/check:document/build PASS；Web type-check/build/client-copy/actions PASS。
- 无 U4-owned DDL、权限放宽、金额/成本/利润公式或状态机变更。
- 当时结论：**U4 PASS / ACCEPTED**；后续 U5-A/U5-B1/U5-B2 `DEV_VERIFIED`，U5-C 后续独立验收 FAIL，U5-D~F 未启动；`GAP-065` 保持 OPEN。

## B2C-1 U5 项目 PHASE 重新对账（2026-10-23）

- 结论：U5-A、U5-B1、U5-B2 `DEV_VERIFIED`；U5-C/U5-D ACCEPTED；U5-E 仅允许重新对账后准备，U5-F 未启动；`GAP-065`、`GAP-071` 保持 OPEN。
- 准备单：`docs/product/domains/product-system-baseline/11-uov-v5-project-phase-plan-v0.1.md`
- 业务依据：`DEC-086 §5`、`BR-052 §4`
- 可复用契约：`BusinessFactNode`、`BusinessFactContract`、U4 已验收事实节点经验
- 不新增 DDL；建议在 `project/project/info` 上增加瞬态 `ProjectFactTrace`，不新增 Provider。

| ID | 消费面 | 现行实现 | U5 缺口 | 目标切片 | 状态 |
| --- | --- | --- | --- | --- | --- |
| U5-API-01 | 项目事实入口 | `project/project/info` 返回瞬态 `factTrace`；Web model/API 严格映射 | 历史返回锚点已闭合；U5-E 业主消费待准备 | U5-D/E | U5-D ACCEPTED / U5-E PENDING |
| U5-PHASE-01 | 阶段与节点 | App/Web 均消费服务端事实；当前与历史节点锚点均通过独立探测 | 无 U5-D 阻断 | U5-D | U5-D ACCEPTED |
| U5-PRESALE-01 | 来源/售前/签约事实 | 后端已汇总客户/房屋/量房/设计/报价/合同节点及版本历史；App/Web 已展示 | 历史返回定位已闭合 | U5-D | U5-D ACCEPTED |
| U5-FULFILL-01 | 履约事实 | 后端已汇总阶段/任务/日报/材料/采购/到货/变更/验收/财务事实；App/Web 已展示 | 采购接合法页面；材料计划/到货显式摘要-only | U5-D | U5-D ACCEPTED |
| U5-OWNER-01 | 业主事实 | `CustomerJourneyContext` 按 section 输出销售/施工/付款等；U5-A 已冻结业主白名单 | 业主项目页尚未消费统一项目事实链 | U5-E | U5-A 契约 DEV_VERIFIED / U5-E PENDING |
| U5-NAV-01 | 跳转与返回 | 当前主卡和历史节点均有 DOM anchor；返回历史自动展开并定位 | 无 U5-D 阻断 | U5-D/E | U5-D ACCEPTED / U5-E PENDING |
| U5-TEST-01 | 契约/权限/性能 | 后端 22/22、成本白名单/边界探针 PASS；App type-check/build/subpackages/navigation PASS；Web 独立探针 35/35、type-check/client-copy/actions/build PASS | 缺真实浏览器返回/刷新、越权和端到端 SQL 预算测试 | U5-F | U5-C/U5-D ACCEPTED / U5-F PENDING |

### U5 prepare 边界

- 默认展示所有已发生正式事实；未来未发生节点不生成空白卡。
- 当前阶段/异常展开；其他阶段折叠摘要；历史/失效节点同卡折叠。
- 排序固定为阶段顺序 + 业务时间 + 记录时间 + 事实类型 + objectId。
- 业主只接收销售/公开事实；成本、利润、内部公式、供应商价和内部审批不可见。
- 不在 U5 实现合同完整上游链（U4 已验收）或 PDF 归档（U6）。

## B2C-1 U5-C App 项目详情 PHASE 视图首轮独立验收 FAIL（历史，2026-10-23）

| ID | 消费面 | 规则 | 当前实现 | 独立验收 | 最小修复 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U5C-COST-01 | `project/project/info` 根响应 | 成本/成本完整性仅 BOSS/店长/财务 | Project/Quote 白名单含 `cost` | **FAIL（P1-01）** | 移除根成本字段或按角色安全投影；加 Provider 白名单回归 | REJECTED |
| U5C-ORDER-01 | App PHASE 同源排序 | 使用服务端稳定顺序，禁止前端重排/兜底 | App 再次 sort，tie-break 与 U5 不一致；缺失 stage/current/version 默认填充 | **FAIL（P1-02）** | 按服务端 nodes 原顺序分组；或完整复制 U5 排序键；缺失字段显示暂不可确认 | REJECTED |
| U5C-PHASE-02 | 8 阶段/当前/异常展开 | 固定阶段、当前/异常默认展开 | 静态探针 PASS | PASS | 无 | MAPPED |
| U5C-HISTORY-02 | 同 nodeKey 历史折叠 | 当前主卡 + 历史卡内展开 | 静态探针 PASS；真实历史数据留 U6 | PASS | 补真实历史/长列表证据 | MAPPED |
| U5C-GAP-02 | 来源缺口/失败态 | 缺口显式、失败不伪空 | 缺口 reason/required 显示；无 trace 显示暂不可确认 | PASS | 补直接恢复 CTA/真实失败证据 | MAPPED |
| U5C-NAV-02 | 返回锚点 | tab=facts + anchor 恢复 | 接线静态 PASS；冷启动阶段折叠风险 | PASS（静态） | U6 真机验证 | MAPPED |
| U5C-BUILD-02 | App 构建/分包/导航 | type-check/build/subpackages/navigation | 全部 PASS | PASS | 无 | MAPPED |
| U5C-DDL-02 | 数据/金额/状态边界 | 无 U5-C 自有 DDL/金额/状态变化 | 无 U5C-owned SQL/migration | PASS | 无 | MAPPED |

- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u5c-independent/README.md`。
- 快照：`/tmp/u5c-backend.independent`、`/tmp/u5c-app.independent`；39 文件清单 SHA-256 `82e212f330f4109df629f1293d8d4b18fb8051964367b05bbc2dc96b1b1cc7f9`。
- 无关失败：`check:ui` 的 `src/App.vue`、`check:owner-preview` 的 TeamLeader 权限差异，均非 U5-C 文件。
- 结论：U5-C **FAIL / ACCEPTANCE_REJECTED**；U5-D~F **BLOCKED**；PI-010 保持 `DEV_IN_PROGRESS`。

## B2C-1 U5-C 第二轮独立验收 PASS（2026-10-23）

| ID | 消费面 | 规则 | 复验结果 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| U5C-COST-02 | `project/project/info` 根响应 | 成本仅授权角色可见 | PASS；Project/Quote 根成本字段移除；白名单探针 1/1 | 独立成本探针 | CLOSED_P1_01 |
| U5C-ORDER-02 | App PHASE 同源排序 | 使用服务端稳定顺序，禁止默认兜底 | PASS；按服务端 nodes 原顺序分组；缺失契约字段报暂不可确认 | UI/同源探针 9/9 | CLOSED_P1_02 |
| U5C-PHASE-03 | 8 阶段/当前/异常/历史 | 固定顺序、当前/异常展开、同 nodeKey 折叠 | PASS（静态/契约） | `ProjectFactPhase.vue` | ACCEPTED |
| U5C-GAP-03 | 来源缺口/失败态 | 缺口显式、失败不伪空 | PASS | 静态探针 | ACCEPTED |
| U5C-NAV-03 | 返回锚点与路由 | tab=facts + anchor + 来源路由 | PASS（静态） | 静态探针 | ACCEPTED |
| U5C-BUILD-03 | App 构建/分包/导航 | type-check/build/subpackages/navigation | PASS | App 命令 | ACCEPTED |
| U5C-DDL-03 | 数据/金额/状态 | 无 U5-C 自有 DDL/金额/状态变化 | PASS | 文件审计 | ACCEPTED |

- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u5c-reacceptance/README.md`。
- 全新快照：`/tmp/u5c-reaccept-backend`、`/tmp/u5c-reaccept-app`；39 files SHA-256 `da8ce198c70e4429f1eb48b6acc376985351c99ff6e06f651a4f395af8894675`；0 mismatch。
- 验证：后端 22/22、package PASS；App type-check/build/subpackages/navigation PASS；UI/同源探针 9/9 PASS。
- 无关失败：`check:ui` 命中既有 `src/App.vue`；`check:owner-preview` 命中 TeamLeader 权限差异。
- 结论：**U5-C PASS / ACCEPTED**；U5-D 已完成开发验证，待独立验收；U5-E~F 与 U6 未启动；`GAP-065`、`GAP-071` 保持 OPEN。

## B2C-1 U5-D Web 项目执行 PHASE 视图开发验证（历史，2026-10-23）

- 结论：`DEV_VERIFIED / ACCEPTANCE_PENDING`；Web 已接入 PHASE，独立验收未开始，U5-E~F/U6 未启动。
- 准备单：`docs/product/domains/product-system-baseline/12-uov-v5d-web-project-phase-plan-v0.1.md`。
- 当前实现：Web `ProjectBrief`/`projectApi` 严格映射 `factTrace`；`ProjectExecutionView` 增加 PHASE/施工执行双视图和来源锚点。
- 已冻结文件边界：project model/API、ProjectExecutionView、新增 ProjectFactPhase panel、U5-D source guard。
- 已冻结交互：PHASE 默认、8 阶段、当前/异常展开、同 nodeKey 历史折叠、来源缺口、服务端顺序、返回锚点、成本角色裁剪。
- 验证：type-check/client-copy/actions/build + source guard PASS；无 DDL/Provider/权限/金额/状态机变更。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5d/README.md`；门禁：U5-D 待独立验收，U5-E~F/U6 未启动。

## B2C-1 U5-D Web PHASE 独立验收 FAIL（2026-10-23）

| ID | 消费面 | 规则 | 当前实现 | 独立验收 | 最小修复 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| U5D-ANCHOR-01 | 历史事实返回锚点 | 返回后定位当前或历史 nodeId | 仅当前主卡有 DOM anchor；历史项没有 | **FAIL（P1-01）** | 给历史项绑定 `anchorId(history.nodeId)`，source guard 检查全部 nodeId | REJECTED |
| U5D-MATERIAL-01 | 材料计划/采购/到货入口 | 材料事实有统一来源入口或明确摘要-only | MaterialPlan/Purchase/GoodsReceipt 无 source route | **FAIL（P2-01）** | 接入现有统一页面，或明确摘要-only 并展示“暂无 Web 来源入口” | REJECTED |
| U5D-PHASE-02 | 8 阶段/历史折叠/缺口/服务端顺序 | 服务端同源 | 结构 PASS；检查通过 | PASS | 无 | MAPPED |
| U5D-FACT-02 | factTrace 严格映射 | 缺失字段不默认 | strict normalizer PASS | PASS | 无 | MAPPED |
| U5D-BUILD-02 | Web 构建/文案/动作/source guard | type-check/client-copy/actions/build | PASS | PASS | 无 | MAPPED |
| U5D-DDL-02 | 无 U5-D 自有 DDL/权限/金额变化 | — | 无 | PASS | 无 | MAPPED |

- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u5d-independent/README.md`。
- 快照：`/tmp/u5d-web-acceptance`；5 files SHA-256 `7d3c3723c613fd44f4fd4a74dfa3d1aba1ed0201b6e19e677a81f0f0e6a7b254`；0 mismatch。
- 反向探针：`PRIMARY_GROUP_ANCHOR=true`、`HISTORY_NODE_ANCHOR=false`、MaterialPlan/Purchase/GoodsReceipt route MISSING。
- 结论：U5-D **FAIL / ACCEPTANCE_REJECTED**；U5-E~F **BLOCKED**；PI-010 `DEV_IN_PROGRESS`。

## B2C-1 U5-D Web PHASE 第二轮独立验收 PASS（2026-10-23）

| ID | 消费面 | 规则 | 当前实现 | 独立验收 | 状态 |
| --- | --- | --- | --- | --- | --- |
| U5D-ANCHOR-03 | 历史事实返回锚点 | 返回后定位当前或历史 nodeId | 当前主卡与历史项均有 stable DOM anchor；返回历史自动展开阶段/历史并滚动定位 | PASS | ACCEPTED |
| U5D-MATERIAL-03 | 材料计划/采购/到货入口 | 有合法页面则接入，否则明确摘要-only | Purchase 在 BOSS/店长接 `finance-material-ordering`；设计师/PM 无路由时摘要-only；MaterialPlan/GoodsReceipt 显式摘要-only | PASS | ACCEPTED |
| U5D-PHASE-03 | 8 阶段/历史折叠/缺口/服务端顺序 | 服务端同源 | SSR 实际渲染和 source guard PASS | PASS | ACCEPTED |
| U5D-FACT-03 | factTrace 严格映射 | 缺失字段不默认 | strict normalizer PASS | PASS | ACCEPTED |
| U5D-ROUTE-03 | 项目执行角色路由 | 页面实际可达 | BOSS/店长/设计师/PM 路由与菜单 4/4；Finance 无项目执行入口 | PASS | ACCEPTED |
| U5D-BUILD-03 | Web 构建/文案/动作/source guard | type-check/client-copy/actions/build | PASS | PASS | ACCEPTED |
| U5D-DDL-03 | 无 U5-D 自有 DDL/权限/金额变化 | — | 无 | PASS | ACCEPTED |

- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md`；首轮 FAIL 证据保留。
- 快照：`/tmp/u5d-web-reacceptance.GsvcnM/web`；HEAD `0cbdc06dd485fc9464622d40eabc495d1d87fea2`，Tree `4202f3ab1b2e4a68ed74532626c8a0e427301d13`；7 files SHA-256 `6623054b4c5d8dda66c7c54430e9badc7206b8c381fa9249c8865109f79c9e54`。
- 独立探针：35/35 PASS；type-check/client-copy/actions/source guard/build PASS。
- 结论：U5-D **PASS / ACCEPTED**；U5-E 仅允许重新对账后准备；U5-F/U6 未启动；PI-010 `DEV_IN_PROGRESS`。

## B2D 项目、施工、材料、变更、验收对账

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B2D-01 | 合同生效后唯一立项 | `BR-002`、`SCN-002` | `ProjectFactTrace` / Project | `project/project/save`、`ProjectContext#createFromEffectiveContract` | App/Web 项目列表与详情 | 重复立项/未生效负向待回归 | 待独立验收 | MAPPED |
| REQ-B2D-02 | 阶段-任务-节点三层施工 | `SCN-003`、`DEC-009` | ProjectStage / ProjectTask / TaskNode | `project/stage/*`、`project/task/*`、`project/task-node/save` | App 完整；Web 节点留证 partial | 节点幂等/隐蔽门禁待回归 | 待独立验收 | MAPPED |
| REQ-B2D-03 | 项目进度与生命周期状态 | `DEC-009`、`DEC-057`、`DEC-087`、`BR-053` | Project.status / Stage.status / Task.status / ProjectSuspension | `ProjectContext#refreshProject`；停复工 Provider 待实现 | App/Web/业主端状态投影 | 停工覆盖、目标状态和并发用例待建 | 未实现/待独立验收 | BIZ_ALIGNED / GAP |
| REQ-B2D-04 | 阶段验收模式承接 | `SCN-003`、`DEC-087`、`BR-053` | ProjectStage.acceptanceMode（D1） | `ContractConstructionStage.acceptanceMode → ProjectStage` | App/Web 已读投影；D2 完成路径未实现 | D1 隔离测试/构建 PASS；D2 待建 | MySQL 迁移/真实页面/独立验收待执行 | DEV_VERIFIED / ACCEPTANCE_PENDING |
| REQ-B2D-05 | 变更审批通过即生效 | `DEC-068`、`BR-041`、`DEC-076` | ContractChange / ContractChangeItem | `project/contract-change/*` | App/Web 增减项工作台 | 来源基线/冲减/越权待回归 | 待独立验收 | MAPPED |
| REQ-B2D-06 | 材料计划到选材 | `DEC-008`、`DEC-056` | MaterialPlan / MaterialSelection | `material/selection/*` | App/Web 选材工作区 | 采购后锁定/版本待回归 | 待独立验收 | MAPPED |
| REQ-B2D-07 | 作业单与采购单分离 | `DEC-072`、`DEC-073` | MaterialOrderSheet / PurchaseOrder | `material/sheet/*`、`project/purchase/*` | App 完整；Web 仅下单 | 分单/幂等/无价待回归 | 待独立验收 | GAP |
| REQ-B2D-08 | 到场、结算与退货 | `DEC-MP-01/02/07`、`MET-002` | GoodsReceipt / MaterialReturn / MaterialSettlement | `project/goods-receipt/save`、`material/settlement/*`、`material/return/page` | App 到场/结算；Web 仅结算 | 到场/结算/退货待回归 | 待独立验收 | GAP |
| REQ-B2D-09 | 业主阶段验收与多轮整改 | `DEC-007`、`DEC-015`、用户整改责任人裁决 | Acceptance / AcceptanceRound | `project/acceptance/*`、`customer/acceptance/update` | App 完整；业主 Web 未裁决 | 轮次/越权/幂等待回归 | 待独立验收 | MAPPED |
| REQ-B2D-10 | 项目/合同事实追踪 | `USER-DEC-20261022-003/004/005`、`BR-052` | ProjectFactTrace / ContractFactChain | 多套现有投影 | App/Web/业主多入口 | 统一契约待研发 | 待独立验收 | PLAN_READY |
| PI-014 | 阶段验收模式与停工/复工 | `ANS-BIZ-20261022-008`、`DEC-087`、`BR-053` | `GAP-066` / PROJ-STATE-D1-D5 | D1 完成；D2-D5 待授权 | D1 只读展示完成 | D1 PASS；其余未开始 | D1 独立验收待执行 | DEV_IN_PROGRESS |
| PI-015 | 履约与主材作业单跨端 | `ANS-BIZ-20261022-009`、`DEC-088`、`BR-054` | `PD-X1 / MAT-X1 / MAT-X2 / MAT-X3 / TRACE-M1 / SUP-X1` | 状态推导、价格投影、异常处置、五节点契约待实现 | App/Web 静态差集已登记 | 未开始 | 未开始 | BIZ_DECIDED |

### B2D 本批结论

- 当前实现骨架已映射：立项、施工阶段/任务/节点、变更、选材、作业单/采购/到场/结算、阶段验收/整改均有代码与服务端门禁。
- 已确认但未完成实现：阶段验收模式继承与三模式完成分流、停工/复工业务动作、App/Web 履约写能力一致、作业单 `Settled/Closed` 状态回写、独立退换货/补发路径。`PI-014/015` 均已完成 T06/T12；等待用户按切片授权后进入研发。
- 专项问题：`PI-20261022-014`、`PI-20261022-015`；另关联 `PI-20261022-004/008/009/010/013`。
- 页面证据仅为静态路由/组件盘点，没有真实浏览器、真机、截图或操作验收，不能声明 UI 通过。
- 本批停在 `SCENARIOS_READY`，不进入编码；下一批为 B2E 财务运营矩阵。

## B2E 财务运营对账

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B2E-01 | 合同 → 应收 → 收款 | `DEC-016`、`DEC-MP-01` | Receivable / Receipt / Ledger | `finance/receivable/*`、`finance/receipt/save` | App/Web 应收与收款 | 超收/幂等/越权待回归 | 待独立验收 | MAPPED |
| REQ-B2E-02 | 收款冲正与客户退款 | `BR-050`、`DEC-077` | ReceiptReversal / ReceiptRefund | `finance/receipt/reverse`、`finance/refund/save` | App 完整；Web 冲正缺失 | 高危主体/金额/幂等待回归 | 未验收 | GAP |
| REQ-B2E-03 | 应付 → 付款 → 核销 | `DEC-MP-01`、`DEC-016` | Payable / Payment / PayableSettlement | `finance/payable/*`、`finance/payment/*` | App/Web 应付付款 | 多应付核销/越权待回归 | 待独立验收 | GAP |
| REQ-B2E-04 | 资金台账与手工记账 | 财务总纲、`BR-034` | Ledger / LedgerRevision | `finance/ledger/*` | App/Web 台账 | 来源边界/作废更正待回归 | 待独立验收 | MAPPED |
| REQ-B2E-05 | 项目成本与毛利 | `DEC-092/BR-058`、`MET-002/006` | ProjectCost / FinanceProjectReport | `ProjectCostProjection` 契约已验收；COST-D2 生产公式计划完成 | App/Web 项目账本/驾驶舱 | COST-D1 新契约 19/19；相关 3 类 36/36；Java22 全量 1069/0/0/0 | D1 PASS / ACCEPTED；D2 PLAN_READY / AUTHORIZATION_REQUIRED | D1_PASS / D2_PLAN_READY / overall PENDING；GAP-071 OPEN |
| REQ-B2E-06 | 项目结算 | 财务总纲、`MET-002` | Settlement | `finance/settlement/*` | App/Web 项目结算 | 应收应付清零/质保/成本完整性待回归 | 待独立验收 | MAPPED |
| REQ-B2E-07 | 员工薪酬 | `DEC-017/043/046/047`、`BR-038` | PayrollSheet / Payment / Payslip | `payroll/*`、`finance/payment/employee/save` | App/Web 薪酬 | B1 独立验收 PASS | B1 PASS，其他切片待验收 | MAPPED |
| REQ-B2E-08 | 凭证模板与套打 | `DEC-005/020`、`BR-006/018` | VoucherRecord / VoucherItem | `document/voucher/*` | App/Web 凭证 | 金额/模板/重打/权限待回归 | 待独立验收 | GAP |
| REQ-B2E-09 | 企业费用与员工报销 | `DEC-091`、`BR-057` | `Expense` / `ExpenseCategory` / `ExpenseAttachment` / `ExpenseRevision` / `Payable(Expense)` / D3 `ExpenseVoucherSnapshot` | D1-D3 已独立验收；D4 只读计划待 PI-017 成本公式冻结 | D1/D2 App/Web 已接线；D3 页面静态接线；真实 UI/OSS BLOCKED | D2 18/18；D3 第二轮 13/13；Java22 全量 attempt2 1078/0/0/0；迁移130 PASS | D1-D3 PASS/ACCEPTED；D4 PLANNED_BLOCKED_PI_017 | PI-018 overall PENDING；D5 受 GAP-070 阻塞 |
| REQ-B2E-10 | 历史承接与期初 | `DEC-011`、`BR-010` | Carryover / OpeningBalance | `carryover/*`、`finance/opening-balance/*` | App 完整；Web partial | 承接确认/期初/首次锁定待回归 | 待独立验收 | MAPPED |
| PI-016 | 财务写路径、来源与门店高危动作 | `ANS/T12-BIZ-20261022-010` | `DEC-089`、`BR-055`、`GAP-068` | FIN-WRITE-D2：按 ID 写路径门店范围、来源门店继承、空门店保持已实现 | App/Web 差集已冻结；本切片不补页面 | Java22 独立完整副本全量 1056/0/0/0；支付并发 3/3；核心/凭证/补充回归 PASS；F1/F2/F3 关闭 | D2 独立验收 PASS；D3 PASS；D1/D4/D5 未关闭 | PASS（仅 FIN-WRITE-D2） |
| PI-017 | 成本与实际毛利口径 | `ANS/T12-BIZ-20261022-017`、`DEC-092/BR-058` | `ProjectCostProjection` 契约 | `ProjectCostContext`/`FinanceReportContext` 待接入 | 静态面已核对 | COST-D1 新契约 19/19；相关 3 类 36/36；Java22 全量 1069/0/0/0 | D1 第三次独立复验 PASS / ACCEPTED；D2-D5 未授权 | DEV_IN_PROGRESS；D1_PASS / overall PENDING；GAP-071 OPEN |
| PI-018 | 企业费用与员工报销 | `ANS/T12-BIZ-20261022-012`、`DEC-091`、`BR-057` | `Expense` / `ExpenseCategory` / `ExpenseAttachment` / `ExpenseRevision` / `Payable(Expense)` / D3 `ExpenseVoucherSnapshot` | D1-D3 已独立验收；D4 只读计划待 PI-017 成本公式冻结 | D1-D3 App/Web 已接线；真实 UI/OSS BLOCKED | D2 18/18；D3 第二轮 13/13；全量 attempt2 265 suites/1078 tests/0/0/0；迁移130 PASS | D1-D3 PASS/ACCEPTED；D4 PLANNED_BLOCKED_PI_017 | PI-018 overall PENDING；D5 受 GAP-070 阻塞 |

### COST-D1 契约修复对账（2026-10-23）

| REQ | 业务规则 | 对象/字段 | D1 契约测试 | 页面/API | 独立验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| COST-D1-01 | `DEC-092/BR-058` 唯一公式与可追溯 | `CURRENT_FORMULA_VERSION`；`formulaVersion` 仅接受当前常量 | `formulaVersionMustUseSingleCurrentContractVersion` | 内部 record，无生产消费方 | 第三次独立复验 PASS | ACCEPTED |
| COST-D1-02 | `SOL-002` 来源可追溯 | `sourceSnapshotVersions` 非空；键非空、版本值为正；复制且不可变 | `sourceSnapshotVersionsMustBeNonEmptyCopiedAndImmutable` 等 4 项 | 内部 record，无 Provider | 第三次独立复验 PASS | ACCEPTED |
| COST-D1-03 | `MET-002` 收入与毛利率边界 | revenue `<= 0` ⇒ margin null；完整阶段校验 profit/margin 与输入一致 | `nonPositiveRevenueMustNeverExposeMarginAcrossStages` 等 4 项 | 内部 record，无页面 | 第三次独立复验 PASS | ACCEPTED |
| COST-D1-04 | `MET-006` 动态 P2 暂估 | `dynamicCostProvisional`；仅 Dynamic 可为 true | `dynamicStageMayUseProvisionalP2CostButKeepsFinalEmpty` 等 3 项 | 内部 record，无页面 | 第三次独立复验 PASS | ACCEPTED |
| COST-D1-05 | 旧字段阶段兼容 | 旧字段映射当前 `costStage`；Estimated `actualCost=null` | `estimatedStageMustNeverExposeActualCost`、`finalStageMustMapAliasesToFinalValues` | 内部 record，无生产消费方 | 第三次独立复验 PASS | ACCEPTED |
| COST-D1-06 | 收入未知时利润未知 | revenue=null ⇒ profit/margin 均为 null；完整/不完整阶段一致 | `completeStageMustRejectProfitWhenRevenueIsUnknown`（三阶段） | 内部 record，无生产消费方 | 第三次独立复验 PASS | ACCEPTED |

- D1 新合同测试仅 `ProjectCostProjectionTest` 19/19；`ProjectCostContextTest` 14/14 与 `ProjectCostUnpricedCompletenessTest` 3/3 是未修改旧实现的回归基线，不证明 D1 新规则已落地。
- 相关 3 类合计 36/36；Java 22 编译 PASS；`mvn -o -q clean test` 为 1,069 tests / 0 failures / 0 errors / 0 skipped，退出码 0。
- 未接 Provider，未改生产公式、权限、App/Web、DDL、迁移、历史数据或结算快照；`GAP-071` 保持 OPEN。
- 独立验收历史 `docs/acceptance/runs/20261023-cost-d1-independent/README.md`：第一次 FAIL、第二次 FAIL、第三次 PASS / ACCEPTED；前两次失败证据保留。
- 第三次独立复验：完整快照 `/tmp/cost-d1-reaccept3.RbOikn`；三阶段 `complete=true + revenue=null + profit!=null` 均被拒绝，零收入一致利润规则未回归；D1 合同 19/19、相关 3 类 36/36、Java22 全量 1,069/0/0/0，RC=0。
- 结论：`COST-D1 ACCEPTED`；`PI-017 overall PENDING`；`COST-D2~D5` 未授权；`GAP-071` OPEN；未改生产公式、权限、DDL 或迁移。

### EXPENSE-D2 第二轮独立复验（2026-10-23）

- 完整快照：`/private/tmp/expense-d2-reacceptance-round2.NaxNWe/full-backend`，提交 `206bc86bc4e2d816c353dd6576925e795a78eacd`，Tree `999a36e064b7a08d1ddba5814a14e103018a14dc`。
- D2 HTTP：17 项 **16 PASS / 1 FAIL / 0 ERROR / 0 SKIP**；P1-001 合法 JSON、Property 白名单、`payeeEmployee` 和 `finance/project/page` 同结构均 PASS。
- P1-002 OPEN：店长门店范围访问 `finance/project/report` 时，`FinanceAccessContext.resolveProject` 在 `open-in-view=false` 下读取懒加载 `Customer.store`，返回 `hibernateErrorCode`，未按门禁返回 403/404；合法本店与跨店场景均受影响。
- Java 22 全量：261 suites / **1069 tests / 0 failures / 0 errors / 0 skipped**，BUILD SUCCESS。
- 证据：`docs/acceptance/runs/20261022-expense-d1/evidence/d2/round2-independent-20261023/`。
- 门禁：D2 不通过；不进入 D3；真实 UI/App/OSS 仍 BLOCKED；`GAP-070` OPEN，D5 BLOCKED；不发布。

### EXPENSE-D2 P1-002 修复（2026-10-23，已验收）

- 状态：`D2_PASS / ACCEPTED`；三次独立 FAIL 历史保留，后续第三轮独立复验通过。
- 修复：财务项目访问使用 `JOIN FETCH customer/customer.store` 专用查询；`resolveProject` 在只读事务内完成门店范围判定。
- 开发侧 HTTP：原 17 项 + 本店/企业范围矩阵共 18/18 PASS；跨店/跨企业均 404，无 `hibernateErrorCode`。
- 回归：Java 22 编译 2086 文件；完整 `mvn -o -q clean test` 为 261 suites / 1069 tests / 0 failures / 0 errors / 0 skipped。
- 证据：`docs/acceptance/runs/20261022-expense-d1/evidence/d2/reaccept-p1-002/`。
- 门禁：D2 已独立验收；D3 `PLAN_READY / DDL_CONFIRMATION_REQUIRED`，D5 BLOCKED，真实 UI/App/OSS 仍 BLOCKED，不发布。

### EXPENSE-D2 第三轮独立复验（2026-10-23）

- 结论：**`D2_PASS / EXPENSE-D2 ACCEPTED / PI-018 overall PENDING`**；D2 历史三次 FAIL 完整保留。
- 完整快照：`/private/tmp/expense-d2-reacceptance-round3.H50jW4/full-backend`，提交 `c77532c1ec18abffbc999951a3ca6d3c9ea5ed63`，Tree `c035c3fd1f1535990a142c9723e9b91f8f26d26b`。
- HTTP：原 D2 17 项 + 独立项目财务门店/企业矩阵，合计 **18/18 PASS / 0 FAIL / 0 ERROR / 0 SKIP**；`open-in-view=false`，隔离 H2 + Undertow 真实 TCP。
- P1-001 保持 PASS；P1-002 CLOSED：本店店长/门店财务、BOSS/总部财务均成功；跨店店长/财务、跨企业 BOSS/财务均业务 404；8 个响应无 `hibernateErrorCode`。
- Java 22 全量：`mvn -o clean test` = **261 suites / 1069 tests / 0 failures / 0 errors / 0 skipped**，BUILD SUCCESS，RC=0。
- 全量测试树 262 个测试 Java 文件，不含 D2 独立验收源码；未连接 dev/test/prod 数据库，未使用真实 OSS。
- 证据：`docs/acceptance/runs/20261022-expense-d1/evidence/d2/round3-independent-20261023/`。
- 门禁：D2 ACCEPTED；D3 已进入 `DEV_VERIFIED / ACCEPTANCE_PENDING`；D5 继续受 `GAP-070` 阻塞；真实 Web/App/OSS 仍 BLOCKED；不发布。

### EXPENSE-D3 开发验证（2026-10-23）

- 状态：`DEV_VERIFIED / ACCEPTANCE_PENDING`；迁移 130 隔离 MySQL 应用/回滚 PASS。
- 范围：已付款费用凭证来源链、费用来源快照、模板版本绑定、PDF 归档和历史只读回读。
- 设计：`VoucherRecord` 增加 `sourceType/sourceId/sourceSnapshot`；复用 `templateVersion/sha256/path`；费用详情和凭证 Provider 同源返回凭证摘要。
- 模板路由：`Direct -> P-EXPENSE`、`Reimbursement -> E-REIMBURSEMENT`、`TRAVEL 报销 -> E-TRAVEL`。
- 任务书：`docs/product/domains/product-system-baseline/EXPENSE-D3-付款凭证来源快照与历史回读研发任务.md`。
- 验证：D2 回归 + D3 HTTP 18/18；Java22 2087 文件编译；全量 264 suites / 1074 tests / 0/0/0；App/Web 构建 PASS（Web 仅临时兼容无关考勤类型/文案）。
- 门禁：研发侧证据保留；后续首轮独立验收结果见下。D5 未启动，真实 UI/OSS 仍 BLOCKED，不发布。

### EXPENSE-D3 首轮独立验收（2026-10-23）

- 结论：**FAIL**；P0=0 / P1=1 / P2=0；D3 不可关闭，PI-018 overall PENDING。
- 独立快照：`/private/tmp/expense-d3-independent.BeBugm/full-backend`；HEAD `7cf83fa2b526156640ea8c227eb348ab81ca27d1`，Tree `0b58e9ba19798cf9863178ad560a8998047a5e0e`；源码清单摘要 `bfd4b50ccb12975ddfc6be38cfaacb7c929f3cc8eb264c1427b680dc39d0dab9`。
- HTTP：真实 Spring Boot `RANDOM_PORT` + TCP、H2 `MODE=MySQL`、`open-in-view=false`；最终 **11 tests / 10 PASS / 1 FAIL / 0 ERROR / 0 SKIP**。独立源码未进入产品 `src/test`。
- PASS：未付款零副作用；`sourceType=Expense`/`sourceId=expenseId`；凭证金额=Payment.amount；来源快照必需键；`P-EXPENSE`/`E-REIMBURSEMENT`/`E-TRAVEL`；同付款重打同 ID/号/哈希/路径且文件不覆盖；费用详情回读；`expenseId` 分页/报表过滤；跨店/跨企业/错误角色拒绝且无 `hibernateErrorCode`。
- **P1-003 首轮 FAIL，修复候选**：Payable 声明 `sourceType=Expense`、但 `sourceId` 指向他企/不可解析费用时，首轮仍发行普通“工资发放凭证”。修复后 `VoucherContext` 强制同企业 Expense、同一 Payable 和 Approved/Closed 状态，否则在模板/PDF/存储副作用前拒绝；开发侧 `ExpenseVoucherLifecycleTest` 2/2、D2 回归+D3 HTTP 18/18、Java22 全量 1074/0/0/0 PASS，等待新快照独立复验。
- 后端：Java 22 主源码 clean compile PASS（2,083 Java / 2,294 classes）；完整 clean test **264 suites / 1,075 tests / 0 failures / 0 errors / 0 skipped**。
- 迁移 130：隔离 MySQL 8.4 apply/rollback PASS；字段、联合索引、备份表及 2 条来源行保留均核对。
- App：type-check/build PASS。Web：type-check PASS；`npm run build` FAIL（`ProjectExecutionView.vue` 未定义 Sass `$shadow-xs`），未修改产品代码或验收预期。
- 真实 Web/App/OSS：`BLOCKED`；不以构建产物或研发截图冒充。
- 证据：`docs/acceptance/runs/20261022-expense-d1/07-acceptance-record-d3.md`、`evidence/d3/independent-20261023/`；D2 PASS 和既有 D3 研发/迁移证据历史保留。
- 首轮 FAIL 记录不回溯改判；P1-003 候选修复需在冻结的新完整快照上独立复验。
- 门禁：P1-003 独立复验 PASS 前不得进入 D4/D5；`GAP-070` 保持 OPEN；不发布。

### EXPENSE-D3 第二轮独立复验（2026-10-23）

- 结论：**PASS / ACCEPTED**；P0=0 / P1=0 / P2=0；`P1-003 CLOSED`。首轮 FAIL 记录和证据原样保留。
- 新完整快照：`/private/tmp/expense-d3-reacceptance-round2.nUlJTV/full-backend`；HEAD `7cf83fa2b526156640ea8c227eb348ab81ca27d1`，Tree `0b58e9ba19798cf9863178ad560a8998047a5e0e`；源码清单摘要 `98f44673f4d99ae4e7d1d4bcefb1a194ea408146a3d53af80f4b4fbc880ad94d`。
- HTTP：真实 `RANDOM_PORT` + TCP、H2 `MODE=MySQL`、`open-in-view=false`；**13 tests / 13 PASS / 0 FAIL / 0 ERROR / 0 SKIP**。首轮原 11 项全部 PASS；P1-003 三分支均 PASS。
- P1-003：不存在 `sourceId` → 404；跨企业 `sourceId` → 404；Expense/Payable 不一致 → 400；三次 Voucher 数量 `0→0`，文件数分别 `22→22`、`25→25`、`27→27`，未降级为普通付款凭证；合法 Expense 来源仍成功。
- 后端：Java 22 主源码 clean compile PASS（2,087 Java / 2,298 class）。完整 `clean test` attempt1 因 `StoreDimensionBindingHttpAcceptanceTest` 非 D3 时间字段秒差失败，定向复跑 PASS；attempt2 **265 suites / 1,078 tests / 0 failures / 0 errors / 0 skipped**，RC=0。
- 迁移 130：隔离 MySQL 8.4 apply/rollback PASS；三字段、联合索引、备份表 2 条来源行和原 4 条凭证历史均核对。
- App/Web：App type-check/build PASS；Web type-check/`npm run build` PASS。真实 Web/App/OSS 仍 `BLOCKED`，无真实页面/真机/OSS 回读证据。
- 快照边界：产品 `src/test` 266 个 Java 文件，独立验收源码命中 0；D3 相关 10 个文件在快照与验收结束时原工作区哈希一致。
- 证据：`docs/acceptance/runs/20261022-expense-d1/07-acceptance-record-d3.md`、`evidence/d3/independent-reacceptance-20261023/`。
- 门禁：D3 ACCEPTED；D4 只读计划完成但被 PI-017/GAP-071 阻断；D5 受 `GAP-070` 阻塞；PI-018 overall PENDING；不发布。

### EXPENSE-D4 计划（2026-10-23）

- 状态：`PLANNED / BLOCKED_PI_017`；未改代码或 DDL。
- 范围：项目费用进入项目成本一次、企业费用不进入项目成本、手工 `ProjectIncidentalCost`/`CompanyExpense` 防复制、账本/结算/报表同源。
- 阻断：`PI-20261022-017` overall PENDING、`GAP-071` OPEN；项目费用应付与 `ProjectCostContext` 估算/实际成本的组合公式尚未冻结。
- 任务书：`docs/product/domains/product-system-baseline/EXPENSE-D4-项目企业归属与手工成本边界研发任务.md`。
- 门禁：需先关闭 PI-017/GAP-071 并确认 D4 编码授权；D5 继续 BLOCKED，不发布。

### B2E 本批结论

- 已映射：应收、收款、退款/冲正、应付、付款、核销、资金台账、项目账本、项目结算、员工薪酬、凭证、期初与历史承接均有业务对象、Provider 和至少一端页面。
- 已确认：财务不是法定总账；应收≠收入、应付≠成本、付款≠成本；工资单确认仅 BOSS；财务门店范围按默认总店派生。
- 已识别：部分财务写路径缺门店范围/来源门禁；P1/P2/P3 与实际毛利公式未同源；企业费用/报销 D2 的 P1-002 项目财务门店隔离缺陷已关闭；App/Web 高危动作与期初/账龄能力不一致。
- 专项问题：`PI-20261022-016`、`PI-20261022-017`、`PI-20261022-018`。PI-016 已取得 `ANS/T12-BIZ-20261022-010`，冻结来源、门店、凭证角色和双端边界；当前仅授权 `FIN-WRITE-D2`，D1/D4 的 DDL/历史回填待用户确认。PI-017 已取得 `ANS/T12-BIZ-20261022-017`、`DEC-092`、`BR-058`，COST-D1 已第三次独立复验 `PASS / ACCEPTED`，COST-D2 只读计划已完成、状态 `PLAN_READY / AUTHORIZATION_REQUIRED`，等待用户明确授权编码；COST-D3~D5 未启动，PI-017 整体仍 `PENDING`，`GAP-071` 保持 OPEN。PI-018 已取得 `ANS/T12-BIZ-20261022-012`、`DEC-091`、`BR-057`；D1-D3 均已独立验收，D3 首轮 FAIL 保留、第二轮 13/13 PASS 且 P1-003 CLOSED；D4 只读计划完成但被 PI-017/GAP-071 阻断，D5 受 `GAP-070` 阻塞。
- 关联问题：`PI-20261022-001/004/010`。
- 页面证据仅静态路由/组件盘点；员工薪酬 B1 有独立验收证据，但其他财务页面不能据此推断已验收。
- PI-016 的 `FIN-WRITE-D2` 已独立复验 PASS：完整隔离副本 Java22 全量 `1056/0/0/0`，F1/F2/F3 关闭，支付并发同键连续 3/3 PASS 且未产生真实重复副作用；D3 亦 PASS。PI-016 整体仍为 DEV_IN_PROGRESS，D1/D4 待用户确认，D5 不提前关闭。PI-018 D1-D3 均已独立 PASS；D2 历史三轮 FAIL 保留，第三轮 18/18 PASS、P1-002 CLOSED；D3 首轮 FAIL 保留，第二轮 13/13 PASS、P1-003 CLOSED；overall 仍 PENDING；其余 B2E 问题继续按专项裁决推进。


### U3-B~F 实现结论（2026-10-23）

- 状态：`DEV_VERIFIED`（仅开发自测）。
- 后端定向：16/16 PASS；App type-check + 小程序构建 PASS；Web type-check + 生产构建 PASS。
- 剩余：真实登录页面、真机截图、跨企业/门店越权、PDF 旧归档回读和独立验收，归 U6。
- U3、U4 均已独立复验 PASS / ACCEPTED；U5-A、U5-B1、U5-B2 已 DEV_VERIFIED；后续 U5-C/U5-D 均独立复验 PASS / ACCEPTED，U5-E 仅允许重新对账后准备，U5-F 尚未启动。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u3/README.md`。
