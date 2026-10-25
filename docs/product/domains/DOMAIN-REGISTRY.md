# 装得清业务域注册表（v0.1）

> 状态：`draft`，用于全域盘点与交付调度，不是功能验收清单，也不是对外能力承诺。
> 事实源：`zhuangdeqing-business / CAP-006`、`CAP-001`、`OBJ-001`、`GAP-058`，并结合当前后端领域包与 App/Web 页面入口做覆盖对账。
> 最近盘点：2026-10-17。
> 角色冻结（2026-10-22）：TeamLeader 已冻结，后续产品域与应用规划不再以 TeamLeader 为目标角色；文档中相关角色引用仅保留历史兼容。存量处置见 `docs/product/issue-governance/issues/PI-20261022-002-TeamLeader冻结后的存量角色与技术收口.md`。

## 1. 结论

装得清不是一个“材料系统 + 若干菜单”，而是一条以客户房屋为根、以项目为经营单元的主链。当前业务智能体已识别：

- 16 段业务主链；
- 4 条横向主轴；
- 公开入口、SaaS/运营端等平台底座；
- 当前只有 `material` 建立了完整的 `docs/product/domains/material/` 可恢复交付包。

因此，正确的全域推进方式不是把 24 个后端包分别重写成 24 套产品，而是：

```text
业务智能体裁决业务域边界
→ 形成领域注册表和跨域契约
→ 每个域建立 domain packet
→ 一次一个垂直切片
→ 代码/页面/测试/验收/对账
```

## 2. 业务域主表

| ID | 业务域 | 核心对象/事实 | 主要角色 | 当前代码/页面证据 | 资料深度 | 当前状态 | 下一道门禁 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A1 | 获客与线索 | Lead、ContentAsset、ContentTask、归因事实 | 业务员、设计师、店长、运营 | `content` 域；App 内容素材/任务/内容经营；Web 经营工作台 | 深 | 内容能力有实现；平台线索按 DEC-031 不进入租户客户主链，承接闭环未完成 | 建平台线索域边界与 A1→A2 交接契约 |
| A2 | 客户与房屋 | Customer、Property、Follow、Tag、归属链 | 业务员、设计师、店长 | `customer`、`property`；App 客户/房屋；Web 客户工作台 | 中 | 已实现，待对象状态与档案边界统一 | 客户/房屋对象卡 + 权限与多房屋场景验收 |
| A3 | 量房与原始区域 | Measure、原始 Area、预约事实 | 设计师、业务员、PM | `bim`、`customer`；App 量房工作台 | 深 | 已实现 | 原始区域/实测/方案继承跨域契约 |
| A4 | 设计 | Requirement、Scheme、Space、DesignItem、Template | 设计师、店长、客户 | `design`、`designtemplate`、`bim`；App/Web 设计工作台 | 中 | 已实现；公式体系仍有冲突 | 设计方案/模版/公式验收 |
| A5 | 报价 | Quote、QuoteItem、CatalogItem、Package、Formula | 设计师、店长、客户 | `quote`、`catalog`、`pack`；App/Web 报价工作台 | 中 | 已实现；报价接受与成本快照需对账 | 报价公式、接受登记、成本快照验收 |
| A6 | 签约与合同 | Contract、ContractItem、签约事实 | 设计师、店长、Boss、财务、客户 | `project`/`quote` 相关合同能力；App/Web 签约工作台 | 中 | 已实现；`contract-change` domain packet 已建立，停在 BLUEPRINT_READY | 合同生效、合同版本、下游交接 |
| A7 | 项目立项与计划 | Project、Stage、Plan、开工日 | Boss、店长、设计师、PM | `project`、`plan`；App/Web 项目工作台 | 中 | 已实现；`project-delivery` domain packet 已建立至 SCENARIOS_READY | 立项、计划、开工日、空阶段验收 |
| A8 | 施工/进度/工地 | Task、TaskNode、Report、Site、Risk | PM、设计师、业主 | `project`、`plan`、`risk`；App/Web 工地页 | 中 | 已实现；project-delivery B1 正在裁决 GAP-034 | 施工计划/节点留证/偏差域验收 |
| A9 | 材料与供应链 | MaterialPlan、Selection、Sheet、Purchase、Receipt、Return、Supplier、Inventory、Settlement | 设计师、PM、财务、材料员 | `material`、`supply`；App/Web 材料工作台 | 深 | 当前唯一完整 domain packet；B6/B7 继续收口 | 独立验收 + 助手上下文入口 + 发布边界 |
| A10 | 增减项与变更履约 | ContractChange、ChangeItem、生效事实 | 设计师、店长、财务、PM | `project` 变更能力；App/Web 增减项工作台 | 深 | `contract-change` domain packet 已建立至 BLUEPRINT_READY；双入口与历史状态待收敛 | 三类动作和跨域事件契约验收 |
| A11 | 阶段验收与整改复验 | Acceptance、Rectification、复验结论 | PM、业主、店长、设计师 | `project` 验收/整改；App/Web 验收页 | 中 | 已实现；迁移和真实多轮验收待确认 | 验收状态机、整改轮次、业主确认验收 |
| A12 | 账务 | Receivable、Receipt、Payable、Payment、Refund、Ledger、Voucher | 财务、Boss、店长 | `finance`、`document`；App/Web 财务工作台 | 深 | 已实现；凭证套打成熟度口径待对账 | 收付/台账/凭证/退款域验收 |
| A13 | 成本与利润 | 成本快照、预计成本、实际成本、ProjectCostContext | 财务、Boss、店长 | `finance`、`project`、`statistic`；App/Web 项目核算 | 中 | 实现存在；成本完整性冲突未闭合 | 应计项范围、costComplete、profit=null 门禁 |
| A14 | 员工、工人与薪酬 | Employee、Worker、Compensation、Attendance、PayrollSheet、Commission | Boss、店长、财务、员工 | `payroll`、`labor`；App/Web 薪酬工作台 | 深 | 已实现；工资/提成/工人档案需统一边界 | 薪酬方案、工资单确认、发放和凭证验收 |
| A15 | 业主交付与售后/复购 | OwnerPortal、Warranty、ServiceTicket、Repurchase | 业主、PM、店长 | App owner 页面；售后对象缺失 | 浅 | 业主项目可见已有实现；售后/质保/复购基本空白 | 建对象卡与最小售后闭环 |
| A16 | 经营复盘与统计指标 | Boss/Store/Team/Designer/PM 报表、MET 指标 | Boss、店长、团队负责人 | `statistic`；App/Web 工作台 | 浅 | 报表有实现，指标分母和责任角色未成体系 | 指标字典 + 角色看板口径验收 |

## 2.1 已建立的领域包

| Domain | 位置 | 当前阶段 | 说明 |
| --- | --- | --- | --- |
| `material` | `docs/product/domains/material/` | 监督管理中 | 材料计划、选材、作业单、采购、到场、结算、材料助手 |
| `contract-change` | `docs/product/domains/contract-change/` | `BLUEPRINT_READY`，B2 等代码租约 | 合同生效后的新增/减项/替换、当前有效剩余、审批与履约联动 |
| `project-delivery` | `docs/product/domains/project-delivery/` | 多批次：业主 UI `DEV_VERIFIED`；`PI-20261022-014/015` 已 `BIZ_DECIDED`，未授权编码 | 合同施工行→阶段→任务→排期→节点留证→日报→进度/偏差；跨端写能力按 `DEC-088/BR-054` |
| `enterprise-management` | `docs/product/domains/enterprise-management/` | `EMG-V2 ACCEPTED`，问题 `DEV_IN_PROGRESS` | 企业、门店、员工生命周期与审计跨端闭环；Team/TeamLeader 冻结，EMG-V2 独立验收 PASS |
| `enterprise-operations-standards` | `docs/product/domains/enterprise-operations-standards/` | `EOP-V4 DEV_IN_PROGRESS` | 五类企业运营标准资产的生命周期、版本/修订、平台基线、历史快照与 App/Web 一致性；V1–V3 已验收 |

## 3. 横向主轴

| ID | 横向能力 | 作用 | 当前证据 | 不能怎样做 |
| --- | --- | --- | --- | --- |
| H1 | 组织与数据权限 | 企业、门店、团队、本人、项目参与关系 | 后端权限上下文、门店维度决策 | 不能只靠前端隐藏按钮 |
| H2 | 单据、模板、凭证与打印 | 业务单据版本、占位符、PDF/重打留痕 | `document`、模板对象、BR-006/OBJ-002/OBJ-004 | 不能把模板变成第二套业务事实 |
| H3 | 同频协同 | 通知、动态、日报、问题整改、交接 | `collab`、消息与业务动态 | 不能把消息当作业务状态源 |
| H4 | 对外口径与 AI 增强 | 业务解释、校核、建议、受控代理 | `ai`、`blueprint`、材料助手运行时 | 不能自动审批/付款/删除/改权限 |

## 4. 平台底座

这些是租户业务域的上游或基础能力，不等同于装企业务主链：

- 公开入口：官网、小程序入口、GEO、内容获客；
- 平台运营：Admin、租户、套餐、订阅、席位、门店开通；
- 数据与基础设施：OSS、微信订阅消息、日志、审计、发布；
- 装得清自身销售线索：按 `DEC-031` 与租户客户主链分域，不得直接混入装企客户。

## 5. 跨域主契约

任何域的开发都不能只画本域对象，还要冻结下面几条跨域接口：

```text
合同生效
  -> 项目立项 + 阶段/计划 + 应收计划 + 材料计划 + 任务

增减项生效
  -> 合同金额/应收变化
  -> 施工任务变化
  -> 材料计划/采购/退货分支
  -> 成本与利润重算

采购下单
  -> 应付/暂估应付
  -> 材料计划状态
  -> 库存/到场

到场确认
  -> 材料计划状态
  -> 实际成本与账期
  -> 验收/交付证据

阶段验收
  -> 项目进度
  -> 业主可见事实
  -> 收款节点/售后起点
```

材料域当前已覆盖其中“选材→作业单→采购→到场→结算”的样板段，但上下游仍需要合同、变更、项目、验收和财务域提供稳定契约。

## 6. 建议推进顺序

### Wave 0：全域注册与契约

先完成本注册表的业务裁决、对象/状态/角色/金额边界和 16 段覆盖矩阵。此阶段不改代码。

### Wave 1：与材料域直接相邻的强依赖

1. `contract-change`：合同生效、合同版本、增减项三类动作；
2. `project-delivery`：立项、阶段、计划、施工节点；
3. `acceptance-rectify`：验收、整改、复验与业主确认；
4. `finance-profit`：应收应付、成本完整性、利润 null、结算与凭证。

### Wave 2：获客到签约的转化链

`lead-content`、`customer-property`、`measure`、`design`、`quote`。

### Wave 3：组织、人和经营

`payroll-labor`、`owner-delivery-after-sales`、`analytics`、`saas-platform`。

## 7. 每个业务域必须交付的五件套

1. 业务域章程：边界、角色、目标、非目标；
2. 真实场景：至少 2 个完整正向场景和异常路径；
3. 结构化蓝图：对象、状态、字段、规则、校核、UI 绑定；
4. 实现与页面证据：代码、API、App/Web、真实数据、截图；
5. 对账与独立验收：设计 ↔ 蓝图 ↔ 代码 ↔ 页面 ↔ 测试 ↔ 验收。

## 8. 当前明确不能宣称的事

- 不能因为后端有包名，就说该业务域已经完整；
- 不能因为 App/Web 有页面，就说业务已经验收；
- 不能把 16 段资料覆盖度当成 16 个已完成产品；
- 不能用材料的成功样板替代合同、财务、项目、验收等域的业务裁决；
- 不能在没有跨域契约的情况下，让多个域各自实现一套金额/成本/状态口径。

## 9. 下一步

1. 由业务智能体对本注册表逐域裁决，补 `domainId`、对象卡、规则卡、场景卡和跨域事件；
2. 把 16 段业务主链拆成可调度的 domain packet；
3. 选定 Wave 1 的第一个域，建立 run envelope；
4. 每个域仍遵守“一次一个垂直切片、外部状态恢复、独立验收”的交付协议。
