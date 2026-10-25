# 统一对象视图一期 U5-D · Web 项目执行 PHASE 视图第二轮独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U5-D` Web 项目执行工作面 PHASE 视图 |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读冻结快照，不修改实现） |
| 前置 | U1/U2/U3/U4/U5-C ACCEPTED；U5-A/U5-B1/U5-B2 `DEV_VERIFIED` |
| 首轮证据 | `docs/acceptance/runs/20261023-unified-object-view-u5d-independent/README.md`（历史 **FAIL**，未改写） |
| 业务依据 | `DEC-086 §5`、`BR-052 §4`、`T12-BIZ-20261022-007` |
| 最终结论 | **PASS**；P0=0、P1=0、P2=0 |
| U5-D 状态 | `ACCEPTED` |
| 后续门禁 | U5-E 仅允许重新对账后准备；U5-F、U6 未启动 |

## 一、结论

首轮独立验收的 `P1-01`、`P2-01` 均已关闭：

1. **P1-01 CLOSED：历史节点返回锚点已闭合。** 当前主卡和历史项均实际渲染稳定 DOM id；返回历史 nodeId 时会自动展开所属阶段和历史分组，再滚动到对应历史节点。历史项具备 `scroll-margin-top: 96px`。
2. **P2-01 CLOSED：材料事实入口口径已闭合。** `Purchase` 在 BOSS/店长具备 `finance-material-ordering` 路由时接入主材下单；设计师/PM 无该路由时保持摘要-only。`MaterialPlan`、`GoodsReceipt` 当前无 Web 精确来源页，均显式显示“暂无 Web 来源入口，请按摘要核对”，不伪造链接。

U5-D 数据映射、8 阶段、历史折叠、缺口、服务端顺序、角色路由和原施工执行能力均未回归。

## 二、独立快照

```text
/tmp/u5d-web-reacceptance.GsvcnM/web
```

- 基线：Web `HEAD` `0cbdc06dd485fc9464622d40eabc495d1d87fea2`，Tree `4202f3ab1b2e4a68ed74532626c8a0e427301d13`。
- 范围：7 个 U5-D 必需文件，包括 5 个实现/门禁文件及 `src/roles/routes.ts`、`src/roles/workbenchConfig.ts` 中仅用于项目执行页可达的角色路由和菜单接线。
- 7 文件清单 SHA-256：`6623054b4c5d8dda66c7c54430e9badc7206b8c381fa9249c8865109f79c9e54`。
- 原修复 5 文件清单 SHA-256：`74b527567a6731fdb698df8d33c121878481d0854a346970aad52362c265796f`，与工作区 0 mismatch。
- U5-D-owned SQL/迁移：无。
- 独立探针：`evidence/independent-u5d-acceptance.mjs`，SHA-256 `62ebb5090c390072533c78d71848b3efc7e5f1cb3e9cdadd7d38163ec86aae81`。

## 三、执行结果

```text
node independent-u5d-acceptance.mjs                PASS 35/35
npm run type-check                                 PASS
npm run check:client-copy                          PASS
npm run check:actions                              PASS
node scripts/verify-project-fact-phase.mjs         PASS
npm run build                                      PASS
```

生产构建仅有既有 Sass deprecation warning，不影响构建结果。

## 四、独立探针覆盖

```text
INDEPENDENT_U5D=35/35
```

### P1-01 历史返回锚点

| 检查 | 结果 |
| --- | --- |
| 当前主卡实际渲染 `fact-anchor-<currentNodeId>` | PASS |
| 历史项实际渲染 `fact-anchor-<historyNodeId>` | PASS |
| `focusAnchor=历史锚点` 时所属阶段展开 | PASS |
| `focusAnchor=历史锚点` 时历史列表自动展开 | PASS |
| 历史项滚动边距与 `scrollIntoView` 恢复 | PASS |
| 同阶段节点保持服务端顺序 | PASS |

### P2-01 材料来源入口

| 检查 | 结果 |
| --- | --- |
| `Purchase` 接 `finance-material-ordering` | PASS |
| BOSS/店长具备采购页静态路由 | PASS |
| 设计师/PM 无采购页时保持摘要-only | PASS |
| `MaterialPlan/Purchase/GoodsReceipt` 无路由时显示摘要-only | PASS 3/3 |
| `Purchase` 有路由时显示“查看来源”且仅其余两项摘要-only | PASS |
| 不伪造 `MaterialPlan/GoodsReceipt` 精确来源路由 | PASS |

### 契约与回归

| 检查 | 结果 |
| --- | --- |
| 固定 8 阶段实际按顺序渲染 | PASS |
| 来源缺口 required/reason 实际渲染 | PASS |
| `factTrace` 严格映射，缺失 stage/current/version 不兜底 | PASS |
| `ProjectBrief.factTrace` 接入项目详情 | PASS |
| 施工排期、任务、节点留证、日报入口保留 | PASS |
| 4 个角色项目执行路由与 4 个菜单入口一致 | PASS |
| 无 U5-D-owned DDL/迁移、权限放宽、金额/成本公式或状态机变更 | PASS |

## 五、历史与边界

- 首轮 `P1-01/P2-01` FAIL 报告保持原样：`docs/acceptance/runs/20261023-unified-object-view-u5d-independent/README.md`。
- 本轮未执行真实登录后的浏览器历史返回、刷新、多分辨率、弱网和跨企业/门店 HTTP 负向；这些仍归 U5-F/U6，不能用本报告替代。
- `GAP-065`、`GAP-071` 保持 OPEN。
- U5-E 只能重新对账后准备，不得直接实施；U5-F、U6 未启动。

## 六、门禁

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED
U5-C：ACCEPTED
U5-D：PASS / ACCEPTED
U5-E：RECONCILIATION_AND_PREPARATION_ONLY
U5-F：NOT_STARTED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```
