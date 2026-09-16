# Data Review Worklog

## Current

- 2026-09-15：接收 `REQ-20260915-ANALYTICS-FRAMEWORK`，建立「装得清」上线前宣传内容的周/月数据复盘框架。
- 框架状态：`framework_ready_data_pending`；真实平台数据状态：`baseline_pending`。
- 当前没有真实平台数据、冻结基线或经确认的效果结论，未编造数字、趋势、渠道排名或归因结论。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 装得清上线前宣传数据复盘框架 | `docs/marketing/analytics/上线前宣传数据复盘框架.md` | topic-selection, material, content-writing, design, product, marketing, user | framework_ready_data_pending |

## Requests

| to | request | status | link |
|---|---|---|---|
| topic-selection | 将本框架作为数据口径和回流约定评审；当前只反馈数据缺口与实验问题，不生成效果排名。 | pending_acceptance | `docs/marketing/analytics/上线前宣传数据复盘框架.md` |
| user / marketing | 确认渠道导出字段、业务异常阈值、采集责任人、受众分段和渠道优先级。 | pending | `docs/marketing/analytics/上线前宣传数据复盘框架.md` §3.5、§4.3 |
| product / CRM / order | 确认有效留资、演示预约、演示到场、试用激活、转化事件及关联键。 | pending | `docs/marketing/analytics/上线前宣传数据复盘框架.md` §2.2、§3.5 |
| material | 提供数据授权、脱敏、素材版本和撤回机制，避免将联调或未授权素材纳入复盘事实。 | pending | `docs/marketing/analytics/上线前宣传数据复盘框架.md` §5.6 |

## Notes

- 输出覆盖：内容/渠道/受众/发布时间/产品版本维度、渠道指标口径、周/月模板、数据来源与采集时间、基线/对比/异常判定、L0-L6 归因、数据质量、复盘结论和跨 lane 回流。
- 选题、视觉方案和素材盘点仍为 `draft`、`internal_draft` 或内部审核状态；框架没有把这些文档中的目标值、联调值、行业数字或素材数量当作有效效果数据。
- 当前所有真实指标单元格保持空值、`—` 或 `null`，统一状态为 `baseline_pending`；缺失不等于 0。
- 渠道运营、投放和营销转化职责位未绑定，相关责任人在框架中保留为 `owner_pending`，未虚构个人或团队。
- 重大风险：追踪链接、UTM/短链、CRM 线索 ID、试用事件、转化事件和业务阈值尚未确认，无法进入正式周/月比较。

## Verification

- 已读取指定的 `topic-selection` 选题规划、`design` 视觉方案、`material` 素材盘点与素材索引。
- 已核对框架包含短视频、图文、直播/演示、官网/私域，以及从曝光到转化的完整漏斗。
- 已核对周报、月报、原始事实表、采集时间、来源和责任人角色均已定义。
- 已核对基线、对比、异常、归因、数据质量、结论模板和 topic-selection / material / content-writing / design / 产品团队回流路径。
- 已检查输出仅新增在 `docs/marketing/analytics/**`，本 worklog 仅修改在 `_系统/协作/lanes/data-review/**`。

## Risks

- `baseline_pending` 期间只能记录事实或数据缺口，不能输出内容效果、渠道优劣或 ROI 结论。
- 渠道平台对播放、完播、阅读、主页访问和咨询的定义可能变化，需通过 `metric_version` 管理。
- 线索和转化数据涉及隐私与授权，未完成脱敏和权限确认前不能进入周/月报明细。

## Next

1. 等待用户或营销负责人评审本框架并确认责任人、阈值、渠道和追踪方案。
2. 由产品、订单和 CRM 确认试用与转化事件口径。
3. 首批真实数据达到 `qualified` 后冻结 `baseline_version`，再生成首份周报和月报。
4. 在 `baseline_pending` 解除前，不执行效果归因、渠道排名或选题效果结论。

## Disabled

- 2026-09-15：用户明确禁用 data-review Lane。
- 当前会话绑定已释放，对应 Codex 任务已归档。
- 历史产出和 worklog 保留；未经用户明确恢复不得接收新任务。
