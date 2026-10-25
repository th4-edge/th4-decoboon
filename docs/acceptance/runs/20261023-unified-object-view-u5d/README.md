# 统一对象视图一期 U5-D · Web 项目执行 PHASE 视图开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U5-D` Web 项目执行工作面 PHASE 视图 |
| 日期 | 2026-10-23 |
| 状态 | `PASS / U5-D ACCEPTED`（第二轮独立验收） |
| 边界 | Web 项目域模型/API、项目执行视图、PHASE 面板、source guard，以及项目执行页 4 角色路由/菜单接线；不改后端、DDL、Provider、权限或金额公式 |
| 隔离目录 | `/tmp/u5d-web-dev` |
| 独立验收 | PASS；证据 `docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md` |

## 一、实现结果

### 1.1 Web 数据契约

- `ProjectBrief.factTrace` 接入 `project/project/info` 响应。
- 新增 `ProjectFactStage/Node/Relation/SourceGap/Trace` 类型。
- `normalizeProjectFactTrace` 严格校验：
  - `nodeId/nodeKey/stage/factType/objectType/objectId/projectId/visibility` 必填；
  - `current` 必须为布尔值；
  - `version` 必须为正整数；
  - `nodes/sourceGaps` 必须为数组；
  - 节点 projectId 必须与 trace projectId 一致；
  - 未知/缺失字段直接报“暂不可确认”，不填默认 stage/current/version。

### 1.2 Web PHASE 面板

- 固定 8 阶段：
  `来源 → 售前 → 签约 → 履约 → 变更 → 验收 → 财务 → 售后`。
- 直接消费 `trace.nodes` 服务端顺序；不重排。
- 当前阶段和存在必须缺口/异常的阶段默认展开。
- 同一 `nodeKey` 当前事实为主卡，历史/失效版本卡内折叠。
- 来源缺口显示“必须补齐/可选”和原因。
- 事实读取失败显示“项目事实暂不可确认”和重新读取，不用空列表伪装。
- 金额、成本完整性、利润/毛利率只展示服务端返回字段，不在前端重算。

### 1.3 项目执行工作面

- 增加“PHASE 事实 / 施工执行”双视图。
- 保留原施工阶段、任务排期、节点留证和现场日报功能。
- 节点来源按当前角色实际路由集合判断，不新增路由权限：
  - 客户/房屋 → 客户 360（当前角色存在时）；
  - 设计/报价/合同 → 对应设计/报价/合同工作区；
  - 主材选材、增减项、验收、财务、反馈 → 当前角色可达工作区；
  - 阶段/任务/现场日报 → 切换施工执行视图并滚动到对应锚点。
- 打开来源节点前写入 `view=facts` 与 `anchor`，同时携带 `returnPath/returnAnchor`；返回后恢复原项目和原事实节点。
- 无精确 Web 来源页的维度（部分材料计划/采购/到货）只展示事实摘要，不伪造链接。

## 二、变更文件

- `src/domains/project/model.ts`
- `src/domains/project/api.ts`
- `src/views/workbench/ProjectExecutionView.vue`
- `src/components/project/ProjectFactPhasePanel.vue`（新增）
- `scripts/verify-project-fact-phase.mjs`（新增）

无后端、DDL、迁移或权限文件修改。

## 三、验证结果

### 3.1 隔离 Web 快照

```text
/tmp/u5d-web-dev
```

- U5-D-owned 文件：5 个。
- 清单 SHA-256：`7d3c3723c613fd44f4fd4a74dfa3d1aba1ed0201b6e19e677a81f0f0e6a7b254`。
- 基于 Web detached `HEAD`，只覆盖 U5-D-owned 文件与 `node_modules` 链接。

### 3.2 命令

```text
npm run type-check                                  PASS
npm run check:client-copy                           PASS
npm run check:actions                               PASS
node scripts/verify-project-fact-phase.mjs          PASS
npm run build                                       PASS
```

source guard 覆盖：

```text
U5-D source guard PASS: PHASE/order/history/gaps/anchor/routes/factTrace
```

### 3.3 构建

Web 生产构建 PASS；`ProjectExecutionView` 懒加载 chunk 正常生成。仅有既有 Sass deprecation warning，不影响构建。

## 四、边界与风险

- 未执行真实登录后的 Web 页面、浏览器交互和多分辨率截图；留 U5-F/U6。
- 未执行真实 Provider HTTP、跨企业/门店/非参与项目负向；留 U5-F/U6。
- 量房来源在 Web 当前只能进入量房工作列表，尚无单条记录详情页。
- 材料计划/采购/到货在当前 Web 没有精确来源详情页，因此只展示摘要、不伪造来源链接。
- 返回锚点在真实浏览器历史、刷新和权限跳转下的行为仍需 U6 验证。
- `GAP-053` 非空区域与 `GAP-071` 成本三态口径仍开放。
- U5-E 业主 journey 与 U5-F 最终验收未启动。

## 五、开发验证阶段门禁（历史）

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED
U5-C：ACCEPTED
U5-D：DEV_VERIFIED（后续第二轮 PASS / ACCEPTED）
U5-E~F：开发验证时未启动
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```

开发验证阶段不宣称 U5-D ACCEPTED；后续第二轮独立验收已 PASS，当前门禁见第七节。

## 六、首轮独立验收退回修复（2026-10-23）

1. `P1-01` 历史节点锚点：历史事实项增加 `:id="anchorId(history.nodeId)"` 和 `scroll-margin-top`；source guard 增加历史锚点检查。
2. `P2-01` 材料入口：`Purchase` 在当前角色存在时接 `finance-material-ordering`；`MaterialPlan/Purchase/GoodsReceipt` 无可达页面时明确显示“暂无 Web 来源入口，请按摘要核对”，不伪造链接。
3. 修复后隔离快照：`/tmp/u5d-web-fix`；5 files SHA-256 `74b527567a6731fdb698df8d33c121878481d0854a346970aad52362c265796f`。
4. 验证：type-check、client-copy、actions、source guard、Web build PASS；等待第二轮独立复验，不得自行 ACCEPTED。

## 七、第二轮独立验收 PASS（2026-10-23）

- 结论：`PASS / U5-D ACCEPTED`，P0/P1/P2=`0/0/0`。
- 独立证据：`docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md`。
- `P1-01` CLOSED：当前与历史事实节点均有 DOM anchor；返回历史锚点时自动展开阶段/历史并滚动定位。
- `P2-01` CLOSED：Purchase 在 BOSS/店长具备路由时接 `finance-material-ordering`；MaterialPlan/GoodsReceipt 与无采购路由角色显式摘要-only。
- 验收快照补齐 `src/roles/routes.ts`、`src/roles/workbenchConfig.ts` 中项目执行页必需的 4 角色路由/菜单接线；连同既有 5 个文件共 7 个 U5-D 必需文件，清单 SHA-256 `6623054b4c5d8dda66c7c54430e9badc7206b8c381fa9249c8865109f79c9e54`。
- 独立探针 35/35；type-check/client-copy/actions/source guard/build PASS。
- 门禁：U5-E 仅允许重新对账后准备；U5-F/U6 未启动；`GAP-065`、`GAP-071` 保持 OPEN。
