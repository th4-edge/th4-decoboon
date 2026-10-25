# 装得清产品体系基线 · 独立验收


## PASS · PI-20261022-017 / COST-D1（2026-10-23）

- 状态：`D1_PASS / overall PENDING`；`COST-D1` ACCEPTED。
- 独立证据：`docs/acceptance/runs/20261023-cost-d1-independent/README.md`。
- 快照：`/tmp/cost-d1-reaccept3.RbOikn`；Java 22。
- 结果：D1 合同 19/19；相关 3 类 36/36；全量 1,069 tests / 0F / 0E / 0S / RC=0。
- 复验关闭：三阶段 `complete=true + revenue=null + profit!=null` 旁路；前两轮缺口保持关闭。
- 边界：`ProjectCostContext`/`FinanceReportContext` 生产公式未改；无 Provider 接入、无 DDL/迁移/权限/App/Web 变更。
- 未完成：PI-017 整体仍 `DEV_IN_PROGRESS / overall PENDING`；`COST-D2~D5` 未授权；`GAP-071` OPEN；不发布。


## PI-20261022-010 · U2 后端报价双投影

- 当前状态：`PASS / ACCEPTED`（仅 U2 后端投影）
- 开发/对账证据：`docs/acceptance/runs/20261022-unified-object-view-u2/README.md`
- 独立验收证据：`docs/acceptance/runs/20261022-unified-object-view-u2-independent/README.md`
- 基线：`7cf83fa2b526156640ea8c227eb348ab81ca27d1`
- 本轮独立快照：`/tmp/uov-v2-independent.82211/repo`
- 快照边界：仅 U1 契约 + U2 后端双投影 + 独立探针；不含套餐快照、pack 迁移、PDF/前端或其他域改动
- 问题整体：`PI-20261022-010` 保持 `DEV_IN_PROGRESS`，不得 `CLOSED`

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 同一报价 REGION/TYPE 双投影 | 两套投影来自同一源行，金额可解释 | 真实 Provider HTTP 返回同额 300，TYPE 来源 2 行 | `U2QuoteProviderHttpProbeTest` | PASS |
| 正向 | TYPE 跨区域合并与来源回溯 | 来源行/区域稳定、空区域最后 | 单测与 HTTP 验证 `[101,102]`、综合项目 | `QuoteViewContextTest`、独立探针 | PASS |
| 反向 | 差异状态/工艺/折扣/固定总价不同 | 不得合并 | U1 契约 + U2 回归 | `QuoteViewContractTest` | PASS |
| 边界 | 缺失金额/成本/组件成本 | 保持 `null`，不得补 0 | 独立探针与回归通过 | `U2IndependentUnitProbeTest` | PASS |
| 越权 | PM 不返回成本/公式/原始根 items | 内部字段 0，根 items 缺失 | 真实 PM HTTP 递归扫描通过 | `U2QuoteProviderHttpProbeTest` | PASS |
| 越权 | 跨企业/跨门店/非参与项目 | 403/404 | 本轮未执行 | 无 | UNCOVERED |
| 静态 | 无 U2-owned DDL/Provider 权限/锁/金额公式变化 | 无新增迁移或权限放宽 | 静态审计通过 | U2 对账报告 | PASS |
| 跨端 | App/Web/业主/合同/项目/PDF | 场景接线后同源 | 不属于 U2 | 无 | OUT_OF_SCOPE |

### 测试与构建结果

- U2 报价相关回归：9 类、54/54，0 failures / 0 errors / 0 skipped。
- U1 契约回归：14/14。
- Provider 属性契约：2/2。
- 独立单元/接口探针：4/4（HTTP 2/2，`open-in-view=false`）。
- 去重合计：13 个测试类、65/65 PASS。
- JDK 22 `mvn -o -q -DskipTests package`：PASS。
- `git diff --check`、JSON/registry、领域账本 strict validate：PASS。

### U2 门禁

```text
U1：PASS
U2：PASS / ACCEPTED
UOV-V3：允许重新对账后准备；U3 整体未验收
GAP-065：OPEN
PI-20261022-010：DEV_IN_PROGRESS
```

## 范围

- Batch：
- 角色：
- 环境：
- 数据/租户：

## 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 |  |  |  |  |  |
| 反向 |  |  |  |  |  |
| 越权 |  |  |  |  |  |
| 幂等 |  |  |  |  |  |
| 边界 |  |  |  |  |  |
| 跨端 |  |  |  |  |  |

## 结论

- 状态：`PASS`（仅 `PI-20261022-010 / U2`，不覆盖 U3-U6）
- 未覆盖：跨企业/门店/非参与项目 HTTP 负向、生产真实非空区域数据、App/Web/业主/合同/项目/PDF/真实 UI/生产发布
- 遗留风险：`GAP-053` 区域真实覆盖、`GAP-065` 前端接线与回退逻辑仍打开
- 是否允许进入下一批：允许 `UOV-V3` 重新对账后准备；U3 整体须独立验收，当前不得宣称完成

## PASS · PI-20261022-010 / U3 第二次独立复验（2026-10-23）

> 独立验收，不修改实现。证据：`docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md`。

### 范围

- Batch：`U3-A~F`
- 角色：员工报价、业主 journey、合同/项目入口、PDF 入口
- 环境：复验快照 `/tmp/u3-reaccept.VBA8s3/{backend,app,web}`；从研发修复快照重新复制并排除 target/dist
- 数据/租户：定向测试、纯函数探针、反射白名单、静态分支；真实页面/越权仍未覆盖

### 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | App/Web 消费服务端 REGION/TYPE | 无本地合并/空间 fallback | 目标路径已切换；`mergeKey/categoryView` 缺失 | 静态检查 | PASS |
| 正向 | TYPE 只读、编辑回原始 itemId | TYPE 不参与写入 | `gridEditable && REGION`；保存使用原始 itemId | 静态检查 | PASS |
| 正向 | 业主/PM 字段裁剪 | 无成本/公式/供应商价/`acceptedBy` | Owner/PM 反射探针通过 | `U3ReacceptProbe` | PASS |
| 正向 | 合同/项目绑定版本 | 不使用最新报价替代 | 静态使用绑定 `quoteId/version` | 静态检查 | PASS（静态） |
| 正向 | PDF 参数贯通 | `quoteId + version + groupMode` | App/Web/后端入口贯通 | 调用点与后端校验 | PASS（入口） |
| 反向 | 金额缺失 | 不得补 0/重算 | `JOURNEY_MONEY_NULL=null`；App 行 null 保持 | Java + 静态探针 | PASS |
| 反向 | 分组投影缺失 | 不得伪装空报价 | `REGION_PROJECTION_MISSING=true`、`TYPE_PROJECTION_MISSING=true`；页面提供重新读取 | Node + 静态探针 | PASS |
| 反向 | 合同读取失败 | 队列/Hero/阶段/金额/进度暂不可确认；`toCreate` 不计数 | `QUEUE_STATUS_ORDER=PASS` 等分支探针 | 静态分支探针 | PASS |
| 越权 | 跨企业/门店/非参与项目 | 403/404 | 未执行 | 无 | UNCOVERED |
| 幂等 | 重复打开/重复生成 PDF | 旧归档不覆盖 | 属于 U6，未执行 | 无 | OUT_OF_SCOPE |
| 跨端 | App/Web/业主同金额、同异常恢复 | 一致且不伪造 | 首轮 P1-01/P1-02 已关闭 | 复验报告 | PASS |

### 首轮问题关闭

| 编号 | 结果 |
| --- | --- |
| `P1-01` 金额缺失补 0/重算 | **CLOSED** |
| `P1-02` 分组缺失伪空态 | **CLOSED** |
| `P2-01` 合同失败伪待发起 | **CLOSED** |

### 测试与构建结果

- 后端 7 类 U3 定向：18/18 PASS，0 failures / 0 errors / 0 skipped。
- JDK22 package：PASS，7.5 MB。
- App type-check + `build:mp-weixin`：PASS。
- Web type-check + production build（含 client-copy）：PASS。
- 三端 diff check、JSON parse、registry ID、domain validate：PASS。
- U3-owned 静态审计：无 DDL、权限放宽、锁、状态机或金额/成本/利润语义变化。

### 结论

- 状态：**PASS（仅 `PI-20261022-010 / U3`）**
- P0 / P1 / P2：`0 / 0 / 0`
- 未覆盖：真实 App/Web/业主页面、真机、跨企业/门店/非参与项目 HTTP 负向、PDF 真实渲染与旧归档回读、`GAP-053` 真实非空区域。
- 下一批门禁：U4 **仅允许重新对账后准备**，不得启动或宣称完成；`PI-20261022-010` 保持 `DEV_IN_PROGRESS`。

## 待执行 · PI-20261022-018 企业费用与员工报销

> 当前状态：`NOT_STARTED`。以下为独立验收用例，不得由研发自测替代。

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 员工提交项目零星费用 → 店长审批 → 财务部分付款 → 付清 → 凭证回读 | 项目成本只计一次；费用、应付、付款、凭证来源一致 |  |  |  |
| 正向 | 店长提交本人企业费用 → BOSS 审批 → 财务付款 | 店长不得自批；费用不进项目成本 |  |  |  |
| 反向 | 财务审批本人费用或给本人报销付款 | 403，且无成本/应付/流水副作用 |  |  |  |
| 反向 | 缺附件、金额为 0、分类停用、项目/企业归属冲突 | 提交或审批被拒，显示可恢复原因 |  |  |  |
| 反向 | 费用审批后修改金额/归属/附件 | 拒绝；退回重提生成新版本 |  |  |  |
| 越权 | 店长访问他店、门店财务跨店、员工查看他人 | 403/404，不泄露记录存在性 |  |  |  |
| 幂等 | 重复提交、重复审批、重复生成应付、重复付款、重复凭证 | 返回同一事实或明确冲突，不新增第二笔 |  |  |  |
| 边界 | 未付款作废、付款后作废/改单、手工费用双计、费用应付重复计成本 | 按规则阻断或走负向调整，历史和审计保留 |  |  |  |
| 跨端 | App/Web 同角色同状态查看、提交、审批、付款、异常恢复 | 字段、金额、状态、下一步、错误文案和权限一致 |  |  |  |
| 阻塞 | `EXPENSE-D5` 经营利润在 `GAP-070` 关闭前 | 不得展示具体经营利润金额 |  |  | BLOCKED |

### EXPENSE-D1 执行入口

- 研发证据：`docs/acceptance/runs/20261022-expense-d1/01-dev-evidence.md`
- 独立验收执行包：`docs/acceptance/runs/20261022-expense-d1/README.md`
- 当前状态：D1 后端 HTTP + 隔离 MySQL `PASS`（P0=0/P1=0/P2=0）；真实 Web/App/OSS `BLOCKED`。

### U3 首轮修复历史（已闭环）

- 首轮 FAIL：P0/P1/P2=`0/2/1`；证据 `docs/acceptance/runs/20261023-unified-object-view-u3-independent/README.md`。
- 修复证据：`docs/acceptance/runs/20261023-unified-object-view-u3-remediation/README.md`。
- 第二次独立复验 PASS / ACCEPTED；证据 `docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md`。
- 历史门禁（当时）：U3 PASS；U4 尚需重新对账后准备；整体 PI-010 `DEV_IN_PROGRESS`。

## U5 IN_PROGRESS · PI-20261022-010（2026-10-23）

- 已验收：U1/U2/U3/U4/U5-C/U5-D PASS；U5-A/U5-B1/U5-B2 `DEV_VERIFIED`。
- U5-D：第二轮独立验收 PASS / ACCEPTED，P0/P1/P2=`0/0/0`；证据 `docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md`。
- 验收覆盖：历史节点锚点与返回定位、材料摘要-only/采购来源路由、8 阶段、历史折叠、来源缺口、服务端顺序、4 角色路由/菜单、type-check/client-copy/actions/build。
- 未覆盖：真实浏览器历史返回/刷新、多分辨率、弱网、跨企业/门店 HTTP 负向和端到端 SQL 预算；留 U5-F/U6。
- 门禁：U5-E 仅允许重新对账后准备，U5-F/U6 未启动；`GAP-065`、`GAP-071` 保持 OPEN；PI-010 保持 `DEV_IN_PROGRESS`。

## U5-C 当前工作区只读独立复核（2026-10-23）

- 结论：**PASS / U5-C ACCEPTED**，P0/P1/P2=`0/0/0`。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5c-current-independent/README.md`。
- 当前 27 个 U5-C changed product files 与既有独立快照 0 mismatch。
- 后端 22/22 PASS；App type-check/build/subpackages/navigation PASS；UI/同源探针 9/9 PASS。
- 越权编码审计：停止指令前临时启动的 U5-E 后端代码和测试修改已全部回退；U5-E 未实现。
- 门禁：U5-C ACCEPTED；U5-E 保持未实现；U5-F/U6 未启动；`GAP-065`、`GAP-071` OPEN。
