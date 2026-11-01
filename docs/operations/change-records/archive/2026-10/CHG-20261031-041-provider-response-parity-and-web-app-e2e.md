# CHG-20261031-041 · Provider 响应契约检查与 Web/小程序主链回归

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-041` |
| 类型 | 缺陷修复 / 接口契约 / 跨端回归 |
| 风险级别 | L2（跨模块、跨端接口契约与完整业务链） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-040`、`BR-059`、`BR-060`、`DEC-095`、`docs/design/31-一后端多端同源架构规范.md` |

## 2. 目标与范围

- 目标：按 Provider 响应序列化门禁检查后端接口，结合装得清 App/Web 实际页面消费字段补齐响应白名单，消除实体图漏标、嵌套白名单缺失、懒加载和 JSON 契约风险；以真实页面完成主链和功能逻辑回归。
- 范围内：所有 `@Service` Provider 的静态分类；未声明 `@Properties` 的存量 Provider；App/Web 实际消费的响应字段；客户、房屋、设计、报价、合同、项目、材料、验收、财务主链；正常、空态、失败、权限和跨企业反例。
- 非范围：不改变业务状态机、权限、金额、成本、利润、支付和对外口径；不新增业务对象或正式字段；不发布生产；不替代独立验收。
- 受影响端：后端、Web、微信小程序。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及读取和展示，但不改变口径。
- 业务事实来源与裁决编号：`BR-059`、`BR-060`、`DEC-095`；具体业务链继续以业务智能体对应 `BR/DEC/GAP` 为准。
- 尚存 GAP 或假设：若页面需要当前 Provider 未暴露且业务主源不存在的字段，建立 `GAP-*` 后停止编码，不在本批自行补业务规则。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 后端静态审计 | 扫描全部 Provider | 每个 Provider 完成响应分类；实体图和读模型具备白名单或书面例外 | 审计脚本输出、契约矩阵 |
| 2 | 后端真实 JSON | 关键主链 Provider | 响应可解析，无递归、懒加载异常、敏感字段和无关归属树；关键字段存在 | 定向测试、HTTP 响应 |
| 3 | Web 主链 | 企业负责人真实账号 | 登录、客户/房屋、设计报价合同、项目材料验收、财务页面可操作并回读一致 | 浏览器截图、console/API 证据 |
| 4 | 小程序主链 | 企业负责人真实账号 | 同一业务事实可进入、操作、刷新并回读；无运行时异常 | 微信开发者工具截图/日志 |
| 5 | 异常与权限 | 普通角色/跨企业/无数据 | 角色范围正确，跨企业拒绝，失败显示暂不可确认和恢复路径，禁止伪 0 | 负向证据 |
| 6 | 跨端一致 | App/Web 同一对象 | 状态、金额、成本完整性、下一步和责任人口径一致 | 两端口径对账 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | Provider 响应白名单、`scripts/audit-provider-response-contract.py`、`scripts/smoke-provider-response.py`、SuccessProgramContext 空日期修复、契约基线 |
| th4-decoboon-web | `dev` | 无代码改动 | 18 个主链页面/动作真实浏览器验证 |
| th4-decoboon-app | `dev` | 无代码改动 | 22 个主链页面、跟进写后回读、用户成功页验证 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| Provider 静态审计 | `python3 scripts/audit-provider-response-contract.py --check` + `--self-test` | PASS，542 Provider / 0 finding | `docs/acceptance/runs/20261031-provider-response-web-app-e2e/provider-response-audit.json` |
| 后端全量测试 | `mvn -q test` | PASS，1170/0/0/0 | `docs/acceptance/runs/20261031-provider-response-web-app-e2e/backend-full-test.json` |
| 代表接口 JSON | `scripts/smoke-provider-response.py` | PASS，17/17，0 敏感字段 | `api-contract-smoke-final.json` |
| Web 真实页面 | 浏览器 1440×900 主链 | PASS，18 个页面/动作，0 console/network failure | `web/batch1.json`、`web/batch2.json`、`web/customer-follow-write.json` |
| 小程序真实页面 | 微信开发者工具主链 | PASS，22 页 + 写后回读 + 成功页 | `app/` |
| App/Web Provider 路由基线 | `audit-frontend-provider-contract.py --check` | PASS，3 条读路由校正为 shared | `docs/design/app-web-provider-contract.json` |
| 用户成功报告 | `mvn -q -Dtest=SuccessProgramContextTest test` + App 页面 | PASS，空日期场景回退当天 | `SuccessProgramContextTest`、`app/provider-response-app-success/` |
| Web 构建 | `npm run build` | PASS | `web-build.log` |
| 小程序构建 | `npm run type-check && npm run build:mp-weixin && npm run check:subpackages` | PASS | `app-typecheck.log`、`app-build.log`、`app-subpackages.log` |

- 未执行项及原因：未做独立验收、未生产发布、未执行正式小程序提审；全产品全部路由未逐页穷举，静态响应门禁已覆盖全部 Provider。
- 已知失败及是否与本次相关：首次小程序 toast 等待超时，但写入已成功且独立写后回读 PASS，属于测试脚本等待方式，不是产品缺陷；已修复的 `statistic/success/report` 空日期异常已由新增单测覆盖。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 当前 `dev` 工作区 |
| 健康检查 | 本地验证 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：响应字段变更导致页面不可用、跨端字段不一致或业务事实变化。
- 回滚命令/步骤：回退本批 Provider 白名单与相关测试。
- 回滚验证：重新执行定向测试和受影响页面回归。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否，发现并修复 Boss 首页非法 JSON、成功报告空日期和前端路由基线漂移
- 返工原因：首轮审计未覆盖普通读模型作为响应根节点的嵌套实体；已扩大静态门禁
- 最终结论：开发自测与真实主链验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，Provider 嵌套白名单和响应基线多次回归
- 是否需要新增门禁：是；新增 `scripts/audit-provider-response-contract.py` 并接入 `scripts/build-release.sh`
- 候选 `EXP/ANT/PATH`：`ANT-普通读模型不视为实体图`、`PATH-Provider响应采用递归白名单审计`
- 后续动作与负责人：继续在每次 Provider 变更和发布构建前执行门禁；独立验收由后续验收责任方执行
