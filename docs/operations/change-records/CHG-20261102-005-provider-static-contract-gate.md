# CHG-20261102-005 · Provider 静态响应契约门禁入库

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-005 |
| 类型 | 缺陷修复 / 接口契约 / 发布门禁 |
| 风险级别 | L2 |
| 状态 | `COMMITTED / CLEAN_HEAD_VERIFIED / INDEPENDENT_ACCEPTANCE_PENDING` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `CHG-20261031-041`、`BR-059`、`BR-060`、`DEC-095`、`docs/design/32-Provider接口响应序列化与懒加载安全规范.md` |

## 2. 目标与范围

- 目标：让 Provider 响应序列化门禁从干净 dev 提交即可运行，并把现有 Provider 的响应白名单缺口收敛为 0。
- 范围内：`scripts/audit-provider-response-contract.py`、`scripts/build-release.sh` 接线、137 个 Provider 的最小白名单注解及必要 import。
- 非范围：不修改业务状态机、权限、金额、成本、业务写逻辑或 DDL；不改运行期响应字段语义，只显式声明既有字段。
- 明确排除：不夹带 Logo 上传、员工选择器分页、客户360页面等其他会话改动。

## 3. 实现

| 仓库 | 分支 | 提交 | 内容 |
| --- | --- | --- | --- |
| th4-decoboon | dev | `a810ab82` | 139 个文件：静态响应门禁脚本、发布构建接线、137 个 Provider 白名单注解 |

关键约束：

- 从 `ac6eec74` 干净 HEAD 首次运行门禁：`220` findings。
- 仅提取当前工作区 Provider 的 `@Properties/@AllowProperties` 注解差异，不复制其他业务实现。
- 对原先没有注解的 Provider，只补注解和必要 import。
- 最终干净提交重跑：`providers=541 properties=536 allow=534 findings=0`。

## 4. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 干净提交静态审计 | PASS：541 Provider / 0 finding | `reports/provider-static-contract-20261102.md` |
| 门禁负向自测 | PASS | `audit-provider-response-contract.py --self-test` |
| 发布构建接线 | PASS：`build-release.sh` 在基线检查后执行 Provider 审计 | `bash -n` + 行号检查 |
| 后端编译 | PASS：`mvn -q -DskipTests compile` | 干净 worktree `a810ab82` |
| 定向测试 | PASS | `DetailResponseWhitelistContractTest,DesignTemplateWhitelistContractTest,ProviderSecurityAcceptanceTest` |
| 原 `CHG-20261031-041` 的全量/页面证据 | 保留为历史证据 | 不用于代替本提交的独立验收 |

## 5. 发布

- 已提交到本地 `dev`；未 push、未部署。
- 不含迁移，不需要数据库回滚。
- 仍不能发布：主工作区有大量未提交业务改动，且独立验收未执行。

## 6. 回滚

- 回退 `a810ab82` 即可恢复旧响应白名单状态。
- 回退后必须在旧 dev 上重新评估 Provider 输出兼容性，不能直接生产发布。
- 实际是否回滚：否。
