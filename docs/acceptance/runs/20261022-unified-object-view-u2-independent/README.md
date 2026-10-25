# 统一对象视图一期 U2「报价服务端 REGION/TYPE 双投影」独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U2` 报价服务端 REGION/TYPE 双投影（不含 U3） |
| 日期 | 2026-10-22 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 基线提交 | `7cf83fa2b526156640ea8c227eb348ab81ca27d1` |
| 隔离快照 | `/tmp/unified-object-u2-acceptance.vXeKb4/repo` |
| 前置 | U1 已独立验收 `PASS` |
| 最终结论 | **PASS（第二轮独立复核 65/65，含接口级 2/2）** |
| 本轮复核快照 | `/tmp/uov-v2-independent.82211/repo` |
| 问题状态 | `DEV_IN_PROGRESS`（不得 `CLOSED`） |
| 当前切片状态 | `ACCEPTED` |
| U3 门禁 | **允许重新对账后准备；U3 未验收、未完成** |

## 一、结论

U2 服务端报价 REGION/TYPE 双投影通过独立验收。指定 23 项契约与属性测试全部通过，独立探针验证了同源投影、TYPE 合并、来源排序、空区域末位、缺失金额/成本保持 `null`，以及项目经理不暴露成本/公式。

U2 只完成服务端投影与读取/响应白名单基础，不代表 App/Web/业主端/PDF 已完成接线；U3 仍需重新对账后准备，不得宣称统一视图跨端完成。

## 二、隔离环境与边界

- 使用全新 detached Git worktree，从 `HEAD` 创建，不直接使用共享工作区 `target/`。
- 快照复制 U1 已验收文件、U2 核心投影/Provider/测试文件，以及 U2 编译所需的 pack 生命周期依赖源码。
- 首次隔离编译缺少 pack 依赖，属于快照复制不完整，不作为 U2 失败证据；补齐依赖闭包后重新运行并通过。
- 共享工作区另有 pack 迁移 `124-套餐模板不可变版本.sql` 及 `package_section_name_snapshot` 并行改动。该改动属于并行专项，不属于 U2 REGION/TYPE 逻辑；U2-owned diff 无新增 DDL、迁移、权限语义、金额公式、状态机或前端代码。
- 未修改实现、权限、DDL、金额/成本/利润公式或业务事实。

## 三、命令与结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| U2 定向测试 | `mvn -o -q -Dtest=QuoteViewContextTest,QuoteUnifiedViewPropertiesTest,QuoteViewContractTest,BusinessFactContractTest test` | PASS，23/23（7/7 + 2/2 + 9/9 + 5/5），0 failures / 0 errors |
| 编译 | `mvn -o -q -DskipTests compile` | PASS |
| U2 `git diff --check` | 快照 U2 文件 | PASS |
| registry JSON | `jq empty`、ID 唯一性、PI-010 状态断言 | PASS |
| 静态审计 | U2-owned diff 的 SQL/迁移、Provider 安全、权限、金额公式、状态机 | PASS；无 U2-owned DDL/迁移或权限放宽 |
| 独立探针 | `U2AcceptanceProbe` + `U2OrderingProbe` | PASS |

## 四、独立探针结果

```text
REGION_GROUP_COUNT=3
TYPE_GROUP_COUNT=1
REGION_LINES_PRESERVED=true
SOURCE_LINE_IDS=[10, 30]
SOURCE_REGION_IDS=[11, 12]
SOURCE_REGION_NAMES=[客厅, 主卧]
SOURCE_COUNT=2
MERGED_AMOUNT=2000.00
MERGED_COST=1300.00
MISSING_AMOUNT_IS_NULL=true
MISSING_COST_IS_NULL=true
MISSING_COMPONENT_COST_IS_NULL=true
EMPTY_REGION_LAST=true
REVERSED_ID_STABLE=true
REVERSED_SOURCES_STABLE=true

PM_HAS_GROUP_COST=false
PM_HAS_LINE_COST=false
PM_HAS_LINE_FORMULA=false
PM_HAS_LINE_QUANTITY_FORMULA=false
PM_HAS_COMPONENT_COST=false
PM_HAS_COMPONENT_FORMULA=false
```

空区域排序探针：

```text
SOURCE_REGION_IDS=[11, 12, null]
SOURCE_REGION_NAMES=[客厅, 走廊, 综合项目]
EMPTY_REGION_LAST=true
```

## 五、逐项判定

### 1. `apply` 同源生成 REGION/TYPE、区域行不丢：PASS

- `QuoteViewContextTest.shouldBuildRegionAndTypeViewsFromSameSnapshot` 通过。
- 独立探针 `REGION_LINES_PRESERVED=true`。
- REGION 与 TYPE 使用同一 `safeItems` 集合。

### 2. TYPE 按 ItemType→Category、跨区域服务端合并：PASS

- TYPE 组使用 `effectiveType` 与规范化 category。
- 使用 U1 `canonicalMergeKey` 与 `typeRowId`。
- 输入顺序反转后 `REVERSED_ID_STABLE=true`、`REVERSED_SOURCES_STABLE=true`。
- 商业字段差异拆行、成本/公式/供应商价不参与分组由 U1 契约测试和 U2 回归覆盖。

### 3. 来源字段排序、去重、空区域最后：PASS

- `sourceLineIds` 按 itemId 数字升序。
- `sourceRegionIds/Names` 按 Space.sort、规范化区域名、spaceId 稳定排序并去重。
- 空区域使用 `null`/“综合项目”并置后。
- `sourceCount` 按原始行计数。

### 4. 数量/金额/成本缺失处理：PASS

- 合并行数量按原始行求和。
- `amount` 缺失时 REGION 组和 TYPE 合并行均为 `null`，没有静默补 0。
- `cost` 缺失时保持 `null`。
- 组件成本缺失时保持 `null`，没有伪造 0。
- 未用合并单价重算金额。

### 5. Provider 字段与角色边界：PASS

- `QuoteUnifiedViewPropertiesTest` 2/2 PASS。
- 报价读取 Provider 暴露 `typeGroups/regionGroups/groupMode/source*`。
- 设计师报价 Provider 不暴露成本字段；项目经理报价 Provider 不暴露 `cost/formula/quantityFormula/costAmount`。
- 独立反射探针确认 PM 的 `QuoteGroup/QuoteLine/QuoteComponent/CatalogItem` 不含相关内部字段。

### 6. 无 DDL、权限、金额公式、状态机和前端接线：PASS

- U2-owned diff 无 SQL/迁移、Provider 鉴权逻辑、金额公式、状态机或 App/Web 代码。
- `@Properties/@AllowProperties` 仅调整响应字段白名单。
- 兼容旧 `constructionGroups/materialGroups/otherGroups` 字段仍保留，属于兼容层而非新事实源。
- 共享 pack 迁移 124 属于并行专项，已在隔离边界外单独标注，不纳入 U2 PASS。

## 六、剩余风险

- 首轮未做真实 Provider JSON/HTTP；第二轮已补 BOSS 报价详情与 PM 项目报价详情 HTTP 2/2。跨企业/门店/非参与项目权限负向和真实页面仍未验证，不能据此宣称越权矩阵或 UI 通过。
- `GAP-053` 的真实非空区域数据仍未验收，不能宣称真实合同区域覆盖完成。
- 兼容分类组仍存在，U3 切换前端消费后应再评估是否保留。
- 共享工作区 pack 迁移 124 与并行快照字段必须在合并/发布前与 U2 变更分离，避免混入 U2 发版边界。

## 七、最终门禁

```text
U1 独立验收：PASS
U2 独立验收：PASS
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
developmentTask.currentSliceStatus：ACCEPTED
U2 acceptance：PASS
U3：允许重新对账后准备；未验收、未完成
```


## 八、第二轮独立复核（2026-10-23）

### 8.1 独立快照与隔离边界

- 基线：`7cf83fa2b526156640ea8c227eb348ab81ca27d1`
- 本轮快照：`/tmp/uov-v2-independent.82211/repo`
- 快照补丁哈希：`1d4381682ea2fb0968d68dd4cd2e69d60e0d2eacfd0e41e5233ad01d1f6a60f7`
- 快照只含 27 个文件：13 个 U1 已验收契约文件、11 个 U2 后端文件、1 个 U2 属性契约测试和 2 个本轮独立探针测试。
- 明确排除：`Quote.java` 的并行套餐快照字段、pack 生命周期/迁移、`QuotePdfRenderer`/PDF 接线、App/Web/业主端及其他域改动。
- `Quote.java` 只带入 `typeGroups` 瞬态字段和存取器；未带入 `packageTemplateCode/version/snapshotHash`。
- 未修改共享工作区实现、DDL、权限、金额/成本/利润业务公式。

### 8.2 首轮冻结快照问题与闭环

任务给出的 `/tmp/uov-v2-src2.jGt9wz` 冻结快照先被独立探针击穿 2 项，不能直接接受：

| 编号 | 冻结快照问题 | 证据 | 当前 U2 范围闭环 |
| --- | --- | --- | --- |
| `F1` | 任一来源 `amount=null` 时被当 0 相加，返回 `100.00` | 独立探针 `expected null but was 100.00` | `QuoteViewContext` 使用可空汇总；缺失金额仍为 `null`；`QuoteViewContextTest.missingSourceAmountShouldRemainUnconfirmable` 与独立探针均 PASS |
| `F2` | PM 实际 HTTP JSON 返回 8 个 `cost` 字段 | 真实 `/api/project/manager/quote/info` 响应 | PM `@Properties` 移除 `cost/formula/quantityFormula/costAmount`；第二轮 HTTP 递归扫描内部字段为 0 |

上述两项均为独立验收发现，不是开发自测结果。当前 U2 范围修复后，F1/F2 关闭。

### 8.3 命令与结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| U2 报价相关回归（9 类） | `mvn -o -q -Dtest=QuoteViewContextTest,QuoteViewContractTest,DesignerQuoteCostContractTest,QuotePdfRendererTest,HybridQuoteBuilderTest,QuoteContextBusinessRulesTest,QuoteCopyContextTest,QuoteWorkbookApplyTest,QuoteWorkbookContextTest test` | PASS，54/54，0 failures / 0 errors / 0 skipped |
| U1 契约回归 | `mvn -o -q -Dtest=QuoteViewContractTest,BusinessFactContractTest test`（本轮总报告中已执行） | PASS，14/14，0 failures / 0 errors |
| U2 Provider 属性契约 | `QuoteUnifiedViewPropertiesTest` | PASS，2/2 |
| 独立单元探针 | `U2IndependentUnitProbeTest` | PASS，2/2 |
| 真实 Provider/HTTP 探针 | `U2QuoteProviderHttpProbeTest` | PASS，2/2，MockMvc + H2 + 真实密码登录；`spring.jpa.open-in-view=false` |
| 去重后本轮测试合计 | 13 个测试类 | **65/65 PASS，0 failures / 0 errors / 0 skipped** |
| JDK 22 打包 | `mvn -o -q -DskipTests package` | PASS，`target/th4-decoboon-1.0.0.jar` 7.6 MB |
| diff check | `git diff --check` | PASS |
| JSON / registry | `jq empty`、issue ID 唯一性、PI-010 状态断言 | PASS |
| 领域账本 | `domain_state.py validate --domain product-system-baseline --strict` | PASS |
| 静态边界 | U2-owned diff 搜索 DDL/SQL、`@DataPermission`、`@Lock`、Provider 鉴权逻辑、金额公式 | PASS：无新增 DDL/迁移、无权限/锁变化、无底层金额/成本/利润公式变化 |

### 8.4 接口级证据

真实登录后调用 `POST /api/quote/info`：

- `regionGroups` 与 `typeGroups` 同时返回；
- `groupMode` 分别为 `REGION/TYPE`；
- TYPE 合并行 `sourceCount=2`、`sourceLineIds=[101,102]`、`sourceRegionIds=[null]`、`sourceRegionNames=["综合项目"]`；
- REGION/TYPE 金额均为 `300`，可由来源行 `100 + 200` 解释；
- 报价头金额仍为 `300`，未重算或改写。

项目经理登录后调用 `POST /api/project/manager/quote/info`：

- 根 `Quote.items` 不序列化；
- `regionGroups/typeGroups` 仍返回；
- 递归扫描 `cost/costAmount/formula/quantityFormula` 为 0；
- 项目经 `manager=PM` 绑定后访问成功；该验证证明投影与白名单在真实 Provider 链路上生效。

### 8.5 逐项符合 `DEC-086` / `BR-052`

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| REGION/TYPE 同源 | PASS | 同一 `safeItems` 生成；HTTP 金额同为 300 |
| TYPE 固定 `ItemType → Category` | PASS | `effectiveType` + `normalizedCategory`；U1 契约回归 |
| TYPE 合并键完整且成本/公式/供应商价不参与分组 | PASS | U1 `canonicalMergeKey` + U2 上下文测试 |
| `typeRowId/sourceLineIds/sourceRegionIds/sourceRegionNames/sourceCount` | PASS | 单元测试与 HTTP 均返回，来源可回溯 |
| 来源排序、去重、空区域最后且不丢行 | PASS | `[101,102]`、空区域最后；顺序反转稳定性测试 PASS |
| 数量/金额按来源汇总，缺失金额不补 0 | PASS | `amount=null` 保持 `null`；组件成本缺失同样为 `null` |
| 场景只裁剪字段，不改变分组/行数/金额 | PASS | 同源投影；PM 保留分组但根 items 隐藏 |
| 设计师/PM 不因统一视图获得成本/公式 | PASS | 属性契约 2/2；PM HTTP 内部字段 0 |
| 无 U2-owned DDL/权限放宽/锁/底层金额公式变化 | PASS | 静态审计；快照无 pack/PDF/前端污染 |
| 跨租户/跨门店/非参与项目、持久化非空区域 | 未覆盖 | 见 8.6 |

### 8.6 未覆盖与风险

- 未执行跨企业、跨门店、非参与项目的 HTTP 403/404 负向；Provider 鉴权代码在 U2 diff 中无变化，但不能据此宣称越权矩阵已全验。
- 未用生产/真实合同数据验证持久化非空区域；本轮非空区域排序由内存对象和接口种子验证，`GAP-053` 仍打开。
- App/Web/业主端、合同事实、项目 PHASE、PDF `version + groupMode`、旧归档、真实 UI/真机和生产发布均不属于 U2，未验收。
- 兼容 `constructionGroups/materialGroups/otherGroups` 仍保留；U3 切换消费后需继续评估，不得成为第二事实源。

### 8.7 最终门禁

```text
U1 独立验收：PASS
U2 独立验收：PASS（仅后端 REGION/TYPE 双投影）
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
GAP-065：OPEN
UOV-V3：允许重新对账后准备；U3 整体未验收
```
