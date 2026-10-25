# UOV-V5D Web 项目执行工作面 PHASE 视图准备单 v0.1

> 日期：2026-10-23
>
> 当前状态：`PASS / U5-D ACCEPTED`
>
> 前置：U1/U2/U3/U4/U5-C ACCEPTED；U5-A/U5-B1/U5-B2 `DEV_VERIFIED`；`project/project/info` 已返回 `factTrace`
>
> 业务依据：`DEC-086 §5`、`BR-052 §4`、`T12-BIZ-20261022-007`
>
> 结论：U5-D 已完成两轮独立验收；首轮历史 FAIL 保留，第二轮修复后 PASS / ACCEPTED。U5-E 仅允许重新对账后准备，U5-F 与 U6 未启动。

## 一、目标

在 Web 企业工作台 `/workbench/project-execution` 增加项目 `PHASE` 事实视图，使项目管理角色在宽屏上：

```text
选择项目
→ 按固定 8 阶段查看当前事实、异常和来源缺口
→ 在当前卡内展开历史版本
→ 点击统一来源对象
→ 回到原项目、原视图、原节点锚点
```

PHASE 只做事实索引和导航，不在 Web 复制报价、设计、材料、采购、验收、财务完整明细。

## 二、真实业务场景

### 场景 1：项目经理核对当前施工与异常

- 使用人：项目经理。
- 触发：早上进入 Web 工作台检查在跑项目。
- 当前动作：逐个打开项目、阶段、任务、日报，难以确认当前事实和历史变更。
- 系统最短动作：选择项目后默认打开 PHASE，当前阶段和异常阶段展开，显示任务/日报/验收/材料缺口；点击任务或验收回到统一对象入口。
- 下一步：项目经理处理任务/日报/验收，系统下一次读取重新推导事实。
- 异常：事实读取失败显示“部分事实暂不可确认”并保留刷新；不得显示“暂无事实”。

### 场景 2：企业负责人复核签约依据和历史版本

- 使用人：BOSS。
- 触发：客户对合同报价或设计版本提出疑问。
- 当前动作：在多个报价、设计、合同页面间手工比对。
- 系统最短动作：PHASE 的签约阶段显示绑定报价/设计版本，历史版本卡内折叠；点击报价/合同进入统一对象视图。
- 下一步：负责人核对后决定是否发起变更。
- 异常：历史版本缺失显示来源缺口或暂不可确认；成本/利润仅 BOSS/店长/财务可见；不得猜版本。

## 三、当前实现与缺口

| 消费面 | 当前实现 | U5-D 缺口 |
| --- | --- | --- |
| `project/project/info` | 已返回瞬态 `factTrace`、nodes、sourceGaps | Web `ProjectBrief` / normalizer 不映射 factTrace |
| `ProjectExecutionView` | 项目列表 + 阶段/任务/日报/节点留证 | 无 PHASE 视图、历史折叠、缺口和返回锚点 |
| Web project model | `ProjectBrief` 无事实节点类型 | 需增加 FactStage/FactNode/FactTrace 类型 |
| Web navigation | 工作面可路由替换 projectId | 无 facts view/anchor 恢复协议 |
| 成本权限 | 后端已按角色裁剪 factTrace 成本节点 | Web 不能另造成本字段或前端猜权限 |
| 响应顺序 | 后端 `ProjectFactTraceContract.ordered` 稳定排序 | Web 必须直接消费服务器顺序，不得重排/兜底 |

## 四、U5-D 文件边界（预计）

实现时优先只改以下文件：

- `src/domains/project/model.ts`
  - 增加 `ProjectFactStage/Node/SourceGap/Trace` 类型和 `ProjectBrief.factTrace`。
- `src/domains/project/api.ts`
  - 严格归一化 `factTrace`；缺失必填契约字段抛错，不默认 stage/current/version。
- `src/views/workbench/ProjectExecutionView.vue`
  - 增加 PHASE / 施工执行视图切换或分区；读取 `projectId`、`view=facts`、`anchor`。
- 新增 `src/components/project/ProjectFactPhasePanel.vue`（或既有组件目录等价文件）
  - 固定 8 阶段、事实卡、历史折叠、缺口、异常和来源导航。
- 可选 `src/domains/project/meta.ts`
  - 只放展示标签/阶段映射，不承载业务权限。
- 目标测试/检查脚本：
  - `npm run type-check`
  - `npm run check:client-copy`
  - `npm run check:actions`
  - `npm run build`
  - 新增 U5-D source guard（8 阶段、服务端顺序、无前端默认、返回锚点）。

不新增 Provider、DDL、迁移；不改 Web 路由权限和金额/成本公式。

## 五、交互与状态冻结

1. 默认视图：PHASE；现有施工排期/任务/日报保留为“施工执行”视图。
2. 阶段顺序固定：
   `来源 → 售前 → 签约 → 履约 → 变更 → 验收 → 财务 → 售后`。
3. 当前阶段和存在必需缺口/异常的阶段默认展开；其他阶段折叠摘要。
4. 同一 `nodeKey` 只显示一张当前摘要卡，历史/失效版本卡内展开。
5. 节点直接使用 `factTrace.nodes` 服务端顺序；禁止前端 sort、重算金额或补造阶段。
6. 来源缺口显示 label、required、reason；事实缺失显示“暂不可确认”，不用空数组伪装。
7. 读取失败显示错误和重新读取；空事实与读取失败必须区分。
8. 成本/成本完整性/利润只在后端授权节点存在时展示；Web 不得自行放开。
9. 节点跳转携带 `projectId + objectId + version + returnPath + returnAnchor`；返回恢复原项目、facts view、阶段展开和节点位置。
10. 当前项目自身节点不伪造来源跳转；无法定位来源的节点不显示“查看来源”。

## 六、角色边界

- BOSS/店长/财务：可按既有权限看到成本/利润/结算事实，且仍受企业/门店数据范围约束。
- 设计师/PM/Marketer/TeamLeader：不得因 PHASE 获得成本、利润、内部公式、供应商价或内部审批。
- 业主不消费本 Web 切片；业主项目 journey 属于 U5-E。
- 页面隐藏不是权限；服务端数据范围和 `@Properties` 裁剪是最终边界。

## 七、退出条件

U5-D 实现完成后，必须满足：

1. `project/project/info` 一个请求返回完整当前事实和缺口；
2. Web normalizer 严格映射 U5 契约，缺失字段不填默认；
3. 8 阶段、当前/异常展开、历史折叠和稳定顺序有 source guard；
4. 项目/合同/报价/设计/任务/验收/财务来源跳转可达；采购在有合法路由时接入，其他材料事实明确摘要-only；
5. 返回 `projectId + view=facts + anchor` 可恢复；
6. 设计师/PM 响应中无成本/利润/内部公式/供应商价；
7. `type-check`、`check:client-copy`、`check:actions`、`build` 全部 PASS；
8. 无 U5-D-owned DDL、迁移、权限放宽、金额/成本/利润公式或状态机变化；
9. 独立验收前不得标记 PASS，真实页面/越权证据留 U5-F/U6。

## 八、回滚

- 回退 `ProjectExecutionView.vue` 到施工执行布局；
- 删除新增 PHASE panel 和 factTrace 映射；
- 保留后端 `project/project/info.factTrace` 兼容字段；
- 不影响任务、日报、阶段和既有权限。

## 九、当前门禁

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-C：ACCEPTED
U5-D：PASS / ACCEPTED
U5-E：RECONCILIATION_AND_PREPARATION_ONLY
U5-F：NOT_STARTED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```

U5-D 第二轮独立验收证据：`docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md`；首轮 FAIL 证据保留于 `docs/acceptance/runs/20261023-unified-object-view-u5d-independent/README.md`。
