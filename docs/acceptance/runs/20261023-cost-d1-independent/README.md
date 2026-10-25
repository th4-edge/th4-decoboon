# COST-D1 独立验收记录（2026-10-23）

## 当前结论

**PASS / ACCEPTED（仅 COST-D1）**

复验历史：第一次独立验收 `FAIL`；第二次独立复验 `FAIL`；第三次独立复验 `PASS / ACCEPTED`（仅 COST-D1，PI-017 整体仍 PENDING）。

第三次 PASS 的完整证据见文末“第三次独立复验”；前两次 FAIL 记录和缺口证据保留在下方。

## 第一次独立验收（2026-10-23）

**FAIL / NOT ACCEPTED**

定向测试和 Java 22 全量测试均通过，但独立契约探针证明 D1 的追溯元数据、零收入毛利率边界和 P2 动态暂估标识仍未形成可执行的契约约束。测试全绿不能替代这些负向验收。

本结论只针对 `PI-20261022-017 / COST-D1`。`COST-D2`～`COST-D5` 未授权、未验收；`GAP-071` 仍为 OPEN；PI-017 整体不得关闭。

## 验收快照

- 路径：`/tmp/cost-d1-independent-full.627lOt`
- 内容：当前工作区全部 Git 可见文件（tracked + 非忽略 untracked），共 6,389 个文件；未复制共享 `target/`
- 源码清单 SHA-256：`2a34262e0c1a1142bfb89d42fe5e652a4ace6694fa3ba129e826172f56f63974`
- 快照内容与源工作区逐文件哈希比对：0 missing / 0 extra / 0 hash diff
- Git：`dev @ 7cf83fa2b526156640ea8c227eb348ab81ca27d1`，快照时 438 个工作区状态项
- 测试后复查：快照涉及的 6,389 个源文件仍全部存在且哈希未变

## 环境

- Java：Oracle JDK `22.0.1+8-16`
- Maven：`3.9.16`
- 模式：离线 `-o`
- OS：macOS 26.2 / arm64

## 测试结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 定向 | `mvn -o -q -Dtest=ProjectCostProjectionTest,ProjectCostContextTest,ProjectCostUnpricedCompletenessTest test` | **PASS**，3 类，23 tests，0F/0E/0S，RC=0 |
| 全量 | `mvn -o -q clean test` | **PASS**，261 个 Surefire 报告，1,056 tests，0F/0E/0S，RC=0，244 秒 |

定向精确结果：

- `ProjectCostProjectionTest`：6/6
- `ProjectCostContextTest`：14/14
- `ProjectCostUnpricedCompletenessTest`：3/3

说明：首个最小副本只含 `pom.xml + src`，导致 5 个依赖 docs/runbook 的基线测试因文件缺失失败；该快照已废弃，不作为验收证据。最终证据来自上述完整受管文件快照。

## 独立核验

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| `Estimated / Dynamic / Final` 枚举 | PASS | `ProjectCostStage` |
| 三态成本、利润、毛利率、分类和完整标志字段 | PASS（结构存在） | `ProjectCostProjection` |
| `Estimated.actualCost` 必须为空 | PASS | `ProjectCostProjectionTest#estimatedStageMustNeverExposeActualCost` |
| Dynamic/Final 旧成本字段映射当前阶段 | PASS | `ProjectCostProjectionTest`；序列化探针 |
| 不完整阶段禁止非空利润/毛利率 | PASS（仅已覆盖的完整性分支） | 构造器校验及 Dynamic 负向测试 |
| 完整阶段必须有成本 | PASS | 构造器校验及测试 |
| `formulaVersion/asOf/sourceSnapshotVersions` 真实可追溯 | **FAIL** | 空 source map 和任意公式串均被接受；无生产生产者 |
| 收入不大于 0 时毛利率必须为空 | **FAIL** | `estimatedRevenue=0` 时仍接受非空 `estimatedMargin` |
| P2 动态暂估显式标识 | **FAIL** | Dynamic 投影无 provisional/暂估标识，无法区分 P2 暂估与 P3 动态事实 |
| 旧字段兼容别名带 `costStage` 输出 | PASS（对象/JSON 结构） | 探针 JSON 同时含 `costStage` 与旧字段 |
| 旧消费方不产生第二套公式 | 边界仅文档声明；生产消费者未接入 | 生产代码中无 `ProjectCostProjection` 消费点；旧 `max`/回退仍在 |
| 负向与边界测试完整 | **FAIL** | 缺少空追溯、任意公式、零收入毛利率、P2 暂估标识测试 |

## 阻断问题

### P1-01 · 追溯元数据仍是调用方可伪造占位

- `ProjectCostProjection` 仅要求 `formulaVersion` 非空、`asOf` 非空；`sourceSnapshotVersions` 为空时被转为 `Map.of()`。
- 测试明确接受 `null` 来源版本并断言结果为空：`ProjectCostProjectionTest:104-109`。
- 独立探针接受 `formulaVersion="caller-supplied-formula"` 和空来源 map：`projection-probe.txt`。
- 生产主代码除类自身外没有 `ProjectCostProjection/ProjectCostStage` 消费点：`source-audit.txt`。
- 结论：当前只是结构字段，不是“真实可追溯”的生产契约；不能证明某个成本数字来自哪个报价/采购/到货/结算/工资/费用版本。

### P1-02 · 零收入毛利率边界未约束

- `BR-058`：收入不大于 0 时毛利率必须为 `null`。
- 构造器只校验“不完整时利润/毛利率为空”，不校验收入与毛利率关系。
- 独立探针成功构造 `estimatedRevenue=0`、`estimatedProfit=-100`、`estimatedMargin=-100`。
- 测试没有收入为 0/负数的边界用例。

### P1-03 · P2 动态暂估无可识别契约

- T12/DEV-D1 要求动态 P2 标注“暂估/未结算”。
- `ProjectCostProjection` 仅有 `Dynamic` 阶段，没有 provisional/sourceState 等字段，无法区分“含 P2 暂估”与仅含已发生 P3 的动态投影。
- `ProjectCostProjectionTest` 的方法名提到 provisional，但没有断言任何可消费的暂估标识。

### P2-01 · 23/23 不能证明新规则覆盖

- `ProjectCostContext`、`ProjectCostContextTest`、`ProjectCostUnpricedCompletenessTest` 与基线相比未被 D1 修改。
- 其中仍有测试断言旧回退口径，例如 `shouldRecalculateActualCostWhenPurchasePriceChanges`、`signedContractMissingCostMustFallbackToQuoteSnapshot`。
- 这与 D1“不替换生产公式、GAP-071 保持 OPEN”的边界一致；但 23/23 只能说明“D1 新契约测试 6/6 + 未回归基线 17/17”，不能证明生产侧已执行三态新规则。

## 旧字段兼容与 GAP 边界

- JSON 探针确认 `costStage` 和 `actualCost/totalCost/profitAmount/grossProfit/grossMargin/costComplete` 同时输出，`Estimated.actualCost=null`。
- 但生产代码没有消费 `ProjectCostProjection`；以下旧口径仍是 OPEN，归 COST-D2/D4，不能算 D1 完成：
  - `ProjectCostContext.actualCost` 回退目录/预计成本；
  - `ProjectCostContext.contractActualCost` 回退合同/报价/预计成本；
  - `FinanceReportContext` / `FinanceProjectLedgerContext` / `BossDashboardContext` 仍使用 `actualCost.max(payableCost)`。
- 因此 D1 的“兼容边界”目前仅存在文档/Javadoc 层，尚无运行时消费点证明所有旧字段调用方都同时处理 `costStage`。

## 回写

本次验收 **FAIL**，因此按任务边界：

- 未修改实现、生产代码、权限、DDL、迁移或测试断言；
- 未把 PI-017 的 acceptance 更新为 `D1_PASS`；
- 未把 COST-D1 标记为 `ACCEPTED`；
- 未修改 `registry.json`、问题单、`state.json` 或 `10-reconciliation.md`；
- `COST-D2`～`COST-D5` 仍未授权，`GAP-071` 仍 OPEN，PI-017 整体仍不得关闭。

## 复审前置

1. 明确并强制 `sourceSnapshotVersions` 的真实来源键/版本与最小完整性；禁止空占位（或明确哪些阶段可空及原因）。
2. 契约或其唯一生产者强制校验三态利润/毛利率公式，至少补齐收入 `<= 0 => margin=null` 和完整阶段利润约束。
3. 为 Dynamic 增加可消费的 P2 暂估标识，或明确用独立字段/枚举表达“含暂估/未结算”。
4. 补齐对应正向、负向、边界测试后，重新从干净完整快照执行三定向类 + 全量。
5. D1 重新 PASS 后，才可回写 acceptance 状态；D2～D5 仍需另行授权。


---

## 第二次独立复验（2026-10-23）

### 结论

**FAIL / NOT ACCEPTED**

此前 4 个 FAIL 点已在契约构造器层真实关闭；但独立探针发现新的 P1 旁路：完整阶段允许 `revenue=null` 且 `profit` 非空，导致无法验证 `profit = revenue - cost`。

### 快照与测试

- 快照：`/tmp/cost-d1-reaccept.Kw0jak`
- 快照文件：6,398 个 Git 可见文件；0 missing / 0 extra / 0 hash diff
- 源码清单 SHA-256：`cf607fc76b7643787db6ab829ac50a7978a65c0984b4ea4952b8bec93d130642`
- Java：Oracle JDK `22.0.1+8-16`；Maven `3.9.16`；离线模式
- 定向：`ProjectCostProjectionTest` 16/16、`ProjectCostContextTest` 14/14、`ProjectCostUnpricedCompletenessTest` 3/3，合计 **33/33，0F/0E/0S，RC=0**
- 全量：`mvn -o -q clean test`，261 个报告，**1,066 tests，0F/0E/0S，RC=0，234 秒**
- 测试后当前工作区 6,398 个受管文件哈希未变化

### 此前 4 个 FAIL 的关闭核验

| 原缺口 | 复验结果 | 独立证据 |
| --- | --- | --- |
| 任意/空公式版本 | **CLOSED**；仅接受 `project-cost-projection-v1` | 任意串、旧串均被构造器拒绝 |
| 来源版本可空/可变 | **CLOSED**；null/空 map、空 key、null key、非正版本均拒绝；防御复制且不可变 | `reaccept-projection-probe.txt` |
| `revenue<=0` 时非空 margin | **CLOSED**；0/负收入非空 margin 被拒绝，0 收入可保留一致 profit 且 margin=null | 同探针 |
| P2 暂估不可消费 | **CLOSED**；Dynamic 可读 `dynamicCostProvisional=true`，Final 保持空；Estimated/Final 携带 true 被拒绝 | 同探针及 JSON |

### 新增阻断

#### P1-NEW · 完整阶段可在 revenue 缺失时携带任意 profit

- `requireStageContract` 在 `revenue == null` 时仅检查 margin：`ProjectCostProjection.java:160-165`。
- 该分支没有要求 `profit=null`，随后直接返回，跳过 `profit = revenue - cost` 校验。
- 独立探针证明 Estimated、Dynamic、Final 三个阶段均接受 `complete=true / revenue=null / cost=70000 / profit=30000 / margin=null`：
  - `NULL_REVENUE_COMPLETE_NONNULL_PROFIT_REJECTED_Estimated=false`
  - `NULL_REVENUE_COMPLETE_NONNULL_PROFIT_REJECTED_Dynamic=false`
  - `NULL_REVENUE_COMPLETE_NONNULL_PROFIT_REJECTED_Final=false`
- `ProjectCostProjectionTest` 未覆盖该边界，因此 16/16 不能证明完整阶段公式在所有边界上被强制。
- 修复边界应为：`complete=true` 且 revenue 无法确认时，profit 必须为 null（或直接拒绝该组合），不得携带未经 revenue-cost 推导的利润。

### 其他复验项

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| 构造器实际强制，而非仅测试断言 | PASS（除上述 revenue=null 旁路） | 独立探针直接调用 builder/constructor |
| `dynamicCostProvisional` 可消费、P2 不冒充 Final | PASS | JSON 含 `dynamicCostProvisional=true`、`costStage=Dynamic`；finalCost/finalProfit 为空 |
| 旧兼容字段仍带 `costStage` | PASS | JSON 同时含 `costStage`、`actualCost`、`totalCost`、`profitAmount`、`grossProfit`、`grossMargin`、`costComplete` |
| 不产生第二套生产公式 | PASS（D1 边界） | `ProjectCostProjection` 无生产消费方；生产旧公式未在本轮改变 |
| `ProjectCostContext` 边界 | PASS | 相对 HEAD 和上一快照均 UNCHANGED |
| `FinanceReportContext` 边界 | PASS | 当前差异仅是项目明细预加载，未改成本/利润公式；相对上一快照 UNCHANGED |
| 05 指标 provenance | PASS | `MET-002`/`MET-006` 均为 approved；引用 EV-402/411/412/413；证据 registry 条目存在；知识库 280 卡/394 证据校验通过 |
| GAP/切片边界 | PASS | `GAP-071` OPEN；COST-D2~D5 未授权；PI-017 整体未验收 |

### 回写边界

第二次复验仍为 FAIL，因此未更新 PI-017 acceptance、registry、问题单、state 或 reconciliation 为 PASS/ACCEPTED。上述状态文件保持候选/待复验。

### 第二次复验证据

- `reaccept-environment.txt`
- `reaccept-snapshot-meta.txt`
- `reaccept-targeted-summary.json`
- `reaccept-full-summary.json`
- `reaccept-projection-probe.txt`
- `reaccept-source-audit.txt`
- `reaccept-provenance.txt`
- `reaccept-source-after-tests.sha256`


---

## 第三次独立复验（2026-10-23）

### 结论

**PASS / ACCEPTED（仅 COST-D1）**

前两次 `FAIL` 记录和证据保留。第三次复验确认第二次唯一新增旁路已关闭，且前两轮所有已关闭项没有回归。

`PI-20261022-017` 整体仍为 `DEV_IN_PROGRESS / overall PENDING`；`COST-D2`～`COST-D5` 未授权；`GAP-071` 仍 OPEN。

### 快照与测试

- 快照：`/tmp/cost-d1-reaccept3.RbOikn`
- 快照文件：6,406 个 Git 可见文件；0 missing / 0 extra / 0 hash diff
- 源码清单 SHA-256：`3aed3a8130351a4ea35ae1777ab66f87ca0d89a3d07d94e6420ace373db40db1`
- Java：Oracle JDK `22.0.1+8-16`；Maven `3.9.16`；离线模式
- 定向：`ProjectCostProjectionTest` 19/19、`ProjectCostContextTest` 14/14、`ProjectCostUnpricedCompletenessTest` 3/3，合计 **36/36，0F/0E/0S，RC=0**
- 全量：`mvn -o -q clean test`，261 个报告，**1,069 tests，0F/0E/0S，RC=0，226 秒**
- 测试后当前工作区 6,406 个受管文件哈希未变化

### 复验矩阵

| 检查项 | 结果 | 独立证据 |
| --- | --- | --- |
| `complete=true + revenue=null + profit!=null` 三阶段旁路 | PASS（Estimated/Dynamic/Final 均拒绝） | `third-projection-probe.txt` |
| `revenue=null` 且 profit/margin 均为 null | PASS（三阶段可安全表达收入未知） | 同探针 |
| `revenue<=0` 时 margin=null、一致 profit 保留 | PASS（0 收入三阶段均保留 profit=-100、margin=null） | 同探针 |
| 唯一公式版本 | PASS | 任意/旧公式均拒绝 |
| 来源版本完整性/不可变 | PASS | null/空/空 key/null key/非正版本均拒绝；防御复制和不可变通过 |
| 正收入 profit/margin 公式一致性 | PASS | 错误 profit、错误 margin、缺失 margin 均拒绝 |
| Dynamic P2 暂估可消费 | PASS | JSON 含 `dynamicCostProvisional=true`；Final 为空；Estimated/Final 携带 true 被拒绝 |
| 旧兼容字段与 `costStage` | PASS | JSON 同时含 `costStage` 和全部旧别名 |
| 生产公式边界 | PASS | `ProjectCostContext` 相对 HEAD/上一快照未变；`FinanceReportContext` 相对上一快照未变且差异仅为项目明细预加载 |
| GAP/切片边界 | PASS | `GAP-071` OPEN；COST-D2~D5 未授权；PI-017 整体 PENDING |

### D1 门禁

```text
COST-D1：PASS / ACCEPTED
PI-20261022-017：DEV_IN_PROGRESS / overall PENDING
GAP-071：OPEN
COST-D2~D5：NOT_AUTHORIZED
无 DDL / 无迁移 / 无权限变更 / 无生产发布
```

### 第三次复验证据

- `third-environment.txt`
- `third-snapshot-meta.txt`
- `third-targeted-summary.json`
- `third-full-summary.json`
- `third-projection-probe.txt`
- `third-source-audit.txt`
- `third-source-after-tests.sha256`

## 证据文件

- `environment.txt`
- `snapshot-meta.txt`
- `targeted-summary.json`
- `full-summary.json`
- `projection-probe.txt`
- `source-audit.txt`
- `source-after-tests.sha256`
