# CHG-20261102-020 · 归档业务域/产品/验收/生产事故 Agent

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-020 |
| 类型 | 重构 / 治理 |
| 风险级别 | L0 |
| 状态 | 已关闭 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户要求归档复杂业务域、App 产品推演、独立验收、Web UI 和生产事故 Agent |

## 2. 范围

- 归档 `domain-delivery-orchestrator`。
- 归档 `zhuangdeqing-product-app-development`。
- 归档 `zhuangdeqing-acceptance-agent`。
- 归档 `backend-prod-sentinel`。
- `zhuangdeqing-ui-craft` 已于 `CHG-20261102-019` 删除归档；本轮保持非活跃。
- 保留 `neat-freak` 和 `zhuangdeqing-smb-reviewer` 两个活跃项目 Skill。
- 非范围：运行时代码、业务规则、Provider、权限、金额、DDL、数据库和生产环境。

## 3. 保留执行能力

| 原能力 | 归档后入口 |
| --- | --- |
| 复杂业务域状态 | `scripts/domain_state.py`、`docs/product/domains/PROGRAM-PROTOCOL.md` |
| Program Watch | `scripts/program_watch.py` |
| 独立验收 RUN | `docs/testing/scripts/create_run.py`、`validate_run.py` |
| 验收模板 | `docs/testing/templates/**` |
| 生产事故 | `docs/operations/incident-playbook.md`、`scripts/prod-monitor.sh`、`scripts/deploy-prod.sh`、`scripts/rollback-prod.sh` |

## 4. 验证

| 检查 | 命令/结果 | 状态 |
| --- | --- | --- |
| `domain_state.py show --domain finance` | 正常回显财务领域状态 | PASS |
| `program_watch.py --json` | 正常产出 JSON | PASS |
| `create_run.py --help` / `validate_run.py --help` | 命令可执行 | PASS |
| Skill Registry | 活跃仅 `neat-freak`、`zhuangdeqing-smb-reviewer` | PASS |
| Disabled Registry | 四个归档路径均存在 | PASS |
| 活跃引用 | 不再依赖四个归档 Skill | PASS |
| 格式 | 三仓 `git diff --check` | PASS |

## 5. 结论

四个 Skill 已从活跃集合移出并归档；关键业务域、验收、生产事故执行工具保留，固定读取纪律继续生效。
