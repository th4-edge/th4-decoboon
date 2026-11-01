# CHG-20261031-046 · 业主与小程序合同行分页同源

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-046` |
| 类型 | 缺陷修复 / 后端分页 / 小程序消费治理 |
| 风险级别 | L1（单合同读取链，跨后端与小程序） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-042`、`CHG-20261031-045`、`DEC-036`、`DEC-055`、`BR-003` |

## 2. 目标与范围

- 目标：让业主合同行与员工合同行使用同一数据库分页规则；小程序合同页显式请求当前页/页大小，头部行数与金额只消费 workbench 服务端汇总。
- 范围内：`customer/contract-line/page` Pageable、`customer/contract/workbench` 行摘要白名单、App 合同行 API、App 合同履约页、跨端分页门禁。
- 非范围：不改变合同对象、金额公式、权限矩阵、变更规则和成本口径；不处理其他仍全量读取合同行的内部上下文；不发布。
- 受影响端：后端、小程序。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及合同行读取范围和业主可见字段，不改变金额/权限语义。
- 业务事实来源与裁决编号：`DEC-036` 分页与聚合、`DEC-055` 当前有效剩余、`BR-003` 成本边界。
- 尚存 GAP 或假设置：无。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 业主默认分页 | 未传 current/size | 第 1 页 20 条，`sort,lineId` 稳定排序 | `backend-test.log` |
| 2 | 业主大页边界 | 请求 size=500 | 服务端截为 50 | `backend-test.log`、`api-customer-contract-lines.json` |
| 3 | 业主汇总 | 已生效合同 | workbench 6 项 / ¥977.80；cost 不出现 | `customer-workbench.json` |
| 4 | 业主相邻页 | size=2 请求前两页 | 行 ID 不重复、不遗漏 | `api-customer-contract-lines.json` |
| 5 | App 请求契约 | Boss 打开合同履约页 | 发送 `current:1,size:20`，不使用固定大页 | `app-h5-contract-line-request.txt` |
| 6 | 小程序原生页面 | 微信开发者工具真实运行 | 显示 6 项、合计 ¥978、全部加载；4/4 检查 PASS | `app-contract-check.json`、`app-contract-native.png` |
| 7 | 回归门禁 | App 源码 | 缺失分页、当前页求和、静默空数组等 5 个负向样本被拦截 | `app-contract-line-gate-self-test.log` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `customer/service/contractlines/page/support/Page.java`、`customer/service/contract/workbench/support/Workbench.java`、`CustomerContractLinePaginationProviderTest` |
| th4-decoboon-web | `dev` | 无改动 | 不适用 |
| th4-decoboon-app | `dev` | 未提交工作区 | `domains/trade/api.ts`、`domains/trade/model.ts`、`subpackages/project/pages/contract.vue`、`scripts/verify-contract-line-pagination-contract.mjs`、`package.json` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -Dtest=CustomerContractLinePaginationProviderTest,ContractFactTraceTest test` | PASS，7/7 | `backend-test.log` |
| Provider 响应门禁 | `audit-provider-response-contract.py --check` + `--self-test` | PASS，543 Provider / 0 finding | `provider-response-audit.txt`、`provider-response-self-test.txt` |
| 前端 Provider 契约 | `audit-frontend-provider-contract.py --check` | PASS，unmatched=0 | `frontend-provider-contract-check.txt` |
| App 类型检查 | `npm run type-check` | PASS | `app-type-check.log` |
| App UI/分包 | `npm run check:ui`、`npm run check:subpackages` | PASS；主包 1,079,102B，总包 4,228,648B | `app-ui-check.log`、`app-subpackages-check.log` |
| 小程序构建 | `npm run build:mp-weixin` | PASS | `app-mp-build.log` |
| 分页门禁 | `check:pagination`、`check:contract-line-pagination`、`:self-test` | PASS；5/5 负向样本被拦截 | `app-*-check.log`、`app-contract-line-gate*.log` |
| 真实业主 API | 临时 Wx 业主登录后请求 | PASS；临时业主凭证/授权已清理为 0 | `api-customer-contract-lines.json`、`cleanup.txt` |
| 小程序原生 | 微信开发者工具 `subpackages/project/pages/contract` | PASS，4/4 | `app-contract-check.json`、`app-contract-native.png` |

- 未执行项及原因：真实合同只有 6 行，原生页面未触发第二页；相邻页由后端单测和真实业主 API `size=2` 验证。独立验收、视觉签字和发布未执行。
- 已知失败及是否与本次相关：H5 开发运行器存在 uni-h5 `clientWidth` 空指针和 favicon 404，页面数据仍正常；不作为小程序原生通过依据，也不扩大为本批缺陷。

## 7. 发布

- 不发布；仅本地验证。无 DDL、迁移、配置和密钥变化。

## 8. 回滚

- 回滚条件：业主合同行越权、成本泄露、合同汇总不一致或小程序合同页无法加载。
- 回滚命令/步骤：回退业主 Pageable、workbench 行摘要白名单和 App 合同行 API/页面改动；不得恢复无界全量合同行查询。
- 回滚验证：重跑 7 条后端测试、App 类型/门禁/构建和微信开发者工具合同页。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次业主 API 验证脚本参数拼接错误，脚本修正后通过，业务实现一次通过
- 返工原因：测试脚本 curl 参数错误
- 最终结论：开发自测、真实业主 API 和小程序原生页面功能验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，前后端分页另一客户端未同步消费属于同一类跨端契约缺陷
- 是否需要新增门禁：是，新增合同行 App 分页契约门禁并内置 5 个负向样本
- 候选 `EXP/ANT/PATH`：`ANT-App合同行不带current/size`、`PATH-复用合同行Pageable+workbench汇总+跨端契约门禁`
- 后续动作与负责人：执行 DQ-X9 独立验收；后续逐个分类 DocumentArchiveContext、ProjectContext、ProjectContractContext、Range 中的合同行全量读取
