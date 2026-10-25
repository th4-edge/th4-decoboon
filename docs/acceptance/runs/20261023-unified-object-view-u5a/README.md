# 统一对象视图一期 U5-A · 项目事实契约开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U5-A` 项目事实追踪契约 |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED / ACCEPTANCE_PENDING` |
| 边界 | 只做契约与排序/分组/权限过滤；不聚合真实节点、不改页面、无 DDL |
| 隔离目录 | `/tmp/u5a.L4Lhdd` |

## 一、代码产物

### 新增

- `src/main/java/com/th4/edge/decoboon/project/configure/ProjectFactViewMode.java`
- `src/main/java/com/th4/edge/decoboon/project/model/ProjectFactTrace.java`
- `src/main/java/com/th4/edge/decoboon/project/model/ProjectFactTraceContract.java`
- `src/test/java/com/th4/edge/decoboon/project/ProjectFactTraceContractTest.java`

### 修改

- `src/main/java/com/th4/edge/decoboon/project/model/Project.java`
  - 仅增加瞬态 `factTrace` 投影字段与 getter/setter；
  - 尚未加入 Provider `@Properties`，U5-A 不输出到接口。

## 二、冻结契约

1. 项目事实追踪固定 8 阶段：来源、售前、签约、履约、变更、验收、财务、售后。
2. 默认视图 `PHASE`，预留时间线、关系链、异常视图。
3. 节点排序沿用 `BusinessFactContract.stableOrder()`。
4. 业主投影只保留 `FactVisibility.OwnerVisible`。
5. 节点必须绑定同一 `projectId`、`nodeId`、`objectType/objectId/version`、`returnPath/returnAnchor`。
6. 来源缺口必须声明匹配的 `stage/factType`、标签和原因。
7. 节点 `nodeId` 重复或跨项目绑定直接拒绝。

## 三、验证

```text
ProjectFactTraceContractTest
BusinessFactContractTest
合计：9/9 PASS，0 failures / 0 errors / 0 skipped

JDK 22 package：PASS
```

覆盖：

- 8 阶段固定顺序与 PHASE 默认视图；
- 稳定排序；
- 业主可见性过滤；
- 跨项目节点拒绝；
- 重复 nodeId 拒绝；
- 来源缺口 stage/factType 一致性；
- 既有 `BusinessFactContract` 回归。

## 四、未覆盖

- U5-B 项目事实聚合；
- `project/project/info` 事实投影返回；
- App/Web/业主页面接线；
- 真实 Provider/HTTP、真实页面和跨租户负向；
- U5 独立验收。

## 五、门禁

```text
U1/U2/U3/U4：PASS / ACCEPTED
U5-A：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-B~F：NOT_STARTED
U6：未启动
GAP-065：OPEN
```
