# AGENTS.md — th4-decoboon 项目入口

> 本文件是 AI 协作入口，不是完整技术手册。只保留跨任务不可违反的边界、任务路由和事实源指针。
> 详细后端规则见 `rules.md`；上下文分层规则见 `docs/operations/智能体上下文分层与阅读纪律.md`。

## 1. 指令优先级与冲突处理

1. 当前用户明确指令。
2. 当前任务目录最近的 `AGENTS.md`。
3. 本文件。
4. 对应业务事实源、需求/设计规范和 `rules.md` 具体章节。

本文件与子目录规范只负责约束和路由，不复制完整业务规则。发现冲突时：

- 安全、权限、金额/成本、迁移、删除等高风险要求取更严格者；
- 业务口径冲突不得自行选择，必须查业务智能体或登记 `GAP-*`；
- 技术细节冲突先查 `rules.md` 对应章节并说明依据。

## 2. 当前协作模式

- 当前为试运营单人模式，默认直接使用 `dev`；不强制 lane、独立验收、RC tag 和全量测试。
- 所有变更先按 `docs/operations/研发运营章程.md` 判定 L0–L3，并建立 `docs/operations/change-records/CHG-*`。
- L0/L1 做定向验证；L2/L3 按章程升级验证。DDL、迁移、权限、金额/成本/利润、支付、跨租户和数据删除必须全量测试、备份、回滚预案和用户确认。
- 同一任务不重复全仓扫描，不全文读取 `rules.md`，不把完整日志倾倒到对话。达到当前风险级别门禁即停止扩查。

## 3. 跨任务硬边界

### 3.1 一后端多客户端同源

- 只有一个业务后端；App、Web、Admin 必须复用同一 Provider、请求/响应语义、权限、状态机、金额/成本口径、错误码和恢复路径。
- 禁止客户端专用同义接口、前端复制业务公式/状态机/正式默认值，或用 `0`、空数组和本地常量伪装事实。
- 缺失或失败必须显示“暂不可确认”并给恢复路径；只验证一端不得声称跨端对齐。

### 3.2 权限与租户

- 登录 Provider 继承 `Security`，当前员工用 `@User` 注入；企业和数据范围只从登录员工推导。
- 禁止信任前端 `companyId/empId/ownerId`；查询范围和响应字段分别由 `@DataPermission` 与 `@Properties` 控制。
- 权限、越权、跨租户和数据删除属于高风险变更，不得走“页面看起来正常”式验收。

### 3.3 业务事实先行

- 涉及业务对象、状态、角色权限、数据范围、金额、成本、利润、指标、客户可见性或宣传口径时，先使用全局技能 `zhuangdeqing-business` 和规范事实源。
- 未取得 `T06/T12` 的业务裁决，只允许调研、证据整理和需求澄清，不得自行编码业务语义。
- 新功能先写至少两个真实中小装企场景，明确使用人、触发事件、当前动作、系统下一步和异常处理。
- 新增或修改实体字段、Provider 字段、页面表单和正式指标前，必须引用或登记唯一业务信息项 ID，并写清主源、维护角色、下游消费者和闭合动作。

### 3.4 界面事实与编号

- 内部编号默认不在 App、Web、Admin 展示；内部 ID 仍用于主键、路由、权限、审计和接口契约。
- 业务对象选择器不得用原生下拉/固定大列表兜底；必须按 `docs/design/35-业务对象选择器与选择结果展示规范.md` 提供专用选择、服务端分页、七态和一次性回传。
- 页面只显示字段、数字或入口但无法推动业务动作时，不算完成。

### 3.5 智能体边界

- Web UI 唯一启用 `zhuangdeqing-ui-craft`；`zhuangdeqing-web-ui-interaction`、`zhuangdeqing-product-web-development`、`zhuangdeqing-web-immersion-audit` 和 `decoboon-web-frame` 均禁用，旧技能和历史规范仅作人工参考。恢复必须经用户明确授权，并同步 `.starwork/skills.json`、`.starwork/disabled-skills.json` 和本文件。
- 独立验收只使用 `zhuangdeqing-acceptance-agent`；执行前读取冻结用例、Oracle Lock、追溯矩阵，不修改业务事实或产品代码。
- 生产巡检与后端事故处理只使用 `backend-prod-sentinel`；只读巡检命令为 `bash scripts/prod-monitor.sh collect "30 minutes ago"`。
- 复杂业务域使用 `domain-delivery-orchestrator`，必须读取领域 `state.json`、章程和对账矩阵；一次只推进一个垂直切片。
- 内容团队使用 `zhuangdeqing-marketing-content` 与 `zhuangdeqing-usage-content`，两者不得定义产品事实或直接发布。

### 3.6 发版边界

- 生产后端统一使用 `scripts/deploy-prod.sh`，失败回滚到 `/opt/decoboon/releases/.previous`；构建/部署前执行既有基线门禁。
- 构建必须来自已进入 `origin/test` 的提交，部署必须满足同一 `SOURCE_COMMIT` 和 `origin/prod=origin/test=origin/dev`；热修必须回灌三支，未回灌或复用历史制品时禁止发布。
- 小程序只允许构建、开发版上传和体验版验证；正式版提审与发布由用户本人完成。
- 发布必须使用同一不可变制品，并满足仓库既有 `SOURCE_COMMIT`、分支基线和跨端契约门禁；禁止绕过硬门禁。

### 3.7 品牌资源

- Logo 只能使用 `logo-export` 同一套 Mark：App 用 `JzBrandLogo`，Admin 用 `BrandLogo`，Site 用 `brand-logo`；禁止页面另存图片或复制路径。

## 4. 按任务读取，不预读全部

| 任务 | 必读入口 | 按需读取 |
| --- | --- | --- |
| 后端 Java/资源实现 | `src/main/AGENTS.md` | `rules.md` 对应章节；`docs/design/31`–`35`；相关实体、仓储、Provider、测试 |
| 后端测试 | `src/test/AGENTS.md` | `rules.md` §4.5、§8；对应 Provider 契约和安全测试 |
| 产品需求/业务方案 | `docs/product/AGENTS.md` | `zhuangdeqing-business`、领域 `state.json`、相关 `BR/DEC/OBJ/GAP` |
| 跨端/接口/选择器设计 | `docs/design/AGENTS.md` | `docs/design/31`–`35`、相关 App/Web 页面和 Provider |
| Web 页面实现 | Web 仓库 `AGENTS.md` | `zhuangdeqing-ui-craft`、Web 标准、后端对应 Provider |
| App 页面实现 | App 仓库 `AGENTS.md` | App 专项规范、后端对应 Provider、真机证据要求 |
| 独立验收 | `zhuangdeqing-acceptance-agent` | 冻结测试资产、Oracle、当前 `RUN-*` 目录 |
| 生产事件 | `backend-prod-sentinel` | 事故记录、最近发布、相关日志和回滚点 |
| 发布/热修 | `docs/operations/试运营单人轻量协作与发版规范.md` | 研发章程、发布脚本、契约门禁和最近发布记录 |

通用要求：

- 先用 `rg -n` 定位，再读取相关 30–80 行；不重复读取已读内容。
- L0 最多读 3 个文件、约 100 行；L1 最多读 8 个文件，一次定向测试和一次构建/类型检查。
- 工具输出只保留失败行、关键结论和 diff；完整日志留在文件。
- 同一问题最多推测一轮；连续两次修正无改善时，停止试错并重新收敛范围。

## 5. 文件与记录边界

- 需求和规格：`docs/product/**`、`docs/plan/**`；实现：`src/main/**`；测试：`src/test/**`；验收：`docs/acceptance/**`。
- 营销素材、选题、脚本、设计和分析分别进入 `docs/marketing/{material,topics,scripts,design,analytics}/**`。
- 项目状态写 `_系统/上下文/当前项目.md`；执行记录写 `_系统/任务/当前工作.md`；草稿放 `workspace/**`。
- 每次变更使用一份 `CHG-*` 记录；同一话题只在 `_系统/协作/工作约定.md` 追加一行，不写重复交接包。
- 多 Agent 以 `CHG-*`、领域状态和代码提交交接，不以聊天记忆作为状态源。

## 6. 交付口径

- 编码完成只表示开发自测完成，不等于独立验收或业务验收。
- 回复只保留结论、证据、失败项、风险和下一步；不得用未运行的命令或页面表现冒充验证结果。
- 没有真实页面、真机、接口或对账证据时，只能标记“待验证/暂不可确认”。
