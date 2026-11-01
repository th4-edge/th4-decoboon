# 装得清 AI 工作索引

> 状态：当前生效  
> 用途：新任务先读这一页，再按任务类型定向读取；不要从全仓或历史目录开始。  
> 当前周期：**IDLE**。活跃问题 0，领域全部 `CYCLE_CLOSED`。

## 1. 所有任务的最小启动

1. 读根 `AGENTS.md`。
2. 按任务读当前目录最近的 `AGENTS.md`。
3. 读当前 `_系统/任务/当前工作.md`；如果无活跃任务，先建立新的 `CHG-*`。
4. 只读本索引指定的事实源和代码，不预读归档。

每个任务必须有：

- 一个用户可见结果；
- 明确非范围；
- 验收证据；
- 停止条件；
- 一个主 Skill 上限；
- 同一问题最多两轮修正。

## 2. 任务路由

| 任务 | 必读 | 按需读取 | 禁止 |
| --- | --- | --- | --- |
| 新需求 | `zhuangdeqing-business`、`docs/product/AGENTS.md` | 场景、`BR/DEC/OBJ`、产品模板 | 直接用当前页面反推需求 |
| 需求变更 | 现行需求/合同/快照、对应 `CHG-*` | 受影响对象、权限、金额、跨端口径 | 覆盖历史快照 |
| BUG 修复 | 最小复现、受影响代码、对应测试 | 最近一次相关 `CHG-*`、运行日志失败行 | 全仓扫描、顺手重构 |
| 后端实现 | `src/main/AGENTS.md` | `rules.md` 对应章节、一个 Provider/Context/Repository、定向测试 | 全文读 `rules.md` |
| 后端测试 | `src/test/AGENTS.md` | 当前测试类、`rules.md` §4.5/§8 | 为跑绿改业务断言 |
| App 实现 | App `AGENTS.md`、`src/AGENTS.md` | 当前 domain/page、一个契约脚本、真机证据 | 前端复制业务规则 |
| Web 实现 | Web `AGENTS.md`、`src/AGENTS.md` | 当前 domain/page、相关标准、真实渲染证据 | 新开同义 Provider |
| 复杂领域 | 领域 `state.json`、`00-charter.md`、`10-reconciliation.md` | `scripts/domain_state.py show/validate`、当前一个切片 | 读完整领域包/evidence |
| 独立验收 | `docs/testing/README.md` | 当前 RUN 摘要、`run.json`、当前 case-result、defect | 完整 raw 目录 |
| 发布 | `docs/operations/试运营单人轻量协作与发版规范.md` | 当前 release、基线脚本、门禁 | 历史制品复用 |
| 生产事故 | `docs/operations/incident-playbook.md` | `scripts/prod-monitor.sh` 失败行、最近发布、回滚点 | 根据历史 ERROR 自动改代码 |

## 3. 固定读取命令

问题按 ID：

```bash
jq '.issues[] | select(.id == "PI-YYYYMMDD-NNN")' docs/product/issue-governance/registry.json
```

历史问题按 ID：

```bash
jq '.issues[] | select(.id == "PI-YYYYMMDD-NNN")' docs/product/issue-governance/archive/20261102-cycle-close/registry.json
```

领域状态：

```bash
python3 scripts/domain_state.py show --domain finance
python3 scripts/domain_state.py validate --domain finance
```

规则章节：

```bash
rg -n '^##|^###' rules.md
```

验收运行：

```bash
python3 docs/testing/scripts/create_run.py --help
python3 docs/testing/scripts/validate_run.py --help
```

## 4. 默认禁止读取

- 完整问题台账和历史问题目录；
- 完整领域包、`evidence/` 和全部领域文档；
- `rules.md` 全文；
- 全部 Skill references；
- `docs/acceptance` 全目录和 raw 日志；
- 构建、监控、测试的完整日志；
- `_系统/归档/**`，除非明确追溯；
- 多个主 Skill 同时叠加。

## 5. 当前活跃 Skill 策略

项目级活跃 Skill 只有：

```text
neat-freak
zhuangdeqing-smb-reviewer
```

两者都不是默认执行入口：

- `neat-freak` 只在明确清理/收尾时使用；
- `zhuangdeqing-smb-reviewer` 只在明确要求中小装企独立审查时使用；
- 普通开发、BUG、需求变更不加载任何项目级 Skill；
- `zhuangdeqing-business` 是业务事实源，不计作执行 Skill。

## 6. 任务完成与退出

任务收尾更新：

- `CHG-*`；
- `_系统/任务/当前工作.md`；
- 必要领域 `state.json`；
- 必要发布/验收证据。

任务没有产生新的用户可见结果、测试结果或页面证据时，不得继续扩查；两轮无进展必须停止并报告阻断。

