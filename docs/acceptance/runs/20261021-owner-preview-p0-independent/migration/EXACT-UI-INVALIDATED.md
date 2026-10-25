# “UI 完全一致”旧结论失效说明

- 日期：2026-10-21
- 失效对象：`EXACT-UI-DECISION.md`、`exact-ui-app-results.json`、`55-exact-ui-owner-page-in-preview.png`
- 状态：**SUPERSEDED / 不得作为四 Tab UI 一致性证据**

## 原因

旧实现让员工预览直接跳转到：

`subpackages/project/pages/journey?preview=1`

`journey.vue` 只是业主首页中“查看我的家全貌”打开的二级旅程页，不是业主端主导航。它无法证明员工看到了真实业主四 Tab：

`我的家 / 进度 / 动态 / 我的`

因此旧“页面结构与业主端 UI 完全一致 PASS”结论无效。原文件保留用于追溯，不删除历史证据。

## 替代证据

新实现与独立复验见：

`docs/acceptance/runs/20261021-owner-preview-four-tabs-independent/README.md`
