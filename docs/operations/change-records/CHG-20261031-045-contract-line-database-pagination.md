# CHG-20261031-045 · 合同基线行数据库分页

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-045` |
| 类型 | 缺陷修复 / 后端分页 / 前端消费治理 |
| 风险级别 | L1（单业务读取链 + 既有合同 Provider） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-042`、`CHG-20261031-044`、`DEC-036`、`DEC-055`、`BR-003` |

## 2. 目标与范围

- 目标：将 `project/contract-line/page` 从固定大页全量读取改为数据库真实分页；合同档案只展示当前页；合同变更来源不再加载合同行，只消费 `project/contract-change/range`。
- 范围内：合同行仓储 Pageable 查询、Provider 默认/上限与稳定排序、成本裁剪测试、Web `ContractLinePage` 契约、合同档案分页组件、合同变更来源降级删除、真实页面与 API 证据。
- 非范围：不改变合同行字段、金额公式、合同状态机、权限矩阵、变更试算和提交规则；不处理其他仍存在全量合同行读取的上下文；不发布。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及合同行读取范围与成本字段可见性，不改变金额或权限口径。
- 业务事实来源与裁决编号：`DEC-036` 分页/聚合、`DEC-055` 当前有效剩余、`BR-003` 成本边界。
- 尚存 GAP 或假设置：无；合同变更来源在 range 失败时显示错误并停止选择，不再用合同行大页或前端推算兜底。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 默认分页 | 未传 current/size | 第 1 页、20 条、`sort,lineId` 稳定排序 | `backend-test.log` |
| 2 | 上限边界 | 请求 size=500 | 服务端截为 50；第二页 offset=50 | `backend-test.log` |
| 3 | 翻页窗口 | 请求相邻两页 | 请求窗口不重叠，排序稳定 | `backend-test.log`、`contract-lines-*-page-2-size-2.json` |
| 4 | Designer 成本 | Designer 查询合同行 | 响应不出现 cost | `contract-lines-designer-page-1.json` |
| 5 | 合同档案 | Boss 打开真实合同档案 | 请求 `current:1,size:20`，显示 6 项/¥977.80 | `browser-contract-line-request.txt`、三档截图 |
| 6 | 合同变更来源 | 发起减项并打开来源 | 仅调用 range，7 个来源可选；无合同行大页请求 | `contract-change-requests.txt` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `ContractLineRepository`、`project/service/contractlines/page/support/Page.java`、`ContractLinePaginationProviderTest` |
| th4-decoboon-web | `dev` | 未提交工作区 | `contract/model.ts`、`contract/api.ts`、`DesignerProjectContractView.vue`、`DesignerContractChangesView.vue`、`.pagination-baseline.json` |
| th4-decoboon-app | `dev` | 无改动 | 不适用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `mvn -Dtest=ContractLinePaginationProviderTest,ContractFactTraceTest test` | PASS，7/7 | `backend-test.log` |
| 后端编译 | `mvn -q -DskipTests compile` | PASS | 本轮命令输出 |
| Provider 响应门禁 | `audit-provider-response-contract.py --check` + `--self-test` | PASS，543 Provider / 0 finding | `provider-response-audit.txt`、`provider-response-self-test.txt` |
| 前端 Provider 契约 | `audit-frontend-provider-contract.py --check` | PASS，unmatched=0 | `frontend-provider-contract-check.txt` |
| Web 构建 | `npm run build` | PASS（含分页、类型、标识符与品牌门禁） | `web-build.log` |
| API 对账 | Boss/Designer/StoreManager，size 2 两页 | PASS：两页 ID 无交集；6 项/¥977.80 | `api-probe.json` |
| 角色成本 | Boss/StoreManager/Designer | PASS：Designer cost 字段 0 个 | `api-probe.json` |
| 真实页面 | `/designer/projects/241940507872198656/contract` | PASS：console 0 error，无 4xx/5xx | `browser-console-contract.txt`、`browser-contract-requests.txt` |
| 页面兼容 | 1440/1280/1024 | PASS：6 行、无横向溢出 | `dom-contract-*.json`、三档截图 |
| 合同变更页面 | 发起减项并打开来源 | PASS：7 来源，仅 range 请求 | `contract-change-sources-1440.png`、`contract-change-requests.txt` |
| 接口时延 | Boss size=20，10 次 | P50 30.16ms、最大 34.67ms、5,175B | `api-timings.json` |

- 未执行项及原因：当前真实合同只有 6 行，真实页面未出现翻页按钮；翻页窗口通过 Provider 单测和真实 API `size=2` 两页请求验证。独立验收、视觉签字和发布未执行。
- 已知失败及是否与本次相关：直接打开深链时 Vue Router 在登录态恢复前产生 1 条 `No match found` warning，页面恢复后正常，无 error；未作为本批缺陷扩大处理。

## 7. 发布

- 不发布；仅本地验证。无 DDL、迁移、配置和密钥变化。

## 8. 回滚

- 回滚条件：合同行页总数/顺序异常、Designer 成本泄露、合同档案无法读取或合同变更来源不可用。
- 回滚命令/步骤：回退合同行 Pageable Provider 和 Web 分页消费改动；合同变更来源恢复前必须保留 range-first 的权限校验，不得恢复无界全量读取。
- 回滚验证：重跑 7 条定向测试、Web build、合同档案和增减项真实页面。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次 Provider 修改存在字段/局部变量重名编译错误，修复后通过
- 返工原因：分页字段 `current` 与当前员工变量同名
- 最终结论：开发自测、真实 API 和页面功能验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，前端固定大页并自行截取前 N 条属于已识别反模式
- 是否需要新增门禁：现有 Web 固定大页门禁已捕获并通过收缩基线；暂无新增门禁
- 候选 `EXP/ANT/PATH`：`ANT-固定size500后前端截取`、`PATH-数据库Pageable+稳定复合排序+服务端汇总`
- 后续动作与负责人：执行 DQ-X8 独立验收；后续批次分类处理其他合同行全量读取上下文
