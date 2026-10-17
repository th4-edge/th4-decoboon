# 材料履约与材料业务助手 · 垂直切片计划

## 北向目标

材料计划、选材、作业单、采购、到场与结算形成可追溯闭环，并提供可解释、可校核的业务助手。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 业务卡核对、结构化蓝图、字段字典 | 产品/Boss/财务/设计/PM | MaterialPlan/Selection/Sheet/Purchase/Receipt/Settlement | 无或只读 blueprint definition | 无 | 现有 OBJ/BR/DEC/SCN 引用 | BLUEPRINT_READY | COMPLETE |
| B2 | Web 蓝图中心 + 项目材料业务体检 | Boss/Store/Finance | 项目/材料/检查 | blueprint runtime/check provider | Web | 选材/下单/到场/成本完整性 | DEV_VERIFIED + UI_REVIEWED | COMPLETE (2026-10-17) |
| B3 | App 业务体检 + 页面解释 | Designer/PM/Boss/Finance | Sheet/Plan/Receipt | assistant context provider | App | 字段/时间/下一步 | DEV_VERIFIED + UI_REVIEWED | PENDING |
| B4 | 只读材料助手 | 全角色（按权限） | 全部材料对象 | assistant orchestrator/tools | Web/App | 50 黄金问题、8–12 校核 | 权限/反向/证据 PASS | PENDING |
| B5 | 独立验收与对账 | independent | 全部 | 无实现 | Web/App | 正向/反向/越权/幂等/边界 | ACCEPTED | PENDING |
| B6 | 体验版/发布准备 | release | App/后端/Web | 构建/发布记录 | App 体验版 | 冒烟/回滚 | RELEASED 或决策包 | PENDING |

## 依赖与顺序

- B1 先于所有实现；没有蓝图层和字段映射，不做页面。
- B2 的检查规则必须复用领域事实，不在页面计算。
- B3 的上下文助手不能直接查库。
- B4 必须依赖 B1 的蓝图、B2 的校核和 B3 的对象上下文。
- B5 必须独立于实现，不能由执行者自验。
- B6 只做体验版；正式版由用户完成。

## 回滚

- 后端/Web 使用 release 目录和 `.previous`；
- App 保留上一体验版；
- 蓝图/助手关闭开关可回退到静态说明；
- 不产生不可逆数据迁移。

## 门禁

- B1 已停在：`BLUEPRINT_READY`，蓝图 v1.0.0 + 对账矩阵已建立；
- B2 已在 2026-10-17 完成 `DEV_VERIFIED + UI_REVIEWED + RECONCILED`：真实浏览器证据见 `evidence/B2/`；
- B2 不宣称独立验收通过；REQ-MAT-001/002/004 的独立验收仍留 B5。

## B2 实施门槛（PLAN_READY，2026-10-17）

### 垂直切片

只交付一条 Web 只读链路：登录员工从「业务蓝图中心」理解材料计划→选材→作业单→采购→到场→结算的业务对象、状态、字段和来源引用；从「项目核算 → 材料业务体检」看到当前项目由真实 Provider 聚合出的材料进度、异常与下一步。不包含 App、不同步推进助手问答、不做写操作。

### 依赖

- 业务事实：`02-business-sync.md` 的 `OBJ-008 / BR-009 / BR-044 / BR-045 / SCN-021 / DEC-072 / DEC-073`；
- 蓝图：`03-blueprint.json` v1.0.0，节点/字段/检查/UI 绑定均已建立；
- 后端只读能力：复用 `MaterialPlanWorkbenchContext`、`FinanceReportContext`、`ProjectCostContext` 和采购/材料计划数据；
- Web 现有壳层：角色静态路由、`RoleAreaShell`、项目核算页和财务项目报告；
- 权限：业务蓝图与体检仅对 Boss、StoreManager、Finance 开放；项目范围继续由财务访问上下文和材料工作台上下文裁决。

### 后端契约

- `blueprint/runtime/info`：返回材料蓝图运行态（版本、来源、角色、成本可见性、规范化蓝图 JSON）；数据来自随构建发布的蓝图资源，并由测试校验与领域包 `03-blueprint.json` 同源；
- `blueprint/check/report`：入参 `projectId`，返回 `MaterialHealthReport`：项目摘要、计划/选材/下单/到场计数、采购缺供应商/未定价、成本完整性、当前阶段、下一步、校核项和聚焦明细；
- 不新增实体表、不新增 DDL、不改变金额/成本/状态/权限语义。

### Web 页面

- `/business-blueprints`：阶段轴、对象图谱、字段字典、规则/校核/例外、来源引用与缺口；
- `/finance/projects/:projectId`：在既有项目核算页内增加「材料业务体检」区块，展示真实项目材料事实，链接到主材下单、主材结算和蓝图中心；
- 空态、失败态、无权限继续走既有错误/空态组件，不用 0 或空数组冒充完成。

### 回滚

- 后端移除两个只读 Provider/上下文/资源即可，无数据迁移；
- Web 回退到上一静态构建；页面入口是加法，不影响既有财务页面动作；
- 不涉及生产写入、自动审批、付款、删除或权限放宽。

### 退出条件

- 定向测试通过：蓝图资源同源、健康报告计数/成本不完整/角色边界；
- 后端编译与 Web `type-check`/构建通过；
- 真实浏览器渲染 `/business-blueprints` 和 `/finance/projects/:projectId`，至少 1440×900 与 1024×768 截图并检查空态/失败态；
- `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md` 与 `state.json` 更新；
- 停在 `UI_REVIEWED`，不宣称 `ACCEPTED`，独立验收留 B5。
