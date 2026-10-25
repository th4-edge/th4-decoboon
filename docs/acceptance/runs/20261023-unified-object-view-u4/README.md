# 统一对象视图一期 U4 · 合同事实追踪开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 批次 | `U4` 合同上游与下游事实追踪 |
| 日期 | 2026-10-23 |
| 结论 | `DEV_VERIFIED`，等待独立验收 |
| 前置 | U1、U2、U3 独立验收 PASS |
| 业务裁决 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 数据变更 | 无 DDL、无迁移 |
| 验收边界 | U5 项目 PHASE 未启动；真实页面和跨租户负向留 U6 |

## 一、实现结果

1. `ContractWorkbench` 新增：
   - `upstreamFacts`
   - `downstreamFacts`
   - `sourceGaps`
2. 合同上游事实按正式顺序生成：
   - 客户
   - 房屋
   - 最近一次已完成量房
   - 报价绑定的设计方案
   - 合同绑定的报价版本
   - 合同
3. 量房事实：
   - 只取 `Completed`；
   - 按 `completedAt desc, measureId desc`；
   - 计算原始区域数、实测区域数、待补数；
   - 无已完成量房显示“无系统量房记录”，不伪造 0。
4. 设计/报价版本：
   - 设计沿 `quote.design` 绑定，不使用最新方案；
   - 报价沿 `ProjectContract.quote` 绑定，不使用最新报价；
   - 无设计来源显示“无设计方案来源”，不声称客户一定为直接签约。
5. 下游事实摘要：
   - 项目
   - 增减项聚合
   - 材料计划数量
   - 验收数量
   - 应收/回款
   - 成本完整性
6. App 合同页新增“签约依据与履约事实”：
   - 展示上游和下游事实卡、版本、状态、时间、责任人、摘要和金额；
   - 缺失来源明确显示；
   - 报价事实可跳统一报价详情。
7. Web 合同交接新增合同事实链区块：
   - 选中合同后读取 `project/contract/workbench` 的事实投影；
   - 上游/下游分栏展示，报价节点可跳统一报价详情。

## 二、验证

### 后端

```text
mvn -o -q -Dtest=ContractFactTraceTest test
结果：2/2 PASS

mvn -o -q -DskipTests compile
结果：PASS
```

覆盖：
- 绑定设计 V3 + 报价 V2 上游顺序；
- 量房实测/待补计数；
- 下游项目/材料/验收/成本事实；
- 无量房、无设计时只输出来源缺口，不伪造对象节点；
- 合同金额不因事实链装配改变。

### App

```text
npm run type-check          PASS
npm run check:document      PASS
npm run build:mp-weixin     PASS
```

### Web

```text
npm run type-check          PASS
npm run check:client-copy   PASS
npm run check:actions       PASS
npm run build               PASS
```

## 三、边界

- 不新增 DDL、权限语义、金额/成本/利润公式或状态机。
- 事实节点只做索引和摘要，不复制完整报价、设计或材料明细。
- 直接签约只显示可证明的“无设计方案来源”，不新增来源字段或统计隔离。
- 项目 PHASE 全链、历史折叠和返回锚点仍属 U5。
- 真实 App/Web 页面、真机截图、跨企业/门店越权仍属 U6。


## 四、首轮独立验收退回修复

首轮独立验收 FAIL，修复如下：

1. `P1-01` 前置：U3 独立验收报告已为 PASS，registry 中旧 `FAIL_REMEDIATED_PENDING_REACCEPTANCE` 口径已按已落盘报告校正为 ACCEPTED。
2. `P1-02` 成本事实：
   - `ContractWorkbenchContext.build(contract, includeInternalCost)` 增加角色参数；
   - 仅 BOSS/店长/财务生成 Cost 下游事实并计算成本；
   - 设计师/项目经理合同事实链不返回 Cost 事实，`workbench.cost=null`。
3. `P2-01` 承接标签：固定输出“项目承接（接管基线）”。
4. `P2-02` 无项目边界：
   - 合同尚未立项时，上游事实 `projectId` 允许为空；
   - 每个无项目事实必须带“项目尚未建立，事实按合同绑定关系展示”；
   - `BusinessFactContract.isPreProjectFact` 显式表达该豁免。

修复后隔离回归：

```text
ContractFactTraceTest 3/3 PASS
相关后端回归合计 19/19 PASS
mvn -o -q -DskipTests compile PASS
```

> 独立复验校正（2026-10-23）：上述“U3 独立验收报告已为 PASS”与当前落盘事实不符。`20261023-unified-object-view-u3-independent/README.md` 仍为 FAIL，`u3-remediation/README.md` 仍为待独立复验。第二轮 U4 因此保持 FAIL/BLOCKED，详见 `20261023-unified-object-view-u4-reacceptance/README.md`。


### 第二轮复验修复

1. `BusinessFactContract.validateNode` 已正式接受 `isPreProjectFact` 豁免：只有无项目合同上游事实且带明确 uncertainty 时允许 `projectId=null`；其它事实仍强制项目绑定。
2. 合同 Provider 根对象统一移除 `signedCost/costComplete`；授权成本只通过角色裁剪后的 `workbench.cost` 暴露。
3. 增加 `ContractFactPropertiesTest`，锁定合同工作台/详情/分页根响应不含成本字段。
4. 修复后定向回归：
   ```text
   ContractFactTraceTest 3/3
   ContractFactPropertiesTest 1/1
   BusinessFactContractTest 5/5
   编译 PASS
   ```
