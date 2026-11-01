# 前端 Provider 契约巡检与漂移门禁

> 状态：生效（2026-10-30）
> 风险级别：L2（跨仓库接口契约治理）
> 关联变更：`CHG-20261030-002`
> 工具：`scripts/audit-frontend-provider-contract.py`
> 基线：`docs/design/app-web-provider-contract.json`
> Web-only 写能力评审：`docs/design/app-web-provider-reviews.json`

## 1. 目标

建立 App、Web 与唯一后端之间的**可机器复现 Provider 使用清单**，先解决三类基础问题：

1. 前端写了后端不存在的 Provider 路由；
2. 同一路由的 App/Web 使用端在无审查情况下发生变化；
3. 动态拼接路由绕过静态清单而无人知晓。

本工具只证明**路由存在与客户端使用关系**，不替代业务能力对账，也不证明请求/响应字段、权限、状态机、金额口径和恢复路径已经完全一致。

## 2. 扫描范围

- 后端：`src/main/java/**/*.java` 中的 `@Service("...")`、`@ServiceName("...")`。
- App：`src/domains/**/*.ts`、`src/subpackages/*/domains/**/*.ts`。
- Web：`src/domains/**/*.ts`。
- 只统计从 `@/core/http` 导入的 `post/get/put/upload` 及其别名，本地同名函数不会被误判为 Provider 调用。
- 动态 URI 调用单独记录为 `dynamicCallSites`；同一 HTTP 文件中可由后端确认的路由字符串会作为动态路由引用纳入归属判断。

## 3. 分类

| 分类 | 含义 |
| --- | --- |
| `shared` | App、Web 都消费该路由 |
| `app_only` | 当前只有 App 消费 |
| `web_only` | 当前只有 Web 消费 |
| `write` | 路由末段不是标准读取能力，按写/命令能力对待 |
| `unmatched` | 前端消费但后端未声明，直接阻断 |

`app_only`、`web_only` 不自动等于错误。它们必须继续经过业务裁决，区分“端侧不需要”“Web 优先能力”和“App/Web 尚未对齐”。

## 4. 使用方式

```bash
# 检查当前工作区是否与已提交基线一致
python3 scripts/audit-frontend-provider-contract.py --check

# 审查路由/端侧使用变化后，更新基线
python3 scripts/audit-frontend-provider-contract.py --write

# 负向自测：共享路由、未声明路由、动态路由均能被识别
python3 scripts/audit-frontend-provider-contract.py --self-test

# 严格模式：存在未关闭的 Web-only 写能力评审债务时失败
python3 scripts/audit-frontend-provider-contract.py --check --strict-web-only
```

前端仓库默认从同级目录解析：

```text
装客宝/
├── 后端代码/th4-decoboon/
└── 前端代码/
    ├── th4-decoboon-app/
    └── th4-decoboon-web/
```

其他目录结构使用：

```bash
DECOBOON_FRONTEND_ROOT=/path/to/前端代码 \
  python3 scripts/audit-frontend-provider-contract.py --check
```

## 5. 当前基线（2026-10-30）

| 指标 | 数量 |
| --- | ---: |
| 后端业务 Service 路由 | 542 |
| 前端消费的唯一路由 | 465 |
| App/Web 共享 | 230 |
| 仅 App | 207 |
| 仅 Web | 28 |
| 仅 Web 写/命令能力 | 12 |
| 未匹配后端路由 | 0 |
| 动态 URI 调用点 | 27 |
| 领域层外直接传输调用 | 0 |
| Web-only 写能力评审覆盖 | 12 / 12 |
| 已关闭评审 | 11 |
| 未关闭评审债务 | 1 |

Web-only 写能力评审分布：

| 状态 | 数量 | 含义 |
| --- | ---: | --- |
| `WEB_FIRST_APPROVED` | 4 | 报价工作台批量编辑/导入导出，已有 `GAP-WEB-002` |
| `PLATFORM_SPECIFIC_APPROVED` | 1 | Web 手机号密码登录，属于端侧认证方式差异 |
| `PARITY_REQUIRED` | 0 | 设计模版 Provider 已全部 App/Web 共享 |
| `ROUTE_MISMATCH` | 0 | 项目成员、设计分项保存/删除已收敛为同一 Provider |
| `WEB_FIRST_PENDING_REGISTRATION` | 1 | 房屋删除，物理删除/软删归档语义待确认（`GAP-WEB-025 OPEN`） |
| `PENDING_REVIEW` | 0 | 暂无未分类项 |

当前已关闭的 Web-only 能力包括报价工作台 4 项、Web 手机号密码登录 1 项、Web 分项排序 1 项、风险处置 5 项。设计模版 11 个读写/版本 Provider 已全部由 App/Web 共享，`PARITY_REQUIRED=0`；真实模板 UI 仍需验收。房屋删除当前后端为物理删除，软删/归档语义尚未确认，因此保持 `GAP-WEB-025 OPEN`，也是唯一未关闭评审。

## 6. 漂移门禁规则

`--check` 在以下情况失败：

- 任一前端路由没有被后端 `@Service` 声明；
- 新增或移除前端消费路由；
- 路由从 `shared` 变为单端，或从单端变为共享；
- 动态 URI 调用点新增、移除或在文件间移动；
- Web-only 写能力评审清单缺失、重复、过期或字段不完整；
- 使用 `--strict-web-only` 时存在 `PARITY_REQUIRED`、`ROUTE_MISMATCH`、`WEB_FIRST_PENDING_REGISTRATION` 或 `PENDING_REVIEW`。

失败后必须：

1. 确认业务能力与唯一 Provider；
2. 完成 App/Web Provider 对账矩阵；
3. 在代码审查中说明端侧差异；
4. 再执行 `--write` 更新基线并提交。

## 7. 已知边界

- 本工具只识别通过 `@/core/http` 的静态字符串或 `post(uri, ...)` 动态调用，不解析复杂运行时拼接值。
- 本工具不识别字段裁剪、枚举值、错误码、数据权限、状态迁移和金额公式差异。
- 动态调用点必须人工确认实际 URI；后续应逐步把动态 URI 收敛为有限常量或路由映射。
- 基线是“当前事实快照”，不是“业务批准清单”。真正的批准清单应在下一阶段建立 `APPROVED / WEB_FIRST / BLOCKED` 审查状态。

## 8. 下一阶段

1. 完成 App 设计模板列表、详情、新建、编辑、发布、复制、停用、新版本的真实页面/真机验收。
2. 确认房屋删除采用物理删除还是软删/归档，关闭 `GAP-WEB-025`。
3. 扩展契约字段：请求/响应关键字段、枚举、错误码、权限范围、金额/成本/利润率、幂等和写后回读要求。
4. 在房屋删除评审关闭后，将 `--check --strict-web-only` 纳入跨端 release preflight。
