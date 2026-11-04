# 装得清测试资产库

> 版本：v0.1  
> 状态：执行中  
> 建立日期：2026-11-01

## 这层资产解决什么

测试智能体只能执行测试，不能成为测试事实源。本目录保存可版本化、可追溯、可复用的测试资产：

```text
业务事实源
→ 测试策略与风险模型
→ 测试计划
→ 测试用例
→ 业务规则到页面/Provider/证据的追溯矩阵
→ 执行运行与缺陷记录（放在 docs/acceptance/runs/）
→ 关门结论
```

智能体后续只允许读取本目录资产并执行，不得每次重新发明预期结果。

## 权威来源

| 类型 | 事实源 |
| --- | --- |
| 业务对象、规则、状态、权限、金额、成本、指标 | `装得清业务智能体` 的 `SCN/BR/DEC/OBJ/MET/CAP/GAP` |
| 项目研发规范与发版门禁 | 项目 `AGENTS.md`、`rules.md`、`docs/operations/研发运营章程.md` |
| 推广前最低业务闭环 | `docs/product/01-推广前最低业务闭环与跨端质量门禁.md` |
| 页面动作、证据、覆盖立方体 | `zhuangdeqing-product-agent` 的 references |
| 执行证据 | `docs/acceptance/runs/<run-id>/` |

业务规则未冻结、存在冲突或实现状态未知时，测试资产只能标记 `BIZ_AMBIGUITY` 或 `BLOCKED`，不得用当前页面行为补成预期结果。

## 目录

```text
docs/testing/
├── 00-test-strategy.md          # 长期测试策略
├── 01-risk-model.md             # 风险分级与选测规则
├── 02-test-case-standard.md     # 用例规范
├── 03-traceability-model.md     # 追溯模型
├── 04-execution-agent.md        # 独立验收执行智能体使用边界
├── plans/                       # 按版本/里程碑的测试计划
├── cases/                       # 机器可读用例库
├── schemas/                     # 测试计划、用例、执行记录 schema
├── traceability/                # oracle lock、用例矩阵、角色动作依赖矩阵、覆盖摘要、未映射项
├── data/                        # 环境、角色、账号和数据夹具说明
└── scripts/                     # 校验和矩阵生成工具
```

## 首版范围

`V0.1` 围绕“签约后工地不乱”建立 P0/P1 核心主链用例，覆盖：

```text
客户与房屋 → 量房 → 设计 → 报价 → 合同 → 合同变更
→ 项目阶段/任务 → 材料 → 验收/整改 → 应收应付/收付款/结算 → 跨端与权限
```

薪酬、企业治理、品牌、内容获客和平台运营先登记为后续批次，不在 V0.1 假装已覆盖。

## 财务账务专项门禁

2026-11-04 起，薪酬与财务账务建立独立专项门禁：

- 规范：`docs/testing/05-finance-accounting-test-gate.md`
- 计划：`docs/testing/plans/TP-20261104-FIN-ACCOUNTING-GATE.md`
- 用例：`docs/testing/cases/finance-accounting-gate-v0.1.json`
- 夹具：`docs/testing/data/finance-accounting-fixtures.md`
- Oracle：`docs/testing/traceability/finance-accounting-gate-oracle.json`

该门禁覆盖薪酬、应收、应付、收款、付款、退款、冲正、费用、成本、结算、台账、凭证和财务汇总。P0 失败即阻断发布；P1 无未接受阻断；涉及金额、状态和权限的用例必须同时具备页面、Provider、DB/账务不变量证据。

## 校验命令

```bash
python3 docs/testing/scripts/test_assets.py validate
python3 docs/testing/scripts/test_assets.py trace
python3 docs/testing/scripts/test_assets.py catalog
python3 docs/testing/scripts/test_assets.py self-test
python3 docs/testing/scripts/finance_gate.py validate
python3 docs/testing/scripts/finance_gate.py self-test
python3 docs/testing/scripts/create_run.py --suite finance --run-id RUN-YYYYMMDD-FIN-001
python3 docs/testing/scripts/validate_run.py docs/acceptance/runs/RUN-YYYYMMDD-FIN-001 --final
python3 docs/testing/scripts/create_run.py --help
python3 docs/testing/scripts/validate_run.py --help
# self-test 同时负向验证动作依赖 Provider 破损会被识别
TH4_TEST_PASSWORD="本地测试密码" python3 docs/testing/scripts/action_provider_smoke.py --output /tmp/action-smoke.json
# 对矩阵中所有角色的依赖 Provider 做实机探测：允许角色要求200且候选非空，拒绝角色要求约定错误码
```

## 状态词语

- `READY`：业务预期已绑定正式规则，可执行；
- `BLOCKED`：缺少业务裁决、环境、身份或数据；
- `STALE`：事实源版本已变化，需要重新核对；
- `UNMAPPED`：只有功能或页面，没有可信业务规则/预期结果；
- `NOT_TESTED`：本次计划未安排；
- `PARTIAL`：只覆盖单端、正向或缺关键反例。

只有 `PASS` 不能单独代表发布通过；必须结合计划范围、反例覆盖、证据等级和退出准则。
