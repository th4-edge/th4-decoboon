# CHG-20261101-026 · App 报价分项与套餐复制

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261101-026 |
| 类型 | 新功能 / 跨端 Provider |
| 风险级别 | L2（报价与套餐复制写入；不改变金额公式、权限范围或数据库结构） |
| 状态 | 开发自测通过，待独立验收 |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/事故/业务裁决 | 用户需求“新增报价分项支持复制，套餐支持复制”；`BR-002`、`BR-051`、`CHG-20261101-021` |

## 2. 目标与范围

- 目标：App 报价调整支持复制有效报价分项；套餐管理支持复制为独立新套餐草稿。
- 范围内：`quote/line/copy`、`pack/template/copy`、App 调整页/套餐管理页、契约门禁和开发自测。
- 非范围：修改报价公式、成本口径、套餐版本规则、权限矩阵或数据结构。
- 受影响端：App、后端。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及报价行新增、套餐草稿创建和按现有口径重算金额；不改变公式和权限。
- 业务事实来源与裁决编号：用户明确需求；沿用 `BR-002`、`BR-051` 和 CHG-021 的套餐生命周期。
- 尚存 GAP 或假设置：复制套餐使用新编码而非同编码新版本；界面文案明确区分“复制套餐”和“创建下一版”。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | Boss 复制有效报价行 | 新增 Add/DesignOnly 行，报价重算，源行不变 | `evidence/api.json`、`02-quote-copy-result.png` |
| 2 | 已扣减行 | 无复制入口，后端拒绝 | App 契约与上下文逻辑 |
| 3 | Boss 复制套餐 | 新编码 V1 Draft，完整复制项目行，不影响源套餐 | `evidence/api.json`、`04-package-copy-result.png` |
| 4 | 权限负向 | Designer 套餐复制 403 | `evidence/api.json` |
| 5 | 页面入口 | 报价行显示“复制”，套餐卡显示“复制套餐”并确认 | `01-quote-copy-action.png`、`03-package-copy-action.png` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-app | `feature/package-quote-copy-20261101` | `77e35f9` | 报价调整页、套餐管理页、API、契约、候选 1.0.77 |
| th4-decoboon | `feature/package-quote-copy-20261101` | `3e585db6` | `quote/line/copy`、`pack/template/copy`、上下文与测试 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据 |
| --- | --- | --- | --- |
| App 契约与 8 个负向夹具 | `check:package-config -- --self-test` | PASS | 本地终端 |
| App UI/类型/构建/分包 | `check:ui`、`type-check`、`build:mp-weixin`、`check:subpackages` | PASS | 本地终端 |
| 后端定向测试 | PackageLifecycleContextTest、PackageProviderInjectionContractTest、QuoteAdjustmentContextTest | PASS | 本地终端 |
| 后端编译/Provider 审计 | compile、audit-provider-response-contract | PASS，findings=0 | 本地终端 |
| 真实 API | 8094 | 报价 4→5 后恢复；套餐复制独立草稿且 4 行 | `evidence/api.json` |
| 微信页面 | 报价调整→复制；套餐管理→复制套餐 | PASS | `evidence/ui.json` 与截图 |

- 未执行项：独立验收、体验版上传、生产发布。
- 已知失败：无。

## 7. 发布与回滚

- 当前未发布。
- 无 DDL；发布时 App 与后端必须同批。
- 回滚：上一 App/后端 release；复制出的业务数据按操作记录人工清理或保留为草稿。

## 8. 结论

- 开发自测通过。
- 进入独立验收前不得宣称上线可用。
