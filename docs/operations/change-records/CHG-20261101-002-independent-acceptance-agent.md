# CHG-20261101-002 独立验收执行智能体

## 1. 基本信息

- 变更 ID：`CHG-20261101-002`
- 风险级别：`L1`
- 负责人：codex
- 状态：`DEV_VERIFIED`
- 关联：`CHG-20261101-001`

## 2. 目标与范围

- 目标：建立只执行冻结测试资产的独立验收执行智能体。
- 范围：技能、运行协议、证据/缺陷规则、关门准则、运行脚手架和运行校验器。
- 非范围：业务规则、产品代码、执行真实业务验收、发布。

## 3. 影响面

- 受影响：项目技能注册、AGENTS 验收执行边界、测试文档。
- 数据库/DDL：无。
- 权限：无运行时权限变更；技能要求拒绝越权和身份替代。
- 金额/成本/利润：无口径变更。

## 4. 主要产物

- `.agents/skills/zhuangdeqing-acceptance-agent/SKILL.md`
- `.agents/skills/zhuangdeqing-acceptance-agent/references/`
- `.agents/skills/zhuangdeqing-acceptance-agent/scripts/create_run.py`
- `.agents/skills/zhuangdeqing-acceptance-agent/scripts/validate_run.py`
- `.starwork/skills.json`
- `AGENTS.md`
- `docs/testing/04-execution-agent.md`

## 5. 验证

```text
skill-creator quick_validate.py: PASS
create_run.py 创建单用例 RUN: PASS
validate_run.py 未执行最终校验拒绝 NOT_RUN: PASS
PASS 无证据被拒绝: PASS
补齐证据和 final report 后最终校验: PASS
test_assets.py validate/trace/catalog/self-test: PASS
```

## 6. 边界

- 智能体没有业务裁决权；
- 智能体没有代码修改权；
- 智能体不能把 API 通过、数据库现状或开发者说明冒充页面验收；
- 高风险动作仍需用户授权主体参与；
- 本次没有执行任何真实业务验收用例，不能宣称产品通过。

## 7. 回滚

- 回滚条件：技能误路由、运行脚本破坏测试资产或边界失效。
- 回滚步骤：移除技能目录和 `.starwork/skills.json` 注册项，恢复 `AGENTS.md` 对应章节。
- 实际是否回滚：否。

## 8. 结论

`DEV_VERIFIED`：独立验收执行智能体已可创建可恢复 RUN、校验结果证据与最终完整性；下一步可选择一个真实批次开始执行。
