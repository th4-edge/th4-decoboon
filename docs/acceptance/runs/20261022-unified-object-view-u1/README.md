# 统一对象视图一期 U1 · 开发验证证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 批次 | `U1` 统一投影契约 |
| 日期 | 2026-10-22 |
| 结论 | `DEV_VERIFIED`，等待独立验收 |
| 业务裁决 | `ANS-BIZ-20261022-007`、`T12-BIZ-20261022-007`、`DEC-086`、`BR-052` |
| 数据变更 | 无 DDL、无迁移、无历史数据回填 |
| 验收边界 | 开发自测不等于独立验收，不标记 `CLOSED`，不进入 U2 |

## 一、U1 冻结内容

1. 报价分组方式：`REGION / TYPE`。
2. 报价场景：`EDIT / READ / OWNER / PROJECT / CONTRACT / SUMMARY / PRINT`。
3. 场景字段/动作边界：
   - `OWNER` 不返回完整原始 `items`，不返回成本、内部公式和 `acceptedBy`；
   - `CONTRACT` 不返回明细分组，只允许跳统一报价视图；
   - `PROJECT` 默认 `REGION`，保留版本轨；
   - `PRINT` 必须绑定 `quoteId + version + groupMode`。
4. TYPE 合并契约：
   - 固定 `ItemType → Category`；
   - 目录项以 `catalogItem.itemId` 为强身份；
   - 自定义行使用 `CUSTOM + 完整业务描述`；
   - 商业字段、固定总价、差异状态、折扣、工艺和组件销售签名参与合并键；
   - 成本、组件成本、实际成本、供应商价和内部公式不参与合并键。
5. 来源字段：`typeRowId / sourceLineIds / sourceRegionIds / sourceRegionNames / sourceCount`，在 `QuoteLine` 上均为 `@Transient`。
6. PDF 契约：必填 `quoteId / version / groupMode`，默认 `REGION`，冻结客户字段白名单和禁止字段。
7. 事实节点契约：8 个固定阶段、默认事实类型、合同上游顺序、稳定排序、来源/版本/可见性/跳转字段。
8. 直接签约不在 U1 新增来源字段或统计隔离；只冻结可证明的展示标签。
9. 在跑承接使用既有 `BusinessOrigin.Carryover`，U1 不改统计查询。

## 二、代码产物

### 新增

- `src/main/java/com/th4/edge/decoboon/quote/configure/QuoteViewMode.java`
- `src/main/java/com/th4/edge/decoboon/quote/configure/QuoteViewScene.java`
- `src/main/java/com/th4/edge/decoboon/quote/configure/QuoteViewAction.java`
- `src/main/java/com/th4/edge/decoboon/quote/model/QuoteViewContract.java`
- `src/main/java/com/th4/edge/decoboon/quote/model/QuotePrintContract.java`
- `src/main/java/com/th4/edge/decoboon/common/configure/FactStage.java`
- `src/main/java/com/th4/edge/decoboon/common/configure/FactType.java`
- `src/main/java/com/th4/edge/decoboon/common/configure/FactVisibility.java`
- `src/main/java/com/th4/edge/decoboon/common/model/BusinessFactNode.java`
- `src/main/java/com/th4/edge/decoboon/common/model/BusinessFactContract.java`
- `src/test/java/com/th4/edge/decoboon/quote/QuoteViewContractTest.java`
- `src/test/java/com/th4/edge/decoboon/common/BusinessFactContractTest.java`

### 修改

- `src/main/java/com/th4/edge/decoboon/quote/model/QuoteLine.java`
  - 仅新增 5 个 `@Transient` 投影来源字段及 getter/setter，不新增数据库列。

## 三、验证命令与结果

由于共享工作区存在多个并行 Maven 进程和未提交改动，验证在隔离快照中执行，避免共享 `target/` 竞争。

```text
临时工作区：/tmp/unified-object-u1.rLBWWk
mvn -o -q -Dtest=QuoteViewContractTest,BusinessFactContractTest test
结果：11/11 PASS
- QuoteViewContractTest：7/7 PASS
- BusinessFactContractTest：4/4 PASS

mvn -o -q -DskipTests compile
结果：PASS

git diff --check（U1 文件）
结果：PASS
```

补充隔离回归中 `DesignerQuoteCostContractTest` 为 `1/1 PASS`，用于确认 U1 未扩大设计师成本可见性。

## 四、并行冲突记录

1. 当前共享工作区同时存在 `U2` 方向的改动：
   - `QuoteViewContext.java`
   - `Quote.java`
   - `QuoteGroup.java`
   - `QuoteViewContextTest.java`
2. 隔离快照中并行 `QuoteViewContextTest.shouldMergeOnlySameBusinessLinesAndKeepSortedSources` 有 `1` 个失败（期望 `2000.00`，实际 `200`）。
3. 该失败不属于 U1 文件，也不代表 U2 通过；U1 不把该结果归入自身验证。
4. `QuoteViewContract.java` 被并行 U2 追加了 `normalizedCategory` 等消费方法；U1 定向契约测试在该最新文件状态下通过。
5. 按 U1→U6 顺序纪律，本批次不修复、不验收、不宣称 U2。

## 五、未覆盖风险

- 未执行真实 Provider、App、Web、业主端或 PDF 页面验证；这些属于 U3/U6。
- 未执行跨企业、跨门店、非参与项目负向接口验证；U1 仅冻结字段和动作契约。
- 未验证 `criterion` 是否未来进入报价行快照；当前已由 `DEC-086` 明确不纳入 U1。
- 区域真实非空数据的 TYPE 合并展示仍受 `GAP-053` 约束。
- 并行 U2 改动存在同工作区冲突风险，后续批次必须先重新对账再执行。

## 六、门禁结论

```text
U1 开发自测完成
→ 等待独立复核 U1 契约与测试
→ 未 PASS 前不得进入 U2
→ 不得将 PI-20261022-010 标记 CLOSED
```


## 七、首轮独立验收退回修复

首轮独立验收结论为 `FAIL`，证据见 `docs/acceptance/runs/20261022-unified-object-view-u1-independent/README.md`。

已修复：

1. `P1-01`：`canonicalMergeKey` 从分隔符拼接改为“长度 + 冒号 + 原值”的无歧义编码；组件销售签名同样长度编码。
2. `P1-02`：`QuoteViewScene.READ.exposesVersionTrack()` 修正为 `true`。
3. `P1-03`：`BusinessFactNode` 增加 `projectId`；`BusinessFactContract.validateNode` 同时校验 `projectId、nodeKey、returnPath、returnAnchor`。
4. `P2-01`：补充分隔符碰撞、READ 版本轨、真实区域差异、`projectId` 与导航绑定缺省拒绝测试。

修复后隔离验证：

```text
QuoteViewContractTest       9/9 PASS
BusinessFactContractTest    5/5 PASS
合计                       14/14 PASS
mvn -o -q -DskipTests compile   PASS
```

当前状态：`REMEDIATED_PENDING_REACCEPTANCE`，未进入 U2。
