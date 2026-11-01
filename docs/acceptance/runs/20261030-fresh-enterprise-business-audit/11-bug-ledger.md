# BUG 缺陷台账

| ID | 严重度 | 状态 | 端面 | 角色 | 最小复现 | 期望 | 实际 | 证据 | 责任建议 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DEF-FRESH-001 | P0 | FIXED_PENDING_ACCEPTANCE | Web + App + API | Boss/Finance/StoreManager | 打开财务汇总/分页或 App 店长首页 | 合法 JSON 与真实指标 | 关联 `Project.property` 处截断，页面失败或伪 0 | EV-012~015 | 后端 Provider 白名单；已补静态契约 |
| DEF-FRESH-002 | P1 | FIXED_PENDING_ACCEPTANCE | App + Web | Finance/StoreManager | App 项目已付 ¥500、应付 ¥2,000；Web 需关注应收 4 笔 | 显示付款进度与需关注笔数 | 显示“成本 25%”“应收与逾期 4” | EV-002/004 | 产品文案；已纠正标签 |
| DEF-FRESH-003 | P1 | BUSINESS_DECISION_PENDING | Web | StoreManager | 打开资金台账，查看手工流水操作列 | 按 FIN-Q07 冻结动作主体和安全门 | 当前可见“更正/删除” | EV-009/017 | 业务/产品/权限；关联 PI-20261022-016 |
| DEF-FRESH-004 | P2 | FIXED_FIXTURE | Web + App | Boss/StoreManager | 客户卡与项目/房屋面积对照 | 同一房屋面积一致 | 112㎡ vs 100㎡ | EV-008/011 | 审计脚本；已修 |

## 状态说明

- `FIXED_PENDING_ACCEPTANCE`：本地开发已修复并有真实页面/定向回归证据，未独立验收、未发布。
- `BUSINESS_DECISION_PENDING`：业务规则未冻结，不通过页面按钮执行高危动作。
