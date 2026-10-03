# AI 经营参谋 P0 基线与 Web Handoff

## Status

`implementation_and_self_test_done_pending_joint_test_and_acceptance`

## Branch

- Backend: `feature/ai-boss-brief`（基于 A0.1 检查点 `9ed122c`）
- Web: `feature/ai-boss-brief-web`（基于 AI 用量页面检查点 `7257482`）

## Backend Scope

- 新增 `ai/brief/report` 受保护 Provider，仅 BOSS 可读。
- 复用 `BossDashboardContext`、`FinanceReportContext`、`BusinessAlertContext`。
- 输出一句话摘要、业务/交付/资金事实、最多三条行动建议、成本完整性和影子用量。
- 新增可替换 `AiBriefGenerator` 接口，首版提供 `BaselineAiBriefGenerator` 规则基线。
- 经营简报调用 `AiUsageContext` 记录影子调用，并显式归入企业公共。
- 不自动修改金额、项目、风险和业务状态。
- 不生成净利润、未来收入、员工责任或无依据因果。

## Web Scope

- 在企业负责人经营总览加入 AI 经营简报卡。
- 首版采用手动生成，不影响原有手工查看。
- 展示业务、交付、资金三类事实和最多三条行动建议。
- 提供“查看依据”抽屉，展示来源事实、生成器版本、资源单位和影子点数。
- 成本不完整时显示“成本暂不可用”，不展示伪毛利。
- 行动按钮仅跳转已有经营页面，不执行自动写操作。

## Verification

- Backend `mvn -o -q -DskipTests compile`：PASS
- `AiBossBriefContextTest`：PASS
- `AiBriefSerializationContractTest`：PASS
- `AiUsageContextTest`、`AiUsageReportContextTest`：PASS
- `ProviderSecurityAcceptanceTest`：PASS，路由总数调整为 344，其中 protected 332。
- Web `npm run type-check`：PASS
- Web `npm run build`：PASS

## Risks

- 当前摘要和事实由规则基线生成，真实大模型尚未接入。
- T06 尚未冻结 AI 生成内容责任、缓存和正式 UsageLedger 状态。
- 当前没有真实 MySQL 联调环境。
- 企业公共归属固定用于经营简报，符合企业级统计口径。
- 规则基线只描述确定性数字，不输出业务因果。

## Next Action

1. 联合 A0.1 与 A1 分支在可用 MySQL/H2 测试环境启动。
2. 验证 `ai/brief/report` 返回和 `AiUsageContext` 写入。
3. 验证经营总览卡、来源抽屉和成本不完整态。
4. 交 acceptance lane 独立验收。
5. 接入真实大模型时只替换 `AiBriefGenerator`，不改变事实聚合、权限、页面和用量链路。
