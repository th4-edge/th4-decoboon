# 统一对象视图一期 U5-C · App 项目 PHASE 视图开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U5-C` App 项目详情 PHASE 视图 |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED / REACCEPTED_PASS` |
| 边界 | 只修改 App 项目详情与项目域模型/API；不改 Web、后端、DDL、Provider、权限或金额公式 |
| 隔离目录 | `/tmp/u5c-app.5LTWLu` |

## 一、业务依据

- `ANS-BIZ-20261022-007` / `T12-BIZ-20261022-007`
- `DEC-086 §5`：固定 8 阶段、当前/异常展开、历史/替代/作废折叠
- `BR-052 §4`：稳定排序、节点版本与项目绑定、财务摘要、业主裁剪
- U5-B1/U5-B2 后端事实索引已 `DEV_VERIFIED`，本切片只消费服务端事实，不在 App 重算金额、成本或利润

## 二、实现结果

### 2.1 数据契约

- `Project.factTrace` 增加 `ProjectFactTrace` 类型。
- 节点完整映射 `nodeKey/current/stage/factType/objectId/version/status/businessTime/recordedAt/amount/costComplete/profitAmount/grossMargin/uncertainty/relations/navigationPath/returnPath/returnAnchor`。
- `normalizeProjectFactTrace()` 统一归一化枚举、日期、金额、数量、成本完整性和来源缺口。

### 2.2 App 项目详情

- 员工端新增“事实”分段；业主端标签不变，不提前消费 U5-E 的 Owner 投影。
- 固定 8 阶段：来源、售前、签约、履约、变更、验收、财务、售后。
- 最近发生事实的阶段默认展开；存在必须补齐缺口或当前异常的阶段默认展开。
- 同一 `nodeKey` 只显示一张当前摘要卡，历史版本卡内展开。
- 缺失事实显示“必须补齐/可选”和具体原因；事实接口缺失显示“暂不可确认”，不使用空列表伪装。
- 节点显示业务时间、记录时间兜底提示、责任人、金额、数量、进度、成本完整性和利润/毛利率摘要。

### 2.3 跳转与返回

- 客户、房屋、量房、设计、报价、合同、阶段、任务、日报、选材、材料计划、采购/到货、变更、验收、财务、意见等按现有 App 路由跳转。
- 跳转携带 `returnPath` 与 `returnAnchor`；返回后重新进入“事实”分段并滚动到原节点。
- 当前项目自身节点不伪造跳转；无法定位来源对象的节点不显示“查看来源”。
- 财务节点仅在 `canViewFinance` 权限下可跳转。

## 三、验证

```text
npm run type-check                    PASS
npm run build:mp-weixin               PASS
npm run check:subpackages             PASS
npm run check:navigation              PASS
U5-C UI source guard                  PASS

构建体积：
主包 1,026,334 B
最大分包 724,747 B
总包 3,980,825 B
主包 14 页 / 17 个分包 / 173 页
```

已知无关失败：

- `npm run check:ui`：`src/App.vue:66` 直接调用 `uni.showModal`，非 U5-C 修改文件。
- `npm run check:owner-preview`：`teamLeader` 缺少 `view:owner-preview`，与既有 TeamLeader 权限并行改动相关，非 U5-C 引入。

## 四、未覆盖

- 真实登录账号、真实后端数据、微信开发者工具/真机截图；
- 多机型布局、长文本、超长历史版本和弱网恢复；
- 跨企业/门店/非参与项目越权；
- 业主项目 journey 消费统一事实白名单（U5-E）；
- Web 项目执行工作面（U5-D）；
- U5-F 独立验收与端到端 SQL 预算测试。

## 五、门禁

```text
U1/U2/U3/U4：PASS / ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED
U5-C：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-D~F：NOT_STARTED
U6：NOT_STARTED
GAP-065：OPEN
GAP-071：OPEN
PI-20261022-010：DEV_IN_PROGRESS
```

## 六、首轮独立验收退回修复（2026-10-23）

1. `P1-01`：`project/project/info` 的 Project/Quote 根白名单移除 `cost`；新增 `ProjectFactTracePropertiesTest` 根成本字段断言。
2. `P1-02`：App 不再对服务端 `trace.nodes` 二次排序；缺失/未知 stage/current/version/nodeKey/factType/projectId/visibility 改为契约错误，不填默认值。
3. 开发验证：后端 19/19、成本/边界探针 3/3、JDK22 package PASS；App type-check/build/subpackages/navigation PASS；UI/同源探针 9/9 PASS。
4. 第二轮独立验收：`docs/acceptance/runs/20261023-unified-object-view-u5c-reacceptance/README.md`，PASS / U5-C ACCEPTED。
