# D-003 · 预览水印缺少固定前缀

> 后续状态：`FIX_VERIFIED_PENDING_REACCEPTANCE`，证据见 `../fix-verification/README.md`。

- 级别：P1
- 页面：`subpackages/project/pages/owner-preview`
- 复现：Boss 打开专用测试项目的业主视角预览，读取页面水印文本。
- 期望：`业主视角预览 · 仅内部使用 · 员工姓名/工号/时间`
- 实际：`验收负责人 · 工号1902102100000000004 · 2026-10-21 03:04`
- 证据：
  - `app/08-watermark-check.json`（`hasFixedPrefix=false`）
  - `app/screenshots/watermark-missing-fixed-prefix.png`
- 根因线索：
  - 后端 `Info.java:114-116` 只拼员工姓名/工号/时间。
  - 前端预览页用该值覆盖了 `OwnerPreviewWatermark` 的默认固定前缀。
- 影响：截图无法直接表明“业主视角预览/仅内部使用”，不满足已确认水印口径。
