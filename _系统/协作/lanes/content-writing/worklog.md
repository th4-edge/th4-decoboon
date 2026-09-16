# Content Writing Worklog

## Current

- 2026-09-15：完成 `REQ-20260915-WRITE-BATCH1` 首批内部内容草案。
- 上游输入：`docs/marketing/topics/装得清上线前选题规划.md` T01-T08、`docs/marketing/material/装得清上线前素材盘点.md`、`docs/marketing/material/素材索引.md`。
- 产出范围：每个选题包含内容大纲、短视频口播脚本、图文结构、建议 CTA、配套文案、素材依据、风险提示和设计交接要求。
- 状态：T01-T08 及整份脚本均标记为 `draft_for_review`，未进入审核通过或发布状态。
- 任务阶段：内部内容草案完成，等待产品、营销、素材授权和设计复核。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 装得清上线前首批内容脚本 | `docs/marketing/scripts/装得清上线前首批内容脚本.md` | 产品负责人、营销负责人、content-writing、design、material | draft_for_review |

## Requests

| to | request | status | link |
|---|---|---|---|
| design | 依据脚本 §9 设计制作总表和 T01-T08 各自的 Design Handoff，准备封面、插图、界面截图、录屏、字幕重点与画面禁用清单；接收后再进入制作。 | prepared_for_handoff_pending_acceptance | `docs/marketing/scripts/装得清上线前首批内容脚本.md` §9 |
| product / marketing | 审批产品一句话、能力边界、渠道、CTA 和 T01-T08 的逐篇文案；未审批前不得外发。 | pending_review | `docs/marketing/scripts/装得清上线前首批内容脚本.md` §0、§9.2 |
| material | 补齐并确认独立演示环境、当前版本截图/录屏、授权与脱敏台账；现有联调截图和 PPT 不能直接作为正式素材。 | pending_material_refresh | `docs/marketing/material/装得清上线前素材盘点.md` §8 |

## Notes

- 职责：负责依据已通过的选题生成内容大纲、短视频口播脚本和配套文案。
- 主动写入范围：`docs/marketing/scripts/**`、`_系统/协作/lanes/content-writing/**`。
- 本轮未修改选题规划、素材盘点、共享台账、设计文件或生产代码。
- 选题规划中的 T01-T08 原状态为 `draft`，topic-selection worklog 原定“审核后才交 content-writing”；本次按用户直接指令先行生成内部草案，状态统一为 `draft_for_review`。
- 所有产品描述按 `P` 级使用；文件、路由和设计说明按 `F` 级使用；537 条项目库、118㎡、两套方案、七区域、每套 74 个分项和报价金额均按 `D` 级演示证据处理。
- CTA 均保留“入口待确认/演示条件待确认”提示，不视为已配置或已上线。
- T08 明确保留发票、退款、对账、真实支付渠道和用量账本等待确认边界。
- 交接必须包含可核验的文件或素材、验证结果、风险和下一步。

## Verification

- 脚本共覆盖 T01-T08 八个选题，每个选题均包含：内容大纲、短视频口播脚本、图文结构、配套文案、建议 CTA、素材依据、风险提示和 Design Handoff。
- 脚本文件共扫描到 8 个 T01-T08 章节；每个章节均包含 `draft_for_review` 状态。
- 设计交接总表覆盖封面、插图/关系图、界面截图、录屏、字幕重点和画面禁区。
- 禁用词检查未发现：`最强`、`第一名`、`银行级安全`、`绝对不泄露`、`绝对安全`、`ROI`、`利润提升`、`返工下降`。
- 对 T04 明确标注 537 条项目库为联调数据，不是行业标准库。
- 对 T05 的 118㎡、两套方案、七区域、每套 74 个分项及两版报价明确标注为演示/联调数据。
- 对 T08 明确标注发票、退款、对账、真实支付渠道和用量账本等待确认边界。
- 未进行产品事实签字、营销审批、素材授权复核、真机录屏、图片脱敏或设计制作。

## Risks

- T01 的产品一句话和 Logo 副标题尚未统一，当前脚本中的定位仅可作为内部草案。
- T02、T04、T05 依赖产品设计和联调样例，不能替代真实客户案例或当前版本全链路验收。
- T06、T07 缺少真实项目过程和跟进数据，不得补充虚构结果指标。
- T07、T08 涉及个人信息和财务数据，正式制作前必须完成授权、脱敏与权限核对。
- T08 的支付、退款、开票、对账和用量账本尚未闭合，不能对外写成完整能力。
- 当前没有独立演示环境和当前版本录屏，设计目前只能先做结构稿、线框稿和素材清单。

## Handoff To Design

- 输入文件：`docs/marketing/scripts/装得清上线前首批内容脚本.md`。
- 逐篇入口：T01-T08 各自的 `Design Handoff`。
- 汇总入口：§9 `T01-T08 设计制作总表`。
- 统一样式要求：品牌主色、Logo SVG/高分辨率 PNG、演示数据角标、统一关系图语言、字体/音乐/图片授权台账。
- 阻断项：产品一句话未确认、CTA 未配置、当前版本录屏缺失、截图未脱敏、财务边界未确认时，不得把草案升级为正式发布素材。

## Shared Outputs 待登记

以下行已准备好，但 `_系统/协作/shared.md` 不属于 content-writing lane 写入范围，未主动修改：

```text
| content-writing | 装得清上线前首批内容脚本 | docs/marketing/scripts/装得清上线前首批内容脚本.md | product-planning / topic-selection / material / design / user | draft_for_review | 2026-09-15 |
```

## Acceptance Needed

- 产品负责人：确认 T01 官方定位，以及 T02-T08 的产品能力、版本范围和表达边界。
- 营销负责人：确认首发渠道、标题、CTA、发布时间和品牌语言。
- material / 授权责任人：确认演示环境、截图、录屏、PPT、字体、音乐和工地素材的授权与脱敏。
- design lane：确认接手脚本与设计交接清单；接手前只算已准备交接，不算设计制作完成。
- 技术与财务责任人：确认 T08 中支付、退款、开票、对账、结算和用量账本的能力边界。

## Next

1. 等待产品、营销和必要授权方 review T01-T08。
2. 等待 design lane 确认接收脚本和设计交接清单。
3. 等待 material lane 提供可重置演示环境、当前版本截图/录屏和授权脱敏台账。
4. 审核意见返回后，在 content-writing lane 内按意见修订脚本，不直接修改其他 lane 文件。

## Disabled

- 2026-09-15：用户明确禁用 content-writing Lane。
- 当前会话绑定已释放，对应 Codex 任务已归档。
- 历史产出和 worklog 保留；未经用户明确恢复不得接收新任务。
