# 统一对象视图一期 U2 · 服务端 REGION/TYPE 双投影开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 批次 | `U2` 报价服务端 REGION/TYPE 双投影 |
| 日期 | 2026-10-22 |
| 结论 | `DEV_VERIFIED`，等待独立验收 |
| 前置 | U1 已独立验收 PASS |
| 业务裁决 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 数据变更 | 无 DDL、无迁移 |
| 验收边界 | 开发自测不等于独立验收；U3 暂不启动 |

## 一、U2 实现结果

1. `QuoteViewContext.apply` 同时生成：
   - `regionGroups`：保留原始区域行；
   - `typeGroups`：`ItemType → Category`，服务端跨区域合并；
   - 既有 `constructionGroups/materialGroups/otherGroups` 兼容字段。
2. `applyManagerView` 保留区域和类型事实，但顶层不返回原始 `items`。
3. TYPE 合并使用 U1 `QuoteViewContract.canonicalMergeKey` 和稳定 `typeRowId`。
4. TYPE 合并行返回来源：
   - `sourceLineIds` 按 `itemId` 升序；
   - `sourceRegionIds/sourceRegionNames` 按区域排序去重；
   - `sourceCount` 为来源行数；
   - 空区域固定“综合项目”并排在最后。
5. 合并行数量、金额、成本均来自原始行服务端汇总；金额或成本缺失时保持 `null`，不以 0 伪装。
6. 组件销售签名一致时合并数量/成本；组件成本缺失时保持 `null`。
7. 报价读取 Provider 暴露 `typeGroups`、`groupMode` 和 U1 来源字段。
8. 项目经理报价 Provider 移除成本、组件成本、成本和工程量公式字段，避免 PM 内部价格泄漏；BOSS 成本查看继续走内部报价详情。

## 二、代码范围

### 核心实现

- `src/main/java/com/th4/edge/decoboon/quote/context/QuoteViewContext.java`
- `src/main/java/com/th4/edge/decoboon/quote/model/Quote.java`
- `src/main/java/com/th4/edge/decoboon/quote/model/QuoteGroup.java`
- `src/main/java/com/th4/edge/decoboon/quote/model/QuoteLine.java`
- `src/main/java/com/th4/edge/decoboon/quote/repository/QuoteLineRepository.java`

### 报价 Provider

- `src/main/java/com/th4/edge/decoboon/quote/service/quotes/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/quote/service/quotes/designerinfo/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/quote/service/quotes/save/support/Save.java`
- `src/main/java/com/th4/edge/decoboon/quote/service/lines/batchsave/support/BatchSave.java`
- `src/main/java/com/th4/edge/decoboon/quote/service/workbook/apply/support/Apply.java`
- `src/main/java/com/th4/edge/decoboon/project/service/manager/quotes/info/support/Info.java`

### 测试

- `src/test/java/com/th4/edge/decoboon/trade/quote/QuoteViewContextTest.java`
- `src/test/java/com/th4/edge/decoboon/quote/QuoteUnifiedViewPropertiesTest.java`

## 三、验证命令与结果

隔离快照：

```text
/tmp/unified-object-u1-recheck.nGf3ac
```

命令：

```text
mvn -o -q -Dtest=QuoteViewContextTest,QuoteUnifiedViewPropertiesTest,QuoteViewContractTest,BusinessFactContractTest test
mvn -o -q -DskipTests compile
```

结果：

| 测试 | 结果 |
| --- | --- |
| `QuoteViewContextTest` | 7/7 PASS |
| `QuoteUnifiedViewPropertiesTest` | 2/2 PASS |
| `QuoteViewContractTest`（U1 回归） | 9/9 PASS |
| `BusinessFactContractTest`（U1 回归） | 5/5 PASS |
| 合计 | **23/23 PASS** |
| 编译 | PASS |
| `git diff --check` | PASS |

## 四、覆盖的正反向场景

- 同一报价同时生成 REGION/TYPE，区域行不丢。
- 跨区域同款合并，数量/金额/成本等于来源行之和。
- `differenceStatus/craft/discount/fixedTotalPrice/组件销售签名` 任一不同不合并。
- 合并结果与输入顺序无关，`typeRowId` 和来源排序稳定。
- 空区域进入“综合项目”并排在最后。
- 金额缺失时合并行和组金额为 `null`，显示“暂不可确认”。
- 组件成本汇总；组件成本缺失时不伪造 0。
- 项目经理报价 Provider 不暴露成本、成本完整性、成本和工程量公式。
- 设计师报价 Provider 继续不暴露成本。
- 内部报价详情仍保留成本字段，由既有角色/数据范围控制。

## 五、残留风险与后续

- 未修改 App/Web/业主/PDF，不宣称跨端已统一；属于 U3/U6。
- 区域真实非空数据覆盖仍受 `GAP-053` 约束，不能宣称区域分组已在真实合同上验收。
- `QuoteViewContext` 仍保留兼容分类组；U3 前端完成消费后可再评估删除。
- 当前共享工作区存在其他专项未提交改动，U2 结论只对应本文列出的 U2 文件和隔离快照。
- 独立验收 PASS 前不得启动 U3。
