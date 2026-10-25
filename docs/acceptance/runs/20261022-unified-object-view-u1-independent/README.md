# 统一对象视图一期 U1「统一投影契约」独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U1` 统一投影契约（不含 U2） |
| 日期 | 2026-10-22 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 基线提交 | `7cf83fa2b526156640ea8c227eb348ab81ca27d1` |
| 首轮隔离快照 | `/tmp/unified-object-u1-independent.WepMNt/repo` |
| 复验隔离快照 | `/tmp/unified-object-u1-reacceptance.rtZ4Po/repo` |
| 最终结论 | **PASS（第二轮复验；首轮为 FAIL）** |
| 问题状态 | `DEV_IN_PROGRESS`（不得 `CLOSED`） |
| 当前切片状态 | `ACCEPTED` |
| U2 门禁 | **允许进入“重新对账后准备”；U2 未验收、未完成** |

> 验收历史：**2026-10-22 首轮 FAIL → 研发修复 → 第二轮独立复验 PASS**。本文第一至七节保留首轮 FAIL 记录；第八节为首轮问题关闭与最终门禁。

## 一、首轮结论（历史）

U1 的指定定向测试、编译和文本检查通过，但独立逐项契约审查发现 3 个阻断性契约偏差：

1. TYPE 合并键的字符串拼接不可逆，不同业务字段组合可生成同一个合并键；
2. `READ` 内部详情场景错误地关闭版本轨；
3. 统一事实节点缺少业务冻结的 `projectId` 绑定字段。

上述问题均属于 U1 契约本身，不是 U2 或真实 UI/API 未覆盖导致。它们会直接造成错误合并、场景字段边界错误或项目事实无法稳定绑定，因此本轮判 `FAIL`，退回研发修复；修复后必须重新独立验收，U2 仍不得启动。

## 二、隔离与证据边界

为避免共享工作区并行 U2 改动和 `target/` 竞争，本次从 `HEAD` 创建 detached Git worktree，仅复制任务指定的 U1 实现与测试文件，不复制共享工作区其他未提交改动。

U1 快照内变更文件仅为：

- `QuoteViewMode.java`
- `QuoteViewScene.java`
- `QuoteViewAction.java`
- `QuoteViewContract.java`
- `QuotePrintContract.java`
- `QuoteLine.java`
- `FactStage.java`
- `FactType.java`
- `FactVisibility.java`
- `BusinessFactNode.java`
- `BusinessFactContract.java`
- `QuoteViewContractTest.java`
- `BusinessFactContractTest.java`

未运行、未修复 `QuoteViewContextTest` 或其他 U2 方向改动，也没有把共享工作区中的 U2 失败归入 U1。

## 三、命令与结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| U1 定向测试 | `mvn -o -q -Dtest=QuoteViewContractTest,BusinessFactContractTest test` | PASS，12/12（8/8 + 4/4），0 failures / 0 errors；快照内已纳入验收期间新增的空白分类回退测试 |
| U1 编译 | `mvn -o -q -DskipTests compile` | PASS |
| U1 `git diff --check` | 指定 13 个 U1 文件 | PASS |
| 成本可见性回归 | `mvn -o -q -Dtest=DesignerQuoteCostContractTest test` | PASS，1/1 |
| registry JSON | `jq empty docs/product/issue-governance/registry.json`；ID 唯一性检查 | PASS |
| DDL / Provider / 权限静态审计 | 对 U1 diff 文件检查 SQL、`@Service`、`@ServiceName`、`@DataPermission` 变更、`@Lock` 等 | PASS：U1 diff 仅 13 个 Java 文件，无迁移/SQL、无 Provider、无权限注解变更 |
| 独立契约探针 | 临时只读 Java probe，调用 U1 公开契约 | **FAIL**，见下 |

定向测试通过只证明现有测试断言成立，不足以证明契约完整符合 `DEC-086` / `BR-052`。

## 四、逐项判定

### 1. 报价 REGION/TYPE 与 7 个场景边界：FAIL

已确认：

- 7 个场景枚举齐全：`EDIT / READ / OWNER / PROJECT / CONTRACT / SUMMARY / PRINT`；
- `OWNER` 不暴露原始 `items`、成本、内部公式和 `acceptedBy`，但暴露 `acceptedAt`；
- `EDIT / READ` 可由后续服务端按既有角色暴露成本、公式和 `acceptedBy`；
- `CONTRACT` 不返回明细分组，只允许跳转；
- `PROJECT` 默认 `REGION`，允许 `TYPE`；
- `PRINT` 默认 `REGION`，允许 `REGION / TYPE`；
- 场景只声明裁剪边界，不改变数据范围或分组结果。

阻断项：

- `QuoteViewScene.READ` 的 `exposesVersionTrack()` 实际为 `false`。`DEC-086 §3.3` 明确版本轨应在内部详情/项目/合同上下文展示；现有测试只断言了 `PROJECT` 的版本轨，未覆盖 `READ`。

独立探针输出：

```text
READ_VERSION_TRACK=false
```

### 2. TYPE 合并键与排除字段：FAIL

已确认：

- 合并键包含目录项 `catalogItem.itemId`、`type/category/name/brand/spec`、`unit/pricingMode/price`、固定总价、`role/differenceType/differenceStatus/included/discountAmount`、`craft` 和组件销售签名；
- 固定总价仅在 `PricingMethod.FixedTotal` 下参与比较；
- 区域、排序、数量、金额、成本、组件成本、实际成本、供应商/采购价、`formula`、`quantityFormula`、`criterion` 未参与合并键；
- `typeRowId` 由 `quoteId + version + 规范化合并键` 生成，版本变化会改变 ID；
- 来源字段常量顺序与冻结字段一致。

阻断项：

- `canonicalMergeKey` 直接使用未转义、未定长的 `|` 拼接字段。字段值本身包含 `|` 时，不同字段组合会产生相同合并键，违反“下列字段必须全部相等才允许合并”。这是可导致错误合并的业务契约缺陷，不只是测试覆盖不足。

独立探针：

```text
KEY_A=CATALOG:88|Material|地面|a|b|c|s|Area|MaterialAndManual|0||Base|None|None|true|0||
KEY_B=CATALOG:88|Material|地面|a|b|c|s|Area|MaterialAndManual|0||Base|None|None|true|0||
COLLISION=true
```

其中 A 的 `name/brand = "A|B"/"C"`，B 的 `name/brand = "A"/"B|C"`，两者其余字段相同，却被判为同一业务身份。

### 3. `typeRowId/source*` 投影字段与无 DDL：PASS

- 5 个来源字段均存在于 `QuoteLine`；
- 5 个字段均标注 `@Transient`；
- U1 diff 无 DDL、迁移或 SQL；
- 未新增数据库列、未回填历史数据。

说明：来源集合/计数的真实装配与异常状态由 U2 负责，本项仅确认 U1 的投影字段边界和持久化边界。

### 4. PDF `quoteId + version + groupMode`、默认 REGION、客户字段白名单：PASS

- 必填参数顺序为 `quoteId / version / groupMode`；
- `requireParameters` 拒绝空参数和不支持的分组方式；
- 默认分组为 `REGION`；
- 文件名后缀包含版本与分组；
- 客户可见字段覆盖销售、折扣、税额、总额和接受时间；
- 禁止字段覆盖成本、利润、内部公式、供应商/采购价、组件成本和 `acceptedBy`；
- U1 未要求也不存在 PDF 分组元数据 DDL。

真实 PDF 生成、旧归档打开和归档不覆盖属于 U3/U6，不在 U1 结论中冒充已验收。

### 5. 合同上游顺序、直接签约/承接标签：PASS

- 上游固定顺序为 `Measure → Design → Quote → Contract`；
- 直接签约展示常量冻结为“无设计方案来源”和“签约报价基线”；
- 承接标签冻结为“项目承接（接管基线）”；
- 未新增直接签约来源字段、枚举或统计隔离；
- 未修改 `BusinessOrigin.Carryover` 既有口径。

### 6. 项目 8 阶段、稳定排序与事实节点：FAIL

已确认：

- 8 个阶段顺序为 `来源 → 售前 → 签约 → 履约 → 变更 → 验收 → 财务 → 售后`；
- 阶段内排序为 `businessTime asc（空值最后）→ recordedAt asc（空值最后）→ factType 顺序 → objectId 升序`；
- `businessTime` 缺失时仍保持稳定排序，不依赖数据库返回顺序。

阻断项：

- `BusinessFactNode` 没有 `projectId` 字段，`BusinessFactContract.validateNode` 也未要求项目绑定；
- 这与 `ANS-BIZ-20261022-007 §6.4` 的“节点携带 `objectType + objectId + version + projectId + nodeKey + returnPath + anchor`”不一致；
- 缺该字段时，项目事实无法在契约层证明属于哪个项目，后续 U5 只能依赖上下文或额外包装，违背统一节点契约。

独立探针输出：

```text
HAS_PROJECT_ID=false
```

### 7. U1 是否修改 Provider 运行时、权限、金额/成本/利润、迁移或状态机：PASS

- 隔离 diff 无 Provider、权限、金额/成本/利润公式、迁移、状态机修改；
- `QuoteLine` 仅新增 5 个 `@Transient` 字段和 getter/setter；
- 无 `@JsonIgnore`；
- 无新增 DDL；
- 未修改 `DesignerQuoteCostContractTest` 所保护的既有成本可见性。

### 8. U2 隔离与门禁：PASS

- 未启动 U2；
- 未修复 U2；
- 未将共享工作区 U2 的 `QuoteViewContextTest` 失败纳入 U1；
- 验收期间 registry 被并行会话写入“UOV-V2 后端双投影已实现，49 个报价相关测试通过 / UOV-V3 待授权”的声明；本次未复核、未认证该声明，仅原样保留在 `developmentTask.parallelSessionClaim`；
- 由于 U1 FAIL，U2 保持 `BLOCKED`，不得进入。

## 五、发现的问题

### P1-01 · TYPE 合并键可碰撞

- 文件：`src/main/java/com/th4/edge/decoboon/quote/model/QuoteViewContract.java`
- 位置：约第 89–106 行
- 影响：不同名称/品牌/规格等字段组合可产生同一键，服务端可能错误合并不同业务行，导致行数、来源和金额展示错误。
- 修复方向：使用定长前缀/长度编码、转义或稳定结构化序列化（如排序后的 JSON/带长度字段）后再哈希；补充包含分隔符的最小反向用例。

### P1-02 · `READ` 场景关闭版本轨

- 文件：`src/main/java/com/th4/edge/decoboon/quote/configure/QuoteViewScene.java`
- 位置：`READ` 构造参数，约第 21–24 行
- 影响：内部详情场景无法展示版本轨，违背 `DEC-086 §3.3` 的版本可见性边界。
- 修复方向：将 `READ.versionTrackVisible` 设为 `true`，并在场景矩阵测试中逐场景断言 `version/status/acceptedAt/acceptedBy/cost/formula` 边界。

### P1-03 · 事实节点缺少 `projectId`

- 文件：`src/main/java/com/th4/edge/decoboon/common/model/BusinessFactNode.java`
- 位置：字段定义约第 22–102 行；校验逻辑在 `BusinessFactContract.java`
- 影响：事实节点不能在契约层证明项目归属，U5 项目 PHASE 索引和返回锚点存在绑定歧义。
- 修复方向：增加 `projectId`，在节点校验中要求非空（或明确允许来源类全局事实的例外并加测试），并在 `BusinessFactContractTest` 覆盖缺省拒绝。

### P2-01 · 契约测试存在盲区

- `QuoteViewContractTest` 未断言 `READ.exposesVersionTrack()`；
- `typeMergeKeyShouldIgnoreRegionQuantityAmountCostSortAndFormatting` 未实际设置 `space`，因此没有证明区域被排除；
- 未覆盖字段值包含 `|` 时的合并键唯一性；
- `BusinessFactContractTest` 只检查字段名存在，未检查 `projectId` 或节点绑定校验。

测试盲区不改变 P1 结论，但说明“11/11 PASS”不能替代独立验收。

## 六、退回研发的阻断条件

在重新提交 U1 独立验收前，必须全部完成：

1. 修复 TYPE 合并键碰撞，并提供能复现不同字段组合不碰撞的反向测试；
2. 将 `READ` 版本轨修正为符合 `DEC-086 §3.3`，并补充完整的 7 场景边界断言；
3. 为事实节点补齐 `projectId` 契约及校验/测试；
4. 重新在隔离快照运行定向测试、编译、`git diff --check`、registry JSON 校验和静态审计；
5. 重新提交独立验收，U1 `PASS` 前 U2 保持 `BLOCKED`。

## 七、首轮门禁（历史）

```text
U1 独立验收：FAIL
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
developmentTask.currentSliceStatus：RETURNED_TO_DEV
U2：BLOCKED
```

本次仅修改验收文档、问题档案和 registry；未修改实现代码、权限、DDL、金额/成本/利润语义或业务事实。

## 八、第二轮独立复验（2026-10-22）

### 8.1 复验隔离与结论

- 结论：**PASS（仅 U1）**。
- 复验快照：`/tmp/unified-object-u1-reacceptance.rtZ4Po/repo`。
- 基线提交：`7cf83fa2b526156640ea8c227eb348ab81ca27d1`。
- 快照继续从 `HEAD` 创建，仅复制最新 U1 实现与测试，不直接使用共享工作区 `target`，不纳入 U2 文件。
- `PI-20261022-010` 整体仍为 `DEV_IN_PROGRESS`，不得 `CLOSED`。
- U1 为 `ACCEPTED`；U2 只允许进入“重新对账后准备”，不得宣称完成或验收通过。

### 8.2 命令结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| U1 定向测试 | `mvn -o -q -Dtest=QuoteViewContractTest,BusinessFactContractTest test` | PASS，14/14（9/9 + 5/5），0 failures / 0 errors |
| U1 编译 | `mvn -o -q -DskipTests compile` | PASS |
| U1 `git diff --check` | 13 个 U1 文件 | PASS |
| 成本可见性回归 | `mvn -o -q -Dtest=DesignerQuoteCostContractTest test` | PASS，1/1 |
| registry JSON | `jq empty`、ID 唯一性、PI-010 状态断言 | PASS |
| 静态审计 | U1 diff 文件边界、SQL/迁移、Provider、权限、金额语义 | PASS：仅 13 个 U1 Java 文件；无 SQL/迁移/Provider/权限注解/金额公式变更 |

### 8.3 首轮问题关闭情况

| 首轮问题 | 独立复验 | 证据 |
| --- | --- | --- |
| `P1-01` TYPE 合并键分隔符碰撞 | **CLOSED** | `DELIMITER_COLLISION=false`；`COMPONENT_DELIMITER_COLLISION=false` |
| `P1-02` `READ` 版本轨关闭 | **CLOSED** | `READ_VERSION_TRACK=true` |
| `P1-03` 事实节点缺少 `projectId` | **CLOSED** | `HAS_PROJECT_ID=true`；缺省项目/节点键/返回路径/锚点均被拒绝 |
| `P2-01` 契约测试盲区 | **CLOSED** | 新增 14/14 测试；真实 `Space` 差异仍保持同一合并身份，区域不进入合并键 |

独立探针输出：

```text
DELIMITER_COLLISION=false
COMPONENT_DELIMITER_COLLISION=false
REGION_DIFFERENCE_IGNORED=true
READ_VERSION_TRACK=true
HAS_PROJECT_ID=true
VALID_NODE_ACCEPTED=true
MISSING_PROJECT_ID_REJECTED=true
MISSING_NODE_KEY_REJECTED=true
MISSING_RETURN_PATH_REJECTED=true
MISSING_RETURN_ANCHOR_REJECTED=true
```

### 8.4 逐项最终判定

| 验收项 | 结论 | 说明 |
| --- | --- | --- |
| 报价 REGION/TYPE、7 场景字段/动作/成本/公式/版本/接受边界 | PASS | 场景矩阵符合 DEC-086；`READ` 版本轨已恢复 |
| TYPE 合并键、成本/公式/供应商价/实时 criterion 排除 | PASS | 长度前缀编码无歧义；独立反向探针通过 |
| `typeRowId/source*` 投影字段且无 DDL | PASS | 5 个字段均为 `@Transient`，无 SQL/迁移 |
| PDF `quoteId + version + groupMode`、默认 REGION、客户字段白名单 | PASS | 契约边界正确；真实 PDF 生成留待 U3/U6 |
| 合同上游顺序、直接签约/承接标签 | PASS | 上游顺序和标签常量符合冻结口径 |
| 项目 8 阶段、稳定排序、事实节点绑定 | PASS | 8 阶段顺序、排序器及 `projectId/nodeKey/returnPath/returnAnchor` 校验通过 |
| U1 未修改 Provider/权限/金额/迁移/状态机 | PASS | 隔离 diff 和静态审计通过 |
| U2 隔离 | PASS | 未进入、未验收、未修复 U2 |

### 8.5 剩余风险

- U1 仅验证契约与单元边界，不代表 Provider、App、Web、业主端或 PDF 已接入；对应 U3/U6 仍需真实验收。
- `GAP-053` 的非空区域真实数据覆盖仍未完成，不能据此宣称区域分组已在真实合同上验收。
- registry 中并行会话对 U2「UOV-V2 / UOV-V3」的声明未由本次复核，保留为并行声明，不构成本次 U1 PASS 证据。
- U2 允许进入“重新对账后准备”，但不得宣称 U2 已完成、已验收或已发布。

### 8.6 最终门禁

```text
U1 独立验收：PASS
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
developmentTask.currentSliceStatus：ACCEPTED
U2：允许重新对账后准备；未验收、未完成
```
