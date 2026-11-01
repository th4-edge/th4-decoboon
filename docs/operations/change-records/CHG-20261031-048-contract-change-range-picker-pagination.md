# CHG-20261031-048 · 增减项来源选择器分页与批量快照

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-048` |
| 类型 | 重构 / 性能结构治理 / 跨端契约 |
| 风险级别 | L2（当前有效剩余、待审批占用和金额读取实现） |
| 状态 | `待发布` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | DQ-X11、`DEC-036`、`DEC-037`、`DEC-055`、`BR-041` |

## 2. 目标与范围

- 目标：`project/contract-change/range` 改为 PICKER_PAGE 响应；原合同行走数据库分页，来源筛选在服务端完成；变更明细只加载一次后批量计算所有来源快照。
- 范围内：Range Provider 分页/筛选/facets、批量快照重载、Web/App 来源选择器、跨端静态门禁、真实 API 和原生页面验证。
- 非范围：不改变当前有效剩余、待审批占用、金额和阻断原因公式；不改变提交/预览复核规则；不发布。
- 受影响端：后端、Web、小程序。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及合同变更当前有效剩余、待审批占用和剩余金额的读取方式，不改变公式。
- 业务事实来源与裁决编号：`DEC-055` 当前有效剩余、`BR-041`、`DEC-036` 分页/聚合、`DEC-037` 先测量后优化。
- 尚存 GAP 或假设：已生效变更目标仍需从“一次合同变更明细批量加载”结果中切页；原合同行已真实数据库分页。后续若变更历史规模显著增长，再做变更目标 SQL 聚合分页。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 默认分页 | 7 个可选来源 | current=1、size=20、total=7、6 原合同 + 1 已生效目标 | `api-probe.json` |
| 2 | 相邻页 | size=2，页 1/2/4 | 返回 2/2/1 条，ID 无重复遗漏 | `validation.txt` |
| 3 | 上限 | size=500 | 截为 50 | `capped.json` |
| 4 | 分组 | group=effective | 只返回 1 个已生效目标 | `effective_only.json` |
| 5 | 类型筛选 | itemType=Material | 返回 2 原合同 + 1 已生效目标，共 3 | `material.json` |
| 6 | 关键词 | 全屋保洁 | 返回 1 条 | `keyword.json` |
| 7 | 非法筛选 | 非法 group/type | 返回业务 400，不返回来源数据 | `api-probe.json` |
| 8 | 批量快照 | 多来源 | 变更明细仓储只调用一次 | `RangeProviderTest`、`ContractChangeRangeContextTest` |
| 9 | Web | 打开来源选择器并筛主材 | 请求 current/size/itemType，页面 3 项 | `web-range-filter-material.png`、请求体 |
| 10 | 小程序原生 | 微信开发者工具筛主材 | 初始 7 项、筛选后 3 项，2/2 PASS | `app-native-check.json`、截图 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `ContractChangeRange`、`ContractLineRepository`、`ContractChangeRangeContext`、`contract-change/range` Provider、Range/Context tests |
| th4-decoboon-web | `dev` | 未提交工作区 | contract API、`DesignerContractChangesView.vue`、`check-contract-range-pagination.mjs`、`package.json` |
| th4-decoboon-app | `dev` | 未提交工作区 | trade API、`change-create.vue`、`verify-contract-range-pagination-contract.mjs`、`package.json` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | RangeProviderTest + ContractChangeRangeContextTest | PASS | `backend-targeted-test.log` |
| L2 全量测试 | `mvn test` | PASS，1186/1186 | `backend-full-test.log` |
| Provider 门禁 | response check + self-test + frontend contract | PASS，543 Provider / 0 finding | `provider-response-*.txt`、`frontend-provider-contract-check.txt` |
| Web 构建 | `npm run build` | PASS（含新分页契约门禁） | `web-build.log` |
| App 构建 | `check:ui`、pagination、range gate/self-test、type-check、`build:mp-weixin`、分包 | PASS；总包 4,229,223B | `app-*.log` |
| 真实 API | Boss、真实合同 | PASS，P50 39.98ms / P95 43.49ms / 3,038B | `api-probe.json`、`api-timings.json` |
| Web 页面 | 发起减项 → 来源选择 → 主材 | PASS，3 项；console 0 error | 截图、请求记录 |
| 小程序原生 | 微信开发者工具同链路 | PASS，2/2 | `app-native-check.json`、`app-range-native-material.png` |

- 未执行项及原因：真实合同只有 7 个来源，Web 分页器未在真实数据下翻页；分页窗口由真实 API size=2 验证。视觉签字和发布未执行。
- 已知失败及是否与本次相关：独立初验发现 `DQ-X11-P1-RANGE-PAGINATION`（连续翻页漏行并重复已生效目标），已修复并复验关闭；首次 API 校验脚本的字符串数值比较问题不影响业务结论；H5 开发运行器既有 uni-h5 控制台错误不作为原生页面依据。

## 7. 发布

- 不发布。无 DDL、迁移、配置、密钥和业务数据写入。

## 8. 回滚

- 回滚条件：来源数量/当前有效剩余不一致、筛选分页错漏、权限泄露或选择器无法完成选择。
- 回滚命令/步骤：回退 Range 分页/批量快照和两端选择器；不得恢复逐来源 repo 查询或无界响应作为生产兜底。
- 回滚验证：重跑 RangeProviderTest、ContractChangeRangeContextTest、L2 全量、Web/App 构建和来源选择链。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；独立验收发现分页 P1，修复后复验通过
- 返工原因：组合分页将全局元素 offset 误作 PageRequest 零基页码
- 最终结论：独立验收通过，P1 已关闭，待发布

## 10. 复盘与沉淀

- 是否出现重复问题：是，Picker 全量响应和逐来源查询属于同一类分页/聚合缺陷
- 是否需要新增门禁：是，Web/App 各新增合同来源分页门禁并内置 4 个负向样本
- 候选 `EXP/ANT/PATH`：`ANT-Range逐来源查询`、`ANT-全局offset误作零基页码`、`PATH-单批变更明细+Picker分页+服务端facets+连续页覆盖测试`
- 后续动作与负责人：待用户确认发布；若变更历史增长，继续把已生效变更目标改成聚合 SQL 分页

