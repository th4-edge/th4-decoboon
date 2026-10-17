# 材料域下一批提示词（B7 材料业务助手上下文入口）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`03-blueprint.json`、`04-requirements.md`（REQ-MAT-016）、`05-plan.md`、`06-implementation.md`、`07-ui-review.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 B4 助手实现与证据：`material/assistant/ask`、`MaterialAssistantContext`、`MaterialAssistantKnowledge`、`MaterialAssistantAnswer`、`evidence/B4/`；
5. 涉及业务事实时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与 SCN-021 / BR-045 / DEC-072 / DEC-073。

当前状态：B6.6 独立复验 **157/157 PASS**，B6 系列 `ACCEPTED`；B7 为材料业务助手上下文入口。以 `state.json` 为准。

## 垂直切片

只交付 App/Web 统一的**单轮「问材料」上下文入口**：用户从当前项目、作业单、采购/结算页面打开抽屉，基于真实上下文问一个问题，看到有证据、有边界、有下一步的只读回答。不做多轮记忆、不做万能聊天框、不新增助手业务规则。

## 入口与上下文

- 项目上下文：项目详情、项目核算/材料体检；
- 作业单上下文：作业单详情、主材下单页；
- 采购/结算上下文：采购/结算相关页面；
- 统一入口文案“问材料”，Web 使用右侧/底部抽屉，App 使用底部弹层；打开时展示当前上下文摘要（项目/作业单/采购/结算名称与状态），允许清除上下文后问通用规则问题；
- 上下文 ID 只能来自当前页面已加载的真实对象，禁止手填或信任前端传入的租户/员工字段；服务端继续以 `@User` 和企业数据范围为准；
- 入口可见性遵守现有角色权限；Designer/PM 成本不可见规则不得因助手入口放宽；
- 未绑定业务对象时明确显示“未绑定业务单据，仅解释材料规则”，不得伪造实时数据。

## API 与展示

- 只复用 `material/assistant/ask`，请求字段：`question`、`projectId`、`sheetId`、`requestId`、`idempotencyKey`；
- 初始建议问题优先使用服务端同一发布蓝图生成的能力；如页面需要预置建议，必须与 `MaterialAssistantKnowledge` 同源，不得前端另写一套业务规则；
- 展示 `answerType`、`answer`、`confidence`、`citations`、`toolCalls`、`nextActions`、`suggestedQuestions`、`auditStatus` 与 `readOnly`；
- `NO_EVIDENCE`、`REFUSAL`、`PERMISSION_DENIED` 按服务端原样展示边界与恢复路径，不以空数组、0、成功态或兜底文案伪装；
- 助手只读：不提供审批、付款、删除、改权限或任何写操作按钮；
- 网络失败、超时、会话失效显示可重试错误态，不把失败渲染成“没有数据”。

## UI 质量

- Web：1440×900 与 1024×768 真实浏览器；
- App：H5 390×844 与微信开发者工具 375×667；
- 覆盖加载、空态、错误/重试、长回答、长证据、无权限、无证据、拒答、清除上下文、建议问题点击；
- 无横向溢出，Console Errors 0；Web 抽屉支持 Esc 关闭与焦点管理，App 处理底部安全区与键盘遮挡；
- 截图与指标落 `evidence/B7/`。

## 测试与验证

- 后端定向：`material/assistant/ask` 上下文 ID 校验、权限、幂等、`NO_EVIDENCE`、`REFUSAL`、建议问题同源；
- App：`type-check`、`build:mp-weixin`、`check:ui`、`check:numeric-inputs`、`check:subpackages`；
- Web：`type-check`、`build`；
- 真实交互：项目、作业单、采购/结算三类上下文各完成一次单轮问答；点击建议问题；清除上下文；错误重试；Designer/PM 权限态；
- 如确需后端改动，仅允许上下文校验、序列化白名单和错误文案；权限、金额/成本/支付语义变化必须停门禁写决策包。

## 退出条件

- `DEV_VERIFIED` + `UI_REVIEWED` + `RECONCILED`；
- 更新 `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md`、`state.json/history` 与 `evidence/B7/`；
- 不宣称 B8 独立验收通过，不执行正式小程序提审/发布；
- 有代码变更时只提交本批领域包、相关代码/测试/页面与新增证据到各自 `dev`，不 stage 仓库既有无关改动。
