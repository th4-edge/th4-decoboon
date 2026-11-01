# Agents Skills

这里放当前项目可用的 Agent skill。

## 当前启用

- `neat-freak`：阶段性清理、收尾和归档。

## 已归档停用

- `zhuangdeqing-web-ui-interaction`：`_系统/归档/20261102/技能/禁用/`
- `zhuangdeqing-product-web-development`：`_系统/归档/20261102/技能/禁用/`
- `zhuangdeqing-web-immersion-audit`：全局 `_archive/20261031/`
- `zhuangdeqing-ui-craft`：`_系统/归档/20261102/技能/删除/`（用户要求删除，不再作为活跃 Skill）
- `domain-delivery-orchestrator`：`_系统/归档/20261102/技能/归档/`
- `zhuangdeqing-product-app-development`：`_系统/归档/20261102/技能/归档/`
- `zhuangdeqing-acceptance-agent`：`_系统/归档/20261102/技能/归档/`
- `backend-prod-sentinel`：`_系统/归档/20261102/技能/归档/`
- `zhuangdeqing-smb-reviewer`：`_系统/归档/20261102/技能/按需归档/`（仅在明确要求独立业务审查时恢复）

## 归档后的替代路径

- 复杂业务域：直接读取 `docs/product/domains/<domain>/state.json`、`00-charter.md`、`10-reconciliation.md`，状态脚本使用 `scripts/domain_state.py`。
- App 产品推演：使用 `docs/product/**`、App 仓库 `AGENTS.md` 和相关业务事实源。
- 独立验收：使用 `docs/testing/**`、`docs/testing/scripts/create_run.py`、`validate_run.py` 和冻结用例。
- 生产事故：使用 `docs/operations/incident-playbook.md`、`scripts/prod-monitor.sh` 和现有发布/回滚脚本。

## 暂停使用

- `zhuangdeqing-marketing-content`：`_系统/归档/20261102/技能/暂停/`
- `zhuangdeqing-usage-content`：`_系统/归档/20261102/技能/暂停/`

历史规范和 references 仅作人工参考；恢复前必须经用户明确确认并更新 Registry。

## 固定读取纪律

- 一个任务只使用一个主 Skill。
- 问题台账按 ID/状态查，不全文读。
- 领域包只读 state/charter/reconciliation。
- `rules.md` 只读相关章节。
- Skill references 只读当前模式需要的一份。
- 验收只读 RUN 摘要和当前用例。
- 构建/监控只读失败行和结论。
