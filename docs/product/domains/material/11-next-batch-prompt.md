# 材料域下一批提示词（B8 助手 UI 聚焦独立验收）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`04-requirements.md`（REQ-MAT-016）、`05-plan.md`、`06-implementation.md`、`07-ui-review.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 B7 证据：`evidence/B7/README.md`、`summary.json`、`verification-results.json`、`ui-metrics.json`、`ui-style-metrics-*.json`、`screenshots/`；
5. 涉及业务事实时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与 BR-045 / OBJ-008 / SCN-021 / DEC-072 / DEC-073。

当前状态：B7 材料业务助手上下文入口已 `RECONCILED`，开发自测 48/48 PASS；B8 为独立验收。以 `state.json` 为准。

## 独立性要求

- 使用独立验收角色/任务，不得由 B7 实现者自批；
- 只做黑盒 API、真实浏览器/开发者工具验证，不修改业务实现；
- 最终是否 `ACCEPTED` 由用户决定；独立验收方只给 PASS/FAIL 与证据；
- 发现缺陷只登记，不在本批修复；下一批修复后重新独立复验。

## 必验范围

### 1. 上下文入口

- Web：`/finance/projects/:projectId` 项目上下文、`/finance/material-ordering` 采购上下文、`/finance/material-settlement` 结算上下文；
- App：项目详情、作业单详情（真实 `sheetId`）、主材结算；
- 入口可见性符合角色权限；Designer/PM 不得因助手入口获得成本/供应商价格可见性；
- 清除上下文后明确显示“未绑定业务单据，仅解释材料规则”；恢复上下文后重新绑定当前真实对象；
- 上下文 ID 只能来自页面真实对象，不能手填或信任前端租户/员工字段。

### 2. 回答与边界

- 结论、证据、只读工具、下一步、建议追问、审计状态均可见；
- `NO_EVIDENCE`、`REFUSAL`、`PERMISSION_DENIED` 原样展示，不得以空数组/0/成功态/兜底话术伪装；
- 助手无审批/付款/删除/改权限按钮；请求不得产生任何写操作；
- 单轮问答：刷新/再次提问不保留上一轮回答作为多轮记忆；
- `material/assistant/ask` 权限、项目/作业单跨租户负向、空白/超长问题、审计幂等；
- `material/assistant/suggestions` 只来自同一黄金问题源，未知上下文回落规则正确。

### 3. UI 质量

- Web：1440×900、1024×768；App H5：390×844；微信开发者工具：375×667；
- 覆盖加载、空态、错误/重试、长回答、长证据、无权限、无证据、拒答、清除/恢复上下文、建议问题点击；
- 无横向溢出、Console Errors 0；Web Esc/焦点管理；App 底部安全区与键盘遮挡；
- 截图、DOM 尺寸、关键文案与 computed style 落 `evidence/B8/`。

### 4. 回归

- B4 50 个黄金问题仍 PASS；
- B6 六项缺陷回归、B62-DEF-001、B64-UI-WEB-SHEET-01 不回归；
- 未定价/暂估应付/成本不完整利润 null/未到场结算门禁不被助手入口绕过；
- 旧直采路线仍不可达。

## 退出条件

- 独立结论 **PASS** 或登记新缺陷；
- 证据落 `evidence/B8/`，更新 `08-acceptance.md`、`10-reconciliation.md`、`state.json/history`；
- PASS 后记录 `ACCEPTED` 证据条件，但最终正式接受/发布仍由用户确认；
- 不执行正式小程序提审/发布；不修改 B7 实现。
