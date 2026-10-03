# AI 用量与经营参谋联合验收交接

> 日期：2026-09-26  
> 状态：`submitted_for_independent_acceptance`
> 范围：A0.1 AI 调用与影子用量底账、A1 AI 经营参谋 P0 基线版

## 1. 代码基线

| 仓库 | 分支/提交 | 内容 |
| --- | --- | --- |
| th4-decoboon | 远端 `dev`，集成分支 `cba829f` | A0.1 + A1 后端 |
| th4-decoboon | `feature/ai-usage-shadow-metering`，`9ed122c` | A0.1 独立后端分支 |
| th4-decoboon | `feature/ai-boss-brief`，`2218ce9` | A1 独立后端分支 |
| th4-decoboon-web | 远端 `dev`，`34ed97c` | 正式 Web AI 经营助手 |

## 2. 后端验收范围

- `ai/usage/report`：企业、门店、企业公共、能力和趋势统计。
- `ai/usage/page`：调用明细分页，不返回企业、幂等键、请求 ID 和平台成本。
- `ai/brief/report`：BOSS 经营简报、业务/交付/资金事实、最多三条行动建议、成本状态和企业公共影子用量。
- 门店归属：业务门店 → 执行门店 → 企业公共。
- 幂等键：同企业同键只形成一条记录。
- 成本不完整时利润和毛利保持为空。

## 3. Web 验收范围

- `/ai/center`：BOSS 专属 AI 经营助手。
- 经营简报标签：生成、事实、行动下钻、来源抽屉、成本不完整态。
- 资源用量标签：企业指标、门店/企业公共、能力分布、调用明细和分页。
- 页面不展示平台成本、提示词、客户正文。
- 非 BOSS 无 `ai` 权限，菜单和路由不可见。

## 4. 已执行验证

- 后端全量 `mvn -o -q test` PASS。
- Provider 安全测试 PASS，路由 348（public 12 + protected 336）。
- Web `npm run type-check` PASS。
- Web `npm run verify:system` PASS。
- Web `npm run build` PASS。
- 迁移 64 在生产 MySQL `SUCCESS`，结构备份与表结构已留存。
- H2 隔离联合启动：后端 8091、Web 8082；新接口无 token 返回 `tokenIsUndefined`，页面模块可加载。

## 5. Acceptance 必须重点验证

1. BOSS 使用真实令牌调用 `ai/brief/report`，确认事实、成本状态、用量。
2. `ai/usage/report` 企业总量 = 门店分摊 + 企业公共。
3. `ai/usage/page` 不泄露企业对象、幂等键、请求 ID、平台成本。
4. 非 BOSS 调三个 AI Provider 全部被拒绝。
5. 重复调用、失败、取消不会形成第二次有效影子点数。
6. BOSS 页面 `/ai/center` 可加载、筛选和分页，非 BOSS 无入口。
7. 迁移 64 目标环境结构与账本一致。

## 6. 当前边界

- A1 当前使用规则基线生成器，未接真实大模型。
- `shadow-direct-v1` 仅供链路验证，不可作为正式费率。
- T06 未完成前不得正式扣点、销售资源包或出具 AI 账单。
