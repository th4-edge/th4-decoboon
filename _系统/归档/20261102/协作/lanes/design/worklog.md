# Design Worklog

## Current

已完成 `REQ-20260915-DESIGN-BATCH1`：依据 content-writing 的 T01-T08 脚本、material lane 素材盘点和素材索引，制作首批内部设计草案。

产物状态统一为 `internal_draft`，不进入发布或 `ready_for_publish`。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 装得清上线前首批视觉方案 | `docs/marketing/design/装得清上线前首批视觉方案.md` | user,content-writing,material,marketing | internal_draft |
| T01-T08 封面草案 | `docs/marketing/design/covers/T01-cover-draft.png` 至 `T08-cover-draft.png` | user,content-writing,material,marketing | internal_draft |
| T01-T08 封面总览 | `docs/marketing/design/covers/T01-T08-cover-contact-sheet.png` | user,content-writing,material,marketing | internal_draft |
| 封面生成脚本 | `docs/marketing/design/covers/generate_covers.py` | design | internal_draft |

## Requests

| to | request | status | link |
|---|---|---|---|
| material | 补充 DG-01 至 DG-08：官方用语、Logo 规范、独立演示环境、当前版本截图/录屏、素材授权和 CTA。 | pending | `docs/marketing/design/装得清上线前首批视觉方案.md` §6 |
| user,product-planning,marketing | 确认产品定位、上线范围、CTA、首发渠道和 T08 财务边界。 | pending | `docs/marketing/design/装得清上线前首批视觉方案.md` §8 |
| shared owner | 将本批设计产物登记到跨 lane Shared Outputs。 | pending | `_系统/协作/shared.md` |

## Notes

- 职责：负责依据已确认的脚本和素材制作封面、插图及视频，不擅自改变选题和脚本核心内容。
- 主动写入范围：`docs/marketing/design/**`、`_系统/协作/lanes/design/**`。
- 本次只制作封面构图和分镜，不制作最终视频。
- 封面使用官方 Logo、品牌色和几何关系图，不使用 AI 生成照片、外部图片、外部字体或不明版权素材。
- 所有演示数据、联调样例和能力边界均保留文字标记。
- 系统字体只用于内部草案；正式发布前必须完成商用授权。

## Verification

- 封面：8 张独立封面 + 1 张总览联系图，均为 `1080 × 1920px`。
- 分镜：60 个镜头，T01-T08 全覆盖；每选题 7-8 个镜头。
- 已核对 T01-T08 的标题、主视觉、风险提示、证据边界和 Design Handoff。
- 已核对品牌主色 `#16A394`、品牌渐变和 `th4-decoboon-app/logo-export/` 官方 Logo。
- 未完成正式素材替换、产品审核、营销审核和素材授权复核。

## Next

1. 等待 material、product 和 marketing 补齐并确认缺口。
2. 收到独立演示环境和当前版本截图后，替换封面结构示意并重录录屏。
3. 完成正式替换后提交产品、营销和素材授权审核。

## Disabled

- 2026-09-15：用户明确禁用 design Lane。
- 当前会话绑定已释放，对应 Codex 任务已归档。
- 历史封面、分镜和视觉方案保留；未经用户明确恢复不得接收新任务。
