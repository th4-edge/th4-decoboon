# Agents Skills

这里放当前项目可用的 Agent skill。

## 当前启用

- `backend-prod-sentinel`：生产后端巡检、事故归档与受控修复。
- `domain-delivery-orchestrator`：复杂业务域的可恢复交付协议。
- `zhuangdeqing-product-app-development`：装得清 App 产品推演、迭代与验证。
- `zhuangdeqing-smb-reviewer`：中小装企视角的独立业务审查。
- `zhuangdeqing-acceptance-agent`：独立黑盒验收、证据、缺陷和复验。
- `neat-freak`：阶段性清理、收尾和归档。

## 已归档停用

- `zhuangdeqing-web-ui-interaction`：`_系统/归档/20261102/技能/禁用/`
- `zhuangdeqing-product-web-development`：`_系统/归档/20261102/技能/禁用/`
- `zhuangdeqing-web-immersion-audit`：全局 `_archive/20261031/`
- `zhuangdeqing-ui-craft`：`_系统/归档/20261102/技能/删除/`（用户要求删除，不再作为活跃 Skill）

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
