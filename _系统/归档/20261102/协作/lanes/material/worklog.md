# Material Worklog

## Current

- 2026-09-15：完成 `REQ-20260915-MATERIAL-INVENTORY`，盘点装得清上线宣传可用的产品知识、业务场景、数据证据、视觉素材和主要缺口。
- 任务阶段：已完成内部盘点与索引，等待产品、技术、法务/负责人确认对外边界。
- 未生成正式选题、脚本、封面或视频。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 装得清上线前素材盘点 | `docs/marketing/material/装得清上线前素材盘点.md` | topic-selection、content-writing、design、产品负责人 | 已完成内部盘点，待对外审核 |
| 素材索引 | `docs/marketing/material/素材索引.md` | topic-selection、content-writing、design、销售/演示维护人 | 已完成，持续维护 |

## Requests

| to | request | status | link |
|---|---|---|---|
| 用户/产品负责人 | 确认首发客群、渠道、版本、官方产品一句话和公开表达边界 | pending | `docs/marketing/material/装得清上线前素材盘点.md` §8、§9 |
| 产品/技术负责人 | 确认微信登录、AppID、业主自助链、支付/续费、真实全链路联调状态 | pending | `docs/marketing/material/素材索引.md` §7 |
| 用户/法务负责人 | 确认客户样例、截图、PPT、工地照片、视频、字体、音乐和品牌素材授权 | pending | `docs/marketing/material/装得清上线前素材盘点.md` §8.1 |
| 市场/数据负责人 | 补齐行业数字的原始 URL、报告标题、年份、样本、统计口径和计算方法 | pending | `docs/marketing/material/装得清上线前素材盘点.md` §5.2 |

## Notes

- 职责：负责收集、整理和登记产品推广素材与业务知识，覆盖业务设计、业务场景、文本和图片资料，重点沉淀短视频及图片素材。
- 主动写入范围：`docs/marketing/material/**`、`_系统/协作/lanes/material/**`。
- 跨出写入范围前，必须先登记共享请求或取得用户明确授权。
- 本轮盘点使用 `F 文件事实`、`P 产品描述`、`D 演示证据`、`I 分析推断`、`U 待验证` 五级标记，避免把规划、演示和数据判断混成对外事实。
- 当前可支撑“产品是什么、如何工作、界面长什么样”的内部内容；尚缺真实客户效果、授权案例、原始行业数据和产品演示视频。
- 已有 `data/media` 约 3.7 GB 泛家装短视频素材，但不是装得清产品宣传成片，不能据此证明产品效果。
- 当前存在页面数量、微信登录、AppID、项目权限和联调状态的版本差异，已记录为待确认项。

## Verification

- 已读取产品白皮书、整体业务设计、领域设计、小程序文档、迭代规划和现有视觉/演示素材。
- 静态扫描确认 176 条唯一 `@Service` 路由。
- 统计 `com.th4.edge.decoboon` 472 个 Java 文件、`tenant` 147 个 Java 文件、`src/test` 20 个测试文件。
- 解析小程序 `pages.json`：10 个主包页面、50 个分包页面，共 60 个注册路由。
- 盘点 14 个 Logo 文件、9 张设计转化截图、2 份谈单 PPT、1 份报价预览和 1 份联调导入结果。
- 盘点 `data/media`：约 3.7 GB、1,199 个文件、30 个非空 `final.mp4`、12 张工地 JPG。
- 未执行第三方数据原文核验、真实客户授权审查、当前版本真机测试和视频专业事实复核。

## Risks

- 白皮书和市场评估中的行业数字缺少可复核原始链接，直接对外有真实性风险。
- 联调客户、报价、小区、截图和 PPT 不能包装成真实客户案例。
- 现有短视频夹有生成过程文本和未经专业审查的施工因果表达。
- 9 月 14 日上线报告与当前代码状态不一致，产品发布范围需要重新确认。
- `pack/quote` 当前静态扫描仍未发现 `extends Security`，对外不得使用泛化安全承诺。
- 价格方案仍要求至少 5 家种子客户访谈，不能作为正式价格公告。

## Next

- 等待用户确认首发目标、渠道、版本和素材授权。
- 收到确认后建立“可对外素材台账和脱敏清单”，再交由 topic-selection 使用。
- 本 lane 不自行启动选题、脚本或视觉制作。

## Shared Outputs 待登记

以下行已准备好，但 `_系统/协作/shared.md` 不属于 material lane 写入范围，未主动修改：

```text
| material | 装得清上线前素材盘点 | docs/marketing/material/装得清上线前素材盘点.md | topic-selection / content-writing / design / user | internal_review_ready | 2026-09-15 |
| material | 素材索引 | docs/marketing/material/素材索引.md | topic-selection / content-writing / design / user | maintained | 2026-09-15 |
```

## Disabled

- 2026-09-15：用户明确禁用 material Lane。
- 当前会话绑定已释放，对应 Codex 任务已归档。
- 历史产出和 worklog 保留；未经用户明确恢复不得接收新任务。
